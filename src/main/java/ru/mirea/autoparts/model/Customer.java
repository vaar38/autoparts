package ru.mirea.autoparts.model;

import java.time.LocalDate;

/**
 * Клиент магазина автозапчастей (таблица customers).
 * Все поля private — доступ к ним только через геттеры и сеттеры (инкапсуляция).
 */
public class Customer {
    private int id;
    private String fullName;
    private String phone;
    private String email;
    private String city;
    private LocalDate registeredAt;

    /** Конструктор для нового клиента: id выдаст база данных при сохранении. */
    public Customer(String fullName, String phone, String email, String city, LocalDate registeredAt) {
        this(0, fullName, phone, email, city, registeredAt);
    }

    /** Конструктор для клиента, прочитанного из базы данных. */
    public Customer(int id, String fullName, String phone, String email, String city, LocalDate registeredAt) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.city = city;
        this.registeredAt = registeredAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDate getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDate registeredAt) {
        this.registeredAt = registeredAt;
    }

    /** Так клиент выглядит в выпадающих списках JavaFX (ComboBox). */
    @Override
    public String toString() {
        return fullName + " (" + phone + ")";
    }
}
