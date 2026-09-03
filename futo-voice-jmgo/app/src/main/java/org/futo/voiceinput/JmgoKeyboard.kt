package org.futo.voiceinput

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class JmgoKeyboardLanguage {
    RUSSIAN,
    ENGLISH,
    NUMBERS,
}

object JmgoKeyboardLayout {
    private val russian = listOf(
        listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"),
        listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
        listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю"),
    )
    private val english = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m"),
    )
    private val numbers = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("-", "/", ":", ";", "(", ")", "₽", "&", "@"),
        listOf(".", ",", "?", "!", "'", "\"", "+", "="),
    )

    fun rows(language: JmgoKeyboardLanguage): List<List<String>> = when (language) {
        JmgoKeyboardLanguage.RUSSIAN -> russian
        JmgoKeyboardLanguage.ENGLISH -> english
        JmgoKeyboardLanguage.NUMBERS -> numbers
    }
}

@Composable
fun JmgoKeyboard(
    language: JmgoKeyboardLanguage,
    selection: JmgoKeyboardSelection,
    onLanguage: () -> Unit,
    onNumbers: () -> Unit,
    onText: (String) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onHide: () -> Unit,
    onEnter: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            JmgoKeyboardLayout.rows(language).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.forEachIndexed { columnIndex, label ->
                        Button(
                            onClick = { onText(label) },
                            modifier = Modifier.weight(1.0f).height(44.dp),
                            colors = jmgoButtonColors(selection == JmgoKeyboardSelection(rowIndex, columnIndex)),
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = onLanguage,
                    modifier = Modifier.weight(1.2f).height(46.dp),
                    colors = jmgoButtonColors(selection == JmgoKeyboardSelection(3, 0)),
                ) {
                    Text(if (language == JmgoKeyboardLanguage.RUSSIAN) "EN" else "РУ")
                }
                Button(
                    onClick = onNumbers,
                    modifier = Modifier.weight(1.2f).height(46.dp),
                    colors = jmgoButtonColors(selection == JmgoKeyboardSelection(3, 1)),
                ) {
                    Text("123")
                }
                Button(
                    onClick = onSpace,
                    modifier = Modifier.weight(5.0f).height(46.dp),
                    colors = jmgoButtonColors(selection == JmgoKeyboardSelection(3, 2)),
                ) {
                    Text("Пробел")
                }
                Button(
                    onClick = onBackspace,
                    modifier = Modifier.weight(1.5f).height(46.dp),
                    colors = jmgoButtonColors(selection == JmgoKeyboardSelection(3, 3)),
                ) {
                    Text("⌫")
                }
                Button(
                    onClick = onHide,
                    modifier = Modifier.weight(1.8f).height(46.dp),
                    colors = jmgoButtonColors(selection == JmgoKeyboardSelection(3, 4)),
                ) {
                    Text("▼ Скрыть")
                }
                Button(
                    onClick = onEnter,
                    modifier = Modifier.weight(1.8f).height(46.dp),
                    colors = jmgoButtonColors(selection == JmgoKeyboardSelection(3, 5)),
                ) {
                    Text("Поиск")
                }
            }
        }
    }
}

@Composable
private fun jmgoButtonColors(selected: Boolean) =
    ButtonDefaults.buttonColors(
        containerColor = if (selected) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
    )
