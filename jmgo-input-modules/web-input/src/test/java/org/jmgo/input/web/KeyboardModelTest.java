package org.jmgo.input.web;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class KeyboardModelTest {
    @Test
    public void exposesRussianEnglishAndNumericLayouts() {
        assertEquals("й", KeyboardModel.rows(KeyboardModel.Language.RUSSIAN).get(0).get(0));
        assertEquals("q", KeyboardModel.rows(KeyboardModel.Language.ENGLISH).get(0).get(0));
        assertEquals("1", KeyboardModel.rows(KeyboardModel.Language.NUMBERS).get(0).get(0));
    }

    @Test
    public void movesProportionallyBetweenRowsAndExposesSixActions() {
        KeyboardModel model = new KeyboardModel();
        for (int index = 0; index < 11; index++) model.move(KeyboardModel.Direction.RIGHT);
        model.move(KeyboardModel.Direction.DOWN);
        assertEquals(new KeyboardModel.Selection(1, 10), model.selection());

        model.setSelection(new KeyboardModel.Selection(3, 4));
        assertEquals(KeyboardModel.KeyType.HIDE, model.selectedKey().type());
        model.move(KeyboardModel.Direction.RIGHT);
        assertEquals(KeyboardModel.KeyType.SUBMIT, model.selectedKey().type());
    }

    @Test
    public void languageActionSwitchesRussianAndEnglishWhileNumbersIsExplicit() {
        KeyboardModel model = new KeyboardModel();
        model.setSelection(new KeyboardModel.Selection(3, 0));
        model.activateModeKey();
        assertEquals(KeyboardModel.Language.ENGLISH, model.language());
        model.activateModeKey();
        assertEquals(KeyboardModel.Language.RUSSIAN, model.language());

        model.setSelection(new KeyboardModel.Selection(3, 1));
        model.activateModeKey();
        assertEquals(KeyboardModel.Language.NUMBERS, model.language());
    }
}
