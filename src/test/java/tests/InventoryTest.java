package tests;

import org.junit.jupiter.api.Test;
import saucedemo.pages.ProductsPage; // Импортируем Page Object
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class InventoryTest extends BaseTest {

    @Test
    public void shouldHaveItemsInStore() {
        ProductsPage productsPage = new ProductsPage(page);

        // Мы заранее знаем требования бизнеса: на SauceDemo ВСЕГДА должно быть 6 товаров
        int expectedItemsCount = 6;

        // Playwright будет ждать до 5 секунд, пока страница догрузится и карточек станет ровно 6
        assertThat(productsPage.getInventoryItemsLocator()).hasCount(expectedItemsCount);

        System.out.println("Тест успешно проверил наличие " + expectedItemsCount + " товаров.");
    }
}