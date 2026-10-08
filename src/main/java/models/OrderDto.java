package models;

public class OrderDto {
    private String user;
    private String status;
    private String orderId;

    // Пустой конструктор (обязателен для Jackson, чтобы десериализовать JSON обратно в объект)
    public OrderDto() {
    }

    public OrderDto(String user, String status, String orderId) {
        this.user = user;
        this.status = status;
        this.orderId = orderId;
    }

    // Геттеры и сеттеры
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}