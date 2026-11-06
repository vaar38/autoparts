package ru.mirea.autoparts.fx.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ru.mirea.autoparts.fx.AppContext;
import ru.mirea.autoparts.fx.util.Combos;
import ru.mirea.autoparts.fx.util.Dialogs;
import ru.mirea.autoparts.fx.util.TableColumns;
import ru.mirea.autoparts.fx.util.UiActions;
import ru.mirea.autoparts.fx.util.ViewLoader;
import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.service.CustomerService;
import ru.mirea.autoparts.service.OrderService;
import ru.mirea.autoparts.service.StatisticsService;
import ru.mirea.autoparts.util.Formats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер вкладки «Клиенты» (CustomersView.fxml).
 * Сверху — список клиентов, снизу — заказы выбранного клиента: так показаны связанные данные
 * (связь «один клиент — много заказов» через внешний ключ orders.customer_id).
 */
public class CustomersController {
    private final AppContext context;
    private final CustomerService customerService;
    private final OrderService orderService;

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final FilteredList<Customer> filteredCustomers = new FilteredList<>(customers);
    private final ObservableList<Order> customerOrders = FXCollections.observableArrayList();

    @FXML private TextField searchField;
    @FXML private ComboBox<String> cityFilter;
    @FXML private TableView<Customer> customersTable;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, String> cityColumn;
    @FXML private TableColumn<Customer, LocalDate> registeredColumn;
    @FXML private Label countLabel;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    @FXML private Label ordersTitle;
    @FXML private Label ordersSummary;
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> orderIdColumn;
    @FXML private TableColumn<Order, LocalDateTime> orderDateColumn;
    @FXML private TableColumn<Order, String> orderPartColumn;
    @FXML private TableColumn<Order, PartCategory> orderCategoryColumn;
    @FXML private TableColumn<Order, Integer> orderQuantityColumn;
    @FXML private TableColumn<Order, BigDecimal> orderTotalColumn;
    @FXML private TableColumn<Order, OrderStatus> orderStatusColumn;

    public CustomersController(AppContext context) {
        this.context = context;
        this.customerService = context.getCustomerService();
        this.orderService = context.getOrderService();
    }

    @FXML
    private void initialize() {
        TableColumns.bind(nameColumn, Customer::getFullName);
        TableColumns.bind(phoneColumn, Customer::getPhone);
        TableColumns.bind(emailColumn, Customer::getEmail);
        TableColumns.bind(cityColumn, Customer::getCity);
        TableColumns.bind(registeredColumn, Customer::getRegisteredAt, Formats::date);

        TableColumns.bind(orderIdColumn, Order::getId);
        TableColumns.bind(orderDateColumn, Order::getCreatedAt, Formats::dateTime);
        TableColumns.bind(orderPartColumn, Order::getPartName);
        TableColumns.bind(orderCategoryColumn, Order::getCategory, PartCategory::getTitle);
        TableColumns.bind(orderQuantityColumn, Order::getQuantity);
        TableColumns.bind(orderTotalColumn, Order::getTotal, Formats::amount);
        TableColumns.bindStatus(orderStatusColumn, Order::getStatus);

        // Стандартная связка для сортировки щелчком по заголовку: компаратор SortedList = компаратор таблицы
        SortedList<Customer> sortedCustomers = new SortedList<>(filteredCustomers);
        sortedCustomers.comparatorProperty().bind(customersTable.comparatorProperty());
        customersTable.setItems(sortedCustomers);
        customersTable.getSortOrder().add(nameColumn);
        customersTable.setPlaceholder(new Label("Клиенты не найдены"));
        ordersTable.setItems(customerOrders);
        ordersTable.setPlaceholder(new Label("Выберите клиента, чтобы увидеть его заказы"));

        searchField.textProperty().addListener(obs -> applyFilter());
        cityFilter.valueProperty().addListener(obs -> applyFilter());

        customersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> showOrders(selected));
        customersTable.setRowFactory(table -> {
            TableRow<Customer> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });

        countLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "Показано клиентов: " + filteredCustomers.size() + " из " + customers.size(),
                filteredCustomers, customers));

        showOrders(null);
        refresh();
    }

    public void refresh() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        refresh(selected == null ? -1 : selected.getId());
    }

    private void refresh(int selectId) {
        UiActions.run("Загрузка клиентов", () -> {
            List<Customer> all = customerService.getAll();
            String city = cityFilter.getValue();
            Combos.setup(cityFilter, customerService.getCities(all), text -> text, "Все города");
            cityFilter.setValue(city != null && all.stream().anyMatch(c -> c.getCity().equals(city)) ? city : null);
            customers.setAll(all);
            customersTable.getItems().stream()
                    .filter(customer -> customer.getId() == selectId)
                    .findFirst()
                    .ifPresentOrElse(customer -> {
                        customersTable.getSelectionModel().select(customer);
                        customersTable.scrollTo(customer);
                    }, () -> customersTable.getSelectionModel().clearSelection());
        });
    }

    private void applyFilter() {
        String text = searchField.getText();
        String city = cityFilter.getValue();
        filteredCustomers.setPredicate(customer -> customerService.matches(customer, text, city));
    }

    /** Связанные данные: загружаем заказы выбранного клиента. */
    private void showOrders(Customer customer) {
        editButton.setDisable(customer == null);
        deleteButton.setDisable(customer == null);
        if (customer == null) {
            customerOrders.clear();
            ordersTitle.setText("Заказы клиента");
            ordersSummary.setText("");
            return;
        }
        UiActions.run("Загрузка заказов клиента", () -> {
            List<Order> orders = orderService.getByCustomer(customer.getId());
            customerOrders.setAll(orders);
            ordersTitle.setText("Заказы клиента: " + customer.getFullName());
            ordersSummary.setText("Всего заказов: " + orders.size()
                    + "   •   на сумму (без отменённых): " + Formats.money(StatisticsService.sumNotCancelled(orders)));
        });
    }

    @FXML
    private void onAdd() {
        UiActions.run("Добавление клиента", () -> openDialog(null).ifPresent(created -> refresh(created.getId())));
    }

    @FXML
    private void onEdit() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        UiActions.run("Изменение клиента", () -> openDialog(selected).ifPresent(updated -> refresh(updated.getId())));
    }

    @FXML
    private void onDelete() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        boolean confirmed = Dialogs.confirm("Удалить клиента?",
                selected.getFullName() + ", тел. " + selected.getPhone()
                        + "\n\nКлиента, у которого есть заказы, удалить нельзя.\nУдаление нельзя отменить.",
                "Удалить");
        if (confirmed) {
            UiActions.run("Удаление клиента", () -> {
                customerService.delete(selected.getId());
                refresh(-1);
            });
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        cityFilter.setValue(null);
    }

    private Optional<Customer> openDialog(Customer customer) {
        ViewLoader.View<DialogPane, CustomerDialogController> view = ViewLoader.load("CustomerDialog.fxml", context);
        return view.controller().showAndWait(customer);
    }
}
