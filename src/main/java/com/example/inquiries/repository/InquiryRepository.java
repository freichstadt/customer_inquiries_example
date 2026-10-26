package com.example.inquiries.repository;

import java.util.UUID;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.example.inquiries.data.InquiryStatus;
import com.example.inquiries.data.model.Inquiry;
import org.jooq.DSLContext;
import com.example.inquiries.data.model.InquiryCursor;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.row;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import static com.example.inquiries.repository.generated.tables.CustomerInquiries.CUSTOMER_INQUIRIES;

@Repository
public class InquiryRepository {
    private final DSLContext dsl;

    public InquiryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Inquiry create(String name, String message) {
        return dsl.insertInto(CUSTOMER_INQUIRIES)
                .set(CUSTOMER_INQUIRIES.ID, UUID.randomUUID())
                .set(CUSTOMER_INQUIRIES.NAME, name)
                .set(CUSTOMER_INQUIRIES.MESSAGE, message)
                .set(CUSTOMER_INQUIRIES.STATUS, InquiryStatus.NEW.name())
                .returning()
                .fetchSingle(InquiryRepository::toInquiry);
    }

    public List<Inquiry> findAll(InquiryCursor cursor, int limit) {
        return dsl.selectFrom(CUSTOMER_INQUIRIES)
                .where(cursor == null ? noCondition() :
                        row(CUSTOMER_INQUIRIES.CREATED_AT, CUSTOMER_INQUIRIES.ID)
                                .lt(row(cursor.createdAt(), cursor.id())))
                .orderBy(CUSTOMER_INQUIRIES.CREATED_AT.desc(), CUSTOMER_INQUIRIES.ID.desc())
                .limit(limit)
                .fetch(InquiryRepository::toInquiry);
    }

    public Optional<Inquiry> findById(UUID id) {
        return dsl.selectFrom(CUSTOMER_INQUIRIES)
                .where(CUSTOMER_INQUIRIES.ID.eq(id))
                .fetchOptional(InquiryRepository::toInquiry);
    }

    public Optional<Inquiry> updateStatus(UUID id, InquiryStatus status) {
        return dsl.update(CUSTOMER_INQUIRIES)
                .set(CUSTOMER_INQUIRIES.STATUS, status.name())
                .where(CUSTOMER_INQUIRIES.ID.eq(id))
                .returning()
                .fetchOptional(InquiryRepository::toInquiry);
    }

    private static Inquiry toInquiry(Record record) {
        return new Inquiry(record.get(CUSTOMER_INQUIRIES.ID),
                record.get(CUSTOMER_INQUIRIES.NAME), record.get(CUSTOMER_INQUIRIES.MESSAGE),
                InquiryStatus.valueOf(record.get(CUSTOMER_INQUIRIES.STATUS)),
                record.get(CUSTOMER_INQUIRIES.CREATED_AT).withOffsetSameInstant(ZoneOffset.UTC));
    }
}
