package ru.mirea.autoparts.fx.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.autoparts.exception.AppException;
import ru.mirea.autoparts.exception.ValidationException;
import ru.mirea.autoparts.fx.AppContext;
import ru.mirea.autoparts.fx.util.Dialogs;
import ru.mirea.autoparts.fx.util.FormErrors;
import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.service.CustomerService;

import java.time.LocalDate;
import java.util.Optional;

/** Контроллер формы клиента (CustomerDialog.fxml) — добавление и редактирование. */
public class CustomerDialogController {
    private final CustomerService customerService;

    @FXML private DialogPane dialogPane;
    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField cityField;
    @FXML private DatePicker registeredAtPicker;
    @FXML private Label errorLabel;

    private FormErrors errors;
    private Customer editedCustomer;   // null — новый клиент
    private Customer savedCustomer;

    public CustomerDialogController(AppContext context) {
        this.customerService = context.getCustomerService();
    }

    @FXML
    private void initialize() {
        errors = new FormErrors(errorLabel)
                .field(CustomerService.FIELD_FULL_NAME, fullNameField)
                .field(CustomerService.FIELD_PHONE, phoneField)
                .field(CustomerService.FIELD_EMAIL, emailField)
                .field(CustomerService.FIELD_CITY, cityField)
                .field(CustomerService.FIELD_REGISTERED_AT, registeredAtPicker);

        // В календаре нельзя выбрать дату в будущем (сервис всё равно проверит это ещё раз)
        registeredAtPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
    }

    public Optional<Customer> showAndWait(Customer customer) {
        this.editedCustomer = customer;
        boolean isNew = customer == null;
        if (isNew) {
            registeredAtPicker.setValue(LocalDate.now());
        } else {
            fullNameField.setText(customer.getFullName());
            phoneField.setText(customer.getPhone());
            emailField.setText(customer.getEmail());
            cityField.setText(customer.getCity());
            registeredAtPicker.setValue(customer.getRegisteredAt());
        }

        ButtonType save = new ButtonType(isNew ? "Добавить" : "Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().setAll(save, cancel);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(dialogPane);
        Dialogs.prepare(dialog, isNew ? "Новый клиент" : "Изменение клиента",
                isNew ? "Новый клиент магазина" : customer.getFullName());

        Node saveButton = dialogPane.lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!trySave()) {
                event.consume();   // ошибка — окно не закрываем
            }
        });

        dialog.setOnShown(event -> fullNameField.requestFocus());
        dialog.showAndWait();
        return Optional.ofNullable(savedCustomer);
    }

    private boolean trySave() {
        errors.clear();
        try {
            savedCustomer = editedCustomer == null
                    ? customerService.create(fullNameField.getText(), phoneField.getText(), emailField.getText(),
                            cityField.getText(), registeredAtPicker.getValue())
                    : customerService.update(editedCustomer.getId(), fullNameField.getText(), phoneField.getText(),
                            emailField.getText(), cityField.getText(), registeredAtPicker.getValue());
            return true;
        } catch (ValidationException e) {
            errors.show(e);
            return false;
        } catch (AppException e) {
            errors.showMessage(e.getMessage());
            return false;
        }
    }
}
