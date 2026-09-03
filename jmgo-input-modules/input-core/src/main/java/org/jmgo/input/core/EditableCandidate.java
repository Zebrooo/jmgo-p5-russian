package org.jmgo.input.core;

public final class EditableCandidate {
    private final String packageName;
    private final boolean editable;
    private final boolean password;
    private final boolean visible;
    private final boolean focused;

    public EditableCandidate(
            String packageName,
            boolean editable,
            boolean password,
            boolean visible,
            boolean focused
    ) {
        this.packageName = packageName;
        this.editable = editable;
        this.password = password;
        this.visible = visible;
        this.focused = focused;
    }

    public String packageName() { return packageName; }
    public boolean isEditable() { return editable; }
    public boolean isPassword() { return password; }
    public boolean isVisible() { return visible; }
    public boolean isFocused() { return focused; }
}
