package com.example.inquiries.exception;

import java.util.UUID;

public class InquiryNotFoundException extends RuntimeException {
    public InquiryNotFoundException(UUID id) {
        super("Inquiry " + id + " was not found");
    }
}
