import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static final String RUSSIAN_ALPHABET = "абвгдежзийклмнопрстуфхцчшщъыьэюя";
    private static final String ENGLISH_ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final double[] RUSSIAN_FREQUENCIES = {
            0.062, 0.014, 0.038, 0.013, 0.025, 0.007, 0.009, 0.016, 0.054, 0.035,
            0.028, 0.041, 0.043, 0.066, 0.090, 0.023, 0.040, 0.045, 0.047, 0.018,
            0.016, 0.009, 0.006, 0.003, 0.014, 0.003, 0.020, 0.016, 0.004, 0.012,
            0.014, 0.003, 0.006
    };
    private static final double[] ENGLISH_FREQUENCIES = {
            0.0817, 0.0149, 0.0278, 0.0425, 0.1270, 0.0223, 0.0202, 0.0609, 0.0697,
            0.0015, 0.0077, 0.0403, 0.0241, 0.0675, 0.0751, 0.0193, 0.0010, 0.0599,
            0.0633, 0.0906, 0.0276, 0.0098, 0.0236, 0.0015, 0.0197, 0.0007
    };
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════");
        System.out.println("    Шифр Цезаря - Инструмент криптоанализа");
        System.out.println("═══════════════════════════════════════════");

        while (true) {
            showMenu();
            int choice = getIntInput("Выберите опцию: ", 0, 6);

            switch (choice) {
                case 1 -> encryptText();
                case 2 -> decryptWithKey();
                case 3 -> bruteForceDecrypt();
                case 4 -> statisticalDecrypt();
                case 5 -> fileInfo();
                case 6 -> {
                    System.out.println("До свидания!");
                    scanner.close();
                    System.exit(0);
                }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    private static void showMenu() {
        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.println("│ 1. Шифрование текста                     │");
        System.out.println("│ 2. Расшифровка известным ключом          │");
        System.out.println("│ 3. Расшифровка перебором (brute force)   │");
        System.out.println("│ 4. Расшифровка статанализом (автоподбор) │");
        System.out.println("│ 5. Информация о файле                    │");
        System.out.println("│ 6. Выход                                 │");
        System.out.println("└─────────────────────────────────────────┘");
    }

    private static void encryptText() {
        System.out.println("\n--- Шифрование текста ---");
        String inputFile = getInputFile("Введите путь к входному файлу: ");
        String content = readFile(inputFile);
        if (content == null) return;

        int shift = getValidKey("Введите ключ сдвига (1-25 для англ, 1-32 для рус): ");
        String language = detectLanguage(content);
        String alphabet = getAlphabet(language);
        if (alphabet == null) {
            System.out.println("Ошибка: Не удалось определить язык текста.");
            return;
        }

        String encrypted = caesarCipher(content, shift, alphabet);
        String outputFile = getOutputFile("Введите путь для сохранения результата: ");
        if (writeFile(outputFile, encrypted)) {
            System.out.println("✓ Текст успешно зашифрован и сохранён в: " + outputFile);
            System.out.println("Оригинал (" + content.length() + " символов) → Шифр (" + encrypted.length() + " символов)");
        }
    }

    private static void decryptWithKey() {
        System.out.println("\n--- Расшифровка известным ключом ---");
        String inputFile = getInputFile("Введите путь к зашифрованному файлу: ");
        String content = readFile(inputFile);
        if (content == null) return;

        int shift = getValidKey("Введите ключ для расшифровки: ");
        String language = detectLanguage(content);
        String alphabet = getAlphabet(language);
        if (alphabet == null) alphabet = RUSSIAN_ALPHABET + ENGLISH_ALPHABET;

        String decrypted = caesarCipher(content, -shift, alphabet);
        String outputFile = getOutputFile("Введите путь для сохранения результата: ");
        if (writeFile(outputFile, decrypted)) {
            System.out.println("✓ Текст успешно расшифрован и сохранён в: " + outputFile);
        }
    }

    private static void bruteForceDecrypt() {
        System.out.println("\n--- Расшифровка перебором всех ключей ---");
        String inputFile = getInputFile("Введите путь к зашифрованному файлу: ");
        String content = readFile(inputFile);
        if (content == null) return;

        String language = detectLanguage(content);
        String alphabet = getAlphabet(language);
        if (alphabet == null) {
            alphabet = RUSSIAN_ALPHABET + ENGLISH_ALPHABET;
            System.out.println("Предполагаемый алфавит: русский + английский (" + alphabet.length() + " букв)");
        }

        int maxShift = alphabet.length();
        System.out.println("\nРезультаты перебора (показаны варианты, содержащие читаемый текст):\n");
        System.out.println("┌─────┬──────────────────────────────────────────────────┐");

        int resultsCount = 0;
        for (int shift = 0; shift < maxShift; shift++) {
            String decrypted = caesarCipher(content, -shift, alphabet);
            if (isReadable(decrypted)) {
                String preview = decrypted.substring(0, Math.min(60, decrypted.length())).replace("\n", " ");
                System.out.printf("│ %3d │ %-48s │%n", shift, preview);
                resultsCount++;
                if (resultsCount >= 20) {
                    System.out.println("│     │ ... (показаны первые 20 вариантов)        │");
                    break;
                }
            }
        }
        System.out.println("└─────┴──────────────────────────────────────────────────┘");

        if (resultsCount == 0) {
            System.out.println("Не найдено читаемых вариантов. Попробуйте статистический анализ.");
        } else {
            int chosenShift = getIntInput("\nВведите ключ для полной расшифровки (или -1 для пропуска): ", -1, maxShift - 1);
            if (chosenShift >= 0) {
                String fullDecrypted = caesarCipher(content, -chosenShift, alphabet);
                String outputFile = getOutputFile("Путь для сохранения: ");
                if (writeFile(outputFile, fullDecrypted)) {
                    System.out.println("✓ Расшифровка сохранена");
                }
            }
        }
    }

    private static void statisticalDecrypt() {
        System.out.println("\n--- Расшифровка статистическим анализом ---");
        String inputFile = getInputFile("Введите путь к зашифрованному файлу: ");
        String content = readFile(inputFile);
        if (content == null) return;

        String language = detectLanguageByFrequencies(content);
        String alphabet = getAlphabet(language);
        if (alphabet == null) {
            System.out.println("Не удалось определить язык. Пробуем оба...");
            int bestShiftRussian = findBestShiftByFrequency(content, RUSSIAN_ALPHABET, RUSSIAN_FREQUENCIES);
            int bestShiftEnglish = findBestShiftByFrequency(content, ENGLISH_ALPHABET, ENGLISH_FREQUENCIES);

            String decryptedRussian = caesarCipher(content, -bestShiftRussian, RUSSIAN_ALPHABET);
            String decryptedEnglish = caesarCipher(content, -bestShiftEnglish, ENGLISH_ALPHABET);

            System.out.println("\nРусский вариант (ключ " + bestShiftRussian + "):");
            System.out.println(decryptedRussian.substring(0, Math.min(200, decryptedRussian.length())) + "...");
            System.out.println("\nАнглийский вариант (ключ " + bestShiftEnglish + "):");
            System.out.println(decryptedEnglish.substring(0, Math.min(200, decryptedEnglish.length())) + "...");

            String choice = getStringInput("Выберите язык (rus/eng): ");
            if (choice.equalsIgnoreCase("rus")) {
                saveDecrypted(decryptedRussian, bestShiftRussian);
            } else if (choice.equalsIgnoreCase("eng")) {
                saveDecrypted(decryptedEnglish, bestShiftEnglish);
            }
            return;
        }

        double[] frequencies = language.equals("rus") ? RUSSIAN_FREQUENCIES : ENGLISH_FREQUENCIES;
        int bestShift = findBestShiftByFrequency(content, alphabet, frequencies);
        System.out.println("\n★ Наиболее вероятный ключ: " + bestShift);

        String decrypted = caesarCipher(content, -bestShift, alphabet);
        System.out.println("\nРасшифрованный текст (фрагмент):");
        System.out.println("─────────────────────────────────────────────────");
        System.out.println(decrypted.substring(0, Math.min(500, decrypted.length())));
        if (decrypted.length() > 500) System.out.println("...");
        System.out.println("─────────────────────────────────────────────────");

        saveDecrypted(decrypted, bestShift);
    }

    private static int findBestShiftByFrequency(String text, String alphabet, double[] expectedFreq) {
        int alphabetSize = alphabet.length();
        double bestScore = Double.MAX_VALUE;
        int bestShift = 0;

        // Получаем частоты букв в исходном тексте
        double[] observedFreq = calculateLetterFrequencies(text, alphabet);

        for (int shift = 0; shift < alphabetSize; shift++) {
            double[] shiftedFreq = new double[alphabetSize];
            for (int i = 0; i < alphabetSize; i++) {
                shiftedFreq[(i + shift) % alphabetSize] = observedFreq[i];
            }
            double score = chiSquaredTest(shiftedFreq, expectedFreq);
            if (score < bestScore) {
                bestScore = score;
                bestShift = shift;
            }
        }
        return bestShift;
    }

    private static double[] calculateLetterFrequencies(String text, String alphabet) {
        int[] counts = new int[alphabet.length()];
        int totalLetters = 0;

        for (char c : text.toLowerCase().toCharArray()) {
            int index = alphabet.indexOf(c);
            if (index >= 0) {
                counts[index]++;
                totalLetters++;
            }
        }

        double[] frequencies = new double[alphabet.length()];
        if (totalLetters > 0) {
            for (int i = 0; i < alphabet.length(); i++) {
                frequencies[i] = (double) counts[i] / totalLetters;
            }
        }
        return frequencies;
    }

    private static double chiSquaredTest(double[] observed, double[] expected) {
        double chiSquare = 0.0;
        for (int i = 0; i < observed.length; i++) {
            double diff = observed[i] - expected[i];
            if (expected[i] > 0) {
                chiSquare += (diff * diff) / expected[i];
            }
        }
        return chiSquare;
    }

    private static String detectLanguageByFrequencies(String text) {
        double scoreRussian = chiSquaredTest(
                calculateLetterFrequencies(text, RUSSIAN_ALPHABET),
                RUSSIAN_FREQUENCIES
        );
        double scoreEnglish = chiSquaredTest(
                calculateLetterFrequencies(text, ENGLISH_ALPHABET),
                ENGLISH_FREQUENCIES
        );
        return scoreRussian < scoreEnglish ? "rus" : "eng";
    }

    private static String caesarCipher(String text, int shift, String alphabet) {
        int alphabetSize = alphabet.length();
        if (alphabetSize == 0) return text;

        shift = ((shift % alphabetSize) + alphabetSize) % alphabetSize;
        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {
            char lowerC = Character.toLowerCase(c);
            int index = alphabet.indexOf(lowerC);
            if (index >= 0) {
                int newIndex = (index + shift) % alphabetSize;
                char newChar = alphabet.charAt(newIndex);
                result.append(Character.isUpperCase(c) ? Character.toUpperCase(newChar) : newChar);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String detectLanguage(String text) {
        int russianCount = 0;
        int englishCount = 0;
        for (char c : text.toLowerCase().toCharArray()) {
            if (RUSSIAN_ALPHABET.indexOf(c) >= 0) russianCount++;
            if (ENGLISH_ALPHABET.indexOf(c) >= 0) englishCount++;
        }
        if (russianCount > englishCount && russianCount > 5) return "rus";
        if (englishCount > russianCount && englishCount > 5) return "eng";
        return "mixed";
    }

    private static String getAlphabet(String language) {
        return switch (language) {
            case "rus" -> RUSSIAN_ALPHABET;
            case "eng" -> ENGLISH_ALPHABET;
            default -> null;
        };
    }

    private static boolean isReadable(String text) {
        String lowerText = text.toLowerCase();
        String[] commonWords = {"и", "в", "на", "с", "по", "к", "у", "за", "из", "от",
                "the", "and", "to", "of", "a", "in", "for", "is", "on", "that"};
        int wordMatches = 0;
        for (String word : commonWords) {
            if (lowerText.contains(word)) wordMatches++;
        }
        return wordMatches >= 2 && text.length() > 20;
    }

    private static String readFile(String filepath) {
        try {
            if (!Files.exists(Path.of(filepath))) {
                System.out.println("Ошибка: Файл не существует - " + filepath);
                return null;
            }
            String content = Files.readString(Path.of(filepath));
            if (content.isBlank()) {
                System.out.println("Предупреждение: Файл пуст");
            }
            return content;
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + e.getMessage());
            return null;
        }
    }

    private static boolean writeFile(String filepath, String content) {
        try {
            Files.writeString(Path.of(filepath), content);
            return true;
        } catch (IOException e) {
            System.out.println("Ошибка записи файла: " + e.getMessage());
            return false;
        }
    }

    private static void saveDecrypted(String decrypted, int key) {
        String save = getStringInput("Сохранить результат? (y/n): ");
        if (save.equalsIgnoreCase("y")) {
            String outputFile = getOutputFile("Путь для сохранения: ");
            if (writeFile(outputFile, decrypted)) {
                System.out.println("✓ Расшифровка сохранена (ключ: " + key + ")");
            }
        }
    }

    private static void fileInfo() {
        System.out.println("\n--- Информация о файле ---");
        String filepath = getInputFile("Введите путь к файлу: ");
        try {
            Path path = Path.of(filepath);
            if (!Files.exists(path)) {
                System.out.println("Файл не существует");
                return;
            }
            String content = Files.readString(path);
            System.out.println("├─ Размер: " + Files.size(path) + " байт");
            System.out.println("├─ Символов: " + content.length());
            System.out.println("├─ Строк: " + content.lines().count());
            System.out.println("├─ Язык (предпол.): " + detectLanguage(content));
            System.out.println("└─ Алфавит: " + (detectLanguage(content).equals("rus") ? "Русский" :
                    detectLanguage(content).equals("eng") ? "Английский" : "Смешанный"));
        } catch (IOException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static int getValidKey(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int key = Integer.parseInt(scanner.nextLine().trim());
                if (key >= 1 && key <= 33) return key;
                System.out.println("Ключ должен быть от 1 до 33 (размер алфавита)");
            } catch (NumberFormatException e) {
                System.out.println("Введите целое число");
            }
        }
    }

    private static int getIntInput(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) return value;
                System.out.printf("Введите число от %d до %d%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Введите целое число");
            }
        }
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String getInputFile(String prompt) {
        System.out.print(prompt);
        String path = scanner.nextLine().trim();
        while (!Files.exists(Path.of(path))) {
            System.out.print("Файл не найден. Введите корректный путь: ");
            path = scanner.nextLine().trim();
        }
        return path;
    }

    private static String getOutputFile(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}