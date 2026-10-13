package tests;

import com.microsoft.playwright.Download;
import io.qameta.allure.*;
import models.E2eOrderDto;
import models.TestDataUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import steps.AssertSteps;
import steps.DbSteps;
import steps.KafkaSteps;
import utils.JsonReader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Epic("Сквозное тестирование (Data-Driven)")
@Feature("Оформление заказа по пользователям из JSON")
@DisplayName("E2E: Параметризованные сценарии покупки")
public class DataDrivenE2eTest extends BaseTest {

    private final KafkaSteps kafka = new KafkaSteps();
    private final AssertSteps verify = new AssertSteps();
    private final DbSteps db = new DbSteps();
    private final String kafkaTopic = "ddt-orders-topic";

    // Метод-поставщик данных для JUnit 5 из нашего JsonReader
    static List<TestDataUser> userProvider() {
        return JsonReader.getUsersData();
    }

    @ParameterizedTest(name = "Прогон E2E для пользователя: {0}")
    @MethodSource("userProvider")
    @Story("Покупка товара под различными учетными записями из внешнего файла")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("DDT: Полный цикл покупки рюкзака через UI, DB и Kafka")
    public void testPurchaseWithDifferentUsers(TestDataUser testUser) throws Exception {
        String username = testUser.getUsername();
        String firstName = testUser.getFirstName();
        String lastName = testUser.getLastName();
        String zipCode = testUser.getZipCode();

        String itemName = "Sauce Labs Backpack";
        String itemPrice = "$29.99";

        // ДОБАВЛЯЕМ СЮДА: Сбрасываем старую дефолтную сессию standard_user, чтобы вернуться на форму логина
        Allure.step("Очистить куки и вернуться на страницу авторизации", () -> {
            String currentBaseUrl = page.url();
            // Очищаем контекст браузера (стирает куки и сессию стандартного юзера из BaseTest)
            page.context().clearCookies();
            // Принудительно переходим на главный URL формы логина
            page.navigate(currentBaseUrl);
        });

        // --- БЛОК 1: ДИНАМИЧЕСКАЯ АВТОРИЗАЦИЯ НА UI ---
        Allure.step("Авторизоваться на сайте под пользователем: " + username, () -> {
            // Вытаскиваем пароль СТРОГО из системы без дефолтных текстовых строк в коде
            String systemPassword = System.getProperty("test.password");

            if (systemPassword == null || systemPassword.isBlank()) {
                throw new IllegalStateException("КРИТИЧЕСКАЯ ОШИБКА: Пароль сайта 'test.password' не передан в систему!");
            }

            page.fill("[data-test='username']", username);
            page.fill("[data-test='password']", systemPassword);
            page.click("[data-test='login-button']");

            page.waitForURL("**/inventory.html");
        });

        // --- БЛОК 2: UI АВТОМАТИЗАЦИЯ (Playwright) ---
        Allure.step("Добавить рюкзак в корзину и перейти к оформлению", () -> {
            page.click("[data-test='add-to-cart-sauce-labs-backpack']");
            page.click("[data-test='shopping-cart-link']");
            page.click("[data-test='checkout']");
        });

        Allure.step("Заполнить информацию о покупателе данными из JSON-файла", () -> {
            page.fill("[data-test='firstName']", firstName);
            page.fill("[data-test='lastName']", lastName);
            page.fill("[data-test='postalCode']", zipCode);
            page.click("[data-test='continue']");
        });

        Allure.step("Проверить детали заказа на финальном UI экране перед подтверждением", () -> {
            Assertions.assertTrue(page.locator(".inventory_item_name").textContent().contains(itemName));
            Assertions.assertTrue(page.locator(".summary_subtotal_label").textContent().contains(itemPrice));
        });

        Allure.step("Завершить оформление заказа кнопкой Finish", () -> {
            page.click("[data-test='finish']");
            Assertions.assertTrue(page.locator(".complete-header").textContent().contains("Thank you for your order!"));
        });

        // --- БЛОК 3: РЕАЛЬНОЕ СКАЧИВАНИЕ PDF ---
        Path targetPdfPath = Paths.get("target/invoice-ddt-" + UUID.randomUUID().toString().substring(0, 5) + ".pdf");

        Allure.step("Нажать кнопку 'generate pdf order' и перехватить загрузку файла", () -> {
            Download download = page.waitForDownload(() -> {
                page.click("#generate-pdf-order");
            });
            download.saveAs(targetPdfPath);
            Assertions.assertTrue(Files.exists(targetPdfPath), "Ошибка: PDF-файл не сохранен на диск!");
            try (InputStream is = Files.newInputStream(targetPdfPath)) {
                Allure.addAttachment("Скачанный PDF-инвойс для " + username, "application/pdf", is, ".pdf");
            }
        });

        String mockGeneratedOrderId = "ORDER-DDT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // --- БЛОК 4: ИНТЕГРАЦИЯ С БАЗОЙ ДАННЫХ (PostgreSQL) ---
        db.saveOrderToDb(mockGeneratedOrderId, firstName, itemName, itemPrice, "CREATED");

        Allure.step("Валидация БД: Проверить SQL-запросом наличие заказа для " + username, () -> {
            String actualStatusInDb = db.getOrderStatus(mockGeneratedOrderId);
            Assertions.assertEquals("CREATED", actualStatusInDb, "Ошибка: Статус заказа в базе данных PostgreSQL не совпадает!");
        });

        // --- БЛОК 5: РАБОТА С ОЧЕРЕДЬЮ (Kafka) ---
        E2eOrderDto expectedEvent = new E2eOrderDto(
                username,
                mockGeneratedOrderId,
                itemName,
                itemPrice,
                "CREATED"
        );

        kafka.sendE2eOrderEvent(kafkaTopic, expectedEvent);
        E2eOrderDto actualEvent = kafka.waitForLatestE2eOrder(kafkaTopic);

        // --- БЛОК 6: ФИНАЛЬНАЯ ВАЛИДАЦИЯ СОБЫТИЙ ---
        verify.assertE2eOrderDetails(expectedEvent, actualEvent);
    }
}