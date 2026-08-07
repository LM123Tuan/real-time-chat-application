package com.tuan.chatserver.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuan.chatserver.exception.InvalidCursorException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class CursorCodec {

    private final ObjectMapper objectMapper;

    public CursorCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> String encode(T cursorData) {
        try {
            String json = objectMapper.writeValueAsString(cursorData);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new InvalidCursorException("Failed to encode cursor", e);
        }
    }

    public <T> T decode(String cursor, TypeReference<T> typeRef) {
        try {
            String json = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new InvalidCursorException("Invalid or corrupted cursor", e);
        }
    }
}