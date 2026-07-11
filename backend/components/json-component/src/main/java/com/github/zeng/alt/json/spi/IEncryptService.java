package com.github.zeng.alt.json.spi;

public interface IEncryptService {

    String encrypt(String plainText);

    String decrypt(String cipherText);

}
