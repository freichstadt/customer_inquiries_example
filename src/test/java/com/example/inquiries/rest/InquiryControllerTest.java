package com.example.inquiries.rest;

import java.util.List;
import java.time.OffsetDateTime;
import com.example.inquiries.data.model.InquiryCursor;
import com.example.inquiries.data.model.InquiryPage;

import static com.example.inquiries.TestHelper.TEST_STRING_UUID;
import static org.hamcrest.Matchers.nullValue;
import com.example.inquiries.exception.InquiryNotFoundException;
import com.example.inquiries.service.InquiryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.example.inquiries.TestHelper.TEST_UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InquiryController.class)
class InquiryControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean InquiryService service;

    @Test
    void rejectsBlankNameBeforeCallingService() throws Exception {
        mvc.perform(post("/api/inquiries").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":" ","message":"Hello"}
                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnknownAndMissingStatus() throws Exception {
        mvc.perform(patch("/api/inquiries/{id}/status", TEST_STRING_UUID).contentType(MediaType.APPLICATION_JSON).content("""
                {"status":"UNKNOWN"}
                """))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/inquiries/{id}/status", TEST_STRING_UUID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedUuidBeforeCallingService() throws Exception {
        mvc.perform(get("/api/inquiries/not-a-uuid"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/inquiries/not-a-uuid/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void mapsMissingInquiryTo404() throws Exception {
        when(service.findById(TEST_UUID)).thenThrow(new InquiryNotFoundException(TEST_UUID));
        mvc.perform(get("/api/inquiries/{id}", TEST_STRING_UUID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail")
                        .value("Inquiry %s was not found".formatted(TEST_STRING_UUID)));
    }

    @Test
    void defaultsToTenAndReturnsPageEnvelope() throws Exception {
        when(service.findAll(null, 10)).thenReturn(
                new InquiryPage(List.of(), null));
        mvc.perform(get("/api/inquiries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
        verify(service).findAll(null, 10);
    }

    @Test
    void passesCursorAndCustomLimit() throws Exception {
        var cursor = new InquiryCursor(OffsetDateTime.parse("2026-01-01T12:00:00Z"), TEST_UUID);
        when(service.findAll(cursor, 2)).thenReturn(
                new InquiryPage(List.of(), null));
        mvc.perform(get("/api/inquiries").param("cursor", cursor.encode()).param("limit", "2"))
                .andExpect(status().isOk());
        verify(service).findAll(cursor, 2);
    }

    @Test
    void rejectsInvalidPaginationParameters() throws Exception {
        for (String limit : List.of("0", "-1", "101", "abc")) {
            mvc.perform(get("/api/inquiries").param("limit", limit))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/inquiries").param("cursor", "not-a-uuid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
