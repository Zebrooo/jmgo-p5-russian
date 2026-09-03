package org.jmgo.input.core;

public final class VoiceRoutePolicy {
    private VoiceRoutePolicy() {}

    public static VoiceRoute select(boolean hasForegroundPackage, boolean resolvesWebVoice) {
        if (!hasForegroundPackage) return VoiceRoute.NONE;
        return resolvesWebVoice ? VoiceRoute.WEB : VoiceRoute.NATIVE;
    }
}
