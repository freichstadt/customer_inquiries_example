package com.example.inquiries.rest;

import java.util.UUID;
import com.example.inquiries.data.model.InquiryCursor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.URI;
import com.example.inquiries.data.model.InquiryPage;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import com.example.inquiries.data.dto.CreateInquiryRequestDto;
import com.example.inquiries.data.dto.UpdateStatusRequestDto;
import com.example.inquiries.data.model.Inquiry;
import com.example.inquiries.service.InquiryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {
    private final InquiryService service;

    public InquiryController(InquiryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Inquiry> create(@Valid @RequestBody CreateInquiryRequestDto request) {
        Inquiry inquiry = service.create(request.name(), request.message());
        return ResponseEntity.created(URI.create("/api/inquiries/" + inquiry.id())).body(inquiry);
    }

    @GetMapping
    public InquiryPage findAll(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        InquiryCursor decoded = null;
        if (cursor != null) {
            try {
                decoded = InquiryCursor.decode(cursor);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
            }
        }
        return service.findAll(decoded, limit);
    }

    @GetMapping("/{id}")
    public Inquiry findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/status")
    public Inquiry updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequestDto request) {
        return service.updateStatus(id, request.status());
    }
}
