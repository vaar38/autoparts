package ru.mirea.autoparts.service;

import ru.mirea.autoparts.model.Order;

import java.util.Comparator;

/**
 * Способы сортировки заказов. Каждая константа enum хранит своё название и свой Comparator —
 * правило сравнения двух заказов.
 */
public enum OrderSort {
    BY_DATE_NEWEST("По дате (сначала новые)",
            Comparator.comparing(Order::getCreatedAt).reversed()),
    BY_TOTAL_DESC("По сумме заказа (сначала дорогие)",
            Comparator.comparing(Order::getTotal).reversed()),
    BY_CUSTOMER_NAME("По ФИО клиента (А–Я)",
            Comparator.comparing(Order::getCustomerName).thenComparingInt(Order::getId)),
    BY_PART_NAME("По названию запчасти (А–Я)",
            Comparator.comparing(Order::getPartName, String.CASE_INSENSITIVE_ORDER));

    private final String title;
    private final Comparator<Order> comparator;

    OrderSort(String title, Comparator<Order> comparator) {
        this.title = title;
        this.comparator = comparator;
    }

    public String getTitle() {
        return title;
    }

    public Comparator<Order> getComparator() {
        return comparator;
    }
}
