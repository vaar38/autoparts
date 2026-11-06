# Магазин автозапчастей — учёт заказов

Настольное приложение на **JavaFX** для работы с базой заказов автозапчастей в PostgreSQL (подключение через JDBC).

- КР №1 — база данных и консольное приложение (вариант «Магазин автозапчастей (заказ автозапчастей)»).
  Консольная версия сохранена в истории git, коммит `0b11c26`.
- **КР №2 — JavaFX-приложение** с той же базой данных и бизнес-правилами (текущая версия).

Подробное объяснение кода для защиты: [docs/GUIDE-KR2.md](docs/GUIDE-KR2.md) (JavaFX) и
[docs/GUIDE-KR1.md](docs/GUIDE-KR1.md) (база данных, JDBC, модели, бизнес-правила).

## Что умеет

- Заказы в таблице: добавление, редактирование, удаление с подтверждением, смена статуса по правилам.
- Клиенты: добавление, редактирование, удаление; заказы выбранного клиента.
- Поиск по названию, артикулу, марке авто и ФИО клиента; фильтры по статусу, категории и периоду дат.
- Сортировка щелчком по столбцу или готовыми вариантами (дата, сумма, клиент, название).
- Проверка ввода с подсветкой неверного поля и понятным сообщением.
- Статистика: 10 показателей, показатели за период, две диаграммы.
- Экспорт текущего списка заказов в Excel (.xlsx) или CSV — путь выбирается в окне «Сохранить как».

## Что нужно установить

| Программа | Версия | Проверка в терминале |
|-----------|--------|----------------------|
| JDK (Java) | 21 или новее | `java -version` |
| PostgreSQL | 14 или новее | служба PostgreSQL запущена |
| Maven | 3.9 или новее (не обязательно: есть `mvnw.cmd`) | `mvn -v` |
| Docker Desktop | для запуска готовой системы из архива преподавателя | `docker compose version` |

## Запуск по шагам (Windows, PowerShell)

Все команды выполняются в папке проекта (там, где лежит `pom.xml`).

**1. Создать базу данных и заполнить её тестовыми данными** (если не сделано в КР №1)

```powershell
psql -U postgres -c "CREATE DATABASE autoparts"
psql -U postgres -d autoparts -f sql/autoparts.sql
```

Если `psql` не найден: `& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres ...`.
Скрипт можно запускать повторно — он пересоздаёт таблицы и возвращает исходные тестовые данные.

*Вместо локального PostgreSQL можно поднять базу в Docker:* скопировать `.env.example` в `.env`, задать пароль и выполнить
`docker compose up -d --wait` — база создастся сама, порт `55433` (подробности в начале `docker-compose.yml`).

**2. Указать пароль от базы**

```powershell
Copy-Item src\main\resources\database.properties.example src\main\resources\database.properties
```

Откройте `src/main/resources/database.properties` и впишите свой пароль. Файл не загружается в git (указан в `.gitignore`).

```properties
db.url=jdbc:postgresql://localhost:5432/autoparts
db.user=postgres
db.password=ВАШ_ПАРОЛЬ
```

**3. Запустить**

```powershell
.\run.cmd
```

`run.cmd` находит Java (через `JAVA_HOME`, `PATH` или Java из расширения VS Code) и выполняет `mvnw.cmd javafx:run`.
Если Maven установлен, можно просто `mvn javafx:run`. В IntelliJ IDEA — открыть `pom.xml` как проект и запустить
Maven-цель `javafx:run` (или класс `ru.mirea.autoparts.fx.Launcher`).

Другие команды Maven:

```powershell
mvn clean compile   # компиляция
mvn test            # unit-тесты (поиск/фильтры, валидация, экспорт)
mvn clean package   # сборка JAR
```

## Структура проекта

```
autoparts/
├── pom.xml                         — Maven: Java 21, JavaFX 21, PostgreSQL JDBC, Apache POI, SLF4J, JUnit
├── run.cmd                         — запуск приложения
├── sql/autoparts.sql               — SQL-скрипт: таблицы + тестовые данные
├── docker-compose.yml              — необязательно: база КР №1 в Docker
├── docs/                           — ER-диаграмма и объяснения для защиты
└── src/
    ├── main/java/ru/mirea/autoparts/
    │   ├── fx/                     — JavaFX: приложение, контроллеры окон, помощники интерфейса
    │   ├── service/                — бизнес-логика, проверки, поиск/фильтр, статистика
    │   ├── repository/             — работа с БД через JDBC (весь SQL здесь)
    │   ├── model/                  — Customer, Order, OrderStatus, PartCategory
    │   ├── exception/              — собственные исключения
    │   └── util/                   — подключение к БД, экспорт в Excel/CSV, форматы
    ├── main/resources/
    │   ├── database.properties     — параметры подключения (создаёте сами из .example)
    │   └── ru/mirea/autoparts/fx/
    │       ├── view/*.fxml         — разметка окон
    │       └── styles/app.css      — оформление
    └── test/java/                  — unit-тесты
```

## Возможные проблемы

| Сообщение | Что делать |
|-----------|------------|
| `Не найден файл настроек database.properties` | Не выполнен шаг 2 |
| `... password authentication failed` | Неверный пароль в `database.properties` |
| `... database "autoparts" does not exist` | Не выполнен шаг 1 |
| `Connection refused` | Не запущена служба PostgreSQL (Win+R → `services.msc` → postgresql → Запустить) |
| `JavaFX runtime components are missing` | Запускать через `mvn javafx:run` / `run.cmd` или класс `Launcher`, а не `AutopartsApp` напрямую |
| `release version 21 not supported` | Установлена Java ниже 21 — нужна JDK 21+ |
