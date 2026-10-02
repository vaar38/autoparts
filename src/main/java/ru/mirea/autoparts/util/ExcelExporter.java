package ru.mirea.autoparts.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.model.Order;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Экспорт в Excel (.xlsx) с помощью библиотеки Apache POI. Два листа: «Заказы» и «Клиенты». */
public class ExcelExporter implements DataExporter {
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Override
    public String getFormatName() {
        return "Excel (.xlsx)";
    }

    @Override
    public Path export(List<Customer> customers, List<Order> orders, Path directory) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            writeOrdersSheet(workbook.createSheet("Заказы"), orders, styles);
            writeCustomersSheet(workbook.createSheet("Клиенты"), customers, styles);

            Files.createDirectories(directory);
            Path file = directory.resolve("autoparts_" + LocalDateTime.now().format(FILE_NAME_TIME) + ".xlsx");
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
            return file;
        }
    }

    private void writeOrdersSheet(Sheet sheet, List<Order> orders, Styles styles) {
        String[] headers = {"ID", "Клиент", "Запчасть", "Артикул", "Марка авто", "Категория",
                "Кол-во", "Цена, руб.", "Сумма, руб.", "Статус", "Дата создания"};
        writeHeader(sheet, headers, styles.header);

        int rowIndex = 1;
        for (Order order : orders) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(order.getId());
            row.createCell(1).setCellValue(order.getCustomerName());
            row.createCell(2).setCellValue(order.getPartName());
            row.createCell(3).setCellValue(order.getPartNumber());
            row.createCell(4).setCellValue(order.getCarBrand());
            row.createCell(5).setCellValue(order.getCategory().getTitle());
            row.createCell(6).setCellValue(order.getQuantity());
            setMoney(row.createCell(7), order.getUnitPrice().doubleValue(), styles.money);
            setMoney(row.createCell(8), order.getTotal().doubleValue(), styles.money);
            row.createCell(9).setCellValue(order.getStatus().getTitle());
            Cell dateCell = row.createCell(10);
            dateCell.setCellValue(order.getCreatedAt());
            dateCell.setCellStyle(styles.dateTime);
        }
        finishSheet(sheet, headers.length);
    }

    private void writeCustomersSheet(Sheet sheet, List<Customer> customers, Styles styles) {
        String[] headers = {"ID", "ФИО", "Телефон", "Email", "Город", "Дата регистрации"};
        writeHeader(sheet, headers, styles.header);

        int rowIndex = 1;
        for (Customer customer : customers) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(customer.getId());
            row.createCell(1).setCellValue(customer.getFullName());
            row.createCell(2).setCellValue(customer.getPhone());
            row.createCell(3).setCellValue(customer.getEmail());
            row.createCell(4).setCellValue(customer.getCity());
            Cell dateCell = row.createCell(5);
            dateCell.setCellValue(customer.getRegisteredAt());
            dateCell.setCellStyle(styles.date);
        }
        finishSheet(sheet, headers.length);
    }

    private void writeHeader(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void setMoney(Cell cell, double value, CellStyle style) {
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /** Закрепляет строку заголовка и подгоняет ширину столбцов под содержимое. */
    private void finishSheet(Sheet sheet, int columns) {
        sheet.createFreezePane(0, 1);
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /** Стили ячеек создаются один раз на книгу. */
    private static class Styles {
        private final CellStyle header;
        private final CellStyle money;
        private final CellStyle date;
        private final CellStyle dateTime;

        Styles(Workbook workbook) {
            Font bold = workbook.createFont();
            bold.setBold(true);
            header = workbook.createCellStyle();
            header.setFont(bold);

            short moneyFormat = workbook.createDataFormat().getFormat("#,##0.00");
            money = workbook.createCellStyle();
            money.setDataFormat(moneyFormat);

            date = workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat("dd.mm.yyyy"));

            dateTime = workbook.createCellStyle();
            dateTime.setDataFormat(workbook.createDataFormat().getFormat("dd.mm.yyyy hh:mm"));
        }
    }
}
