package ru.mirea.autoparts.util;

import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.model.Order;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Интерфейс экспорта данных. Меню работает с переменной типа DataExporter и не знает,
 * какой именно формат выбран — Excel или CSV. Это и есть полиморфизм.
 */
public interface DataExporter {

    /** Название формата для меню, например «Excel (.xlsx)». */
    String getFormatName();

    /** Сохраняет данные в папку directory и возвращает путь к созданному файлу. */
    Path export(List<Customer> customers, List<Order> orders, Path directory) throws IOException;
}
