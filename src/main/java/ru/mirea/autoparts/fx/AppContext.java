package ru.mirea.autoparts.fx;

import ru.mirea.autoparts.repository.CustomerRepository;
import ru.mirea.autoparts.repository.OrderRepository;
import ru.mirea.autoparts.service.CustomerService;
import ru.mirea.autoparts.service.OrderService;
import ru.mirea.autoparts.service.StatisticsService;
import ru.mirea.autoparts.util.CsvExporter;
import ru.mirea.autoparts.util.DataExporter;
import ru.mirea.autoparts.util.DatabaseManager;
import ru.mirea.autoparts.util.ExcelExporter;

import java.util.List;

/**
 * «Сборщик» приложения: один раз создаёт объекты всех слоёв и связывает их между собой
 * (Repository получает DatabaseManager, Service получает Repository).
 * Контроллеры получают AppContext в конструкторе и берут из него нужные сервисы.
 */
public class AppContext {
    private final DatabaseManager databaseManager;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final StatisticsService statisticsService;
    private final List<DataExporter> exporters;

    public AppContext(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;

        // Слой Repository / DAO (JDBC)
        CustomerRepository customerRepository = new CustomerRepository(databaseManager);
        OrderRepository orderRepository = new OrderRepository(databaseManager);

        // Слой Service (бизнес-логика)
        this.customerService = new CustomerService(customerRepository, orderRepository);
        this.orderService = new OrderService(orderRepository, customerRepository);
        this.statisticsService = new StatisticsService(customerRepository, orderRepository);

        this.exporters = List.of(new ExcelExporter(), new CsvExporter());
    }

    public String getDatabaseUrl() {
        return databaseManager.getUrl();
    }

    public CustomerService getCustomerService() {
        return customerService;
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public StatisticsService getStatisticsService() {
        return statisticsService;
    }

    public List<DataExporter> getExporters() {
        return exporters;
    }
}
