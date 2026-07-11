package com.github.zeng.alt.json.spi;

/**
 * 数据库字典翻译服务 SPI。用户需实现此接口，并在 Spring 中注册为 Bean。
 * <p>
 * 当 {@code @DictFormat(dictType = "xxx")} 序列化时，会通过此接口将编码转为展示文本。
 *
 * @see com.github.zeng.alt.json.annotation.DictFormat
 */
public interface IDictTranslateService {

    /** 根据字典类型和编码返回翻译后的展示文本，未匹配时返回 {@code null}。 */
    String translate(String dictType, String code);

}
