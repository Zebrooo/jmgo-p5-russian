package ru.zagonka.tv.wrapper;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VoiceSearchScriptTest {
    @Test
    public void safelyQuotesTextAndTargetsSearchInput() {
        String script = VoiceSearchScript.forQuery("фильм \"Мир\" \\ тест\n");

        assertTrue(script.contains("фильм \\\"Мир\\\" \\\\ тест\\n"));
        assertTrue(script.contains("input[type=search]"));
        assertTrue(script.contains("aria-label=\"Поиск\""));
        assertTrue(script.contains("setTimeout"));
        assertTrue(script.contains("dispatchEvent"));
        assertTrue(script.contains("focus"));
    }

    @Test
    public void focusesTheVisibleSearchFieldForRemoteVoiceInput() {
        String script = VoiceSearchScript.focusSearchField();

        assertTrue(script.contains("document.activeElement"));
        assertTrue(script.contains("input[type=search]"));
        assertTrue(script.contains("i.focus()"));
    }

    @Test
    public void opensTheFirstAutocompleteResultAfterItsAsyncArrival() {
        String script = VoiceSearchScript.submitFirstResult();

        assertTrue(script.contains("customScrollContentList"));
        assertTrue(script.contains("a[tabindex=\"0\"]"));
        assertTrue(script.contains("link.click()"));
        assertTrue(script.contains("setTimeout"));
    }
}
