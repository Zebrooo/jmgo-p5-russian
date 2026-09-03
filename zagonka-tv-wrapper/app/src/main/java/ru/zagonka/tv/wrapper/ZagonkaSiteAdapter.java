package ru.zagonka.tv.wrapper;

import org.jmgo.input.web.DefaultWebSiteAdapter;

final class ZagonkaSiteAdapter extends DefaultWebSiteAdapter {
    @Override
    public String submitScript() {
        return VoiceSearchScript.submitFirstResult();
    }
}
