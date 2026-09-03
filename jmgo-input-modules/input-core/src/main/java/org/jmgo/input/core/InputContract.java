package org.jmgo.input.core;

public final class InputContract {
    public static final String ACTION_WEB_VOICE = "org.jmgo.input.action.WEB_VOICE";
    public static final String ACTION_WEB_VOICE_STARTED = "org.jmgo.input.action.WEB_VOICE_STARTED";
    public static final String ACTION_WEB_VOICE_RESULT = "org.jmgo.input.action.WEB_VOICE_RESULT";
    public static final String ACTION_NATIVE_VOICE_RESULT = "org.jmgo.input.action.NATIVE_VOICE_RESULT";
    public static final String ACTION_FINISH_RECOGNITION = "com.jmgo.action.AI_VOICE";
    public static final String EXTRA_SESSION_ID = "org.jmgo.input.extra.SESSION_ID";
    public static final String EXTRA_ORIGIN_PACKAGE = "org.jmgo.input.extra.ORIGIN_PACKAGE";
    public static final String EXTRA_RESULT = "org.jmgo.input.extra.RESULT";
    public static final String FUTO_PACKAGE = "org.futo.voiceinput.jmgo";
    public static final String RUSSIAN_LANGUAGE = "ru-RU";
    public static final long DEFAULT_SESSION_TIMEOUT_MS = 60_000L;

    private InputContract() {}
}
