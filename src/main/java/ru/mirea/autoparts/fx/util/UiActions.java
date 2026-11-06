package ru.mirea.autoparts.fx.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.autoparts.exception.AppException;
import ru.mirea.autoparts.exception.BusinessException;

/**
 * Единая обработка исключений в обработчиках кнопок.
 * Вместо одинаковых try/catch в каждом контроллере пишем: UiActions.run("Удаление заказа", () -> ...).
 * Пользователь видит понятное сообщение, а подробности (stack trace) уходят только в журнал.
 */
public final class UiActions {
    private static final Logger log = LoggerFactory.getLogger(UiActions.class);

    private UiActions() {
    }

    public static void run(String actionName, Runnable action) {
        try {
            action.run();
        } catch (BusinessException e) {
            // Нарушено бизнес-правило или неверные данные — это ожидаемая ситуация, а не сбой программы
            log.info("{}: {}", actionName, e.getMessage());
            Dialogs.error(actionName + " не выполнено", e.getMessage());
        } catch (AppException e) {
            log.error("{}: {}", actionName, e.getMessage(), e);
            Dialogs.error(actionName + ": ошибка базы данных", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Непредвиденная ошибка: {}", actionName, e);
            Dialogs.error(actionName + ": непредвиденная ошибка",
                    "Операция не выполнена из-за внутренней ошибки программы (" + e.getClass().getSimpleName()
                            + "). Подробности записаны в журнал.");
        }
    }
}
