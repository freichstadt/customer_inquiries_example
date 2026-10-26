package com.example.inquiries.data.model;

import java.util.UUID;
import java.time.OffsetDateTime;
import com.example.inquiries.data.InquiryStatus;

public record Inquiry(UUID id, String name, String message, InquiryStatus status, OffsetDateTime createdAt) {
}
