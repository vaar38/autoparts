package ru.mirea.autoparts.service;

import ru.mirea.autoparts.exception.ValidationException;
import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Условия поиска и фильтрации заказов. record — компактный неизменяемый класс:
 * Java сама создаёт конструктор, геттеры (text(), status() ...), equals и toString.
 * Значение null у любого условия означает «не фильтровать по этому полю».
 *
 * @param text     строка поиска: ищется в названии запчасти, артикуле, марке авто и ФИО клиента
 * @param status   фильтр по статусу заказа
 * @param category фильтр по категории запчасти
 * @param dateFrom начало периода (дата создания заказа), включительно
 * @param dateTo   конец периода, включительно
 */
public record OrderFilter(String text, OrderStatus status, PartCategory category,
                          LocalDate dateFrom, LocalDate dateTo) {

    public static final String FIELD_DATE_RANGE = "dateRange";

    /** Компактный конструктор record: проверяем логическую корректность периода. */
    public OrderFilter {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ValidationException(FIELD_DATE_RANGE, "Начальная дата периода не может быть позже конечной.");
        }
        text = (text == null || text.isBlank()) ? null : text.trim().toLowerCase(Locale.ROOT);
    }

    /** Подходит ли заказ под все условия сразу (логическое И). */
    public boolean matches(Order order) {
        return matchesText(order)
                && (status == null || order.getStatus() == status)
                && (category == null || order.getCategory() == category)
                && (dateFrom == null || !order.getCreatedAt().toLocalDate().isBefore(dateFrom))
                && (dateTo == null || !order.getCreatedAt().toLocalDate().isAfter(dateTo));
    }

    /** Поиск без учёта регистра по нескольким смысловым полям: достаточно совпадения в любом из них. */
    private boolean matchesText(Order order) {
        if (text == null) {
            return true;
        }
        return contains(order.getPartName())
                || contains(order.getPartNumber())
                || contains(order.getCarBrand())
                || contains(order.getCustomerName());
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(text);
    }
}
