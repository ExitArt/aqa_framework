package tests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import saucedemo.pages.LoginPage;
import java.util.Collections;

public class BaseTest {
    // Используем ThreadLocal для изоляции ресурсов внутри каждого отдельного потока
    private static final ThreadLocal<Playwright> playwrightThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Browser> browserThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<BrowserContext> contextThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Page> pageThreadLocal = new ThreadLocal<>();

    // Удобные геттеры, чтобы ваши классы-наследники (тесты) могли вызывать page без изменений кода
    protected Page getPage() {
        return pageThreadLocal.get();
    }

    @BeforeEach
    void setUp() {
        // Инициализируем Playwright для текущего потока
        playwrightThreadLocal.set(Playwright.create());

        String browserParam = System.getProperty("chosen.browser", "chromium");
        String headlessParam = System.getProperty("chosen.headless", "true");

        if (System.getenv("CI") != null && System.getProperty("chosen.headless") == null) {
            headlessParam = "true";
        }
        boolean isHeadless = Boolean.parseBoolean(headlessParam);

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(isHeadless)
                .setArgs(Collections.singletonList("--no-sandbox"));

        // Инициализируем Browser для текущего потока
        if ("firefox".equalsIgnoreCase(browserParam)) {
            browserThreadLocal.set(playwrightThreadLocal.get().firefox().launch(options));
        } else {
            browserThreadLocal.set(playwrightThreadLocal.get().chromium().launch(options));
        }

        // Инициализируем контекст и страницу для текущего потока
        contextThreadLocal.set(browserThreadLocal.get().newContext());
        pageThreadLocal.set(contextThreadLocal.get().newPage());

        // Увеличим дефолтный таймаут до 30 секунд, чтобы нивелировать тормоза серверов GitHub Actions
        getPage().setDefaultTimeout(30000);

        String usernameParam = System.getProperty("test.username", "standard_user");
        String passwordParam = System.getProperty("test.password", "secret_sauce");

        // --- РЕФАКТОРИНГ ПО POM ---
        LoginPage loginPage = new LoginPage(getPage());
        loginPage.navigate(); // Открываем сайт
        loginPage.login(usernameParam, passwordParam); // Логинимся

        try {
            getPage().waitForURL("**/inventory.html", new Page.WaitForURLOptions().setTimeout(10000));
        } catch (PlaywrightException e) {
            System.err.println("ОШИБКА: Не удалось авторизоваться в потоке " + Thread.currentThread().getName() + "! Текущий URL: " + getPage().url());
            throw e;
        }
    }

    @AfterEach
    void tearDown() {
        // Закрываем все ресурсы строго в обратном порядке и чистим ThreadLocal для предотвращения утечек памяти
        if (pageThreadLocal.get() != null) {
            pageThreadLocal.get().close();
            pageThreadLocal.remove();
        }
        if (contextThreadLocal.get() != null) {
            contextThreadLocal.get().close();
            contextThreadLocal.remove();
        }
        if (browserThreadLocal.get() != null) {
            browserThreadLocal.get().close();
            browserThreadLocal.remove();
        }
        if (playwrightThreadLocal.get() != null) {
            playwrightThreadLocal.get().close();
            playwrightThreadLocal.remove();
        }
    }
}