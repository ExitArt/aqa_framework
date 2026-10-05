package tests;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import saucedemo.pages.ProductsPage; // Импортируем наш Page Object
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AddToCartTest extends BaseTest { // Наследуемся от базового класса

    @Test
    public void testSuccessAddToCart() {
        // Инициализируем страницу продуктов, передавая page из BaseTest
        ProductsPage productsPage = new ProductsPage(page);

        int totalButtonsExpected = productsPage.getAddToCartButtonsCount();
        System.out.println("Найдено кнопок для нажатия: " + totalButtonsExpected);

        // Взаимодействуем со страницей через понятный бизнес-метод
        productsPage.addAllProductsToCart();

        // Проверяем состояние кнопок
        List<Locator> removeButtonsList = productsPage.getAllRemoveButtons();
        for (Locator button : removeButtonsList) {
            // Использование динамического ассерта Playwright (без waitForTimeout костылей)
            assertThat(button).hasAttribute("class", java.util.regex.Pattern.compile(".*btn_secondary.*"));
        }

        System.out.println("Все кнопки успешно сменились на 'Remove'");
    }
}