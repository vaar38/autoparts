package ru.mirea.autoparts.service;

import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.repository.CustomerRepository;
import ru.mirea.autoparts.repository.OrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Считает показатели системы для пункта меню «Статистика». */
public class StatisticsService {
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public StatisticsService(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Возвращает показатели в виде «название -> значение».
     * LinkedHashMap сохраняет порядок добавления, поэтому показатели выводятся в нужном порядке.
     */
    public Map<String, String> collect() {
        List<Order> orders = orderRepository.findAll();

        // Количество заказов в каждом статусе: {NEW=4, CONFIRMED=3, ...}
        Map<OrderStatus, Long> byStatus = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus,
                        () -> new EnumMap<>(OrderStatus.class), Collectors.counting()));

        long active = orders.stream().filter(order -> order.getStatus().isActive()).count();

        BigDecimal revenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BigDecimal> notCancelledTotals = orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotal)
                .toList();
        BigDecimal average = notCancelledTotals.isEmpty() ? BigDecimal.ZERO
                : notCancelledTotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(notCancelledTotals.size()), 2, RoundingMode.HALF_UP);

        // Категория, в которой заказано больше всего штук
        String popularCategory = orders.stream()
                .collect(Collectors.groupingBy(Order::getCategory, Collectors.summingInt(Order::getQuantity)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().getTitle() + " (" + entry.getValue() + " шт.)")
                .orElse("нет данных");

        // Клиент с наибольшей суммой неотменённых заказов
        String topCustomer = orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(Order::getCustomerName,
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotal, BigDecimal::add)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey() + " (" + money(entry.getValue()) + ")")
                .orElse("нет данных");

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("Всего клиентов", String.valueOf(customerRepository.count()));
        stats.put("Всего заказов", String.valueOf(orders.size()));
        stats.put("Активных заказов (в работе)", String.valueOf(active));
        for (OrderStatus status : OrderStatus.values()) {
            stats.put("  — " + status.getTitle(), String.valueOf(byStatus.getOrDefault(status, 0L)));
        }
        stats.put("Выручка по доставленным заказам", money(revenue));
        stats.put("Средняя сумма заказа (без отменённых)", money(average));
        stats.put("Самая популярная категория", popularCategory);
        stats.put("Лучший клиент по сумме заказов", topCustomer);
        return stats;
    }

    private static String money(BigDecimal value) {
        return String.format("%,.2f руб.", value);
    }
}
