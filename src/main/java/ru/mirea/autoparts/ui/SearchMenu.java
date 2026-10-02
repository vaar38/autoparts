package ru.mirea.autoparts.ui;

import ru.mirea.autoparts.service.OrderService;

import java.util.List;

/** Подменю «Поиск» — поиск заказов по части текста (SQL LIKE). */
public class SearchMenu extends BaseMenu {
    private final OrderService orderService;

    public SearchMenu(ConsoleInput input, OrderService orderService) {
        super(input);
        this.orderService = orderService;
    }

    @Override
    protected String getTitle() {
        return "ПОИСК ЗАКАЗОВ";
    }

    @Override
    protected List<String> getOptions() {
        return List.of(
                "По названию запчасти",
                "По артикулу",
                "По марке автомобиля",
                "По ФИО клиента");
    }

    @Override
    protected void handle(int choice) {
        String text = input.readRequired("Введите текст для поиска: ");
        switch (choice) {
            case 1 -> TablePrinter.printOrders(orderService.searchByPartName(text));
            case 2 -> TablePrinter.printOrders(orderService.searchByPartNumber(text));
            case 3 -> TablePrinter.printOrders(orderService.searchByCarBrand(text));
            case 4 -> TablePrinter.printOrders(orderService.searchByCustomerName(text));
            default -> throw new IllegalStateException("Неизвестный пункт меню: " + choice);
        }
    }
}
