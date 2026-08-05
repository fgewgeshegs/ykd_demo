package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.ai.llm.LLMClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SimpleChatClassifier 单元测试。
 *
 * <p>验证三阶判断的每一层：精确白名单 → 长度守卫 → CHAT_ONLY 规则 → NEED_TOOLS 规则 → 兜底。
 * 核心原则：宁可误判为需要Agent，也不误判为普通聊天。
 */
@DisplayName("SimpleChatClassifier 本地规则分类测试")
class SimpleChatClassifierTest {

    private LLMClient llmClient;
    private SimpleChatClassifier classifier;

    @BeforeEach
    void setUp() {
        llmClient = mock(LLMClient.class);
        classifier = new SimpleChatClassifier(llmClient);
    }

    // ==================== Guard 边界 ====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   "})
    @DisplayName("null / 空 / 纯空格 → false")
    void emptyOrNullShouldReturnFalse(String input) {
        assertFalse(classifier.isSimpleChat(input));
    }

    // ==================== Tier 0: 精确白名单（在长度守卫之前）====================

    @Nested
    @DisplayName("Tier 0 — 精确白名单（不受长度守卫限制）")
    class ExactWhitelist {

        @ParameterizedTest
        @ValueSource(strings = {
                "你好", "你好呀", "嗨", "哈喽", "hello", "hi", "hey",
                "早上好", "中午好", "下午好", "晚上好", "晚安", "早啊", "早呀"
        })
        @DisplayName("纯问候 → true（含 2 字短问候）")
        void greetingsShouldReturnTrue(String input) {
            assertTrue(classifier.isSimpleChat(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "谢谢", "感谢", "多谢", "辛苦了", "谢谢啦", "谢谢您", "感谢您"
        })
        @DisplayName("纯感谢 → true")
        void thanksShouldReturnTrue(String input) {
            assertTrue(classifier.isSimpleChat(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "再见", "拜拜", "bye", "回头见", "回见", "明天见", "下次见"
        })
        @DisplayName("纯道别 → true")
        void farewellShouldReturnTrue(String input) {
            assertTrue(classifier.isSimpleChat(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "哈哈", "嘿嘿", "呵呵", "哈哈哈", "太棒了", "厉害了"
        })
        @DisplayName("纯笑/感叹 → true")
        void laughterAndExclamationShouldReturnTrue(String input) {
            assertTrue(classifier.isSimpleChat(input));
        }
    }

    // ==================== 长度守卫（≤2 字非白名单）====================

    @Nested
    @DisplayName("长度守卫 — ≤2 字非白名单 → false（可能为方案确认/选择）")
    class LengthGuard {

        @ParameterizedTest
        @ValueSource(strings = {
                "好", "行", "嗯", "是", "对", "不", "啊", "哦",
                "OK", "ok", "Ok", "否", "选", "可"
        })
        @DisplayName("1-2 字非白名单 → false")
        void nonWhitelistShortMessagesShouldReturnFalse(String input) {
            assertFalse(classifier.isSimpleChat(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "好的", "收到", "可以", "不错", "很好", "知道了", "明白了", "嗯嗯"
        })
        @DisplayName("2 字确认/收到类 → false（可能为方案回应）")
        void ambiguousAcknowledgmentShouldReturnFalse(String input) {
            // 这些虽常见于闲聊，但也可能是对 Agent 方案的确认——宁可走 Agent
            assertFalse(classifier.isSimpleChat(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "继续", "确认", "取消", "算了"
        })
        @DisplayName("2 字操作词 → false（必须走 Agent）")
        void actionWordsShouldReturnFalse(String input) {
            assertFalse(classifier.isSimpleChat(input));
        }
    }

    // ==================== Tier 1: 高置信 CHAT_ONLY 规则 ====================

    @Nested
    @DisplayName("Tier 1 — 规则匹配 CHAT_ONLY（>2 字非白名单）")
    class RuleBasedChatOnly {

        @Test
        @DisplayName("叠字笑声 → true")
        void repeatedLaughterShouldReturnTrue() {
            assertTrue(classifier.isSimpleChat("哈哈哈哈"));
            assertTrue(classifier.isSimpleChat("嘿嘿嘿嘿嘿"));
            assertTrue(classifier.isSimpleChat("哈哈哈哈哈！"));
            assertTrue(classifier.isSimpleChat("呵呵呵呵"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "谢谢！", "太感谢了", "感谢您！", "多谢！", "谢谢啦！"
        })
        @DisplayName("感谢变体（带标点/修饰）→ true")
        void thanksVariantsShouldReturnTrue(String input) {
            assertTrue(classifier.isSimpleChat(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "好的谢谢", "哈哈好的", "收到谢谢",
                "好的辛苦了", "好的再见", "好的好的谢谢"
        })
        @DisplayName("复合闲聊（全由安全片段组成）→ true")
        void compoundChatShouldReturnTrue(String input) {
            assertTrue(classifier.isSimpleChat(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "嗯嗯知道了",   // "知道了" 可能表示确认，已从安全片段移除
                "知道了谢谢"    // 同上
        })
        @DisplayName("含确认语义片段的复合消息 → false（宁可误判走 Agent）")
        void compoundWithAcknowledgmentShouldReturnFalse(String input) {
            // "知道了" "明白了" 可能是对 Agent 方案的确认 → 不安全，走 Agent
            assertFalse(classifier.isSimpleChat(input));
        }

        @Test
        @DisplayName("复合闲聊不误判")
        void compoundChatEdgeCases() {
            // 全闲聊片段 → true
            assertTrue(classifier.isSimpleChat("好的谢谢"));

            // 含非闲聊成分 → 不应为 true（走后续判断）
            assertFalse(classifier.isSimpleChat("好的帮我"));
        }
    }

    // ==================== Tier 2: 高置信 NEED_TOOLS ====================

    @Nested
    @DisplayName("Tier 2 — NEED_TOOLS 命中")
    class ConfidentNeedTools {

        @ParameterizedTest
        @ValueSource(strings = {
                // weather
                "今天天气怎么样", "北京气温多少", "明天下雨吗", "深圳会下雪吗",
                "刮风了吗", "最近降温了吗", "上海多少度",
                // travel + transport
                "我想去旅游", "有什么旅行推荐", "帮我规划出游", "查看路线",
                "帮我导航", "叫车", "帮我打车", "怎么去高铁站", "附近有什么好吃的",
                // campus
                "查一下课表", "今天有什么课程", "考试安排", "什么时间上课",
                "下学期选什么课", "老师在哪个教室",
                // anime
                "推荐动漫", "最近有什么新番", "这部番怎么样", "追番推荐",
                // image
                "生成一张图片", "帮我画一幅山水画", "生成图片", "水墨画",
                // scout / search
                "搜索一下新闻", "推荐电影", "关注这个话题",
                "有什么新消息", "帮我搜集资料", "汇总信息",
                // schedule
                "设置提醒", "定时提醒我", "日程安排",
                // other
                "翻译一段英文", "帮我计算预算", "酒店预订"
        })
        @DisplayName("Skill 领域关键词 → false")
        void skillDomainKeywordsShouldReturnFalse(String input) {
            assertFalse(classifier.isSimpleChat(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "帮我写一篇文章", "告诉我今天有什么新闻", "有没有好吃的推荐",
                "怎么学Java", "哪里有打印店", "什么是Spring Boot",
                "深圳到北京多少公里", "什么时候考试", "查一下快递",
                "搜一下资料", "我要去北京", "我想学Python",
                "请问你是做什么的", "能不能帮我翻译", "可不可以推荐一下",
                "帮我查天气", "帮我看一下", "帮我找资源", "给我推荐一本书",
                "怎么去机场", "推荐一下电影"
        })
        @DisplayName("请求/询问表达 → false")
        void requestExpressionsShouldReturnFalse(String input) {
            assertFalse(classifier.isSimpleChat(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "今天下雨了吗", "你是AI吗", "这个怎么做呢",
                "明天有空吗", "你知道吧", "能帮我吗"
        })
        @DisplayName("疑问语气词 → false")
        void questionMarkersShouldReturnFalse(String input) {
            assertFalse(classifier.isSimpleChat(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "今天天气真好啊", "帮我查一下天气吧", "搜索Python教程",
                "我想去北京旅游", "推荐一部动漫给我"
        })
        @DisplayName("关键词+请求组合 → false")
        void combinedKeywordsShouldReturnFalse(String input) {
            assertFalse(classifier.isSimpleChat(input));
        }
    }

    // ==================== Tier 3: 不确定 → false ====================

    @Nested
    @DisplayName("Tier 3 — 不确定 → false（宁可误判为需要Agent）")
    class Uncertain {

        @ParameterizedTest
        @ValueSource(strings = {
                "我觉得挺有意思的",           // 有内容但不是明确请求
                "今天心情不错",               // 个人陈述
                "原来如此",                   // 理解确认（3字无关键词）
                "我也是这么想的",             // 附和
                "听起来不错哦",               // 评价
                "让我想想",                   // 思考中
                "有点复杂，我看不太懂",       // 表达困惑
                "还不错吧我觉得",             // 模糊评价
                "你说的对",                   // 认同
                "好像是的",                   // 模糊确认
        })
        @DisplayName("不确定消息 → false（走 Agent）")
        void uncertainMessagesShouldReturnFalse(String input) {
            assertFalse(classifier.isSimpleChat(input));
        }
    }

    // ==================== LLM 不被调用 ====================

    @Nested
    @DisplayName("LLM 调用次数验证")
    class LlmNotCalled {

        @Test
        @DisplayName("精确白名单命中 → LLM 不被调用")
        void llmNotCalledForExactWhitelist() {
            assertTrue(classifier.isSimpleChat("你好"));
            assertTrue(classifier.isSimpleChat("谢谢"));
            assertTrue(classifier.isSimpleChat("再见"));
            verifyNoInteractions(llmClient);
        }

        @Test
        @DisplayName("长度守卫命中 → LLM 不被调用")
        void llmNotCalledForLengthGuard() {
            // 1-2 字非白名单 → 直接 false
            assertFalse(classifier.isSimpleChat("好"));
            assertFalse(classifier.isSimpleChat("OK"));
            verifyNoInteractions(llmClient);
        }

        @Test
        @DisplayName("规则 CHAT_ONLY 命中 → LLM 不被调用")
        void llmNotCalledForRuleBasedChatOnly() {
            assertTrue(classifier.isSimpleChat("哈哈哈哈"));
            assertTrue(classifier.isSimpleChat("太感谢了"));
            assertTrue(classifier.isSimpleChat("好的谢谢"));
            verifyNoInteractions(llmClient);
        }

        @Test
        @DisplayName("NEED_TOOLS 命中 → LLM 不被调用")
        void llmNotCalledForNeedTools() {
            assertFalse(classifier.isSimpleChat("今天天气怎么样"));
            assertFalse(classifier.isSimpleChat("帮我查一下"));
            assertFalse(classifier.isSimpleChat("怎么去北京"));
            verifyNoInteractions(llmClient);
        }

        @Test
        @DisplayName("不确定 → LLM 不被调用（直接 false）")
        void llmNotCalledForUncertain() {
            assertFalse(classifier.isSimpleChat("我觉得挺有意思的"));
            assertFalse(classifier.isSimpleChat("今天心情不错"));
            verifyNoInteractions(llmClient);
        }
    }

    // ==================== 回归：关键场景 ====================

    @Nested
    @DisplayName("行为回归——关键场景验证")
    class Regression {

        @Test
        @DisplayName("明确需要工具的请求 → false")
        void toolRequestsShouldBeFalse() {
            assertFalse(classifier.isSimpleChat("深圳今天天气"));
            assertFalse(classifier.isSimpleChat("帮我规划去北京的路线"));
            assertFalse(classifier.isSimpleChat("查一下明天课表"));
            assertFalse(classifier.isSimpleChat("生成一张猫咪图片"));
            assertFalse(classifier.isSimpleChat("推荐好看的动漫"));
            assertFalse(classifier.isSimpleChat("设置明天8点的闹钟"));
        }

        @Test
        @DisplayName("方案确认/延续 → false（需要 Agent 上下文）")
        void continuationShouldBeFalse() {
            assertFalse(classifier.isSimpleChat("继续"));
            assertFalse(classifier.isSimpleChat("继续生成"));
            assertFalse(classifier.isSimpleChat("选第一个方案"));
            assertFalse(classifier.isSimpleChat("确认"));
            assertFalse(classifier.isSimpleChat("取消"));
            assertFalse(classifier.isSimpleChat("好的"));   // 2字，可能是对方案的确认
            assertFalse(classifier.isSimpleChat("收到"));   // 2字，可能是确认收到任务
        }

        @Test
        @DisplayName("纯社交表达 → true（可走快速通道）")
        void casualChatShouldBeTrue() {
            assertTrue(classifier.isSimpleChat("你好"));
            assertTrue(classifier.isSimpleChat("早上好"));
            assertTrue(classifier.isSimpleChat("谢谢"));
            assertTrue(classifier.isSimpleChat("再见"));
            assertTrue(classifier.isSimpleChat("太感谢了"));
        }
    }
}