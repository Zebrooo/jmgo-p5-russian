package org.jmgo.input.core;

import java.util.List;

public final class EditableTargetPolicy {
    private EditableTargetPolicy() {}

    public static int select(List<EditableCandidate> candidates, String originPackage) {
        if (candidates == null || originPackage == null || originPackage.isEmpty()) return -1;
        int fallback = -1;
        for (int index = 0; index < candidates.size(); index++) {
            EditableCandidate candidate = candidates.get(index);
            if (!isSafe(candidate, originPackage)) continue;
            if (candidate.isFocused()) return index;
            if (fallback < 0) fallback = index;
        }
        return fallback;
    }

    private static boolean isSafe(EditableCandidate candidate, String originPackage) {
        return candidate != null
                && originPackage.equals(candidate.packageName())
                && candidate.isEditable()
                && !candidate.isPassword()
                && candidate.isVisible();
    }
}
