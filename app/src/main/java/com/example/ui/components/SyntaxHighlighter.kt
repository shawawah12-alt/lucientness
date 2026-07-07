package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class CodeVisualTransformation(private val fileName: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = buildAnnotatedString {
            val content = text.text
            val isHtml = fileName.endsWith(".html") || fileName.endsWith(".xml")
            
            if (isHtml) {
                // Simple HTML highlighting
                val tagRegex = Regex("</?[a-zA-Z0-9]+.*?>")
                val stringRegex = Regex("\".*?\"|'.*?'")
                var lastIndex = 0
                
                // This is a naive regex highlighter for HTML
                // For a robust app, a proper lexer should be used.
                val tagMatches = tagRegex.findAll(content)
                for (match in tagMatches) {
                    // Append text before tag
                    withStyle(SpanStyle(color = TextPrimary)) {
                        append(content.substring(lastIndex, match.range.first))
                    }
                    
                    // Inside the tag, we highlight strings as green
                    val tagStr = match.value
                    var tagLastIndex = 0
                    val strMatches = stringRegex.findAll(tagStr)
                    withStyle(SpanStyle(color = AccentCyan)) {
                        for (sMatch in strMatches) {
                            append(tagStr.substring(tagLastIndex, sMatch.range.first))
                            withStyle(SpanStyle(color = AccentGreen)) {
                                append(sMatch.value)
                            }
                            tagLastIndex = sMatch.range.last + 1
                        }
                        append(tagStr.substring(tagLastIndex, tagStr.length))
                    }
                    lastIndex = match.range.last + 1
                }
                withStyle(SpanStyle(color = TextPrimary)) {
                    append(content.substring(lastIndex, content.length))
                }
                
            } else {
                // Simple JS / Generic highlighting
                val keywords = listOf("function", "var", "let", "const", "return", "if", "else", "for", "while", "class", "import", "from", "export", "default")
                val keywordRegex = Regex("\\b(${keywords.joinToString("|")})\\b")
                val stringRegex = Regex("\".*?\"|'.*?'|`.*?`")
                val commentRegex = Regex("//.*|/\\\\*[\\\\s\\\\S]*?\\\\*/")
                val numberRegex = Regex("\\b\\d+\\b")
                
                // Tokenize and highlight (Very naive, does not handle overlaps perfectly)
                // We'll just do a simple pass
                val tokens = mutableListOf<Token>()
                commentRegex.findAll(content).forEach { tokens.add(Token(it.range, AccentCyan, 1)) }
                stringRegex.findAll(content).forEach { tokens.add(Token(it.range, AccentGreen, 2)) }
                keywordRegex.findAll(content).forEach { tokens.add(Token(it.range, PrimaryPurple, 3)) }
                
                // Resolve overlaps (higher priority wins or first wins)
                val sortedTokens = tokens.sortedBy { it.range.first }
                val validTokens = mutableListOf<Token>()
                var currentEnd = -1
                for (token in sortedTokens) {
                    if (token.range.first > currentEnd) {
                        validTokens.add(token)
                        currentEnd = token.range.last
                    }
                }
                
                var lastIndex = 0
                for (token in validTokens) {
                    withStyle(SpanStyle(color = TextPrimary)) {
                        append(content.substring(lastIndex, token.range.first))
                    }
                    withStyle(SpanStyle(color = token.color)) {
                        append(content.substring(token.range.first, token.range.last + 1))
                    }
                    lastIndex = token.range.last + 1
                }
                withStyle(SpanStyle(color = TextPrimary)) {
                    append(content.substring(lastIndex, content.length))
                }
            }
        }
        
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
    
    private data class Token(val range: IntRange, val color: Color, val priority: Int)
}
