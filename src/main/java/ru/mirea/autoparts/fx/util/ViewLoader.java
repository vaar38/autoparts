package ru.mirea.autoparts.fx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import ru.mirea.autoparts.fx.AppContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

/**
 * Загружает FXML-файлы из папки resources/ru/mirea/autoparts/fx/view.
 * FXMLLoader читает разметку, создаёт элементы интерфейса (Node), создаёт контроллер из атрибута
 * fx:controller и заполняет его поля с аннотацией @FXML по совпадению fx:id.
 */
public final class ViewLoader {
    private static final String VIEW_FOLDER = "/ru/mirea/autoparts/fx/view/";
    private static final String STYLESHEET = "/ru/mirea/autoparts/fx/styles/app.css";

    private ViewLoader() {
    }

    /** Результат загрузки: корневой элемент разметки и его контроллер. */
    public record View<R extends Parent, C>(R root, C controller) {
    }

    public static <R extends Parent, C> View<R, C> load(String fileName, AppContext context) {
        URL location = ViewLoader.class.getResource(VIEW_FOLDER + fileName);
        if (location == null) {
            throw new IllegalStateException("Не найден файл разметки " + fileName);
        }
        FXMLLoader loader = new FXMLLoader(location);
        // Фабрика контроллеров: FXMLLoader создаёт контроллер через конструктор с AppContext,
        // поэтому контроллеру сразу доступны сервисы. Фабрика действует и на вложенные fx:include.
        loader.setControllerFactory(type -> createController(type, context));
        try {
            R root = loader.load();
            C controller = loader.getController();
            return new View<>(root, controller);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить окно " + fileName + ": " + e.getMessage(), e);
        }
    }

    /** Адрес файла стилей CSS — подключается к главной сцене и к диалоговым окнам. */
    public static String stylesheet() {
        URL url = ViewLoader.class.getResource(STYLESHEET);
        if (url == null) {
            throw new IllegalStateException("Не найден файл стилей " + STYLESHEET);
        }
        return url.toExternalForm();
    }

    private static Object createController(Class<?> type, AppContext context) {
        try {
            return type.getConstructor(AppContext.class).newInstance(context);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Не удалось создать контроллер " + type.getSimpleName(), e);
        }
    }
}
