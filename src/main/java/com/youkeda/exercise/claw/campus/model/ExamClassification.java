package com.youkeda.exercise.claw.campus.model;

public record ExamClassification(
    NoticeType type,
    double confidence,
    String reason,
    String scoreSource
) {}
