package com.example.inquiries.data.dto;

import com.example.inquiries.data.InquiryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequestDto(@NotNull InquiryStatus status) {
}
