package ru.zagonka.tv.wrapper;

final class VoiceSearchScript {
    private VoiceSearchScript() {}

    static String focusSearchField() {
        return "(function(){"
                + "var a=document.activeElement;"
                + "var i=(a&&(a.tagName==='INPUT'||a.tagName==='TEXTAREA'))?a:"
                + "document.querySelector('input[type=search],input[name*=search i],input[placeholder*=\"поиск\" i],input[type=text]');"
                + "if(!i){return false;}i.focus();return document.activeElement===i;"
                + "})()";
    }

    static String submitFirstResult() {
        return "(function(){"
                + "var attempts=0;"
                + "function submit(){"
                + "var list=document.querySelector('[data-testid=\"customScrollContentList\"]');"
                + "var link=list&&list.querySelector('a[tabindex=\"0\"]');"
                + "if(link&&link.getBoundingClientRect().width>0){link.click();return;}"
                + "attempts++;if(attempts<30){setTimeout(submit,100);}"
                + "}submit();return true;"
                + "})()";
    }

    static String forQuery(String query) {
        String quoted = quote(query == null ? "" : query);
        return "(function(q){"
                + "var open=document.querySelector('button[aria-label=\"Поиск\"]');"
                + "if(open&&open.getBoundingClientRect().width){open.click();}"
                + "setTimeout(function(){"
                + "var a=document.activeElement;var i=(a&&(a.tagName==='INPUT'||a.tagName==='TEXTAREA'))?a:"
                + "document.querySelector('input[type=search],input[name*=search i],input[placeholder*=\"поиск\" i],input[type=text]');"
                + "if(!i){return;}i.focus();"
                + "var p=(i.tagName==='TEXTAREA'?HTMLTextAreaElement:HTMLInputElement).prototype;"
                + "var s=Object.getOwnPropertyDescriptor(p,'value').set;s.call(i,q);"
                + "i.dispatchEvent(new Event('input',{bubbles:true}));"
                + "i.dispatchEvent(new Event('change',{bubbles:true}));"
                + "},180);return true;})(" + quoted + ")";
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default: out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
