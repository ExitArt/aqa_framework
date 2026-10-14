package steps;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.E2eOrderDto;

public class ApiSteps {

    // Спецификация запроса настраивается под фреймворк
    private final RequestSpecification requestSpec = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .log(LogDetail.ALL)
            .build();

    private final ResponseSpecification responseSpec = new ResponseSpecBuilder()
            .log(LogDetail.ALL)
            .build();

    @Step("API: Отправить POST-запрос на создание заказа в систему")
    public E2eOrderDto createOrderViaApi(String endpointUrl, E2eOrderDto orderPayload) {
        // используем встроенный в REST Assured сериализатор,
        // который автоматически превратит Java-объект E2eOrderDto в JSON-строку
        return RestAssured.given()
                .spec(requestSpec)
                .body(orderPayload)
                .when()
                // Так как реального эндпоинта SauceDemo нет, отправляем запрос на заглушку (Mock),
                // либо тестируем локальную отправку данных
                .post(endpointUrl)
                .then()
                .spec(responseSpec)
                // На реальном проекте тут будет проверка .statusCode(201)
                .extract()
                .as(E2eOrderDto.class); // Автоматически превращает JSON-ответ обратно в Java-объект
    }
}