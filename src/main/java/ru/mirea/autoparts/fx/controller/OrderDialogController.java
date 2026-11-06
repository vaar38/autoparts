package ru.mirea.autoparts.fx.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.autoparts.exception.AppException;
import ru.mirea.autoparts.exception.ValidationException;
import ru.mirea.autoparts.fx.AppContext;
import ru.mirea.autoparts.fx.util.Combos;
import ru.mirea.autoparts.fx.util.Dialogs;
import ru.mirea.autoparts.fx.util.FormErrors;
import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.service.CustomerService;
import ru.mirea.autoparts.service.OrderService;
import ru.mirea.autoparts.service.Validation;
import ru.mirea.autoparts.util.Formats;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер формы заказа (OrderDialog.fxml) — одна форма и для добавления, и для редактирования.
 * Клиент выбирается из выпадающего списка (ComboBox), а не вводом ID: так пользователь не ошибётся
 * в номере, а внешний ключ orders.customer_id всегда указывает на существующего клиента.
 */
public class OrderDialogController {
    private final OrderService orderService;
    private final CustomerService customerService;

    @FXML private DialogPane dialogPane;
    @FXML private ComboBox<Customer> customerCombo;
    @FXML private TextField partNameField;
    @FXML private TextField partNumberField;
    @FXML private TextField carBrandField;
    @FXML private ComboBox<PartCategory> categoryCombo;
    @FXML private TextField quantityField;
    @FXML private TextField priceField;
    @FXML private Label totalLabel;
    @FXML private Label errorLabel;

    private FormErrors errors;
    private Order editedOrder;   // null — создаём новый заказ
    private Order savedOrder;    // результат успешного сохранения

    public OrderDialogController(AppContext context) {
        this.orderService = context.getOrderService();
        this.customerService = context.getCustomerService();
    }

    @FXML
    private void initialize() {
        Combos.setup(customerCombo, customerService.getAll(), Customer::toString, null);
        Combos.setup(categoryCombo, List.of(PartCategory.values()), PartCategory::getTitle, null);

        // Имена полей совпадают с теми, что сервис указывает в ValidationException
        errors = new FormErrors(errorLabel)
                .field(OrderService.FIELD_CUSTOMER, customerCombo)
                .field(OrderService.FIELD_PART_NAME, partNameField)
                .field(OrderService.FIELD_PART_NUMBER, partNumberField)
                .field(OrderService.FIELD_CAR_BRAND, carBrandField)
                .field(OrderService.FIELD_CATEGORY, categoryCombo)
                .field(OrderService.FIELD_QUANTITY, quantityField)
                .field(OrderService.FIELD_UNIT_PRICE, priceField);

        // Сумма заказа пересчитывается прямо при вводе количества и цены
        quantityField.textProperty().addListener(obs -> updateTotal());
        priceField.textProperty().addListener(obs -> updateTotal());
    }

    /** Показывает форму и ждёт, пока пользователь её закроет. Возвращает сохранённый заказ или пустой Optional. */
    public Optional<Order> showAndWait(Order order) {
        this.editedOrder = order;
        boolean isNew = order == null;
        if (isNew) {
            quantityField.setText("1");
        } else {
            fillForm(order);
        }
        updateTotal();

        ButtonType save = new ButtonType(isNew ? "Добавить" : "Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().setAll(save, cancel);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(dialogPane);
        Dialogs.prepare(dialog, isNew ? "Новый заказ" : "Изменение заказа",
                isNew ? "Новый заказ автозапчасти" : "Заказ №" + order.getId() + " (статус «" + order.getStatus().getTitle() + "»)");

        // Фильтр события кнопки «Сохранить»: если данные неверны, consume() отменяет закрытие окна,
        // и пользователь видит ошибку, не теряя введённые данные
        Node saveButton = dialogPane.lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!trySave()) {
                event.consume();
            }
        });

        dialog.setOnShown(event -> customerCombo.requestFocus());
        dialog.showAndWait();
        return Optional.ofNullable(savedOrder);
    }

    private void fillForm(Order order) {
        customerCombo.getItems().stream()
                .filter(customer -> customer != null && customer.getId() == order.getCustomerId())
                .findFirst()
                .ifPresent(customerCombo::setValue);
        partNameField.setText(order.getPartName());
        partNumberField.setText(order.getPartNumber());
        carBrandField.setText(order.getCarBrand());
        categoryCombo.setValue(order.getCategory());
        quantityField.setText(String.valueOf(order.getQuantity()));
        priceField.setText(order.getUnitPrice().toPlainString());
    }

    /**
     * Проверка и сохранение. Сначала разбираем числа из текстовых полей, затем сервис проверяет
     * все правила (обязательность, длину, диапазоны, существование клиента) и пишет в базу.
     */
    private boolean trySave() {
        errors.clear();
        try {
            Customer customer = customerCombo.getValue();
            int customerId = customer == null ? 0 : customer.getId();
            int quantity = Validation.parseInt(quantityField.getText(), OrderService.FIELD_QUANTITY,
                    "Количество", 1, OrderService.MAX_QUANTITY);
            BigDecimal price = Validation.parseMoney(priceField.getText(), OrderService.FIELD_UNIT_PRICE, "Цена за штуку");

            savedOrder = editedOrder == null
                    ? orderService.create(customerId, partNameField.getText(), partNumberField.getText(),
                            carBrandField.getText(), categoryCombo.getValue(), quantity, price)
                    : orderService.update(editedOrder.getId(), customerId, partNameField.getText(),
                            partNumberField.getText(), carBrandField.getText(), categoryCombo.getValue(), quantity, price);
            return true;
        } catch (ValidationException e) {
            errors.show(e);
            return false;
        } catch (AppException e) {
            errors.showMessage(e.getMessage());
            return false;
        }
    }

    private void updateTotal() {
        try {
            int quantity = Validation.parseInt(quantityField.getText(), OrderService.FIELD_QUANTITY,
                    "Количество", 1, OrderService.MAX_QUANTITY);
            BigDecimal price = Validation.parseMoney(priceField.getText(), OrderService.FIELD_UNIT_PRICE, "Цена");
            totalLabel.setText(Formats.money(price.multiply(BigDecimal.valueOf(quantity))));
        } catch (ValidationException e) {
            // Поля ещё не заполнены до конца — сумму пока не показываем; ошибку покажем только при сохранении
            totalLabel.setText("—");
        }
    }
}
