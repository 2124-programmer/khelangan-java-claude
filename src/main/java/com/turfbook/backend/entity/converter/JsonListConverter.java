package com.turfbook.backend.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class JsonListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializing list to JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JsonNode node = MAPPER.readTree(dbData);
            // Some JSON column types (e.g. H2's native JSON) hand the value back as a
            // quoted JSON string (e.g. "[\"A\"]"); unwrap one layer in that case.
            if (node.isTextual()) {
                node = MAPPER.readTree(node.asText());
            }
            List<String> out = new ArrayList<>();
            if (node.isArray()) {
                node.forEach(n -> out.add(n.asText()));
            }
            return out;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error deserializing JSON to list", e);
        }
    }
}
