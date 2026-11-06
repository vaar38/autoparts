package ru.mirea.autoparts.util;

import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.model.Order;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Экспорт текущего списка заказов в CSV (текстовая таблица, столбцы разделены точкой с запятой).
 * Список клиентов здесь не нужен: имя клиента уже есть в каждом заказе.
 */
public class CsvExporter implements DataExporter {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String SEPARATOR = ";";

    @Override
    public String getFormatName() {
        return "CSV (.csv)";
    }

    @Override
    public String getExtension() {
        return "csv";
    }

    @Override
    public void export(List<Order> orders, List<Customer> customers, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write('﻿');   // BOM — чтобы Excel правильно показал русские буквы
            writer.write(String.join(SEPARATOR, "№ заказа", "Клиент", "Запчасть", "Артикул", "Марка авто",
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
    }

    /** Значение с ; или кавычками заключаем в кавычки по правилам CSV. */
    private String escape(String value) {
        if (value.contains(SEPARATOR) || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
