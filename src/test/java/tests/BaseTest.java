package tests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import saucedemo.pages.LoginPage; // Импортируем наш Page Object
import java.util.Collections;

public class BaseTest {
    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();

        String browserParam = System.getProperty("chosen.browser", "chromium");
        String headlessParam = System.getProperty("chosen.headless", "true");

        if (System.getenv("CI") != null && System.getProperty("chosen.headless") == null) {
            headlessParam = "true";
        }
        boolean isHeadless = Boolean.parseBoolean(headlessParam);

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(isHeadless)
                .setArgs(Collections.singletonList("--no-sandbox"));

        if ("firefox".equalsIgnoreCase(browserParam)) {
            browser = playwright.firefox().launch(options);
        } else {
            browser = playwright.chromium().launch(options);
        }

        context = browser.newContext();
        page = context.newPage();
        page.setDefaultTimeout(15000);

        String usernameParam = System.getProperty("test.username", "standard_user");
        String passwordParam = System.getProperty("test.password", "secret_sauce");

        // --- РЕФАКТОРИНГ ПО POM ---
        LoginPage loginPage = new LoginPage(page);
        loginPage.navigate(); // Открываем сайт
        loginPage.login(usernameParam, passwordParam); // Логинимся через метод класса страницы

        try {
            page.waitForURL("**/inventory.html", new Page.WaitForURLOptions().setTimeout(5000));
        } catch (PlaywrightException e) {
            System.err.println("ОШИБКА: Не удалось авторизоваться в потоке " + Thread.currentThread().getName() + "! Текущий URL: " + page.url());
            throw e;
        }
    }

    @AfterEach
    void tearDown() {
        if (page != null) page.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}