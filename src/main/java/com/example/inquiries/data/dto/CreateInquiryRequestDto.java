package com.example.inquiries.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInquiryRequestDto(@NotBlank String name, @NotBlank String message) {
}
