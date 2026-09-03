package org.jmgo.input.web;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class WebDomScripts {
    private WebDomScripts() {}

    public static String runtime(Context context) throws IOException {
        try (InputStream input = context.getAssets().open("jmgo-web-input.js");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public static String install() { return "window.JmgoWebInput&&window.JmgoWebInput.install();"; }
    public static String insert(String text) { return call("insert", text); }
    public static String backspace() { return "window.JmgoWebInput&&window.JmgoWebInput.backspace();"; }
    public static String submit() { return "window.JmgoWebInput&&window.JmgoWebInput.submit();"; }
    public static String hasSafeActiveElement() {
        return "!!(window.JmgoWebInput&&window.JmgoWebInput.hasSafeActiveElement());";
    }

    private static String call(String method, String value) {
        return "window.JmgoWebInput&&window.JmgoWebInput." + method + "(" + quote(value) + ");";
    }

    static String quote(String value) {
        String source = value == null ? "" : value;
        StringBuilder out = new StringBuilder("\"");
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            switch (character) {
                case '\\': out.append("\\\\"); break;
                case '\"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (character < 0x20) out.append(String.format("\\u%04x", (int) character));
                    else out.append(character);
            }
        }
        return out.append('\"').toString();
    }
}
