package com.hireflow.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Saves a list of skills as ONE text column ("Java|Spring Boot|Docker"), so no extra table is needed.
// Skills are cleaned before saving and never contain "|".
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        return list == null || list.isEmpty() ? null : String.join("|", list);
    }

    @Override
    public List<String> convertToEntityAttribute(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(text.split("\\|")));
    }
}