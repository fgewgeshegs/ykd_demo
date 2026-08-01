package com.youkeda.exercise.claw.feature.schedule.imports;

/**
 * 课程数据来源
 *
 * <p>多级导入优先级：{@link #ZHENGFANG}(最高) → {@link #EXCEL} → {@link #PDF} → {@link #OCR}(兜底)。
 * 持久化到 {@code course_schedule.source} 列，用于溯源与按来源诊断识别错误。
 */
public enum SourceType {

    /** 正方教务系统粘贴文本/数据（最高优先级，结构化最高） */
    ZHENGFANG("正方文本"),
    /** Excel 文件 */
    EXCEL("Excel"),
    /** PDF 文件 */
    PDF("PDF"),
    /** 图片视觉模型识别（最后兜底） */
    OCR("图片识别"),
    /** 通用文档（doc/txt 等，Tika 文本 + LLM） */
    DOC("文档"),
    /** 手动/对话录入 */
    MANUAL("手动录入");

    private final String displayName;

    SourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 从字符串（DB 值）解析来源，未知值回退 {@link #MANUAL}
     */
    public static SourceType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return MANUAL;
        }
        for (SourceType type : values()) {
            if (type.name().equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return MANUAL;
    }

    /** 持久化值 */
    public String code() {
        return name();
    }
}
