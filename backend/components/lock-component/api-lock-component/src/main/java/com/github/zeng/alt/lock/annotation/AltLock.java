package com.github.zeng.alt.lock.annotation;
import com.github.zeng.alt.lock.executor.LockExecutor;
import com.github.zeng.alt.lock.model.DefaultLockFailureStrategy;
import com.github.zeng.alt.lock.model.DefaultLockKeyBuilder;
import com.github.zeng.alt.lock.model.LockFailureStrategy;
import com.github.zeng.alt.lock.model.LockKeyBuilder;
import java.lang.annotation.*;
/**
 * 鍒嗗竷寮忛攣娉ㄨВ锛屾敮鎸?SpEL 琛ㄨ揪寮忚В鏋?key
 *
 * @author zengJiaJun
 * @since 2026骞?6鏈?9鏃?
 * @version 1.0
 */
@Repeatable(AltLock.List.class)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface AltLock {
    /**
     * 鏉′欢琛ㄨ揪寮忥紝褰撶粨鏋滀负 {@code true} 鎴?{@code 'true'} 鏃舵墠鎵ц閿佹搷浣?
     */
    String condition() default "";
    /**
     * 閿佽祫婧愬悕绉帮紝涓虹┖鍒欎娇鐢?鍖呭悕+绫诲悕+鏂规硶鍚?
     */
    String name() default "";
    /**
     * 閿?key 鍚庣紑锛堟敮鎸?SpEL 琛ㄨ揪寮忥級锛屾渶缁?key = prefix:name#keys
     */
    String[] keys() default {};
    /**
     * 閿佽繃鏈熸椂闂达紙姣锛夛紝榛樿 -1 浣跨敤鍏ㄥ眬閰嶇疆
     */
    long expire() default -1;
    /**
     * 鑾峰彇閿佽秴鏃舵椂闂达紙姣锛夛紝榛樿 -1 浣跨敤鍏ㄥ眬閰嶇疆
     */
    long acquireTimeout() default -1;
    /**
     * 閿佹墽琛屽櫒
     */
    Class<? extends LockExecutor<?>> executor() default LockExecutor.class;
    /**
     * 鏂规硶鎵ц瀹屾垚鍚庢槸鍚﹁嚜鍔ㄩ噴鏀鹃攣
     */
    boolean autoRelease() default true;
    /**
     * 閿佸け璐ョ瓥鐣?
     */
    Class<? extends LockFailureStrategy> failStrategy() default LockFailureStrategy.class;
    /**
     * key 鏋勫缓鍣ㄧ瓥鐣?
     */
    Class<? extends LockKeyBuilder> keyBuilderStrategy() default LockKeyBuilder.class;
    /**
     * 鍙噸澶嶆敞瑙ｅ鍣?
     */
    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Documented
    @interface List {
        AltLock[] value();
    }
}
