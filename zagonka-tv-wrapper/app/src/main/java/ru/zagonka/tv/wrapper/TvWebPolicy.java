package ru.zagonka.tv.wrapper;

final class TvWebPolicy {
    private TvWebPolicy() {}

    static String userAgent() {
        return "Mozilla/5.0 (Linux; Android TV 13; C611) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/101.0.4951.61 Safari/537.36";
    }

    static String viewportScript() {
        return "(function(){var m=document.querySelector('meta[name=viewport]');"
                + "if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}"
                + "m.content='width=1920, initial-scale=1, maximum-scale=1, user-scalable=no';"
                + "document.documentElement.style.minWidth='1920px';return [innerWidth,innerHeight];})()";
    }
}
