package com.umc.devine.domain.project.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DurationRange {
    ONE_TO_THREE("1~3개월", 1, 3),
    THREE_TO_SIX("3~6개월", 3, 6),
    SIX_TO_TWELVE("6~12개월", 6, 12),
    TWELVE_PLUS("12개월 이상", 12, 13);

    private final String displayName;
    private final int minMonths;
    private final int maxMonths;

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static DurationRange from(String value) {
        for (DurationRange range : DurationRange.values()) {
            if (range.name().equalsIgnoreCase(value) || range.displayName.equals(value)) {
                return range;
            }
        }
        throw new IllegalArgumentException("Invalid DurationRange: " + value);
    }
}