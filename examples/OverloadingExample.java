/**
 * Пример перегрузки методов (method overloading).
 *
 * Перегрузка — это несколько методов в одном классе с ОДИНАКОВЫМ именем,
 * но РАЗНЫМИ параметрами (разное количество, разные типы или разный порядок).
 * Какой из методов вызвать, Java решает при компиляции — по аргументам в вызове.
 *
 * Запуск (из папки проекта): java examples/OverloadingExample.java
 */
public class OverloadingExample {

    /** Калькулятор: один метод add в четырёх вариантах. */
    static class Calculator {

        // 1) два целых числа
        public int add(int a, int b) {
            System.out.println("  вызван add(int, int)");
            return a + b;
        }

        // 2) три целых числа — отличается КОЛИЧЕСТВОМ параметров
        public int add(int a, int b, int c) {
            System.out.println("  вызван add(int, int, int)");
            return a + b + c;
        }

        // 3) два дробных числа — отличается ТИПОМ параметров
        public double add(double a, double b) {
            System.out.println("  вызван add(double, double)");
            return a + b;
        }

        // 4) две строки — тоже другой тип: «сложение» строк = склеивание
        public String add(String a, String b) {
            System.out.println("  вызван add(String, String)");
            return a + b;
        }

        // ОШИБКА, так нельзя: отличается только тип результата, параметры те же, что у варианта 1.
        // public long add(int a, int b) { return a + b; }
    }

    public static void main(String[] args) {
        Calculator calc = new Calculator();

        System.out.println("add(2, 3):");
        System.out.println("  результат = " + calc.add(2, 3));

        System.out.println("add(1, 2, 3):");
        System.out.println("  результат = " + calc.add(1, 2, 3));

        System.out.println("add(2.5, 1.5):");
        System.out.println("  результат = " + calc.add(2.5, 1.5));

        System.out.println("add(\"Привет, \", \"мир\"):");
        System.out.println("  результат = " + calc.add("Привет, ", "мир"));
    }
}
