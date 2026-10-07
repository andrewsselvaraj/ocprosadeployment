package com.m2its.question.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** answer_info_v_two.correct_answer stores 'Yes' / 'No'; the entity exposes a boolean. */
@Converter
public class YesNoConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Yes" : "No";
    }

    @Override
    public Boolean convertToEntityAttribute(String value) {
        return "yes".equalsIgnoreCase(value);
    }
}
