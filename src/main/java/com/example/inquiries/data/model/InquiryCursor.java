package com.example.inquiries.data.model;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.DateTimeException;
import java.util.Base64;
import java.util.UUID;

public record InquiryCursor(OffsetDateTime createdAt, UUID id) {
    public InquiryCursor {
        createdAt = createdAt.withOffsetSameInstant(ZoneOffset.UTC);
    }

    public static InquiryCursor from(Inquiry inquiry) {
        return new InquiryCursor(inquiry.createdAt(), inquiry.id());
    }

    public String encode() {
        String value = createdAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static InquiryCursor decode(String value) {
        try {
            if (value.isBlank() || value.length() > 200) {
                throw new IllegalArgumentException("Invalid cursor");
            }
            String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
                    .split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid cursor");
            }
            UUID id = UUID.fromString(parts[1]);
            if (!id.toString().equals(parts[1])) {
                throw new IllegalArgumentException("Invalid cursor");
            }
            return new InquiryCursor(OffsetDateTime.parse(parts[0]), id);
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new IllegalArgumentException("Invalid cursor", exception);
        }
    }
}
