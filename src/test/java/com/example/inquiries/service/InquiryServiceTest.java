package com.example.inquiries.service;

import java.util.List;
import java.time.OffsetDateTime;
import com.example.inquiries.data.model.InquiryCursor;
import java.util.UUID;
import java.util.Optional;

import com.example.inquiries.data.InquiryStatus;
import com.example.inquiries.data.model.Inquiry;
import com.example.inquiries.exception.InquiryNotFoundException;
import com.example.inquiries.repository.*;
import org.junit.jupiter.api.Test;

import static com.example.inquiries.TestHelper.TEST_STRING_UUID;
import static com.example.inquiries.TestHelper.TEST_UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InquiryServiceTest {
    private final InquiryRepository repository = mock(InquiryRepository.class);
    private final InquiryService service = new InquiryService(repository);

    @Test
    void trimsInputBeforeCreatingInquiry() {
        var expected = new Inquiry(UUID.randomUUID(), "Fab", "Help", InquiryStatus.NEW, OffsetDateTime.parse("2026-01-01T12:00:00.123456Z"));
        when(repository.create("Fab", "Help")).thenReturn(expected);
        assertThat(service.create("  Fab  ", " Help ")).isEqualTo(expected);
    }

    @Test
    void reportsMissingInquiry() {
        when(repository.findById(TEST_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(TEST_UUID))
                .isInstanceOf(InquiryNotFoundException.class).hasMessageContaining(TEST_STRING_UUID);
    }

    @Test
    void reportsMissingInquiryWhenUpdatingStatus() {
        when(repository.updateStatus(TEST_UUID, InquiryStatus.CLOSED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(TEST_UUID, InquiryStatus.CLOSED))
                .isInstanceOf(InquiryNotFoundException.class);
    }

    @Test
    void usesLastReturnedItemAsCursorAndExcludesLookahead() {
        var first = new Inquiry(UUID.randomUUID(), "A", "Help", InquiryStatus.NEW, OffsetDateTime.parse("2026-01-01T12:00:00.123456Z"));
        var second = new Inquiry(UUID.randomUUID(), "B", "Help", InquiryStatus.NEW, OffsetDateTime.parse("2026-01-01T12:00:00.123456Z"));
        var extra = new Inquiry(UUID.randomUUID(), "C", "Help", InquiryStatus.NEW, OffsetDateTime.parse("2026-01-01T12:00:00.123456Z"));
        when(repository.findAll(null, 3)).thenReturn(List.of(first, second, extra));
        var page = service.findAll(null, 2);
        assertThat(page.items()).containsExactly(first, second);
        assertThat(InquiryCursor.decode(page.nextCursor())).isEqualTo(InquiryCursor.from(second));
    }

    @Test
    void exactLimitAndEmptyPagesHaveNoNextCursor() {
        var item = new Inquiry(TEST_UUID, "A", "Help", InquiryStatus.NEW, OffsetDateTime.parse("2026-01-01T12:00:00.123456Z"));
        when(repository.findAll(null, 2)).thenReturn(List.of(item));
        assertThat(service.findAll(null, 1).nextCursor()).isNull();
        when(repository.findAll(InquiryCursor.from(item), 2)).thenReturn(List.of());
        var end = service.findAll(InquiryCursor.from(item), 1);
        assertThat(end.items()).isEmpty();
        assertThat(end.nextCursor()).isNull();
    }
}
