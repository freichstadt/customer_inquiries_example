package com.example.inquiries.service;

import java.util.UUID;
import com.example.inquiries.data.model.InquiryCursor;
import com.example.inquiries.data.model.InquiryPage;
import com.example.inquiries.data.model.Inquiry;
import com.example.inquiries.exception.InquiryNotFoundException;
import com.example.inquiries.repository.InquiryRepository;
import com.example.inquiries.data.InquiryStatus;
import org.springframework.stereotype.Service;

@Service
public class InquiryService {
    private final InquiryRepository repository;

    public InquiryService(InquiryRepository repository) {
        this.repository = repository;
    }

    public Inquiry create(String name, String message) {
        return repository.create(name.strip(), message.strip());
    }

    public InquiryPage findAll(InquiryCursor cursor, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
        // One extra row tells us whether another page exists, without a count query.
        var rows = repository.findAll(cursor, limit + 1);
        boolean hasMore = rows.size() > limit;
        var items = rows.subList(0, Math.min(rows.size(), limit));
        String nextCursor = hasMore ? InquiryCursor.from(items.getLast()).encode() : null;
        return new InquiryPage(items, nextCursor);
    }

    public Inquiry findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new InquiryNotFoundException(id));
    }

    public Inquiry updateStatus(UUID id, InquiryStatus status) {
        return repository.updateStatus(id, status)
                .orElseThrow(() -> new InquiryNotFoundException(id));
    }
}
