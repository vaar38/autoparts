package ru.mirea.autoparts.ui;

import ru.mirea.autoparts.service.OrderService;
import ru.mirea.autoparts.service.OrderSort;

import java.util.Arrays;
import java.util.List;

/** Подменю «Сортировка». Пункты меню строятся из констант enum OrderSort. */
public class SortMenu extends BaseMenu {
    private final OrderService orderService;

    public SortMenu(ConsoleInput input, OrderService orderService) {
        super(input);
        this.orderService = orderService;
    }

    @Override
    protected String getTitle() {
        return "СОРТИРОВКА ЗАКАЗОВ";
    }

    @Override
    protected List<String> getOptions() {
        return Arrays.stream(OrderSort.values())
                .map(OrderSort::getTitle)
                .toList();
    }

    @Override
    protected void handle(int choice) {
        OrderSort sort = OrderSort.values()[choice - 1];
        TablePrinter.printOrders(orderService.sort(sort));
    }
}
