package com.github.zeng.alt.json.spi;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
public interface IEncryptService {

    String encrypt(String plainText);

    String decrypt(String cipherText);

}
