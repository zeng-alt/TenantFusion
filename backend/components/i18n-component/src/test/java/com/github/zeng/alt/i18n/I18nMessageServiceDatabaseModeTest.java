package com.github.zeng.alt.i18n;

import com.github.zeng.alt.i18n.core.I18nMessageService;
import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import com.github.zeng.alt.i18n.repository.I18nMessageRepository;
import io.vavr.control.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = I18nMessageServiceDatabaseModeTest.TestApplication.class, properties = {
        "alt.i18n.mode=database",
        "spring.datasource.url=jdbc:h2:mem:i18n_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "spring.main.web-application-type=none"
})
class I18nMessageServiceDatabaseModeTest {

    @SpringBootApplication
    static class TestApplication {
    }

    @Autowired
    private I18nMessageService i18nMessageService;

    @Autowired
    private I18nMessageRepository i18nMessageRepository;

    @Autowired
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        i18nMessageRepository.deleteAll();
    }

    @Test
    void testSaveAndFindByCodeAndLocale() {
        i18nMessageService.save(createMessage("user.login.success", "zh_CN", "登录成功"));
        Option<I18nMessageDO> found = i18nMessageService.findByCodeAndLocale("user.login.success", "zh_CN");
        assertTrue(found.isDefined());
        assertEquals("登录成功", found.get().getMessage());
    }

    @Test
    void testSaveMultipleLocales() {
        i18nMessageService.save(createMessage("greeting", "zh_CN", "你好"));
        i18nMessageService.save(createMessage("greeting", "en_US", "Hello"));
        assertEquals(2, i18nMessageService.findByCode("greeting").size());
    }

    @Test
    void testFindByCodeAndLocaleMissing() {
        assertTrue(i18nMessageService.findByCodeAndLocale("nonexistent", "zh_CN").isEmpty());
    }

    @Test
    void testFindByCode() {
        i18nMessageService.save(createMessage("key1", "zh_CN", "值1"));
        i18nMessageService.save(createMessage("key1", "en_US", "val1"));
        assertEquals(2, i18nMessageService.findByCode("key1").size());
    }

    @Test
    void testFindByLocale() {
        i18nMessageService.save(createMessage("a", "zh_CN", "A"));
        i18nMessageService.save(createMessage("b", "zh_CN", "B"));
        assertEquals(2, i18nMessageService.findByLocale("zh_CN").size());
    }

    @Test
    void testFindAll() {
        i18nMessageService.save(createMessage("x", "zh_CN", "X"));
        i18nMessageService.save(createMessage("y", "en_US", "Y"));
        assertTrue(i18nMessageService.findAll().size() >= 2);
    }

    @Test
    void testUpdateMessage() {
        i18nMessageService.save(createMessage("update.key", "zh_CN", "旧值"));
        I18nMessageDO existing = i18nMessageService.findByCodeAndLocale("update.key", "zh_CN").get();
        existing.setMessage("新值");
        i18nMessageService.save(existing);
        assertEquals("新值",
                i18nMessageService.findByCodeAndLocale("update.key", "zh_CN").get().getMessage());
    }

    @Test
    void testDeleteById() {
        I18nMessageDO saved = i18nMessageService.save(createMessage("del", "zh_CN", "删除"));
        assertTrue(i18nMessageService.findByCodeAndLocale("del", "zh_CN").isDefined());
        i18nMessageService.deleteById(saved.getId());
        assertTrue(i18nMessageService.findByCodeAndLocale("del", "zh_CN").isEmpty());
    }

    @Test
    void testDeleteByCodeAndLocale() {
        i18nMessageService.save(createMessage("del2", "zh_CN", "删除"));
        i18nMessageService.deleteByCodeAndLocale("del2", "zh_CN");
        assertTrue(i18nMessageService.findByCodeAndLocale("del2", "zh_CN").isEmpty());
    }

    @Test
    void testMessageSourceResolvesSavedMessage() {
        i18nMessageService.save(createMessage("ms.key", "zh_CN", "通过MessageSource解析"));
        assertEquals("通过MessageSource解析",
                messageSource.getMessage("ms.key", null, Locale.CHINA));
    }

    @Test
    void testMessageSourceThrowsForMissingKey() {
        assertThrows(org.springframework.context.NoSuchMessageException.class,
                () -> messageSource.getMessage("nonexistent", null, Locale.CHINA));
    }

    @Test
    void testCacheEvictionOnUpdate() {
        i18nMessageService.save(createMessage("cache.test", "zh_CN", "原值"));
        assertEquals("原值", messageSource.getMessage("cache.test", null, Locale.CHINA));
        I18nMessageDO msg = i18nMessageService.findByCodeAndLocale("cache.test", "zh_CN").get();
        msg.setMessage("新值");
        i18nMessageService.save(msg);
        assertEquals("新值", messageSource.getMessage("cache.test", null, Locale.CHINA));
    }

    @Test
    void testCacheEvictionOnDelete() {
        I18nMessageDO saved = i18nMessageService.save(createMessage("cache.del", "zh_CN", "将被删除"));
        assertEquals("将被删除", messageSource.getMessage("cache.del", null, Locale.CHINA));
        i18nMessageService.deleteById(saved.getId());
        assertThrows(org.springframework.context.NoSuchMessageException.class,
                () -> messageSource.getMessage("cache.del", null, Locale.CHINA));
    }

    private I18nMessageDO createMessage(String code, String locale, String message) {
        I18nMessageDO msg = new I18nMessageDO();
        msg.setCode(code);
        msg.setLocale(locale);
        msg.setMessage(message);
        return msg;
    }
}
