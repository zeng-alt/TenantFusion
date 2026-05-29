package com.github.zeng.alt.i18n;

import com.github.zeng.alt.i18n.config.I18nAutoConfiguration;
import com.github.zeng.alt.i18n.core.I18nMessageService;
import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件模式下的 i18n 服务测试
 * <p>
 * 验证 {@code alt.i18n.mode=file} 时：
 * <ul>
 *   <li>从资源文件正确读取消息</li>
 *   <li>写入操作抛出 {@link UnsupportedOperationException}</li>
 * </ul>
 * </p>
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
@SpringBootTest(classes = I18nAutoConfiguration.class, properties = {
        "alt.i18n.mode=file",
        "alt.i18n.basename=test-messages",
        "spring.main.web-application-type=none"
})
class I18nMessageServiceFileModeTest {

    @Autowired
    private I18nMessageService i18nMessageService;

    @Autowired
    private MessageSource messageSource;

    @Test
    void testMessageSourceResolvesExistingKey() {
        String msg = messageSource.getMessage("test.hello", null, Locale.CHINA);
        assertEquals("你好", msg);
    }

    @Test
    void testMessageSourceThrowsForMissingKey() {
        assertThrows(NoSuchMessageException.class,
                () -> messageSource.getMessage("nonexistent", null, Locale.CHINA));
    }

    @Test
    void testFindByCodeAndLocaleExisting() {
        Option<I18nMessageDO> result = i18nMessageService.findByCodeAndLocale("test.hello", "zh_CN");
        assertTrue(result.isDefined());
        assertEquals("你好", result.get().getMessage());
    }

    @Test
    void testFindByCodeAndLocaleMissing() {
        Option<I18nMessageDO> result = i18nMessageService.findByCodeAndLocale("nonexistent", "zh_CN");
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByLocaleReturnsMessages() {
        List<I18nMessageDO> list = i18nMessageService.findByLocale("zh_CN");
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(m -> "test.hello".equals(m.getCode())));
    }

    @Test
    void testFindAllReturnsMessages() {
        List<I18nMessageDO> list = i18nMessageService.findAll();
        assertFalse(list.isEmpty());
    }

    @Test
    void testSaveThrowsUnsupportedOperation() {
        I18nMessageDO msg = new I18nMessageDO();
        msg.setCode("test.new");
        msg.setLocale("zh_CN");
        msg.setMessage("新消息");
        assertThrows(UnsupportedOperationException.class, () -> i18nMessageService.save(msg));
    }

    @Test
    void testDeleteByIdThrowsUnsupportedOperation() {
        assertThrows(UnsupportedOperationException.class, () -> i18nMessageService.deleteById(1L));
    }

    @Test
    void testDeleteByCodeAndLocaleThrowsUnsupportedOperation() {
        assertThrows(UnsupportedOperationException.class,
                () -> i18nMessageService.deleteByCodeAndLocale("test.hello", "zh_CN"));
    }
}
