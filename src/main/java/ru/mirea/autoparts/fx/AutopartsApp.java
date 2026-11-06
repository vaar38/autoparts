package ru.mirea.autoparts.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.autoparts.exception.DatabaseException;
import ru.mirea.autoparts.fx.controller.MainController;
import ru.mirea.autoparts.fx.util.Dialogs;
import ru.mirea.autoparts.fx.util.ViewLoader;
import ru.mirea.autoparts.util.DatabaseManager;

/**
 * Главный класс JavaFX-приложения (его запускает команда mvn javafx:run).
 *
 * Stage — окно операционной системы; Scene — содержимое окна; Node — любой элемент внутри сцены
 * (кнопка, таблица, панель). Метод start получает от JavaFX готовый Stage — главное окно.
 */
public class AutopartsApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(AutopartsApp.class);

    @Override
    public void start(Stage stage) {
        // Любая неперехваченная ошибка в потоке интерфейса — понятное сообщение вместо «тихого» сбоя
        Thread.currentThread().setUncaughtExceptionHandler((thread, e) -> {
            log.error("Неперехваченная ошибка", e);
            Dialogs.error("Непредвиденная ошибка", "Произошла внутренняя ошибка программы. Подробности записаны в журнал.");
        });

        AppContext context;
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.checkConnection();
            context = new AppContext(databaseManager);
        } catch (DatabaseException e) {
            log.error("Нет подключения к базе данных", e);
            Dialogs.error("Нет подключения к базе данных", e.getMessage());
            Platform.exit();
            return;
        }

        ViewLoader.View<Parent, MainController> view = ViewLoader.load("MainView.fxml", context);
        Scene scene = new Scene(view.root(), 1320, 780);
        scene.getStylesheets().add(ViewLoader.stylesheet());

        stage.setTitle("Магазин автозапчастей — учёт заказов");
        stage.setMinWidth(1050);
        stage.setMinHeight(640);
        stage.setScene(scene);
        Dialogs.setOwner(stage);
        stage.show();
        log.info("Приложение запущено, база данных: {}", context.getDatabaseUrl());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
