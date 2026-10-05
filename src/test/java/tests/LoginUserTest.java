package tests;

import org.junit.jupiter.api.Test;
import saucedemo.pages.ProductsPage; // Импортируем нашу страницу
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LoginUserTest extends BaseTest {

    @Test
    public void AllUsersAuthorization(){
        // Вызываем авторизацию (предположим, логин происходит в BaseTest)
        // Инициализируем Page Object для страницы продуктов
        ProductsPage productsPage = new ProductsPage(getPage());

        // Чистые проверки без локаторов внутри теста!
        assertThat(productsPage.getLogo()).hasText("Swag Labs");
        assertThat(productsPage.getTitle()).hasText("Products");

        System.out.println("User authorized");
    }
}