package ru.mirea.autoparts.fx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * Все стандартные диалоговые окна приложения в одном месте: ошибка, информация, подтверждение, выбор.
 * Контроллеры вызывают эти методы и не повторяют код настройки Alert.
 */
public final class Dialogs {
    private static Window owner;

    private Dialogs() {
    }

    /** Главное окно — диалоги открываются поверх него и блокируют его, пока не закрыты. */
    public static void setOwner(Window window) {
        owner = window;
    }

    public static void error(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, new ButtonType("Закрыть", ButtonBar.ButtonData.OK_DONE));
        prepare(alert, "Ошибка", header);
        alert.showAndWait();
    }

    public static void info(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        prepare(alert, "Готово", header);
        alert.showAndWait();
    }

    /** Подтверждение опасного действия. Возвращает true, только если пользователь нажал кнопку действия. */
    public static boolean confirm(String header, String message, String actionText) {
        ButtonType action = new ButtonType(actionText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, action, cancel);
        prepare(alert, "Подтверждение", header);
        // По умолчанию фокус на «Отмена», чтобы случайное нажатие Enter ничего не удалило
        ((Button) alert.getDialogPane().lookupButton(action)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        return alert.showAndWait().filter(action::equals).isPresent();
    }

    /** Выбор одного значения из списка (например, нового статуса заказа). */
    public static <T> Optional<T> choose(String header, String label, List<T> options) {
        ChoiceDialog<T> dialog = new ChoiceDialog<>(options.get(0), options);
        prepare(dialog, "Выбор", header);
        dialog.setContentText(label);
        return dialog.showAndWait();
    }

    /** Общая настройка любого диалога: заголовок, владелец и наш CSS. */
    public static void prepare(Dialog<?> dialog, String title, String header) {
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.getDialogPane().getStylesheets().add(ViewLoader.stylesheet());
        dialog.setResizable(true);
    }
}
