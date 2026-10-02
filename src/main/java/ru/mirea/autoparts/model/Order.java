package ru.mirea.autoparts.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Заказ автозапчасти (таблица orders) — основная сущность варианта.
 * Каждый заказ принадлежит одному клиенту: поле customerId ссылается на customers.id.
 */
public class Order {
    private int id;
    private int customerId;
    private String customerName;   // не хранится в orders, подтягивается через JOIN для удобного вывода
    private String partName;
    private String partNumber;     // артикул запчасти
    private String carBrand;
    private PartCategory category;
    private int quantity;
    private BigDecimal unitPrice;  // BigDecimal, а не double: деньги нельзя считать с погрешностью
    private OrderStatus status;
    private LocalDateTime createdAt;

    /** Конструктор для нового заказа: статус всегда «Новый», дата — текущий момент. */
    public Order(int customerId, String partName, String partNumber, String carBrand,
                 PartCategory category, int quantity, BigDecimal unitPrice) {
        this(0, customerId, null, partName, partNumber, carBrand, category, quantity, unitPrice,
                OrderStatus.NEW, LocalDateTime.now());
    }

    /** Конструктор для заказа, прочитанного из базы данных. */
    public Order(int id, int customerId, String customerName, String partName, String partNumber,
                 String carBrand, PartCategory category, int quantity, BigDecimal unitPrice,
                 OrderStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.partName = partName;
        this.partNumber = partNumber;
        this.carBrand = carBrand;
        this.category = category;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Сумма заказа = цена за штуку × количество. */
    public BigDecimal getTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getCarBrand() {
        return carBrand;
    }

    public void setCarBrand(String carBrand) {
        this.carBrand = carBrand;
    }

    public PartCategory getCategory() {
        return category;
    }

    public void setCategory(PartCategory category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Заказ #" + id + ": " + partName + " (" + partNumber + ") x" + quantity
                + ", " + getTotal() + " руб., " + status.getTitle();
    }
}
