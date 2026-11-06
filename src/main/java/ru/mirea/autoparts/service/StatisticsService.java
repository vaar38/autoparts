package ru.mirea.autoparts.service;

import ru.mirea.autoparts.exception.ValidationException;
import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.repository.CustomerRepository;
import ru.mirea.autoparts.repository.OrderRepository;
import ru.mirea.autoparts.util.Formats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Считает показатели системы для вкладки «Статистика». Все расчёты — через Stream API. */
public class StatisticsService {
    public static final String FIELD_PERIOD = "period";

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public StatisticsService(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Общие показатели в виде «название -> значение».
     * LinkedHashMap сохраняет порядок добавления, поэтому показатели выводятся в нужном порядке.
     */
    public Map<String, String> collect() {
        List<Order> orders = orderRepository.findAll();

        long active = orders.stream().filter(order -> order.getStatus().isActive()).count();
        long cancelled = orders.stream().filter(order -> order.getStatus() == OrderStatus.CANCELLED).count();

        BigDecimal revenue = sum(orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .toList());

        // Минимум, максимум и среднее считаем по неотменённым заказам
        List<BigDecimal> totals = orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotal)
                .toList();
        BigDecimal min = totals.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal max = totals.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal average = average(totals);

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
                .map(entry -> entry.getKey() + " (" + Formats.money(entry.getValue()) + ")")
                .orElse("нет данных");

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("Всего клиентов", String.valueOf(customerRepository.count()));
        stats.put("Всего заказов", String.valueOf(orders.size()));
        stats.put("Активных заказов (в работе)", String.valueOf(active));
        stats.put("Доля отменённых заказов", Formats.percent(cancelled, orders.size()));
        stats.put("Выручка по доставленным заказам", Formats.money(revenue));
        stats.put("Средняя сумма заказа", Formats.money(average));
        stats.put("Минимальная сумма заказа", Formats.money(min));
        stats.put("Максимальная сумма заказа", Formats.money(max));
        stats.put("Самая популярная категория", popularCategory);
        stats.put("Лучший клиент по сумме заказов", topCustomer);
        return stats;
    }

    /** Показатели за выбранный период (по дате создания заказа, границы включительно). */
    public Map<String, String> collectForPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException(FIELD_PERIOD, "Укажите обе даты периода: «с» и «по».");
        }
        if (from.isAfter(to)) {
            throw new ValidationException(FIELD_PERIOD, "Начальная дата периода не может быть позже конечной.");
        }
        List<Order> inPeriod = orderRepository.findAll().stream()
                .filter(order -> {
                    LocalDate date = order.getCreatedAt().toLocalDate();
                    return !date.isBefore(from) && !date.isAfter(to);
                })
                .toList();
        List<Order> notCancelled = inPeriod.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("Заказов за период", String.valueOf(inPeriod.size()));
        stats.put("Из них не отменено", String.valueOf(notCancelled.size()));
        stats.put("Сумма заказов (без отменённых)", Formats.money(sum(notCancelled)));
        stats.put("Средняя сумма заказа", Formats.money(average(notCancelled.stream().map(Order::getTotal).toList())));
        return stats;
    }

    /** Количество заказов в каждом статусе — для столбчатой диаграммы. EnumMap хранит статусы в порядке объявления. */
    public Map<OrderStatus, Long> countByStatus() {
        Map<OrderStatus, Long> result = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            result.put(status, 0L);
        }
        orderRepository.findAll().forEach(order -> result.merge(order.getStatus(), 1L, Long::sum));
        return result;
    }

    /** Сумма неотменённых заказов по категориям — для круговой диаграммы. */
    public Map<PartCategory, BigDecimal> totalByCategory() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(Order::getCategory, () -> new EnumMap<>(PartCategory.class),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotal, BigDecimal::add)));
    }

    private static BigDecimal sum(List<Order> orders) {
        return orders.stream().map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    /** Сумма неотменённых заказов из списка — например, итог по заказам одного клиента. */
    public static BigDecimal sumNotCancelled(List<Order> orders) {
        return sum(orders.stream().filter(order -> order.getStatus() != OrderStatus.CANCELLED).toList());
    }
}
