package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
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

    Row(modifier = modifier
        .fillMaxSize()
        .background(DarkBackground)) {
        
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
        
        // Editor
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(16.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle,
                cursorBrush = SolidColor(SecondaryBronze),
                visualTransformation = CodeVisualTransformation(fileName),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
