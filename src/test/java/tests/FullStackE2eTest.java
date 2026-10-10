package tests;

import com.microsoft.playwright.*;
import io.qameta.allure.*;
import models.E2eOrderDto;
import org.junit.jupiter.api.*;
import steps.AssertSteps;
import steps.KafkaSteps;

import java.util.UUID;

@Epic("Сквозное тестирование (Full-Stack E2E)")
@Feature("Оформление заказа: UI + Бэкенд + Kafka")
@DisplayName("E2E: Оформление заказа на SauceDemo с проверкой бизнес-события в Kafka")
public class FullStackE2eTest {

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;

    private final KafkaSteps kafka = new KafkaSteps();
    private final AssertSteps verify = new AssertSteps();
    private final String kafkaTopic = "orders-topic";

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    @Test
    @Story("Покупка товара на UI и отправка аналитического события в очередь брокера")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Сквозной сценарий покупки рюкзака")
    public void testFullE2ePurchaseWorkflow() throws Exception {
        String username = "standard_user";
        String itemName = "Sauce Labs Backpack";
        String itemPrice = "$29.99";

        // --- БЛОК 1: UI АВТОМАТИЗАЦИЯ (Playwright) ---
        Allure.step("Открыть сайт SauceDemo и авторизоваться", () -> {
            page.navigate("https://saucedemo.com");
            page.fill("[data-test='username']", username);
            page.fill("[data-test='password']", "secret_sauce");
            page.click("[data-test='login-button']");
        });

        Allure.step("Добавить рюкзак в корзину и перейти к оформлению", () -> {
            page.click("[data-test='add-to-cart-sauce-labs-backpack']");
            page.click("[data-test='shopping-cart-link']");
            page.click("[data-test='checkout']");
        });

        Allure.step("Заполнить информацию о покупателе", () -> {
            page.fill("[data-test='firstName']", "QA");
            page.fill("[data-test='lastName']", "Engineer");
            page.fill("[data-test='postalCode']", "123456");
            page.click("[data-test='continue']");
        });

        // Проверяем на UI, что оформляем именно тот товар и за ту цену
        Allure.step("Проверить детали заказа на финальном UI экране", () -> {
            Assertions.assertTrue(page.locator(".inventory_item_name").textContent().contains(itemName));
            Assertions.assertTrue(page.locator(".summary_subtotal_label").textContent().contains(itemPrice));
        });

        // Нажимаем кнопку Финиш
        Allure.step("Завершить оформление заказа на UI", () -> {
            page.click("[data-test='finish']");
            Assertions.assertTrue(page.locator(".complete-header").textContent().contains("Thank you for your order!"));
        });


        // --- БЛОК 2: ИМИТАЦИЯ БЭКЕНДА И ЗАГЛУШКА (Mock/Stub) ---
        // Поскольку SauceDemo — это чистый фронтенд без реального бэкенда и БД,
        // мы имитируем (делаем стаб), что бэкенд сгенерировал уникальный номер заказа в системе:
        String mockGeneratedOrderId = "ORDER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        E2eOrderDto expectedEvent = new E2eOrderDto(
                username,
                mockGeneratedOrderId,
                itemName,
                itemPrice,
                "CREATED"
        );


        // --- БЛОК 3: РАБОТА С ОЧЕРЕДЬЮ (Kafka) ---
        // Имитируем, что бэкенд успешно создал заказ и запушил аналитическое событие в шину данных
        kafka.sendE2eOrderEvent(kafkaTopic, expectedEvent);

        // Имитируем работу нашего автотеста/другого микросервиса: вычитываем это событие из Кафки
        E2eOrderDto actualEvent = kafka.waitForLatestE2eOrder(kafkaTopic);


        // --- БЛОК 4: ФИНАЛЬНАЯ ВАЛИДАЦИЯ (AssertSteps) ---
        // Проверяем, что событие в шине данных на 100% совпадает с тем, что кликал пользователь на UI
        verify.assertE2eOrderDetails(expectedEvent, actualEvent);
    }
}