package ru.mirea.autoparts.ui;

import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.service.CustomerService;
import ru.mirea.autoparts.service.OrderService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Подменю «Заказы» — CRUD для основной сущности. */
public class OrderMenu extends BaseMenu {
    private final OrderService orderService;
    private final CustomerService customerService;

    public OrderMenu(ConsoleInput input, OrderService orderService, CustomerService customerService) {
        super(input);
        this.orderService = orderService;
        this.customerService = customerService;
    }

    @Override
    protected String getTitle() {
        return "ЗАКАЗЫ АВТОЗАПЧАСТЕЙ";
    }

    @Override
    protected List<String> getOptions() {
        return List.of(
                "Создать заказ",
                "Показать все заказы",
                "Найти заказ по ID",
                "Изменить заказ",
                "Изменить статус заказа",
                "Удалить заказ");
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1 -> create();
            case 2 -> TablePrinter.printOrders(orderService.getAll());
            case 3 -> TablePrinter.printOrderDetails(orderService.getById(input.readId("Введите ID заказа: ")));
            case 4 -> update();
            case 5 -> changeStatus();
            case 6 -> delete();
            default -> throw new IllegalStateException("Неизвестный пункт меню: " + choice);
        }
    }

    private void create() {
        TablePrinter.printCustomers(customerService.getAll());
        int customerId = input.readId("ID клиента из списка выше: ");
        String partName = input.readRequired("Название запчасти: ");
        String partNumber = input.readRequired("Артикул: ");
        String carBrand = input.readRequired("Марка автомобиля: ");
        PartCategory category = input.choose("Категория запчасти:",
                Arrays.asList(PartCategory.values()), PartCategory::getTitle);
        int quantity = input.readInt("Количество (1–" + OrderService.MAX_QUANTITY + "): ", "количество должно быть целым числом.");
        BigDecimal price = input.readDecimal("Цена за штуку, руб.: ");

        Order order = orderService.create(customerId, partName, partNumber, carBrand, category, quantity, price);
        System.out.println("Заказ создан:");
        TablePrinter.printOrderDetails(order);
    }

    private void update() {
        Order order = orderService.getById(input.readId("Введите ID заказа: "));
        TablePrinter.printOrderDetails(order);
        System.out.println("Нажмите Enter, чтобы оставить текущее значение.");
        int customerId = input.readIntOrKeep("ID клиента", order.getCustomerId());
        String partName = input.readOrKeep("Название запчасти", order.getPartName());
        String partNumber = input.readOrKeep("Артикул", order.getPartNumber());
        String carBrand = input.readOrKeep("Марка автомобиля", order.getCarBrand());
        PartCategory category = order.getCategory();
        if (input.confirm("Изменить категорию (сейчас «" + category.getTitle() + "»)?")) {
            category = input.choose("Категория запчасти:", Arrays.asList(PartCategory.values()), PartCategory::getTitle);
        }
        int quantity = input.readIntOrKeep("Количество", order.getQuantity());
        BigDecimal price = input.readDecimalOrKeep("Цена за штуку", order.getUnitPrice());

        Order updated = orderService.update(order.getId(), customerId, partName, partNumber, carBrand,
                category, quantity, price);
        System.out.println("Заказ обновлён:");
        TablePrinter.printOrderDetails(updated);
    }

    private void changeStatus() {
        Order order = orderService.getById(input.readId("Введите ID заказа: "));
        OrderStatus current = order.getStatus();
        List<OrderStatus> allowed = current.getAllowedNext();
        if (allowed.isEmpty()) {
            System.out.println("Статус «" + current.getTitle() + "» окончательный, изменить его нельзя.");
            return;
        }
        OrderStatus next = input.choose("Текущий статус: «" + current.getTitle() + "». Новый статус:",
                allowed, OrderStatus::getTitle);
        orderService.changeStatus(order.getId(), next);
        System.out.println("Статус заказа #" + order.getId() + " изменён на «" + next.getTitle() + "».");
    }

    private void delete() {
        Order order = orderService.getById(input.readId("Введите ID заказа: "));
        TablePrinter.printOrderDetails(order);
        if (input.confirm("Удалить этот заказ?")) {
            orderService.delete(order.getId());
            System.out.println("Заказ удалён.");
        } else {
            System.out.println("Удаление отменено.");
        }
    }
}
