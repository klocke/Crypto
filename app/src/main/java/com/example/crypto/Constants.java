package com.example.crypto;

/**
 * Created by Tobias on 27.04.16.
 */
public final class Constants {

    private Constants() {
        throw new IllegalStateException("No instances.");
    }

    public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;

    // Argumente für Bundle
    public static final String ARG_PATH = PACKAGE_NAME + ".PATH";

    // Andere
    public static final int ACTION_OPEN_DOCUMENT_REQUEST_CODE = 1;
    public static final int BACK_PRESS_MILLIS_TIME_INTERVAL = 2000;

}
