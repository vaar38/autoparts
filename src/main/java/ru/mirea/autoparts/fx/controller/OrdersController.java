package ru.mirea.autoparts.fx.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.autoparts.exception.BusinessException;
import ru.mirea.autoparts.exception.ValidationException;
import ru.mirea.autoparts.fx.AppContext;
import ru.mirea.autoparts.fx.util.Combos;
import ru.mirea.autoparts.fx.util.Dialogs;
import ru.mirea.autoparts.fx.util.FormErrors;
import ru.mirea.autoparts.fx.util.TableColumns;
import ru.mirea.autoparts.fx.util.UiActions;
import ru.mirea.autoparts.fx.util.ViewLoader;
import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.service.CustomerService;
import ru.mirea.autoparts.service.OrderFilter;
import ru.mirea.autoparts.service.OrderService;
import ru.mirea.autoparts.service.OrderSort;
import ru.mirea.autoparts.util.DataExporter;
import ru.mirea.autoparts.util.Formats;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер вкладки «Заказы» (разметка OrdersView.fxml).
 * Получает действия пользователя и вызывает сервисы. SQL здесь нет — он в слое Repository.
 *
 * Цепочка коллекций для таблицы:
 *   orders (ObservableList, все заказы из БД)
 *     -> filteredOrders (FilteredList: только подходящие под поиск и фильтры)
 *       -> sortedOrders (SortedList: в нужном порядке) -> TableView.
 * Когда меняется исходный список или условие, JavaFX сам пересчитывает цепочку и перерисовывает таблицу.
 */
public class OrdersController {
    private static final Logger log = LoggerFactory.getLogger(OrdersController.class);

    private final AppContext context;
    private final OrderService orderService;
    private final CustomerService customerService;

    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final FilteredList<Order> filteredOrders = new FilteredList<>(orders);
    private final SortedList<Order> sortedOrders = new SortedList<>(filteredOrders);

    // ---- Поиск, фильтры, сортировка
    @FXML private TextField searchField;
    @FXML private ComboBox<OrderStatus> statusFilter;
    @FXML private ComboBox<PartCategory> categoryFilter;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;
    @FXML private ComboBox<OrderSort> sortCombo;
    @FXML private Label filterErrorLabel;

    // ---- Таблица
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> idColumn;
    @FXML private TableColumn<Order, LocalDateTime> dateColumn;
    @FXML private TableColumn<Order, String> customerColumn;
    @FXML private TableColumn<Order, String> partNameColumn;
    @FXML private TableColumn<Order, String> partNumberColumn;
    @FXML private TableColumn<Order, String> carBrandColumn;
    @FXML private TableColumn<Order, PartCategory> categoryColumn;
    @FXML private TableColumn<Order, Integer> quantityColumn;
    @FXML private TableColumn<Order, BigDecimal> priceColumn;
    @FXML private TableColumn<Order, BigDecimal> totalColumn;
    @FXML private TableColumn<Order, OrderStatus> statusColumn;
    @FXML private Label countLabel;

    // ---- Кнопки, которые зависят от выбранной строки
    @FXML private Button editButton;
    @FXML private Button statusButton;
    @FXML private Button deleteButton;

    // ---- Панель «Сведения о заказе»
    @FXML private Label detailsPlaceholder;
    @FXML private VBox detailsBox;
    @FXML private Label detailsTitle;
    @FXML private Label detailsStatus;
    @FXML private Label detailsCreated;
    @FXML private Label detailsCustomer;
    @FXML private Label detailsPhone;
    @FXML private Label detailsEmail;
    @FXML private Label detailsCity;
    @FXML private Label detailsPart;
    @FXML private Label detailsNumber;
    @FXML private Label detailsBrand;
    @FXML private Label detailsCategory;
    @FXML private Label detailsQuantity;
    @FXML private Label detailsPrice;
    @FXML private Label detailsTotal;
    @FXML private Label detailsNextStatus;

    /** Клиенты по id — чтобы показать в сведениях о заказе телефон и email клиента (связанные данные). */
    private Map<Integer, Customer> customersById = Map.of();

    public OrdersController(AppContext context) {
        this.context = context;
        this.orderService = context.getOrderService();
        this.customerService = context.getCustomerService();
    }

    /** Вызывается FXMLLoader-ом автоматически после того, как все поля @FXML заполнены. */
    @FXML
    private void initialize() {
        setupColumns();
        setupFilters();
        setupSorting();

        ordersTable.setItems(sortedOrders);
        ordersTable.setPlaceholder(new Label("Заказы не найдены. Измените условия поиска или нажмите «Сбросить»."));

        // Выбор строки: обновляем панель сведений и доступность кнопок
        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> showDetails(selected));

        // Двойной щелчок по строке — редактирование, клавиша Delete — удаление
        ordersTable.setRowFactory(table -> {
            TableRow<Order> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });
        ordersTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                onDelete();
            }
        });

        // Количество строк пересчитывается при любом изменении отфильтрованного списка
        countLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "Показано заказов: " + filteredOrders.size() + " из " + orders.size(),
                filteredOrders, orders));

        showDetails(null);
        refresh();
    }

    private void setupColumns() {
        TableColumns.bind(idColumn, Order::getId);
        TableColumns.bind(dateColumn, Order::getCreatedAt, dateTime -> Formats.date(dateTime.toLocalDate()));
        TableColumns.bind(customerColumn, Order::getCustomerName);
        TableColumns.bind(partNameColumn, Order::getPartName);
        TableColumns.bind(partNumberColumn, Order::getPartNumber);
        TableColumns.bind(carBrandColumn, Order::getCarBrand);
        TableColumns.bind(categoryColumn, Order::getCategory, PartCategory::getTitle);
        TableColumns.bind(quantityColumn, Order::getQuantity);
        TableColumns.bind(priceColumn, Order::getUnitPrice, Formats::amount);
        TableColumns.bind(totalColumn, Order::getTotal, Formats::amount);
        TableColumns.bindStatus(statusColumn, Order::getStatus);
        // Категория и статус сортируются по русскому названию, а не по порядку констант enum
        categoryColumn.setComparator(Comparator.comparing(PartCategory::getTitle));
        statusColumn.setComparator(Comparator.comparing(OrderStatus::getTitle));
    }

    private void setupFilters() {
        Combos.setup(statusFilter, List.of(OrderStatus.values()), OrderStatus::getTitle, "Все статусы");
        Combos.setup(categoryFilter, List.of(PartCategory.values()), PartCategory::getTitle, "Все категории");
        statusFilter.setValue(null);
        categoryFilter.setValue(null);

        // Любое изменение условий сразу применяет фильтр — отдельная кнопка «Найти» не нужна
        searchField.textProperty().addListener(obs -> applyFilter());
        statusFilter.valueProperty().addListener(obs -> applyFilter());
        categoryFilter.valueProperty().addListener(obs -> applyFilter());
        dateFromPicker.valueProperty().addListener(obs -> applyFilter());
        dateToPicker.valueProperty().addListener(obs -> applyFilter());
        hideFilterError();
    }

    /**
     * Сортировка двумя способами:
     * 1) щелчок по заголовку любого столбца (встроенная сортировка TableView, Shift+щелчок — по нескольким столбцам);
     * 2) готовые варианты из списка «Сортировка» (OrderSort: по дате, сумме, клиенту, названию).
     * TableView сам строки не переставляет: он только сообщает свой компаратор (comparatorProperty),
     * а переставляет строки SortedList, компаратор которого мы привязываем ниже.
     */
    private void setupSorting() {
        Combos.setup(sortCombo, List.of(OrderSort.values()), OrderSort::getTitle, "По столбцу таблицы");
        sortedOrders.comparatorProperty().bind(Bindings.createObjectBinding(() -> {
            if (!ordersTable.getSortOrder().isEmpty()) {
                return ordersTable.getComparator();
            }
            OrderSort sort = sortCombo.getValue();
            return sort == null ? null : sort.getComparator();
        }, ordersTable.comparatorProperty(), ordersTable.getSortOrder(), sortCombo.valueProperty()));
        // Сортировку выполняет SortedList, поэтому таблице достаточно сказать «готово»
        ordersTable.setSortPolicy(table -> true);

        // Выбрали вариант в списке — убираем сортировку по столбцам, и наоборот
        sortCombo.valueProperty().addListener((obs, old, sort) -> {
            if (sort != null) {
                ordersTable.getSortOrder().clear();
            }
        });
        ordersTable.getSortOrder().addListener((ListChangeListener<TableColumn<Order, ?>>) change -> {
            if (!ordersTable.getSortOrder().isEmpty()) {
                sortCombo.setValue(null);
            }
        });
        sortCombo.setValue(OrderSort.BY_DATE_NEWEST);
    }

    // ------------------------------------------------------------------ Загрузка и фильтрация

    /** Перечитывает данные из базы, сохраняя выбранную строку. Вызывается кнопкой «Обновить» и после изменений. */
    public void refresh() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        refresh(selected == null ? -1 : selected.getId());
    }

    private void refresh(int selectId) {
        UiActions.run("Загрузка заказов", () -> {
            customersById = customerService.getAll().stream()
                    .collect(Collectors.toMap(Customer::getId, customer -> customer));
            // setAll заменяет содержимое ObservableList — таблица узнаёт об изменении и перерисовывается сама
            orders.setAll(orderService.getAll());
            select(selectId);
        });
    }

    private void select(int orderId) {
        sortedOrders.stream()
                .filter(order -> order.getId() == orderId)
                .findFirst()
                .ifPresentOrElse(order -> {
                    ordersTable.getSelectionModel().select(order);
                    ordersTable.scrollTo(order);
                }, () -> ordersTable.getSelectionModel().clearSelection());
    }

    private void applyFilter() {
        try {
            OrderFilter filter = new OrderFilter(searchField.getText(), statusFilter.getValue(),
                    categoryFilter.getValue(), dateFromPicker.getValue(), dateToPicker.getValue());
            // Predicate — условие «показывать строку или нет»; FilteredList проверяет им каждый заказ
            filteredOrders.setPredicate(filter::matches);
            hideFilterError();
        } catch (ValidationException e) {
            filterErrorLabel.setText(e.getMessage());
            filterErrorLabel.setVisible(true);
            filterErrorLabel.setManaged(true);
            dateFromPicker.getStyleClass().add(FormErrors.ERROR_CLASS);
            dateToPicker.getStyleClass().add(FormErrors.ERROR_CLASS);
        }
    }

    private void hideFilterError() {
        filterErrorLabel.setVisible(false);
        filterErrorLabel.setManaged(false);
        dateFromPicker.getStyleClass().removeAll(FormErrors.ERROR_CLASS);
        dateToPicker.getStyleClass().removeAll(FormErrors.ERROR_CLASS);
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        statusFilter.setValue(null);
        categoryFilter.setValue(null);
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        sortCombo.setValue(OrderSort.BY_DATE_NEWEST);
    }

    /** Быстрый период: заказы за последние 30 дней. */
    @FXML
    private void onLastMonth() {
        dateToPicker.setValue(LocalDate.now());
        dateFromPicker.setValue(LocalDate.now().minusDays(30));
    }

    // ------------------------------------------------------------------ CRUD

    @FXML
    private void onAdd() {
        UiActions.run("Добавление заказа", () -> openOrderDialog(null).ifPresent(created -> refresh(created.getId())));
    }

    @FXML
    private void onEdit() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        UiActions.run("Изменение заказа", () -> {
            orderService.ensureEditable(selected);   // бизнес-правило проверяется до открытия формы
            openOrderDialog(selected).ifPresent(updated -> refresh(updated.getId()));
        });
    }

    @FXML
    private void onChangeStatus() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        List<OrderStatus> allowed = selected.getStatus().getAllowedNext();
        if (allowed.isEmpty()) {
            Dialogs.info("Статус изменить нельзя", "Заказ №" + selected.getId() + " в статусе «"
                    + selected.getStatus().getTitle() + "» — это финальный статус.");
            return;
        }
        List<String> titles = allowed.stream().map(OrderStatus::getTitle).toList();
        Dialogs.choose("Заказ №" + selected.getId() + ": сейчас «" + selected.getStatus().getTitle() + "»",
                        "Новый статус:", titles)
                .map(title -> allowed.get(titles.indexOf(title)))
                .ifPresent(newStatus -> UiActions.run("Смена статуса", () -> {
                    orderService.changeStatus(selected.getId(), newStatus);
                    refresh(selected.getId());
                }));
    }

    @FXML
    private void onDelete() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        boolean confirmed = Dialogs.confirm("Удалить заказ №" + selected.getId() + "?",
                selected.getPartName() + " (" + selected.getPartNumber() + "), клиент: " + selected.getCustomerName()
                        + "\nСумма: " + Formats.money(selected.getTotal())
                        + "\n\nУдаление нельзя отменить.", "Удалить");
        if (confirmed) {
            UiActions.run("Удаление заказа", () -> {
                orderService.delete(selected.getId());
                refresh(-1);
            });
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    /** Открывает форму заказа (OrderDialog.fxml). order == null — новый заказ. */
    private Optional<Order> openOrderDialog(Order order) {
        ViewLoader.View<DialogPane, OrderDialogController> view = ViewLoader.load("OrderDialog.fxml", context);
        return view.controller().showAndWait(order);
    }

    // ------------------------------------------------------------------ Экспорт

    /** Экспорт ТЕКУЩЕГО набора: только отфильтрованные заказы и в том порядке, в каком они видны в таблице. */
    @FXML
    public void onExport() {
        List<Order> visible = new ArrayList<>(sortedOrders);
        if (visible.isEmpty()) {
            Dialogs.info("Нечего экспортировать", "В таблице нет заказов. Измените условия поиска.");
            return;
        }
        List<DataExporter> exporters = context.getExporters();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт заказов");
        for (DataExporter exporter : exporters) {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(exporter.getFormatName(), "*." + exporter.getExtension()));
        }
        chooser.setInitialFileName("orders_" + LocalDate.now());
        // Путь выбирает пользователь; null — значит он нажал «Отмена»
        File file = chooser.showSaveDialog(ordersTable.getScene().getWindow());
        if (file == null) {
            return;
        }
        DataExporter exporter = exporters.get(chooser.getExtensionFilters().indexOf(chooser.getSelectedExtensionFilter()));
        String extension = "." + exporter.getExtension();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(extension)) {
            file = new File(file.getParentFile(), file.getName() + extension);
        }
        File target = file;
        UiActions.run("Экспорт", () -> {
            try {
                exporter.export(visible, customerService.getAll(), target.toPath());
            } catch (IOException e) {
                log.error("Ошибка записи файла {}", target, e);
                throw new BusinessException("Не удалось сохранить файл " + target.getName()
                        + ": " + e.getMessage() + ". Возможно, файл открыт в другой программе.");
            }
            Dialogs.info("Экспорт завершён", "Сохранено заказов: " + visible.size() + "\nФайл: " + target.getAbsolutePath());
        });
    }

    // ------------------------------------------------------------------ Сведения о выбранном заказе

    private void showDetails(Order order) {
        boolean selected = order != null;
        editButton.setDisable(!selected);
        statusButton.setDisable(!selected);
        deleteButton.setDisable(!selected);
        detailsBox.setVisible(selected);
        detailsBox.setManaged(selected);
        detailsPlaceholder.setVisible(!selected);
        detailsPlaceholder.setManaged(!selected);
        if (!selected) {
            return;
        }
        detailsTitle.setText("Заказ №" + order.getId());
        detailsStatus.setText(order.getStatus().getTitle());
        detailsStatus.getStyleClass().removeIf(styleClass -> styleClass.startsWith("status-"));
        detailsStatus.getStyleClass().add("status-" + order.getStatus().name().toLowerCase(Locale.ROOT));
        detailsCreated.setText(Formats.dateTime(order.getCreatedAt()));

        Customer customer = customersById.get(order.getCustomerId());
        detailsCustomer.setText(order.getCustomerName());
        detailsPhone.setText(customer == null ? "—" : customer.getPhone());
        detailsEmail.setText(customer == null ? "—" : customer.getEmail());
        detailsCity.setText(customer == null ? "—" : customer.getCity());

        detailsPart.setText(order.getPartName());
        detailsNumber.setText(order.getPartNumber());
        detailsBrand.setText(order.getCarBrand());
        detailsCategory.setText(order.getCategory().getTitle());
        detailsQuantity.setText(order.getQuantity() + " шт.");
        detailsPrice.setText(Formats.money(order.getUnitPrice()));
        detailsTotal.setText(Formats.money(order.getTotal()));

        List<OrderStatus> next = order.getStatus().getAllowedNext();
        detailsNextStatus.setText(next.isEmpty() ? "нет (финальный статус)"
                : next.stream().map(OrderStatus::getTitle).collect(Collectors.joining(", ")));
    }
}
