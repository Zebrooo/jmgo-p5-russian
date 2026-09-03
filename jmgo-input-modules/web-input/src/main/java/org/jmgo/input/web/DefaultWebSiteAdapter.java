package org.jmgo.input.web;

public class DefaultWebSiteAdapter implements WebSiteAdapter {
    @Override
    public String focusScript() { return WebDomScripts.hasSafeActiveElement(); }

    @Override
    public String insertScript(String text) { return WebDomScripts.insert(text); }

    @Override
    public String backspaceScript() { return WebDomScripts.backspace(); }

    @Override
    public String submitScript() { return WebDomScripts.submit(); }
}
