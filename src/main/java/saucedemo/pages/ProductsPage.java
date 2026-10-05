package saucedemo.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.ArrayList;
import java.util.List;

public class ProductsPage {
    private final Page page;

    // 1. Все локаторы страницы собраны в одном месте
    private final Locator logo;                  // Для LoginUserTest
    private final Locator title;                 // Для LoginUserTest
    private final Locator addToCartButtons;      // Для AddToCartTest
    private final Locator removeFromCartButtons; // Для AddToCartTest
    private final Locator productNames;          // Для CheckUniqProductsTest
    private final Locator productImages;         // Для CheckUniqProductsTest
    private final Locator inventoryItems;        // Для InventoryTest

    // 2. Инициализируем их строго в конструкторе
    public ProductsPage(Page page) {
        this.page = page;
        this.logo = page.locator(".app_logo");
        this.title = page.locator(".title[data-test='title']");
        this.addToCartButtons = page.locator(".btn_primary.btn_inventory");
        this.removeFromCartButtons = page.locator(".btn_secondary.btn_inventory");
        this.productNames = page.locator(".inventory_item_name");
        this.productImages = page.locator(".inventory_item_img img");
        this.inventoryItems = page.locator(".inventory_item[data-test='inventory-item']");
    }

    // --- МЕТОДЫ ДЛЯ LoginUserTest ---
    public Locator getLogo() {
        return this.logo;
    }

    public Locator getTitle() {
        return this.title;
    }

    // --- МЕТОДЫ ДЛЯ AddToCartTest ---
    public void addAllProductsToCart() {
        while (addToCartButtons.first().isVisible()) {
            addToCartButtons.first().click();
        }
    }

    public List<Locator> getAllRemoveButtons() {
        return removeFromCartButtons.all();
    }

    public int getAddToCartButtonsCount() {
        return addToCartButtons.count();
    }

    // --- МЕТОДЫ ДЛЯ CheckUniqProductsTest ---
    public Locator getProductNamesLocator() {
        return this.productNames;
    }

    public Locator getProductImagesLocator() {
        return this.productImages;
    }

    public List<String> getAllProductNames() {
        return productNames.allTextContents();
    }

    public List<String> getAllProductImagesSources() {
        int imgCount = productImages.count();
        List<String> sources = new ArrayList<>();
        for (int i = 0; i < imgCount; i++) {
            sources.add(productImages.nth(i).getAttribute("src"));
        }
        return sources;
    }

    // --- МЕТОДЫ ДЛЯ InventoryTest ---
    public Locator getInventoryItemsLocator() {
        return this.inventoryItems;
    }

    public int getInventoryItemsCount() {
        return this.inventoryItems.count();
    }
}