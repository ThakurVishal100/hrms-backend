package com.hrms.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            // Convert Java Map -> JSON String
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting map to JSON", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }
        try {
            // Convert JSON String -> Java Map
            return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting JSON to map", e);
        }
    }
}