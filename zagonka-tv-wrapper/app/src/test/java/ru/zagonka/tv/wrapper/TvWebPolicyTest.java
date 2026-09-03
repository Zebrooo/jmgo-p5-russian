package ru.zagonka.tv.wrapper;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TvWebPolicyTest {
    @Test
    public void usesDesktopTvViewport() {
        assertTrue(TvWebPolicy.userAgent().contains("Android TV"));
        assertTrue(TvWebPolicy.userAgent().contains("Safari"));
        assertTrue(TvWebPolicy.viewportScript().contains("width=1920"));
    }
}
