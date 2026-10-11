package tests;

import io.qameta.allure.*;
import models.E2eOrderDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import steps.AssertSteps;
import steps.KafkaSteps;

import java.util.UUID;

@Epic("Сквозное тестирование (Full-Stack E2E)")
@Feature("Оформление заказа: UI + Бэкенд + Kafka")
@DisplayName("E2E: Оформление заказа на SauceDemo с проверкой бизнес-события в Kafka")
// ИСПРАВЛЕНО: Наследуемся от твоего единого BaseTest.
// Все аннотации @BeforeEach, @AfterEach и создание браузера теперь берутся из родителя!
public class FullStackE2eTest extends BaseTest {

    private final KafkaSteps kafka = new KafkaSteps();
    private final AssertSteps verify = new AssertSteps();
    private final String kafkaTopic = "e2e-orders-topic";

    @Test
    @Story("Покупка товара на UI и отправка аналитического события в очередь брокера")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Сквозной сценарий покупки рюкзака")
    public void testFullE2ePurchaseWorkflow() throws Exception {
        // Логин больше не нужен! Браузер уже находится на странице /inventory.html благодаря BaseTest. setUp()
        String username = "standard_user";
        String itemName = "Sauce Labs Backpack";
        String itemPrice = "$29.99";

        // --- БЛОК 1: UI АВТОМАТИЗАЦИЯ (Playwright) ---
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

        Allure.step("Проверить детали заказа на финальном UI экране", () -> {
            Assertions.assertTrue(page.locator(".inventory_item_name").textContent().contains(itemName));
            Assertions.assertTrue(page.locator(".summary_subtotal_label").textContent().contains(itemPrice));
        });

        Allure.step("Завершить оформление заказа на UI", () -> {
            page.click("[data-test='finish']");
            Assertions.assertTrue(page.locator(".complete-header").textContent().contains("Thank you for your order!"));
        });


        // --- БЛОК 2: ИМИТАЦИЯ БЭКЕНДА И ЗАГЛУШКА (Mock/Stub) ---
        String mockGeneratedOrderId = "ORDER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        E2eOrderDto expectedEvent = new E2eOrderDto(
                username,
                mockGeneratedOrderId,
                itemName,
                itemPrice,
                "CREATED"
        );


        // --- БЛОК 3: РАБОТА С ОЧЕРЕДЬЮ (Kafka) ---
        kafka.sendE2eOrderEvent(kafkaTopic, expectedEvent);
        E2eOrderDto actualEvent = kafka.waitForLatestE2eOrder(kafkaTopic);


        // --- БЛОК 4: ФИНАЛЬНАЯ ВАЛИДАЦИЯ (AssertSteps) ---
        verify.assertE2eOrderDetails(expectedEvent, actualEvent);
    }
}