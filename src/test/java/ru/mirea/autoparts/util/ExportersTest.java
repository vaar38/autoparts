package ru.mirea.autoparts.util;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Экспорт пишет именно в выбранный файл и сохраняет все переданные заказы. @TempDir — временная папка теста. */
class ExportersTest {

    private final List<Order> orders = List.of(new Order(1, 1, "Иванов Иван Петрович", "Масляный фильтр; оригинал",
            "W7008", "Toyota", PartCategory.FILTERS, 2, new BigDecimal("690.00"), OrderStatus.NEW,
            LocalDateTime.of(2026, 7, 5, 12, 40)));
    private final List<Customer> customers = List.of(new Customer(1, "Иванов Иван Петрович", "+79161234567",
            "ivanov@mail.ru", "Москва", LocalDate.of(2026, 1, 15)));

    @Test
    void csvContainsHeaderAndEscapedRow(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("orders.csv");
        new CsvExporter().export(orders, customers, file);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).contains("№ заказа;Клиент;Запчасть");
        // Значение с точкой с запятой взято в кавычки, иначе оно «разъехалось» бы на два столбца
        assertThat(lines.get(1)).contains("\"Масляный фильтр; оригинал\"").contains("1380.00").contains("Новый");
    }

    @Test
    void xlsxHasOrdersAndCustomersSheets(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("orders.xlsx");
        new ExcelExporter().export(orders, customers, file);

        try (InputStream in = Files.newInputStream(file); Workbook workbook = new XSSFWorkbook(in)) {
            assertThat(workbook.getSheet("Заказы").getLastRowNum()).isEqualTo(1);
            assertThat(workbook.getSheet("Заказы").getRow(1).getCell(2).getStringCellValue()).isEqualTo("Масляный фильтр; оригинал");
            assertThat(workbook.getSheet("Клиенты").getRow(1).getCell(1).getStringCellValue()).isEqualTo("Иванов Иван Петрович");
        }
    }
}
