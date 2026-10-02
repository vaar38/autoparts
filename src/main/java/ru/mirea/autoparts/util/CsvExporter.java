package ru.mirea.autoparts.util;

import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.model.Order;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Экспорт заказов в CSV (текстовая таблица, столбцы разделены точкой с запятой).
 * Список клиентов здесь не нужен: имя клиента уже есть в каждом заказе.
 */
public class CsvExporter implements DataExporter {
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String SEPARATOR = ";";

    @Override
    public String getFormatName() {
        return "CSV (.csv)";
    }

    @Override
    public Path export(List<Customer> customers, List<Order> orders, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve("orders_" + LocalDateTime.now().format(FILE_NAME_TIME) + ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write('﻿');   // BOM — чтобы Excel правильно показал русские буквы
            writer.write(String.join(SEPARATOR, "ID", "Клиент", "Запчасть", "Артикул", "Марка авто",
                    "Категория", "Кол-во", "Цена", "Сумма", "Статус", "Дата создания"));
            writer.newLine();
            for (Order order : orders) {
                writer.write(String.join(SEPARATOR,
                        String.valueOf(order.getId()),
                        escape(order.getCustomerName()),
                        escape(order.getPartName()),
                        escape(order.getPartNumber()),
                        escape(order.getCarBrand()),
                        order.getCategory().getTitle(),
                        String.valueOf(order.getQuantity()),
                        order.getUnitPrice().toPlainString(),
                        order.getTotal().toPlainString(),
                        order.getStatus().getTitle(),
                        order.getCreatedAt().format(DATE_TIME)));
                writer.newLine();
            }
        }
        return file;
    }

    /** Значение с ; или кавычками заключаем в кавычки по правилам CSV. */
    private String escape(String value) {
        if (value.contains(SEPARATOR) || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
