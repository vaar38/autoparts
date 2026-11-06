package ru.mirea.autoparts.util;

import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.model.Order;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Интерфейс экспорта данных. Окно работает с переменной типа DataExporter и не знает,
 * какой именно формат выбран — Excel или CSV. Это и есть полиморфизм.
 */
public interface DataExporter {

    /** Название формата для окна выбора файла, например «Excel (.xlsx)». */
    String getFormatName();

    /** Расширение файла без точки, например «xlsx». */
    String getExtension();

    /** Сохраняет данные в файл, который пользователь выбрал через FileChooser. */
    void export(List<Order> orders, List<Customer> customers, Path file) throws IOException;
}
