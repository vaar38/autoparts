package ru.mirea.autoparts.fx;

/**
 * Запасная точка входа для запуска из IDE кнопкой Run.
 * Если запускать напрямую класс-наследник Application, а JavaFX подключена не как модуль,
 * Java сообщает «JavaFX runtime components are missing». Обычный класс с main обходит эту проверку.
 * Основной способ запуска — mvn javafx:run (он использует AutopartsApp).
 */
public class Launcher {
    public static void main(String[] args) {
        AutopartsApp.main(args);
    }
}
