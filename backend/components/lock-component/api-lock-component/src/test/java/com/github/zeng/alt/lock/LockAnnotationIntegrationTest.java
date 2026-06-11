package com.github.zeng.alt.lock;

import com.github.zeng.alt.lock.annotation.Lock;
import com.github.zeng.alt.lock.api.LockTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 闂傚倸鍊搁崐鎼佸磹缁嬭娑樜旈埀顒冪亱濠电偛妫欓幐鎼佸垂閸屾稒鍙忔慨妤€妫楅崝鎺楁煕閿旇骞愰柛瀣崌閺佹劖鎯旈垾宕囶啋婵犵鈧啿绾ч柟顔煎€垮濠氬Ω閵夈垺顫嶅┑鐐叉閿氶柛鎾睹琽ck 婵犵數濮烽弫鎼佸磻濞戔懞鍥敇閵忕姷顦棅顐㈡处閸戝綊寮?AOP 闂傚倸鍊烽懗鍫曞箠閹捐鍚归柡宥庡幗閳锋棃鏌涢弴銊ョ仩闁?
 *
 * @author zengJiaJun
 * @since 2026婵?6闂?1闂?
 * @version 1.0
 */
@SpringBootTest(classes = {LockAnnotationIntegrationTest.TestApplication.class, LockAnnotationIntegrationTest.LockTestService.class}, properties = {
        "spring.main.web-application-type=none"
})
class LockAnnotationIntegrationTest {

    @SpringBootApplication
    @EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
    static class TestApplication {
    }

    @Autowired
    private LockTestService lockTestService;

    @Autowired
    private LockTemplate lockTemplate;

    @Test
    void testLockAnnotationOnMethod() {
        String result = lockTestService.lockedMethod("testUser");
        assertEquals("locked:testUser", result);
    }

    @Test
    void testLockAnnotationWithConditionTrue() {
        String result = lockTestService.conditionalMethod(true);
        assertEquals("conditional:true", result);
    }

    @Test
    void testLockAnnotationWithConditionFalse() {
        String result = lockTestService.conditionalMethod(false);
        assertEquals("conditional:false", result);
    }

    @Test
    void testLockAnnotationWithSpelKey() {
        String result = lockTestService.spelKeyMethod("entity-123");
        assertEquals("spel:entity-123", result);
    }

    @Test
    void testMultipleLockAnnotations() {
        String result = lockTestService.multipleLocksMethod("multi-key");
        assertEquals("multi:multi-key", result);
    }

    @Test
    void testLockAnnotationAutoRelease() {
        // 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顖氱疀妞ゆ帊绶″Λ鐔兼⒑閼姐倕鏋戦柣鐔村劤閳ь剚鍑归崳锝嗕繆鐎涙绡€闁搞儯鍔庨崢鐢电磽娴ｈ娈曠紒瀣灥铻炴い鎾卞灩缂佲晛霉閻樺樊鍎愰柛濠勬暬閺岋綁鎮㈢粙娆炬闂佺粯甯楅幃鍌炲蓟閿熺姴鐒垫い鎺戝閺呮煡鏌涢埄鍐炬闁搞們鍊曢埞鎴︻敊閻偒浜畷顖炲垂椤旇鐏佹繛瀵稿帶閻°劑鎮￠弴銏＄厸闁稿本绻嶉崵娆忊攽椤旂厧鈧潡寮诲☉銏℃櫜闁糕剝锚閸炲鎮?
        String result = lockTestService.autoReleaseMethod("release-test");
        assertEquals("autoRelease:release-test", result);
    }

    @Service
    static class LockTestService {

        @Lock(name = "test-lock", keys = {"#p0"})
        public String lockedMethod(String userId) {
            return "locked:" + userId;
        }

        @Lock(name = "condition-lock", condition = "#p0", keys = {"#p0"})
        public String conditionalMethod(boolean condition) {
            return "conditional:" + condition;
        }

        @Lock(name = "spel-lock", keys = {"#p0"})
        public String spelKeyMethod(String entityId) {
            return "spel:" + entityId;
        }

        @Lock.List({
                @Lock(name = "multi-lock-1", keys = {"#p0"}),
                @Lock(name = "multi-lock-2", keys = {"#p0"})
        })
        public String multipleLocksMethod(String key) {
            return "multi:" + key;
        }

        @Lock(name = "auto-release", keys = {"#p0"}, autoRelease = true)
        public String autoReleaseMethod(String key) {
            return "autoRelease:" + key;
        }
    }
}
