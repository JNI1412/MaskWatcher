package ru.evgeniynekrasov.linewater.monoblock.working_with_text;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MaskWatcher implements TextWatcher {

    private static final String LOG_TAG = MaskWatcher.class.getSimpleName();
    //Объект класса StringBuilder (воспользуемся им, синхронизация не важна, все в одном потоке)
    private static final StringBuilder formattedString = new StringBuilder();
    // Переменная для отслеживания удаления символа
    private static boolean isDeleting = false;
    // Переменная для отслеживания первого входа
    private static boolean isFirstPass = true;
    // Переменная, содержащая символ маски
    private static char maskSymbol = '*';
    // Автоматически расширяемый массив, содержащий символы разделителя
    private static ArrayList<Character> separatorSymbolArray;
    // Переменная, в которой храним положение курсора
    private static int cursorPosition = 0;
    // Переменная, в которой храним имя используемого шаблона
    private static String templateName;
    // Переменная, в которой храним используемый шаблон
    private static String template;

    // Переменная, в которой сохраняем количестов символов после внесения изменений
    private int afterChanges;
    // Собственно сам виновник торжества
    private final EditText editText;


    public MaskWatcher(EditText editText, String requiredTemplate)  {
        this.editText = editText;
        templateName = requiredTemplate;
    }

    // s - строка
    // start - стартовая позиция
    // count - количество символов в строке до внесения изменений
    // after - количестов символов в строке после внесения изменений
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        afterChanges = after;
        // Проверяем, небыло ли удаления символов
        isDeleting = count > after;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        Log.e(LOG_TAG, "@@onTextChanged s=" + s
                + " . start="+ start+" . before=" + before+" . count="+ count);

        // Данная переменная содержит последний внесенный пользователем символ
        char lastInsertedChar = '*';
        // Позиция последнего внесенного пользователем символа
        int lastInsertedCharIndex = 0;
        // Переменная, в которой храним позицию последнего удалённого символа
        int lastDeleteCharIndex = 0;

        // Проверяем, является ли данный проход цикла первым
        if (isFirstPass) {
            // Получаем шаблон для формирования строки
            template = getTemplate ('0',templateName);
            // Сортируем шаблон на маску и разделители
            sortingTheTemplate (template);
            // Присваеваем форматируемой строке шаблон
            formattedString.append(template);
            isFirstPass = false;
        }
        // Получаем измененную строку
        String newText = s.toString();

        // Данная переменная используется при движении назад (удалении символов)
        // она содержит часть выражения которая находится перед курсором
        String textPrefix = newText.substring(0, start);

        // Данная переменная содержит вновь внесенные символы
        String textInserted = newText.substring(start, start + afterChanges);

        // Данная переменная содержит часть выражения, которая находится
        // после места внесения изменений
        String textSuffix = newText.substring(start + afterChanges);

        // Данная переменная содержит строку до места установки курсора
        String textBeforeCursor = textPrefix + textInserted;

        // Вся строка
        String newTextClean = textPrefix + textInserted + textSuffix;


    // Проверяем, не удаляет ли пользователь символы, и если нет, то выполняем следующий код
    if (!isDeleting) {

        // Если start будет равен 0 и textBeforeCursor.length() равен 0, то мы получим out of bounds,
        // но start, также,  может быть равен 0 если мы не меняли позицию курсора намеренно,
        // при этом textBeforeCursor.length() вполне может быть 0 уже не равен, поэтому на
        // переменную start, в это деле, полагаться нельзя
        // Выполним следующую проверку
        if ((textBeforeCursor.length() - 1) >= 0) {

            // Данная переменная содержит последний внесенный пользователем символ
            lastInsertedChar = textBeforeCursor.charAt(textBeforeCursor.length() - 1);
            // Позиция последнего внесенного пользователем символа
            lastInsertedCharIndex = (textBeforeCursor.length() - 1);

            // Если вызывается шаблон "PHONE", то приведем его в соответствие коду страны
            if (templateName.equals("PHONE")&& lastInsertedCharIndex == 0) {
                // Очистим форматирование
                formattedString.setLength(0);
                // Загрузим шаблон в соответствии с первым введенным символом
                formattedString.append(getTemplate (lastInsertedChar,templateName));
            }

        } else {
            // Если start будет равен 0 и textBeforeCursor.length() равен 0 то инициализируем
            // lastInsertedChar нулевым символом строки textInserted.
            // Такая ситуация, в основном, возникает в результате поворота экрана
            // По хорошему это надо проконтролировать, но в моём проекте в этом, пока, нет необходимости
            lastInsertedChar = textInserted.charAt(0);

        }

        // Проверим индекс символа, чтобы не вывалился за пределы маски
        if ((0 <= cursorPosition) && (cursorPosition <= template.length()-1)) {

            // Проверяем, не является ли позиция вводимого сивола, позицией разделителя
            if (separatorSymbolArray.contains(template.charAt(lastInsertedCharIndex))){
            // Если является, то рисуем разделитель
                formattedString.setCharAt(lastInsertedCharIndex, template.charAt(lastInsertedCharIndex));
            }else {
                // иначе, проверяем символ на соответствие "частоте рассы"
               if (checkChar (lastInsertedChar, templateName)) {
                   // рисуем последний введенный символ
                   formattedString.setCharAt(lastInsertedCharIndex, lastInsertedChar);
               }
            }
        }

        // Подкорректируем позицию курсора если выбранный шаблон "PHONE"
        // пока считаем, что все номера принадлежат России и имеют 11 значный код
        if (templateName.equals("PHONE")) {
            switch (lastInsertedCharIndex) {
                case (0) : {
                  // Если первый символ 8, то заменим его на +7 и сдвинем курсор в позицию ввода кода оператора
                  if (lastInsertedChar == '8') {lastInsertedCharIndex = 3;
                  }
                }
                break;
                case (1) : {
                    // Если пользователь решил сам вводить код, нарисуем '+' и подождём код страны
                    // затем сдвинем курсор в позицию ввода кода оператора
                    lastInsertedCharIndex = 3;
                }
                break;
                case (6) : {
                    // сдвинем курсор в позицию ввода первых трех цифр номера телефона
                    lastInsertedCharIndex = 8;
                }
                break;
                case (11) : {
                    // сдвинем курсор в позицию ввода следующих двух цифр
                    lastInsertedCharIndex = 12;
                }
                break;
                case (14) : {
                    // последние две цифры
                    lastInsertedCharIndex = 15;
                }
                break;
            }
        }
            // Увеличиваем позицию курсора на единицу относительно позиции введенного символа
            cursorPosition = lastInsertedCharIndex + 1;


    // Если пользователь удаляет символы, то замещаем их символами маски
    } else {

        lastDeleteCharIndex = (textBeforeCursor.length());

        // Проверяем, не является ли позиция удаляемого сивола, позицией разделителя
        if (separatorSymbolArray.contains(template.charAt(lastDeleteCharIndex))){
            // Если является, то рисуем разделитель
            formattedString.setCharAt(lastDeleteCharIndex, template.charAt(lastDeleteCharIndex));
        }else {
            // иначе заменяем удаляемый символ символом маски
            formattedString.setCharAt(lastDeleteCharIndex, maskSymbol);
        }
        // Позиция курсора равна позиции последнего символа до него
        cursorPosition = lastDeleteCharIndex;
        isDeleting = false;
    }

    // Здесь выполняем проверку на ошибку положения курсора
    // Если положение курсора (каким-то загадочным образом) оказалось больше длинны маски,
    // то фиксируем положение курсора в конце строки
    if (cursorPosition >= template.length()) {
        cursorPosition = template.length();
    }

        this.editText.removeTextChangedListener(this);
    this.editText.setText(formattedString);
    this.editText.addTextChangedListener(this);
    this.editText.setSelection(cursorPosition);

}


    @Override
    public void afterTextChanged(Editable s) {
    }


    // Метод возвращающий шаблон форматирования для TextEdit
    private String getTemplate (char firstCharacter, String maskName){

        if (maskName.equals("ID")) {
            return "****.****.****.****";
        }

        if (maskName.equals("PHONE")) {
            if (firstCharacter == '8'){
                return "+7 (***) ***-**-**";
            }else{
                return "+* (***) ***-**-**";
            }
        }

        return "+(*) ***-**-**";
    }

    // Проверяем является ли символ цифрой или буквой латинского алфавита,
    // спецсимволы, знаки пунктуации и кирилицу игнорируем
    private boolean checkChar (char c, String maskName) {
        if (maskName.equals("ID")) {
            return (c >= '0' && c <= '9')||(c >= 'a' && c <= 'z')||(c >= 'A' && c <= 'Z');
        }
        if (maskName.equals("PHONE")) {
            return (c >= '0' && c <= '9');
        }
        return (c >= '0' && c <= '9');
    }

    // В данном методе отсортируем маску по символам: переберём все символы и выберем из них
    // символы маски и символы разделителя. Символы, которых больше всего в строке, считаем
    // символами маски, остальные символы - разделители
    private void sortingTheTemplate (String template) {

        // Для выполнения сортировки используем ассоциативный массив, эта структура данных,
        // в данном случае, подходит просто идеально, т.к. ключ всегда уникален
        // Ключем, конечно же, будет выступать сам символ,
        // ну а значением - количество его вхождений в строку
        Map<Character,Integer> map = new HashMap<Character,Integer>();

        // создаем две пременных в первую занесём ключ с максимальным ассоциированным значением,
        // вторая будет содержать максимальное значение
        char keyWithMaximumValue = '*';
        int maximumValue = 0;

        // Далее цикл сортировки
        for (int i = 0; i < template.length(); i++) {
            // Получаем из строки символ с индексом i
            char c = template.charAt(i);
            // Проверяем наличие в массиве ключа с полученым, из строки, символом
            if (map.containsKey(c)) {
                // Если совпадение найдено, то увеличеваем значение, ассоциированное с ключом на единицу
                // На всякий случай, проверим map.get(c) на null, чтобы не получить NullPointerException
                if (map.get(c) != null) {
                    // Получаем значение по ключу и присваеваем переменной
                    int cnt = map.get(c);
                    map.put(c, ++cnt);

                    // Проверяем на максимальное значение
                    if (cnt > maximumValue) {
                        maximumValue = cnt;
                        keyWithMaximumValue = c;
                    }
                }
            // Иначе, создаем новую пару ключ - значение
            } else {
                map.put(c, 1);
            }
        }
        // Присваиваем символ максимального ключа глобальной переменной
        maskSymbol = keyWithMaximumValue;
        // Удаляем символ маски из ассоциативного массива
        map.remove(keyWithMaximumValue);
        // Записываем символы разделителя в глобальный автоматически расширяемый массив
        separatorSymbolArray = new ArrayList<>(map.keySet());
    }

    private String toString(int[] array)  {
        StringBuilder sb= new StringBuilder();
        for(int i=0;i< array.length;i++) {
            if(i == 0) {
                sb.append("[").append(array[i]);
            } else {
                sb.append(", ").append(array[i]);
            }
        }
        sb.append("]");
        return sb.toString();

    }

}
