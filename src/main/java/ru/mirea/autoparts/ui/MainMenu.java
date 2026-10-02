package ru.mirea.autoparts.ui;

import ru.mirea.autoparts.service.CustomerService;
import ru.mirea.autoparts.service.OrderService;
import ru.mirea.autoparts.service.StatisticsService;
import ru.mirea.autoparts.util.DataExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Главное меню программы. */
public class MainMenu extends BaseMenu {
    private static final Path EXPORT_DIRECTORY = Path.of("exports");

    private final CustomerService customerService;
    private final OrderService orderService;
    private final StatisticsService statisticsService;
    private final List<DataExporter> exporters;

    private final CustomerMenu customerMenu;
    private final OrderMenu orderMenu;
    private final SearchMenu searchMenu;
    private final FilterMenu filterMenu;
    private final SortMenu sortMenu;

    public MainMenu(ConsoleInput input, CustomerService customerService, OrderService orderService,
                    StatisticsService statisticsService, List<DataExporter> exporters) {
        super(input);
        this.customerService = customerService;
        this.orderService = orderService;
        this.statisticsService = statisticsService;
        this.exporters = exporters;
        this.customerMenu = new CustomerMenu(input, customerService, orderService);
        this.orderMenu = new OrderMenu(input, orderService, customerService);
        this.searchMenu = new SearchMenu(input, orderService);
        this.filterMenu = new FilterMenu(input, orderService);
        this.sortMenu = new SortMenu(input, orderService);
    }

    @Override
    protected String getTitle() {
        return "МАГАЗИН АВТОЗАПЧАСТЕЙ — ЗАКАЗЫ";
    }

    @Override
    protected List<String> getOptions() {
        return List.of(
                "Клиенты",
                "Заказы",
                "Поиск",
                "Фильтрация",
                "Сортировка",
                "Статистика",
                "Экспорт данных",
                "Вывести таблицы базы данных");
    }

    @Override
    protected String getExitLabel() {
        return "Выход";
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1 -> customerMenu.run();
            case 2 -> orderMenu.run();
            case 3 -> searchMenu.run();
            case 4 -> filterMenu.run();
            case 5 -> sortMenu.run();
            case 6 -> showStatistics();
            case 7 -> exportData();
            case 8 -> showTables();
            default -> throw new IllegalStateException("Неизвестный пункт меню: " + choice);
        }
    }

    private void showStatistics() {
        System.out.println("СТАТИСТИКА СИСТЕМЫ");
        Map<String, String> stats = statisticsService.collect();
        for (Map.Entry<String, String> entry : stats.entrySet()) {
            System.out.printf("%-40s %s%n", entry.getKey() + ":", entry.getValue());
        }
    }

    private void exportData() {
        // Полиморфизм: exporter может быть ExcelExporter или CsvExporter,
        // а вызов exporter.export(...) выглядит одинаково.
        DataExporter exporter = input.choose("Формат экспорта:", exporters, DataExporter::getFormatName);
        try {
            Path file = exporter.export(customerService.getAll(), orderService.getAll(), EXPORT_DIRECTORY);
            System.out.println("Данные экспортированы в файл: " + file.toAbsolutePath());
        } catch (IOException e) {
            ConsoleInput.printError("не удалось сохранить файл: " + e.getMessage());
        }
    }

    private void showTables() {
        System.out.println("ТАБЛИЦА customers");
        TablePrinter.printCustomers(customerService.getAll());
        System.out.println();
        System.out.println("ТАБЛИЦА orders");
        TablePrinter.printOrders(orderService.getAll());
    }
}
