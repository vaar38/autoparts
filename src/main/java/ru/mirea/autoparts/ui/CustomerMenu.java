package ru.mirea.autoparts.ui;

import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.service.CustomerService;
import ru.mirea.autoparts.service.OrderService;

import java.util.List;

/** Подменю «Клиенты». */
public class CustomerMenu extends BaseMenu {
    private final CustomerService customerService;
    private final OrderService orderService;

    public CustomerMenu(ConsoleInput input, CustomerService customerService, OrderService orderService) {
        super(input);
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @Override
    protected String getTitle() {
        return "КЛИЕНТЫ";
    }

    @Override
    protected List<String> getOptions() {
        return List.of(
                "Добавить клиента",
                "Показать всех клиентов",
                "Найти клиента по ID",
                "Изменить клиента",
                "Удалить клиента",
                "Показать заказы клиента");
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1 -> create();
            case 2 -> TablePrinter.printCustomers(customerService.getAll());
            case 3 -> TablePrinter.printCustomerDetails(customerService.getById(input.readId("Введите ID клиента: ")));
            case 4 -> update();
            case 5 -> delete();
            case 6 -> TablePrinter.printOrders(orderService.filterByCustomer(input.readId("Введите ID клиента: ")));
            default -> throw new IllegalStateException("Неизвестный пункт меню: " + choice);
        }
    }

    private void create() {
        String fullName = input.readRequired("ФИО: ");
        String phone = input.readRequired("Телефон (например +79161234567): ");
        String email = input.readRequired("Email: ");
        String city = input.readRequired("Город: ");
        Customer customer = customerService.create(fullName, phone, email, city);
        System.out.println("Клиент добавлен, ID = " + customer.getId());
    }

    private void update() {
        Customer customer = customerService.getById(input.readId("Введите ID клиента: "));
        System.out.println("Нажмите Enter, чтобы оставить текущее значение.");
        String fullName = input.readOrKeep("ФИО", customer.getFullName());
        String phone = input.readOrKeep("Телефон", customer.getPhone());
        String email = input.readOrKeep("Email", customer.getEmail());
        String city = input.readOrKeep("Город", customer.getCity());
        customerService.update(customer.getId(), fullName, phone, email, city);
        System.out.println("Данные клиента обновлены.");
    }

    private void delete() {
        Customer customer = customerService.getById(input.readId("Введите ID клиента: "));
        if (input.confirm("Удалить клиента «" + customer.getFullName() + "»?")) {
            customerService.delete(customer.getId());
            System.out.println("Клиент удалён.");
        } else {
            System.out.println("Удаление отменено.");
        }
    }
}
