package com.youkeda.exercise.claw.feature.campus;

import com.youkeda.exercise.claw.domain.campus.CampusConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * CampusNotifier 调度行为测试。
 *
 * <p>修复背景：旧实现注入 {@code List<NotificationSource>}（自动收集全部 7 个源，
 * 含动漫 AnimeSource/AnimeSeasonSource），且 isSourceEnabled 恒返回 true →
 * 动漫源被校园调度重复执行，季度推荐可能一天推两次。
 *
 * <p>本测试聚焦 CampusNotifier 的调度语义：只消费注入的校园源、逐个执行 check、
 * 尊重配置开关。非校园源（动漫等）的隔离由 {@link CampusSource} 标记接口 + 构造器
 * {@code List<CampusSource>} 类型约束在编译期保证，无需运行时断言。
 */
@ExtendWith(MockitoExtension.class)
class CampusNotifierTest {

    @Mock
    private CampusSource sourceA;

    @Mock
    private CampusSource sourceB;

    private CampusNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new CampusNotifier(List.of(sourceA, sourceB));
    }

    @Test
    void notifyAllInvokesEachInjectedCampusSource() {
        CampusConfig config = new CampusConfig();

        notifier.notifyAll(config);

        verify(sourceA).check();
        verify(sourceB).check();
    }

    @Test
    void disabledSourceIsSkipped() {
        when(sourceA.getName()).thenReturn("ACTIVITY");
        CampusConfig config = new CampusConfig();
        config.setSourceEnabled(sourceA.getName(), false);

        notifier.notifyAll(config);

        verify(sourceA, never()).check();
        verify(sourceB).check();
    }

    @Test
    void oneSourceThrowingDoesNotBlockOthers() {
        CampusConfig config = new CampusConfig();
        doThrow(new RuntimeException("crawl down")).when(sourceA).check();

        notifier.notifyAll(config);

        verify(sourceA).check();
        verify(sourceB).check();
    }
}
