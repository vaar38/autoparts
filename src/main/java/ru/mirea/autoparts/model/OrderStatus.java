package ru.mirea.autoparts.model;

import java.util.List;

/**
 * Статус заказа. В базе хранится имя константы (NEW, CONFIRMED, ...),
 * пользователю показывается русское название title.
 */
public enum OrderStatus {
    NEW("Новый"),
    CONFIRMED("Подтверждён"),
    SHIPPED("Отправлен"),
    DELIVERED("Доставлен"),
    CANCELLED("Отменён");

    private final String title;

    OrderStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Бизнес-правило: в какие статусы можно перевести заказ из текущего.
     * Новый -> Подтверждён / Отменён; Подтверждён -> Отправлен / Отменён;
     * Отправлен -> Доставлен; Доставлен и Отменён — финальные.
     */
    public List<OrderStatus> getAllowedNext() {
        return switch (this) {
            case NEW -> List.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> List.of(SHIPPED, CANCELLED);
            case SHIPPED -> List.of(DELIVERED);
            case DELIVERED, CANCELLED -> List.of();
        };
    }

    public boolean canChangeTo(OrderStatus next) {
        return getAllowedNext().contains(next);
    }

    /** Заказ ещё в работе (не доставлен и не отменён). */
    public boolean isActive() {
        return this == NEW || this == CONFIRMED || this == SHIPPED;
    }
}
