package com.example.crypto;

import java.security.KeyPair;

import javax.crypto.SecretKey;

/**
 * Created by Tobias on 27.04.16.
 */
public final class KeyWrapper {

    private final KeyPair _keyPair; // enthält PrivateKey und PublicKey
    private final SecretKey _secretKey;

    public KeyWrapper(KeyPair keyPair) {
        _keyPair = keyPair;
        _secretKey = null;
    }

    public KeyWrapper(SecretKey secretKey) {
        _secretKey = secretKey;
        _keyPair = null;
    }

    public KeyPair getKeyPair() {
        return _keyPair;
    }

    public SecretKey getSecretKey() {
        return _secretKey;
    }

    public boolean hasSecretKey() {
        return _secretKey != null;
    }

}
