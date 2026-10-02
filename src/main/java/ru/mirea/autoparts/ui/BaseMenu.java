package ru.mirea.autoparts.ui;

import ru.mirea.autoparts.exception.BusinessException;
import ru.mirea.autoparts.exception.DatabaseException;
import ru.mirea.autoparts.exception.EntityNotFoundException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Общая основа для всех меню (абстрактный класс).
 * Метод run() одинаков для всех: показать пункты, прочитать выбор, выполнить действие, повторить.
 * А вот заголовок, пункты и сами действия каждое меню определяет по-своему (полиморфизм).
 */
public abstract class BaseMenu {
    protected final ConsoleInput input;

    protected BaseMenu(ConsoleInput input) {
        this.input = input;
    }

    protected abstract String getTitle();

    /** Пункты меню без нумерации; номера 1, 2, 3... расставляются автоматически. */
    protected abstract List<String> getOptions();

    /** Выполняет пункт меню с номером choice (от 1 до getOptions().size()). */
    protected abstract void handle(int choice);

    /** Подпись пункта 0. В главном меню переопределяется на «Выход». */
    protected String getExitLabel() {
        return "Назад";
    }

    public void run() {
        while (true) {
            printMenu();
            int choice = input.readInt("Выберите действие: ", "введите номер пункта меню.");
            if (choice == 0) {
                return;
            }
            if (choice < 0 || choice > getOptions().size()) {
                ConsoleInput.printError("такого пункта меню нет.");
                continue;
            }
            System.out.println();
            try {
                handle(choice);
            } catch (EntityNotFoundException | BusinessException e) {
                ConsoleInput.printError(e.getMessage());
            } catch (DatabaseException e) {
                ConsoleInput.printError("проблема с базой данных. " + e.getMessage());
            } catch (NoSuchElementException e) {
                throw e;   // ввод закрыт (Ctrl+Z / Ctrl+D) — выходим из программы
            } catch (RuntimeException e) {
                ConsoleInput.printError("непредвиденная ошибка: " + e);
            }
        }
    }

    private void printMenu() {
        String line = "=".repeat(50);
        System.out.println();
        System.out.println(line);
        System.out.println(" " + getTitle());
        System.out.println(line);
        List<String> options = getOptions();
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }
        System.out.println("0. " + getExitLabel());
    }
}
