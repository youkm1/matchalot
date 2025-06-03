package com.smwu.matchalot.domain.model.vo;


//vo는 equals, hashCode, toString 제공
public record Semester(int year, String season) {
    public static final String SPRING = "1학기";
    public static final String SUMMER = "여름계절";
    public static final String FALL = "2학기";
    public static final String WINTER = "겨울계절";

    public Semester {
        if (season == null || (!season.equals(SPRING) && !season.equals(SUMMER) && !season.equals(FALL) && !season.equals(WINTER))) {
            throw new IllegalArgumentException("올바른 학기를 입력");
        }
    }

    public static Semester of(int year, String season) {
        return new Semester(year, season);
    }

    public String getDisplayName() {
        return year + "년 " + season;
    }
}
