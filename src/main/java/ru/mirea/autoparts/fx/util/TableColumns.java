package ru.mirea.autoparts.fx.util;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import ru.mirea.autoparts.model.OrderStatus;

import java.util.Locale;
import java.util.function.Function;

/**
 * Настройка столбцов TableView, общая для всех таблиц приложения.
 *
 * cellValueFactory — ОТКУДА столбец берёт значение (например, Order::getTotal);
 * cellFactory — КАК значение рисуется в ячейке (например, «12 500,00 руб.»).
 * Сортировка по столбцу идёт по самому значению (числу, дате), а не по тексту на экране.
 */
public final class TableColumns {

    private TableColumns() {
    }

    /** Связывает столбец с геттером модели. */
    public static <S, T> void bind(TableColumn<S, T> column, Function<S, T> getter) {
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue())));
    }

    /** Связывает столбец с геттером и задаёт, как превратить значение в текст ячейки. */
    public static <S, T> void bind(TableColumn<S, T> column, Function<S, T> getter, Function<T, String> formatter) {
        bind(column, getter);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.apply(item));
            }
        });
    }

    /** Столбец статуса заказа: цветная «плашка», цвет задаётся в CSS классом status-new, status-shipped и т.д. */
    public static <S> void bindStatus(TableColumn<S, OrderStatus> column, Function<S, OrderStatus> getter) {
        bind(column, getter);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(OrderStatus status, boolean empty) {
                super.updateItem(status, empty);
                // Ячейки переиспользуются при прокрутке, поэтому старый класс статуса нужно убрать
                getStyleClass().removeIf(styleClass -> styleClass.startsWith("status-"));
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status.getTitle());
                    getStyleClass().add("status-" + status.name().toLowerCase(Locale.ROOT));
                }
            }
        });
    }
}
