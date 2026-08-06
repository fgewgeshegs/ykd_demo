package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.ai.llm.LLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 快速对话分类器。
 *
 * <p>三阶判断：
 * <ol>
 *   <li>本地规则高置信 CHAT_ONLY → true（跳过 LLM，直接进快速通道）</li>
 *   <li>本地规则高置信 NEED_TOOLS → false（走完整 Agent 流程）</li>
 *   <li>不确定 → false（宁可误判为需要Agent，也不误判为普通聊天）</li>
 * </ol>
 *
 * <p>判断顺序：精确白名单 → 长度守卫 → 复合闲聊/感谢变体 → NEED_TOOLS → 兜底 false。
 * 精确白名单在长度守卫之前，确保 "你好""谢谢" 等短问候不被误杀。
 *
 * <p>原 LLM 分类能力保留于 {@link #llmClassify}，当前策略下不确定直接返回 false 不触发 LLM。
 */
public class SimpleChatClassifier {

    private static final Logger log = LoggerFactory.getLogger(SimpleChatClassifier.class);

    // ==== 高置信 CHAT_ONLY：纯社交/情感表达，绝无可能是操作确认 ====

    /**
     * 精确匹配白名单——这些消息在任何上下文中都不会是工具操作确认。
     * 不含 "好的""收到""行""可以""ok" 等可能表示确认/同意的词。
     */
    private static final Set<String> CHAT_ONLY_EXACT = Set.of(
            // 纯问候
            "你好", "你好呀", "嗨", "哈喽", "hello", "hi", "hey",
            "早上好", "中午好", "下午好", "晚上好", "晚安", "早啊", "早呀",
            // 纯感谢
            "谢谢", "感谢", "多谢", "辛苦了", "谢谢啦", "谢谢您", "感谢您",
            // 纯道别
            "再见", "拜拜", "bye", "回头见", "回见", "明天见", "下次见",
            // 纯笑/感叹（非确认语义）
            "哈哈", "嘿嘿", "呵呵", "哈哈哈", "太棒了", "厉害了"
    );

    /** 复合消息成分——用于剥离后判断组合消息（如"好的谢谢"→剥离后为空→闲聊）。 */
    private static final Set<String> CHAT_ONLY_FRAGMENTS = Set.of(
            "好的", "谢谢", "感谢", "嗯嗯", "收到",
            "哈哈", "嘿嘿", "哈哈哈", "你好", "嗨",
            "辛苦了", "再见", "拜拜", "太棒了", "厉害了"
    );

    // ==== NEED_TOOLS：明确的工具/信息请求 ====

    /**
     * Skill 领域关键词（覆盖 skills.yml / skill-triggers.yml 中所有 skill 的触发域）。
     * 命中任一词 → 大概率需要调用工具。
     */
    private static final Set<String> SKILL_DOMAIN_KEYWORDS = Set.of(
            // weather
            "天气", "气温", "多少度", "下雨", "下雪", "刮风", "降温",
            // travel + transport + map
            "旅游", "旅行", "出游", "攻略", "路线", "导航",
            "打车", "叫车", "代驾", "坐车", "出行", "交通",
            "地图", "附近",
            // campus
            "课表", "课程", "考试", "上课", "学期", "学校", "班级",
            "教务处", "选课", "老师", "教室", "学分", "排课", "导入",
            // anime
            "动漫", "番剧", "新番", "追番", "看番", "追更", "补番",
            // image
            "生成图片", "生成一张", "帮我画", "给我画", "画一张", "画个", "水墨", "绘图", "图片生成",
            // scout / search
            "搜索", "查询", "新闻", "推荐", "关注", "搜集", "收集", "汇总",
            "有什么新消息", "搜搜看",
            // schedule
            "提醒", "定时", "日程",
            // other tool-heavy domains
            "翻译", "预算", "费用", "酒店", "机票", "火车", "距离",
            "计算"
    );

    /** 请求/询问表达——用户明确在索取信息或请求操作 */
    private static final Set<String> REQUEST_EXPRESSIONS = Set.of(
            "帮我", "告诉我", "有没有", "怎么", "哪里",
            "什么", "多少", "什么时候", "查一下", "搜一下",
            "我要", "我想", "请问", "能不能", "可不可以",
            "帮我查", "帮我看", "帮我找", "给我", "推荐一下",
            "怎么去", "持续关注", "帮我关注", "订阅资讯", "帮我留意"
    );

    /** 疑问标记 */
    private static final Pattern QUESTION_MARKERS = Pattern.compile("[？?吗呢吧]");

    private final LLMClient llmClient;

    public SimpleChatClassifier(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 快速判断用户消息是否不需要工具即可回复。
     *
     * @param userMessage 用户消息
     * @return true = 闲聊，可走快速通道；false = 需要走完整 Agent 流程
     */
    public boolean isSimpleChat(String userMessage) {
        // Guard
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return false;
        }

        String msg = userMessage.trim();

        // ==== Tier 0: 精确白名单（在长度守卫之前）====
        // "你好""谢谢""再见" 等短消息必须先于此检查，否则会被长度守卫误杀
        if (CHAT_ONLY_EXACT.contains(msg)) {
            log.debug("SimpleChatClassifier: 精确白名单 CHAT_ONLY | msg={}", msg);
            return true;
        }

        // 极短消息（≤2 字）：非白名单则太模糊，可能是方案确认/选择/否认，必须走 Agent
        if (msg.length() <= 2) {
            log.debug("SimpleChatClassifier: 极短消息 → NEED_TOOLS | msg={}", msg);
            return false;
        }

        // ==== Tier 1: 高置信 CHAT_ONLY（本地规则）====
        if (isConfidentChatOnly(msg)) {
            log.debug("SimpleChatClassifier: 本地规则 CHAT_ONLY | msg={}", msg);
            return true;
        }

        // ==== Tier 2: 高置信 NEED_TOOLS（本地规则）====
        if (isConfidentNeedTools(msg)) {
            log.debug("SimpleChatClassifier: 本地规则 NEED_TOOLS | msg={}", msg);
            return false;
        }

        // ==== Tier 3: 不确定 → false（宁可误判为需要Agent）====
        log.debug("SimpleChatClassifier: 不确定，返回 false（走 Agent）| msg={}", msg);
        return false;
    }

    // ==================== 本地规则实现 ====================

    /**
     * 高置信 CHAT_ONLY 判断。规则严格保守，只命中最明确的纯社交/情感表达。
     */
    private boolean isConfidentChatOnly(String msg) {
        // 规则 1：纯笑声/感叹叠词（如 "哈哈哈哈"、"嘿嘿嘿嘿"、"呵呵呵"）
        if (msg.matches("^[哈哈嘿呵嘻哦喔哟哇呀哎唉嗯]{3,}[！!。.]*$")) {
            return true;
        }

        // 规则 2：感谢变体（如 "谢谢！"、"太感谢了"、"多谢！"）
        if (msg.matches("^(谢谢|感谢|多谢|太感谢了|谢谢啦|谢谢您|感谢您)[！!。.]*$")) {
            return true;
        }

        // 规则 3：复合闲聊消息（如 "好的谢谢"、"嗯嗯知道了"、"哈哈好的"）
        // 剥离闲聊片段和标点后，若无剩余内容且总长≤15字，判定为闲聊
        if (msg.length() <= 15) {
            String stripped = msg;
            for (String fragment : CHAT_ONLY_FRAGMENTS) {
                stripped = stripped.replace(fragment, "");
            }
            stripped = stripped.replaceAll("[，。！？!?,，\\s~～]+", "").trim();
            if (stripped.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 高置信 NEED_TOOLS 判断。覆盖所有明确的工具/信息请求。
     */
    private boolean isConfidentNeedTools(String msg) {
        // 规则 1：Skill 领域关键词
        for (String keyword : SKILL_DOMAIN_KEYWORDS) {
            if (msg.contains(keyword)) {
                return true;
            }
        }

        // 规则 2：请求/询问表达
        for (String expr : REQUEST_EXPRESSIONS) {
            if (msg.contains(expr)) {
                return true;
            }
        }

        // 规则 3：疑问标记
        if (QUESTION_MARKERS.matcher(msg).find()) {
            return true;
        }

        return false;
    }

    // ==================== LLM 兜底（保留，当前不调用） ====================

    /**
     * 原 LLM 分类能力，保留为兜底。
     *
     * <p>当前策略：{@link #isSimpleChat} 中不确定直接返回 false，不调用此方法。
     * 保留以支持未来可能的「高精度模式」开关——在本地规则不确定时启用 LLM 二次判断。
     *
     * @param userMessage 用户消息
     * @return true = CHAT_ONLY，false = NEED_TOOLS
     */
    @SuppressWarnings("unused")
    private boolean llmClassify(String userMessage) {
        String prompt = "你是一个分类器。判断用户消息是否需要调用工具才能完整回答。\n"
                + "需要工具：查天气、查地图/地点/路线、查时间/日期/节假日、搜索网页、"
                + "生成图片、生成文件/文档、语音合成、交通推荐、打车、预算计算、"
                + "查课表/今天课表/导入课表/课程信息/考试安排/考试提醒、"
                + "设置提醒/定时提醒/自定义提醒/创建提醒/管理提醒、"
                + "动漫/番剧推荐、追番/订阅番剧/查询播出时间、"
                + "旅游/出游规划、持续关注/订阅/跟踪某类信息、管理记忆、操作文件。\n"
                + "不需要工具：纯粹的聊天、问答、解释、翻译、写作、闲聊、感谢。\n"
                + "判断规则：只要用户消息可能触发上述任一工具场景（包括提及\"推荐番剧\""
                + "\"追番\"\"旅游规划\"\"持续关注\"等），就返回 NEED_TOOLS。\n"
                + "如果用户消息很短（如\"好\"\"可以\"\"继续\"），可能是在回应之前提出的方案，"
                + "需要让工具系统处理，返回 NEED_TOOLS。\n"
                + "不确定时返回 NEED_TOOLS。\n"
                + "只返回一个词：NEED_TOOLS 或 CHAT_ONLY。";

        String result = llmClient.chatWithSystemPrompt(prompt, userMessage);
        return "CHAT_ONLY".equals(result != null ? result.trim() : "");
    }
}