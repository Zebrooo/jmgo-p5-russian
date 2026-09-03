package ru.zagonka.tv.wrapper;

import java.net.URI;
import java.net.URISyntaxException;

final class UrlPolicy {
    private static final String HOST = "www.zagonka-tv.org";

    private UrlPolicy() {}

    static boolean isAllowed(String value) {
        if (value == null) return false;
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && HOST.equalsIgnoreCase(uri.getHost());
        } catch (URISyntaxException ignored) {
            return false;
        }
    }
}
