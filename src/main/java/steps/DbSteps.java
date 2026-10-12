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
    public void createOrdersTable() {
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
    }

    @Step("БД: Записать новый заказ {1} в таблицу orders")
    public void saveOrderToDb(String orderId, String username, String itemName, String price, String status) {
        jdbi.useHandle(handle -> handle.execute(
                "INSERT INTO orders (order_id, username, item_name, price, status) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT (order_id) DO UPDATE SET status = EXCLUDED.status",
                orderId, username, itemName, price, status
        ));
    }

    @Step("БД: Получить статус заказа из базы по его orderId: {0}")
    public String getOrderStatus(String orderId) {
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT status FROM orders WHERE order_id = :orderId")
                .bind("orderId", orderId)
                .mapTo(String.class)
                .findFirst()
                .orElse("NOT_FOUND")
        );
    }

    @Step("БД: Полностью очистить таблицу заказов")
    public void clearOrdersTable() {
        jdbi.useHandle(handle -> handle.execute("TRUNCATE TABLE orders"));
    }
}