package com.example.inquiries.rest;

import static com.example.inquiries.TestHelper.TEST_STRING_UUID;
import static org.hamcrest.Matchers.nullValue;
import org.jooq.DSLContext;
import static com.example.inquiries.repository.generated.tables.CustomerInquiries.CUSTOMER_INQUIRIES;
import java.util.UUID;
import java.time.OffsetDateTime;
import com.example.inquiries.data.model.InquiryCursor;
import static org.assertj.core.api.Assertions.assertThat;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // Rolls each test back; the application database is never used.
class InquiryApiIT {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;

    @Test
    void createsListsFetchesAndUpdatesAnInquiry() throws Exception {
        String response = mvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fab\",\"message\":\"Please help\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(response).get("id").asText());
        String createdAt = json.readTree(response).get("createdAt").asText();
        assertThat(OffsetDateTime.parse(createdAt)).isNotNull();
        mvc.perform(get("/api/inquiries"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(id.toString()));
        mvc.perform(get("/api/inquiries/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Fab"));
        mvc.perform(patch("/api/inquiries/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.createdAt").value(createdAt));
        mvc.perform(get("/api/inquiries/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.createdAt").value(createdAt));
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        mvc.perform(post("/api/inquiries").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","message":"Help"}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/inquiries/{id}/status", TEST_STRING_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/inquiries/{id}/status", TEST_STRING_UUID).
                        contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingIds() throws Exception {
        mvc.perform(get("/api/inquiries/{id}", TEST_STRING_UUID))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/inquiries/{id}/status", TEST_STRING_UUID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isNotFound());
    }

    @Autowired DSLContext dsl;

    @Test
    void paginatesNewestFirstIncludingTiesAndDeletedCursorRow() throws Exception {
        var table = CUSTOMER_INQUIRIES;
        dsl.deleteFrom(table).execute(); // Rolled back with the test transaction.
        var recent = OffsetDateTime.parse("2026-01-02T12:00:00.123456Z");
        var older = recent.minusDays(1);
        for (int i = 1; i <= 12; i++) {
            dsl.insertInto(table).set(table.ID, new UUID(0, i))
                    .set(table.NAME, "Sender").set(table.MESSAGE, "Hello")
                    .set(table.STATUS, "NEW").set(table.CREATED_AT, i == 12 ? older : recent).execute();
        }
        String first = mvc.perform(get("/api/inquiries"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.items[0].id").value(new UUID(0, 11).toString()))
                .andExpect(jsonPath("$.items[9].id").value(new UUID(0, 2).toString()))
                .andReturn().getResponse().getContentAsString();
        String cursor = json.readTree(first).get("nextCursor").asText();
        assertThat(InquiryCursor.decode(cursor)).isEqualTo(new InquiryCursor(recent, new UUID(0, 2)));
        dsl.deleteFrom(table).where(table.ID.eq(new UUID(0, 2))).execute();
        dsl.insertInto(table).set(table.ID, new UUID(0, 13))
                .set(table.NAME, "New arrival").set(table.MESSAGE, "Hello")
                .set(table.CREATED_AT, recent.plusDays(1)).execute();
        mvc.perform(get("/api/inquiries").param("cursor", cursor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(new UUID(0, 1).toString()))
                .andExpect(jsonPath("$.items[1].id").value(new UUID(0, 12).toString()))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
        mvc.perform(get("/api/inquiries").param("cursor", new InquiryCursor(older, new UUID(0, 12)).encode()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }
}
