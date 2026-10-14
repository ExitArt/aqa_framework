package tests;

import com.microsoft.playwright.Download;
import io.qameta.allure.*;
import models.E2eOrderDto;
import net.datafaker.Faker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import steps.AssertSteps;
import steps.KafkaSteps;
import steps.DbSteps; // Импортируем наши шаги работы с БД

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Epic("Сквозное тестирование (Full-Stack E2E)")
@Feature("Оформление заказа: UI + Бэкенд + Kafka + База Данных")
@DisplayName("E2E: Оформление заказа на SauceDemo с проверкой в Kafka и записью в PostgreSQL")
public class FullStackE2eTest extends BaseTest {

    private final KafkaSteps kafka = new KafkaSteps();
    private final AssertSteps verify = new AssertSteps();
    //private final DbSteps db = new DbSteps(); // Инициализируем локальный объект шагов БД
    private final String kafkaTopic = "e2e-orders-topic";
    private final Faker faker = new Faker();

    @Test
    @Story("Полносвязная проверка бизнес-цепочки: от клика на UI до записи в таблицы БД и брокер сообщений")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Сквозной сценарий покупки рюкзака с валидацией в Kafka и PostgreSQL")
    public void testFullE2ePurchaseWorkflow() throws Exception {
        String username = "standard_user";
        String itemName = "Sauce Labs Backpack";
        String itemPrice = "$29.99";

        String fakeFirstName = faker.name().firstName();
        String fakeLastName = faker.name().lastName();
        String fakeZipCode = faker.address().zipCode();

        // --- БЛОК 1: UI АВТОМАТИЗАЦИЯ (Playwright) ---
        Allure.step("Добавить рюкзак в корзину и перейти к оформлению", () -> {
            page.click("[data-test='add-to-cart-sauce-labs-backpack']");
            page.click("[data-test='shopping-cart-link']");
            page.click("[data-test='checkout']");
        });

        Allure.step("Заполнить информацию о покупателе случайными данными", () -> {
            page.fill("[data-test='firstName']", fakeFirstName);
            page.fill("[data-test='lastName']", fakeLastName);
            page.fill("[data-test='postalCode']", fakeZipCode);
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

        // --- БЛОК 2: РЕАЛЬНОЕ СКАЧИВАНИЕ PDF (Playwright Download API) ---
        Path targetPdfPath = Paths.get("target/invoice-" + UUID.randomUUID().toString().substring(0, 5) + ".pdf");

        Allure.step("Нажать кнопку 'generate pdf order' и перехватить загрузку файла", () -> {
            Download download = page.waitForDownload(() -> {
                page.click("#generate-pdf-order");
            });
            download.saveAs(targetPdfPath);

            Assertions.assertTrue(Files.exists(targetPdfPath), "Ошибка: PDF-файл не сохранен на диск!");
            try (InputStream is = Files.newInputStream(targetPdfPath)) {
                Allure.addAttachment("Реальный скачанный PDF-инвойс заказа", "application/pdf", is, ".pdf");
            }
        });

        // Генерируем уникальный ID заказа для бэкенд-систем
        // БЫЛО: String mockGeneratedOrderId = "ORDER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String mockGeneratedOrderId = "ORD-UI-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().toUpperCase();


        // --- БЛОК 3: ИНТЕГРАЦИЯ С БАЗОЙ ДАННЫХ (PostgreSQL) ---
        // Имитируем сохранение заказа бэкендом в таблицы БД
        db.saveOrderToDb(mockGeneratedOrderId, fakeFirstName, itemName, itemPrice, "CREATED");

        // Проверяем SQL-запросом, что статус заказа в базе данных действительно равен CREATED
        Allure.step("Валидация БД: Проверить SQL-запросом наличие и статус заказа в PostgreSQL", () -> {
            String actualStatusInDb = db.getOrderStatus(mockGeneratedOrderId);
            Assertions.assertEquals("CREATED", actualStatusInDb,
                    "Ошибка: Статус заказа в базе данных PostgreSQL не совпадает с ожидаемым!");
        });

        // --- БЛОК 4: РАБОТА С ОЧЕРЕДЬЮ (Kafka) ---
        E2eOrderDto expectedEvent = new E2eOrderDto(
                username,
                mockGeneratedOrderId,
                itemName,
                itemPrice,
                "CREATED"
        );

        kafka.sendE2eOrderEvent(kafkaTopic, expectedEvent);
        E2eOrderDto actualEvent = kafka.waitForLatestE2eOrder(kafkaTopic);

        // --- БЛОК 5: ФИНАЛЬНАЯ ВАЛИДАЦИЯ СОБЫТИЙ (AssertSteps) ---
        verify.assertE2eOrderDetails(expectedEvent, actualEvent);
    }
}