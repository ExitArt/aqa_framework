package tests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import saucedemo.pages.LoginPage;
import steps.DbSteps;

import java.util.Collections;

public class BaseTest {
    // Переводим менеджеры Playwright и Browser в потокобезопасные контейнеры ThreadLocal
    private static final ThreadLocal<Playwright> playwrightThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Browser> browserThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> contextThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Page> pageThreadLocal = new ThreadLocal<>();

    // Удобные геттеры, чтобы тесты могли обращаться к page напрямую
    protected Page page;

    // Создаем бд
    private static final DbSteps db = new DbSteps();

    // Создаст таблицу в пустом GitHub Actions или пропустит шаг
    @BeforeAll
    static void initDatabaseStructure(){
        db.createOrdersTable();
        db.clearOrdersTable();
    }

    @BeforeEach
    void setUp() {
        // Инициализируем Playwright строго для текущего потока
        Playwright playwright = Playwright.create();
        playwrightThreadLocal.set(playwright);

        String browserParam = System.getProperty("chosen.browser", "chromium");
        String headlessParam = System.getProperty("chosen.headless", "true");

        if (System.getenv("CI") != null && System.getProperty("chosen.headless") == null) {
            headlessParam = "true";
        }
        boolean isHeadless = Boolean.parseBoolean(headlessParam);

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(isHeadless)
                .setArgs(Collections.singletonList("--no-sandbox"));

        Browser browser;
        if ("firefox".equalsIgnoreCase(browserParam)) {
            browser = playwright.firefox().launch(options);
        } else {
            browser = playwright.chromium().launch(options);
        }
        browserThreadLocal.set(browser);

        // Создаем изолированный контекст и страницу для текущего потока
        BrowserContext context = browser.newContext();
        contextThreadLocal.set(context);

        Page localPage = context.newPage();
        localPage.setDefaultTimeout(30000);
        pageThreadLocal.set(localPage);

        // Сохраняем ссылку в protected переменную для обратной совместимости с тестами
        this.page = localPage;

        String usernameParam = System.getProperty("test.username", "standard_user");
        String passwordParam = System.getProperty("test.password", "secret_sauce");

        // Авторизация выполняется изолированно в каждом потоке
        LoginPage loginPage = new LoginPage(this.page);
        loginPage.navigate();
        loginPage.login(usernameParam, passwordParam);

        try {
            this.page.waitForURL("**/inventory.html", new Page.WaitForURLOptions().setTimeout(10000));
        } catch (PlaywrightException e) {
            System.err.println("ОШИБКА: Не удалось авторизоваться в потоке " + Thread.currentThread().getName() + "! Текущий URL: " + this.page.url());
            throw e;
        }
    }

    @AfterEach
    void tearDown() {
        // Аккуратно закрываем ресурсы текущего потока и очищаем ThreadLocal переменные
        Page localPage = pageThreadLocal.get();
        if (localPage != null) localPage.close();

        BrowserContext localContext = contextThreadLocal.get();
        if (localContext != null) localContext.close();

        Browser localBrowser = browserThreadLocal.get();
        if (localBrowser != null) localBrowser.close();

        Playwright localPlaywright = playwrightThreadLocal.get();
        if (localPlaywright != null) localPlaywright.close();

        // Удаляем значения из памяти, чтобы избежать утечек памяти (Memory Leaks)
        pageThreadLocal.remove();
        contextThreadLocal.remove();
        browserThreadLocal.remove();
        playwrightThreadLocal.remove();
    }
}