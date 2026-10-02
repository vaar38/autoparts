package ru.mirea.autoparts.ui;

import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Подменю «Фильтрация» — отбор заказов по условию (Stream API в OrderService). */
public class FilterMenu extends BaseMenu {
    private final OrderService orderService;

    public FilterMenu(ConsoleInput input, OrderService orderService) {
        super(input);
        this.orderService = orderService;
    }

    @Override
    protected String getTitle() {
        return "ФИЛЬТРАЦИЯ ЗАКАЗОВ";
    }

    @Override
    protected List<String> getOptions() {
        return List.of(
                "По статусу",
                "По категории запчасти",
                "По клиенту",
                "По диапазону суммы заказа",
                "По диапазону дат");
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1 -> {
                OrderStatus status = input.choose("Статус:", Arrays.asList(OrderStatus.values()), OrderStatus::getTitle);
                TablePrinter.printOrders(orderService.filterByStatus(status));
            }
            case 2 -> {
                PartCategory category = input.choose("Категория:", Arrays.asList(PartCategory.values()), PartCategory::getTitle);
                TablePrinter.printOrders(orderService.filterByCategory(category));
            }
            case 3 -> TablePrinter.printOrders(orderService.filterByCustomer(input.readId("Введите ID клиента: ")));
            case 4 -> {
                BigDecimal min = input.readDecimal("Сумма от, руб.: ");
                BigDecimal max = input.readDecimal("Сумма до, руб.: ");
                TablePrinter.printOrders(orderService.filterByTotalRange(min, max));
            }
            case 5 -> {
                LocalDate from = input.readDate("Дата с (ДД.ММ.ГГГГ): ");
                LocalDate to = input.readDate("Дата по (ДД.ММ.ГГГГ): ");
                TablePrinter.printOrders(orderService.filterByDateRange(from, to));
            }
            default -> throw new IllegalStateException("Неизвестный пункт меню: " + choice);
        }
    }
}
