package steps;

import io.qameta.allure.Step;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockSteps {

    // Локально и в GitHub Actions контейнер доступен по адресу localhost:8080
    private static final String WIREMOCK_HOST = "localhost";
    private static final int WIREMOCK_PORT = 8080;

    static {
        // Один раз при старте класса связываем статические методы Java-клиента с внешним Docker-контейнером
        configureFor(WIREMOCK_HOST, WIREMOCK_PORT);
    }

    @Step("WireMock [STUB]: Создать контракт для эндпоинта /api/v1/orders (Ответ 201 Created)")
    public void stubCreateOrder(String expectedOrderId, String itemName) {
        // Генерируем правило (Stub) и отправляем его по сети прямо в контейнер Докера
        stubFor(post(urlEqualTo("/api/v1/orders"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.item", equalTo(itemName)))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"orderId\": \"" + expectedOrderId + "\",\n" +
                                "  \"status\": \"CREATED\",\n" +
                                "  \"message\": \"Успешно обработано WireMock в Docker!\"\n" +
                                "}")));
    }

    @Step("WireMock [MOCK]: Проверить, что бэкенд получил ровно 1 POST-запрос с товаром '{0}'")
    public void verifyOrderWasSent(String itemName) {
        // Запрашиваем у контейнера историю вызовов (Mock-верификация)
        verify(exactly(1), postRequestedFor(urlEqualTo("/api/v1/orders"))
                .withRequestBody(matchingJsonPath("$.item", equalTo(itemName))));
    }

    @Step("WireMock: Полностью очистить виртуальные заглушки и логи в контейнере")
    public void resetWireMock() {
        // Очищает контейнер между тестами, чтобы правила не накапливались
        reset();
    }
}