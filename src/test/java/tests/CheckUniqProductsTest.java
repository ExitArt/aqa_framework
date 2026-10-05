package tests;

import org.junit.jupiter.api.Test;
import saucedemo.pages.ProductsPage; // Импортируем наш Page Object
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CheckUniqProductsTest extends BaseTest {

    @Test
    void checkUniq() {
        // Инициализируем Page Object
        ProductsPage productsPage = new ProductsPage(page);

        // 1. Ожидаем появление элементов с помощью умных ассертов Playwright
        assertThat(productsPage.getProductNamesLocator()).hasCount(6);
        assertThat(productsPage.getProductImagesLocator()).hasCount(6);

        // 2. ПРОВЕРКА НАЗВАНИЙ ТОВАРОВ
        List<String> allProductNames = productsPage.getAllProductNames();
        System.out.println("Найдено товаров на странице: " + allProductNames.size());

        Set<String> uniqueProductNames = new HashSet<>(allProductNames);
        assertEquals(
                allProductNames.size(),
                uniqueProductNames.size(),
                "Обнаружены дубликаты среди карточек товаров! Список товаров: " + allProductNames
        );

        // 3. ПРОВЕРКА КАРТИНОК ТОВАРОВ
        List<String> allImagesSource = productsPage.getAllProductImagesSources();
        System.out.println("Search pictures on page: " + allImagesSource.size());

        Set<String> uniqImgSource = new HashSet<>(allImagesSource);
        assertEquals(
                allImagesSource.size(),
                uniqImgSource.size(),
                "Caution! Duplicates images found: " + allImagesSource
        );
    }
}