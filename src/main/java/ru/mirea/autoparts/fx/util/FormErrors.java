package ru.mirea.autoparts.fx.util;

import javafx.beans.Observable;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import ru.mirea.autoparts.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Показ ошибок в форме: текст ошибки под полями и красная рамка у поля, которое заполнено неверно.
 * Используется обеими формами (заказ и клиент), поэтому код не дублируется.
 */
public class FormErrors {
    public static final String ERROR_CLASS = "field-error";

    private final Label messageLabel;
    private final Map<String, Control> fields = new HashMap<>();

    public FormErrors(Label messageLabel) {
        this.messageLabel = messageLabel;
        hideMessage();
    }

    /** Регистрирует поле формы под именем, которое использует сервис в ValidationException. */
    public FormErrors field(String name, Control control) {
        fields.put(name, control);
        // Как только пользователь изменил значение поля, убираем с него красную рамку
        Observable value = control instanceof TextInputControl text ? text.textProperty()
                : control instanceof ComboBoxBase<?> combo ? combo.valueProperty()
                : null;
        if (value != null) {
            value.addListener(obs -> control.getStyleClass().remove(ERROR_CLASS));
        }
        return this;
    }

    public void clear() {
        fields.values().forEach(control -> control.getStyleClass().remove(ERROR_CLASS));
        hideMessage();
    }

    /** Ошибка конкретного поля: подсвечиваем поле и ставим в него курсор. */
    public void show(ValidationException e) {
        Control control = fields.get(e.getField());
        if (control != null) {
            if (!control.getStyleClass().contains(ERROR_CLASS)) {
                control.getStyleClass().add(ERROR_CLASS);
            }
            control.requestFocus();
        }
        showMessage(e.getMessage());
    }

    /** Ошибка, не относящаяся к одному полю (например, нарушено бизнес-правило). */
    public void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        // Метка с ошибкой добавила высоту — увеличиваем окно, чтобы кнопки не ушли за его край
        if (messageLabel.getScene() != null && messageLabel.getScene().getWindow() != null) {
            messageLabel.getScene().getWindow().sizeToScene();
        }
    }

    private void hideMessage() {
        messageLabel.setText("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);   // managed=false — скрытая метка не занимает место в окне
    }
}
