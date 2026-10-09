package steps;

import io.qameta.allure.Step;
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
}