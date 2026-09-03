package ru.zagonka.tv.wrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jmgo.input.web.DefaultWebSiteAdapter;
import org.junit.Test;

public final class ZagonkaSiteAdapterTest {
    @Test
    public void keepsAutocompleteSelectionOutOfTheGenericAdapter() {
        String zagonkaSubmit = new ZagonkaSiteAdapter().submitScript();
        String genericSubmit = new DefaultWebSiteAdapter().submitScript();

        assertTrue(zagonkaSubmit.contains("customScrollContentList"));
        assertFalse(genericSubmit.contains("customScrollContentList"));
    }
}
