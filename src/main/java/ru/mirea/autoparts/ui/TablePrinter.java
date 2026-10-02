package ru.mirea.autoparts.ui;

import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.model.Order;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Вывод клиентов и заказов в консоль в виде таблиц. */
public final class TablePrinter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String ORDER_ROW = "%-4s %-16s %-24s %-11s %-10s %-9s %4s %11s %-11s %-10s%n";
    private static final String CUSTOMER_ROW = "%-4s %-28s %-13s %-20s %-10s %-10s%n";

    private TablePrinter() {
        // служебный класс: только статические методы, объекты не создаются
    }

    public static void printOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            System.out.println("Заказы не найдены.");
            return;
        }
        System.out.printf(ORDER_ROW, "ID", "Клиент", "Запчасть", "Артикул", "Марка", "Категория",
                "Шт", "Сумма", "Статус", "Дата");
        System.out.println("-".repeat(115));
        for (Order order : orders) {
            System.out.printf(ORDER_ROW,
                    order.getId(),
                    cut(order.getCustomerName(), 16),
                    cut(order.getPartName(), 24),
                    cut(order.getPartNumber(), 11),
                    cut(order.getCarBrand(), 10),
                    order.getCategory().getTitle(),
                    order.getQuantity(),
                    order.getTotal().toPlainString(),
                    order.getStatus().getTitle(),
                    order.getCreatedAt().format(DATE));
        }
        System.out.println("Всего записей: " + orders.size());
    }

    public static void printCustomers(List<Customer> customers) {
        if (customers.isEmpty()) {
            System.out.println("Клиенты не найдены.");
            return;
        }
        System.out.printf(CUSTOMER_ROW, "ID", "ФИО", "Телефон", "Email", "Город", "Регистр.");
        System.out.println("-".repeat(90));
        for (Customer customer : customers) {
            System.out.printf(CUSTOMER_ROW,
                    customer.getId(),
                    cut(customer.getFullName(), 28),
                    customer.getPhone(),
                    cut(customer.getEmail(), 20),
                    cut(customer.getCity(), 10),
                    customer.getRegisteredAt().format(DATE));
        }
        System.out.println("Всего записей: " + customers.size());
    }

    public static void printOrderDetails(Order order) {
        System.out.println("Заказ #" + order.getId());
        System.out.println("  Клиент:        " + order.getCustomerName() + " (ID " + order.getCustomerId() + ")");
        System.out.println("  Запчасть:      " + order.getPartName());
        System.out.println("  Артикул:       " + order.getPartNumber());
        System.out.println("  Марка авто:    " + order.getCarBrand());
        System.out.println("  Категория:     " + order.getCategory().getTitle());
        System.out.println("  Количество:    " + order.getQuantity() + " шт.");
        System.out.println("  Цена за шт.:   " + order.getUnitPrice().toPlainString() + " руб.");
        System.out.println("  Сумма:         " + order.getTotal().toPlainString() + " руб.");
        System.out.println("  Статус:        " + order.getStatus().getTitle());
        System.out.println("  Создан:        " + order.getCreatedAt().format(DATE_TIME));
    }

    public static void printCustomerDetails(Customer customer) {
        System.out.println("Клиент #" + customer.getId());
        System.out.println("  ФИО:           " + customer.getFullName());
        System.out.println("  Телефон:       " + customer.getPhone());
        System.out.println("  Email:         " + customer.getEmail());
        System.out.println("  Город:         " + customer.getCity());
        System.out.println("  Регистрация:   " + customer.getRegisteredAt().format(DATE));
    }

    /** Обрезает длинный текст, чтобы таблица не разъезжалась. */
    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "~";
    }
}
