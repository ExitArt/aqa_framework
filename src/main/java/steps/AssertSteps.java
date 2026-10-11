package steps;

import io.qameta.allure.Step;
import models.E2eOrderDto;
import models.OrderDto;
import static org.junit.jupiter.api.Assertions.*;

public class AssertSteps {

    @Step("Проверить, что данные полученного из Kafka заказа полностью совпадают с ожидаемыми")
    public void assertOrderDetails(OrderDto expected, OrderDto actual) {
        assertNotNull(actual, "Ошибка: Объект заказа из Kafka равен null!");

        assertAll("Проверка полей заказа",
                () -> assertEquals(expected.getUser(), actual.getUser(), "Ошибка: Неверное имя пользователя (user)!"),
                () -> assertEquals(expected.getOrderId(), actual.getOrderId(), "Ошибка: Неверный ID заказа (orderId)!"),
                () -> assertEquals(expected.getStatus(), actual.getStatus(), "Ошибка: Неверный статус заказа (status)!")
        );
    }

    @io.qameta.allure.Step("Проверить, что событие в Kafka полностью соответствует оформленному на UI заказу")
    public void assertE2eOrderDetails(E2eOrderDto expected, E2eOrderDto actual) {
        assertNotNull(actual, "Ошибка: Событие из Kafka не получено!");
        assertAll("Валидация Full-Stack E2E Заказа",
                () -> assertEquals(expected.getUser(), actual.getUser(), "Пользователь не совпадает!"),
                () -> assertEquals(expected.getOrderId(), actual.getOrderId(), "ID заказа не совпадает!"),
                () -> assertEquals(expected.getItem(), actual.getItem(), "Товар в Kafka не совпадает с UI!"),
                () -> assertEquals(expected.getPrice(), actual.getPrice(), "Цена в Kafka не совпадает с UI!"),
                () -> assertEquals(expected.getStatus(), actual.getStatus(), "Статус заказа не совпадает!")
        );
    }
}