package com.m2its.common.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link RecordStatus} to the one-letter codes stored in the legacy tables.
 * Use explicitly: {@code @Convert(converter = RecordStatusConverter.class)}.
 */
@Converter
public class RecordStatusConverter implements AttributeConverter<RecordStatus, String> {

    @Override
    public String convertToDatabaseColumn(RecordStatus status) {
        return status == null ? null : status.code();
    }

    @Override
    public RecordStatus convertToEntityAttribute(String code) {
        return code == null ? null : RecordStatus.fromCode(code);
    }
}
