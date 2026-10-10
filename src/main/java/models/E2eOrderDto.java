package models;

public class E2eOrderDto {
    private String user;
    private String orderId;
    private String item;
    private String price;
    private String status;

    public E2eOrderDto() {}

    public E2eOrderDto(String user, String orderId, String item, String price, String status) {
        this.user = user;
        this.orderId = orderId;
        this.item = item;
        this.price = price;
        this.status = status;
    }

    // Геттеры и сеттеры
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}