package com.example.indoorairqualitycollector;

import java.util.Arrays;

public class Converter
{
    public static <T> String arrayToString(T[] array, String delimiter) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            result.append(array[i].toString());
            if (i < array.length - 1) {
                result.append(delimiter);
            }
        }
        return result.toString();
    }
}
