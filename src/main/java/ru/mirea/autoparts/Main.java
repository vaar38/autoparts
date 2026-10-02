package ru.mirea.autoparts;

import ru.mirea.autoparts.exception.DatabaseException;
import ru.mirea.autoparts.repository.CustomerRepository;
import ru.mirea.autoparts.repository.OrderRepository;
import ru.mirea.autoparts.service.CustomerService;
import ru.mirea.autoparts.service.OrderService;
import ru.mirea.autoparts.service.StatisticsService;
import ru.mirea.autoparts.ui.ConsoleInput;
import ru.mirea.autoparts.ui.MainMenu;
import ru.mirea.autoparts.util.CsvExporter;
import ru.mirea.autoparts.util.DatabaseManager;
import ru.mirea.autoparts.util.ExcelExporter;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Точка входа. Main только создаёт объекты всех слоёв, связывает их между собой
 * и запускает главное меню. Никакой логики здесь нет.
 */
public class Main {
    public static void main(String[] args) {
        DatabaseManager databaseManager;
        try {
            databaseManager = new DatabaseManager();
            databaseManager.checkConnection();
        } catch (DatabaseException e) {
            System.out.println("Ошибка: " + e.getMessage());
            System.out.println("Проверьте, что PostgreSQL запущен, база autoparts создана "
                    + "и в файле db.properties указаны верные логин и пароль.");
            return;
        }

        // Слой Repository (JDBC)
        CustomerRepository customerRepository = new CustomerRepository(databaseManager);
        OrderRepository orderRepository = new OrderRepository(databaseManager);

        // Слой Service (бизнес-логика)
        CustomerService customerService = new CustomerService(customerRepository, orderRepository);
        OrderService orderService = new OrderService(orderRepository, customerRepository);
        StatisticsService statisticsService = new StatisticsService(customerRepository, orderRepository);

        // Слой Console UI
        ConsoleInput input = new ConsoleInput();
        MainMenu mainMenu = new MainMenu(input, customerService, orderService, statisticsService,
                List.of(new ExcelExporter(), new CsvExporter()));

        try {
            mainMenu.run();
        } catch (NoSuchElementException e) {
            System.out.println();   // ввод закрыт — просто завершаем работу
        }
        System.out.println("Работа завершена. До свидания!");
    }
}
