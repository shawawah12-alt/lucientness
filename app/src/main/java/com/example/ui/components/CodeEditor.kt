package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SecondaryBronze
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fileName: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val lineCount = value.text.count { it == '\n' } + 1

    val textStyle = TextStyle(
        color = TextPrimary,
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 20.sp
    )

    // FIX (v1.3.0): BasicTextField inside a horizontalScroll(Box.fillMaxSize)
    // would not show the soft keyboard when the user tapped the empty area
    // of a freshly-created empty file. The horizontal scroll gesture
    // detector intercepted the tap before it reached the text field, so
    // the field never got focus.
    //
    // Fix:
    //   1. Move horizontalScroll onto the BasicTextField itself (instead
    //      of the wrapper Box), so the text field owns its own gesture
    //      handling and is focusable everywhere.
    //   2. Wrap the whole Row in a clickable that requests focus and
    //      shows the keyboard — so a tap anywhere on the editor region
    //      (including the empty area below the last line) focuses the
    //      field and pops up the keyboard.
    //   3. Auto-focus the field once on first composition (keyed to the
    //      file name) so the user can start typing immediately after
    //      opening or creating a file.
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(fileName) {
        // Small delay so the Compose tree is settled before we request
        // focus; otherwise the focus request can race with layout.
        kotlinx.coroutines.delay(100)
        runCatching { focusRequester.requestFocus() }
        keyboardController?.show()
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            // Tap anywhere on the editor region -> focus field + show keyboard.
            .clickable {
                runCatching { focusRequester.requestFocus() }
                keyboardController?.show()
            }
    ) {

        // Gutter (Line Numbers)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(48.dp)
                .background(DarkSurface)
                .padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            val linesText = (1..lineCount).joinToString("\n")
            Text(
                text = linesText,
                style = textStyle.copy(color = TextSecondary, textAlign = TextAlign.End),
                modifier = Modifier.fillMaxSize()
            )
        }

        // Editor — horizontalScroll is now on the BasicTextField itself so
        // the field receives taps across the whole editor area and can
        // request the soft keyboard normally.
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(SecondaryBronze),
            visualTransformation = CodeVisualTransformation(fileName),
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(16.dp)
                .focusRequester(focusRequester)
        )
    }
}
