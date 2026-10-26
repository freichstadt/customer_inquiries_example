package com.example.inquiries.data.model;

import java.util.List;

public record InquiryPage(List<Inquiry> items, String nextCursor) {
    public InquiryPage {
        items = List.copyOf(items);
    }
}
