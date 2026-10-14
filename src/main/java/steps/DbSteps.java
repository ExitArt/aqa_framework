package steps;

import io.qameta.allure.Step;
import org.jdbi.v3.core.Jdbi;
import utils.DbConfig;

public class DbSteps {

    // Инициализируем JDBI через наш конфигуратор секретов
    private final Jdbi jdbi = Jdbi.create(
            DbConfig.getUrl(),
            DbConfig.getUser(),
            DbConfig.getPassword()
    );

    @Step("БД: Создать таблицу заказов 'orders' (если её нет)")
    public synchronized void createOrdersTable() {
        try {
            jdbi.useHandle(handle -> handle.execute(
                    "CREATE TABLE IF NOT EXISTS orders (" +
                            "id SERIAL PRIMARY KEY, " +
                            "order_id VARCHAR(50) UNIQUE NOT NULL, " +
                            "username VARCHAR(100) NOT NULL, " +
                            "item_name VARCHAR(100) NOT NULL, " +
                            "price VARCHAR(20) NOT NULL, " +
                            "status VARCHAR(50) NOT NULL" +
                            ")"
            ));
        } catch (Exception e) {
            System.out.println("Запрос создания таблицы обработан параллельным потоком: " + e.getMessage());
        }
    }

    @Step("БД: Записать новый заказ {1} в таблицу orders")
    public void saveOrderToDb(String orderId, String username, String itemName, String price, String status) {
        jdbi.useTransaction(handle -> handle.execute(
                "INSERT INTO orders (order_id, username, item_name, price, status) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT (order_id) DO UPDATE SET status = EXCLUDED.status",
                orderId, username, itemName, price, status
        ));
    }

    @Step("БД: Получить статус заказа из базы по его orderId: {0} (с ожиданием)")
    public String getOrderStatus(String orderId) {
        try {
            // Используем Awaitility (она у тебя подключена в pom.xml)
            return org.awaitility.Awaitility.await()
                    .atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                    .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .until(
                            () -> jdbi.withHandle(handle -> handle.createQuery(
                                            "SELECT status FROM orders WHERE order_id = ?") // ИСПРАВЛЕНО: заменили именованный параметр на "?"
                                    .bind(0, orderId) // ИСПРАВЛЕНО: привязка по индексу 0
                                    .mapTo(String.class)
                                    .findFirst()
                                    .orElse("NOT_FOUND")),
                            status -> !"NOT_FOUND".equals(status)
                    );
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            return "NOT_FOUND";
        }
    }

    @Step("БД: Полностью очистить таблицу заказов и сбросить автоинкремент ID")
    public void clearOrdersTable() {
        // Добавили RESTART IDENTITY — теперь при каждом прогоне ID снова начнётся с 1!
        jdbi.useHandle(handle -> handle.execute("TRUNCATE TABLE orders RESTART IDENTITY"));
    }
}