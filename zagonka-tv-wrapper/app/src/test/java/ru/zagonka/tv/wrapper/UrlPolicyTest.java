package ru.zagonka.tv.wrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UrlPolicyTest {
    @Test
    public void allowsOnlyHttpsPagesOnZagonkaDomain() {
        assertTrue(UrlPolicy.isAllowed("https://www.zagonka-tv.org/"));
        assertTrue(UrlPolicy.isAllowed("https://www.zagonka-tv.org/7-filmy"));
        assertFalse(UrlPolicy.isAllowed("http://www.zagonka-tv.org/"));
        assertFalse(UrlPolicy.isAllowed("https://evil.example/"));
        assertFalse(UrlPolicy.isAllowed("https://www.zagonka-tv.org.evil.example/"));
    }
}
