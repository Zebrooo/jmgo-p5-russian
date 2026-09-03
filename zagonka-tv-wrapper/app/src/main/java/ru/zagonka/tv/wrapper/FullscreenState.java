package ru.zagonka.tv.wrapper;

final class FullscreenState {
    private boolean fullscreen;

    boolean enter() {
        if (fullscreen) return false;
        fullscreen = true;
        return true;
    }

    boolean exit() {
        if (!fullscreen) return false;
        fullscreen = false;
        return true;
    }

    boolean isFullscreen() {
        return fullscreen;
    }
}
