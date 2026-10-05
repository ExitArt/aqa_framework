package saucedemo.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

public class LoginPage {
    private final Page page;

    // 1. Объявляем локаторы элементов
    private final Locator usernameInput;
    private final Locator passwordInput;
    private final Locator loginButton;

    // 2. Инициализируем их в конструкторе
    public LoginPage(Page page) {
        this.page = page;
        this.usernameInput = page.locator("[data-test='username']");
        this.passwordInput = page.locator("[data-test='password']");
        this.loginButton = page.locator("[data-test='login-button']");
    }

    // 3. Бизнес-логика: открытие страницы и авторизация
    public void navigate() {
        page.navigate("https://www.saucedemo.com", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
    }

    public void login(String username, String password) {
        usernameInput.fill(username);
        passwordInput.fill(password);
        loginButton.click();
    }
}