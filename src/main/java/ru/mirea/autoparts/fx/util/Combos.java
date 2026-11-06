package ru.mirea.autoparts.fx.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

import java.util.Collection;
import java.util.function.Function;

/** Настройка выпадающих списков ComboBox: как показывать элементы и пункт «все». */
public final class Combos {

    private Combos() {
    }

    /**
     * Заполняет ComboBox и задаёт текст элементов.
     * Если nullText не null, первым пунктом добавляется пустое значение (null) с этим текстом —
     * например «Все статусы»: выбор этого пункта означает «фильтр выключен».
     */
    public static <T> void setup(ComboBox<T> combo, Collection<T> items, Function<T, String> toText, String nullText) {
        combo.getItems().clear();
        if (nullText != null) {
            combo.getItems().add(null);
        }
        combo.getItems().addAll(items);
        // cellFactory рисует строки раскрытого списка, buttonCell — выбранное значение в самом поле
        combo.setCellFactory(list -> new TextCell<>(toText, nullText));
        combo.setButtonCell(new TextCell<>(toText, nullText));
    }

    private static class TextCell<T> extends ListCell<T> {
        private final Function<T, String> toText;
        private final String nullText;

        TextCell(Function<T, String> toText, String nullText) {
            this.toText = toText;
            this.nullText = nullText;
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                setText(toText.apply(item));
            } else {
                setText(nullText);
            }
        }
    }
}
