package org.jmgo.input.web;

public interface WebSiteAdapter {
    String focusScript();
    String insertScript(String text);
    String backspaceScript();
    String submitScript();
}
