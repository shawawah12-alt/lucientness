package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Renders a chat-style message containing Markdown-formatted text.
 *
 * Why we render to native Compose instead of using a WebView:
 *  - The chat surface must be selectable (long-press -> copy). Native
 *    [SelectionContainer] gives us that for free; a WebView would force us
 *    to wire up JS-based selection.
 *  - We want the message to inherit the app's dark-fantasy theme colors,
 *    which is trivial with [Text] but requires CSS injection in a WebView.
 *
 * Markdown features supported (a pragmatic subset, not full CommonMark):
 *  - **bold**           -> SpanStyle(fontWeight = Bold)
 *  - *italic* / _italic_ -> SpanStyle(fontStyle = Italic)
 *  - ~~strikethrough~~  -> SpanStyle(textDecoration = LineThrough)
 *  - `inline code`      -> SpanStyle(fontFamily = Monospace, bg tinted)
 *  - ```fenced code```  -> rendered as its own dark Card so multi-line code
 *                          blocks stay readable and copyable as one unit
 *  - # / ## / ### headings -> larger / bolder text
 *  - [label](url)       -> underlined accent-colored text (no nav handler
 *                          here; long-press copy lets the user grab the URL)
 *  - unordered lists (-, *, +) -> bullet glyph + indented text
 *  - ordered lists (1. 2. 3.) -> number prefix preserved
 *  - > blockquote       -> accent-colored italic text with vertical bar
 *  - horizontal rule (---) -> thin spacer
 *  - blank lines split paragraphs
 *
 * Unsupported constructs (tables, nested lists deeper than one level, etc.)
 * degrade gracefully to plain text.
 */

// ---------------------------------------------------------------------------
// Public Composable
// ---------------------------------------------------------------------------

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary,
    codeBlockBackground: Color = DarkSurface,
    codeBlockStroke: Color = AccentRed
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block -> renderBlock(block, textColor, codeBlockBackground, codeBlockStroke) }
    }
}

// ---------------------------------------------------------------------------
// Block model
// ---------------------------------------------------------------------------

private sealed class MdBlock {
    data class Paragraph(val spans: List<MdSpan>) : MdBlock()
    data class Heading(val level: Int, val spans: List<MdSpan>) : MdBlock()
    data class CodeBlock(val language: String?, val code: String) : MdBlock()
    data class Quote(val spans: List<MdSpan>) : MdBlock()
    data class ListItem(val ordered: Boolean, val index: Int, val spans: List<MdSpan>) : MdBlock()
    object HorizontalRule : MdBlock()
}

private sealed class MdSpan {
    data class Text(val text: String) : MdSpan()
    data class Bold(val inner: List<MdSpan>) : MdSpan()
    data class Italic(val inner: List<MdSpan>) : MdSpan()
    data class Strike(val inner: List<MdSpan>) : MdSpan()
    data class InlineCode(val text: String) : MdSpan()
    data class Link(val label: String, val url: String) : MdSpan()
}

// ---------------------------------------------------------------------------
// Inline parser
// ---------------------------------------------------------------------------

/**
 * Parse a single line of markdown into a list of styled spans.
 *
 * Order of precedence matters: we scan left-to-right, and the longest
 * matching delimiter wins. Code spans (`...`) take priority over
 * everything inside them, so we treat their content as literal text.
 */
private fun parseInline(raw: String): List<MdSpan> {
    val spans = mutableListOf<MdSpan>()
    val buffer = StringBuilder()
    var i = 0
    fun flushText() {
        if (buffer.isNotEmpty()) {
            spans.add(MdSpan.Text(buffer.toString()))
            buffer.clear()
        }
    }
    while (i < raw.length) {
        val c = raw[i]
        when {
            // Inline code: `...` (greedy, content is literal)
            c == '`' -> {
                val end = raw.indexOf('`', i + 1)
                if (end != -1) {
                    flushText()
                    spans.add(MdSpan.InlineCode(raw.substring(i + 1, end)))
                    i = end + 1
                    continue
                }
            }
            // Bold: **...** or __...__
            (c == '*' || c == '_') && i + 1 < raw.length && raw[i + 1] == c -> {
                val close = raw.indexOf("$c$c", i + 2)
                if (close != -1) {
                    flushText()
                    val inner = parseInline(raw.substring(i + 2, close))
                    spans.add(MdSpan.Bold(inner))
                    i = close + 2
                    continue
                }
            }
            // Italic: *...* or _..._
            c == '*' || c == '_' -> {
                val close = raw.indexOf(c, i + 1)
                if (close != -1 && close > i + 1) {
                    flushText()
                    val inner = parseInline(raw.substring(i + 1, close))
                    spans.add(MdSpan.Italic(inner))
                    i = close + 1
                    continue
                }
            }
            // Strikethrough: ~~...~~
            c == '~' && i + 1 < raw.length && raw[i + 1] == '~' -> {
                val close = raw.indexOf("~~", i + 2)
                if (close != -1) {
                    flushText()
                    val inner = parseInline(raw.substring(i + 2, close))
                    spans.add(MdSpan.Strike(inner))
                    i = close + 2
                    continue
                }
            }
            // Link: [label](url)
            c == '[' -> {
                val labelEnd = raw.indexOf(']', i + 1)
                if (labelEnd != -1 && labelEnd + 1 < raw.length && raw[labelEnd + 1] == '(') {
                    val urlEnd = raw.indexOf(')', labelEnd + 2)
                    if (urlEnd != -1) {
                        flushText()
                        val label = raw.substring(i + 1, labelEnd)
                        val url = raw.substring(labelEnd + 2, urlEnd)
                        spans.add(MdSpan.Link(label, url))
                        i = urlEnd + 1
                        continue
                    }
                }
            }
        }
        buffer.append(c)
        i++
    }
    flushText()
    return spans
}

// ---------------------------------------------------------------------------
// Block parser
// ---------------------------------------------------------------------------

private fun parseMarkdownBlocks(src: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = src.replace("\r\n", "\n").split("\n")
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        when {
            // Fenced code block ```lang ... ```
            trimmed.startsWith("```") -> {
                val lang = trimmed.removePrefix("```").trim().takeIf { it.isNotEmpty() }
                val sb = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(lines[i])
                    i++
                }
                if (i < lines.size) i++ // consume closing ```
                blocks.add(MdBlock.CodeBlock(lang, sb.toString()))
            }
            // Horizontal rule: --- or *** or ___ (3+ same chars, only those chars on line)
            Regex("^([-*_])\\1{2,}$").matches(trimmed) -> {
                blocks.add(MdBlock.HorizontalRule)
                i++
            }
            // Heading: # .. ######
            trimmed.startsWith("#") -> {
                val level = trimmed.takeWhile { it == '#' }.length.coerceAtMost(6)
                val content = trimmed.removePrefix("#".repeat(level)).trim()
                blocks.add(MdBlock.Heading(level, parseInline(content)))
                i++
            }
            // Blockquote: > ...
            trimmed.startsWith(">") -> {
                val sb = StringBuilder(trimmed.removePrefix(">").trim())
                i++
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    sb.append('\n').append(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                blocks.add(MdBlock.Quote(parseInline(sb.toString())))
            }
            // Unordered list: - / * / + followed by space
            Regex("^[-*+]\\s+.+").matches(trimmed) -> {
                val content = trimmed.drop(1).trim()
                blocks.add(MdBlock.ListItem(ordered = false, index = 0, spans = parseInline(content)))
                i++
            }
            // Ordered list: 1. / 2. ...
            Regex("^\\d+\\.\\s+.+").matches(trimmed) -> {
                val match = Regex("^(\\d+)\\.(.+)").find(trimmed)!!
                val idx = match.groupValues[1].toIntOrNull() ?: 1
                val content = match.groupValues[2].trim()
                blocks.add(MdBlock.ListItem(ordered = true, index = idx, spans = parseInline(content)))
                i++
            }
            // Blank line -> paragraph separator
            trimmed.isEmpty() -> i++
            // Paragraph: collect until blank line or block-starter
            else -> {
                val sb = StringBuilder(trimmed)
                i++
                while (i < lines.size) {
                    val next = lines[i].trim()
                    if (next.isEmpty() ||
                        next.startsWith("#") ||
                        next.startsWith("```") ||
                        next.startsWith(">") ||
                        Regex("^[-*+]\\s+.+").matches(next) ||
                        Regex("^\\d+\\.\\s+.+").matches(next) ||
                        Regex("^([-*_])\\1{2,}$").matches(next)
                    ) break
                    sb.append('\n').append(next)
                    i++
                }
                blocks.add(MdBlock.Paragraph(parseInline(sb.toString())))
            }
        }
    }
    return blocks
}

// ---------------------------------------------------------------------------
// Block renderer
// ---------------------------------------------------------------------------

@Composable
private fun renderBlock(
    block: MdBlock,
    textColor: Color,
    codeBg: Color,
    codeStroke: Color
) {
    when (block) {
        is MdBlock.Paragraph -> {
            Text(
                text = renderSpans(block.spans, textColor),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        }
        is MdBlock.Heading -> {
            val (size, weight) = when (block.level) {
                1 -> 22.sp to FontWeight.Bold
                2 -> 20.sp to FontWeight.Bold
                3 -> 18.sp to FontWeight.SemiBold
                4 -> 16.sp to FontWeight.SemiBold
                else -> 14.sp to FontWeight.Medium
            }
            Text(
                text = renderSpans(block.spans, textColor),
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = size,
                    fontWeight = weight
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
            )
        }
        is MdBlock.CodeBlock -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = codeBg),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, codeStroke.copy(alpha = 0.4f))
            ) {
                Text(
                    text = block.code,
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        is MdBlock.Quote -> {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                // Visible vertical accent bar.
                Spacer(modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(AccentRed))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = renderSpans(block.spans, AccentRed),
                    color = AccentRed,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        is MdBlock.ListItem -> {
            val prefix = if (block.ordered) "${block.index}." else "•"
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Text(
                    text = prefix,
                    color = AccentRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(20.dp)
                )
                Text(
                    text = renderSpans(block.spans, textColor),
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        MdBlock.HorizontalRule -> {
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TextSecondary.copy(alpha = 0.4f))
                .padding(vertical = 4.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Span -> AnnotatedString
// ---------------------------------------------------------------------------

private fun renderSpans(spans: List<MdSpan>, baseColor: Color): AnnotatedString =
    buildAnnotatedString {
        for (span in spans) renderSpan(span, baseColor)
    }

private fun AnnotatedString.Builder.renderSpan(span: MdSpan, color: Color) {
    when (span) {
        is MdSpan.Text -> append(span.text)
        is MdSpan.Bold -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                for (inner in span.inner) renderSpan(inner, color)
            }
        }
        is MdSpan.Italic -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = color)) {
                for (inner in span.inner) renderSpan(inner, color)
            }
        }
        is MdSpan.Strike -> {
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = color)) {
                for (inner in span.inner) renderSpan(inner, color)
            }
        }
        is MdSpan.InlineCode -> {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = color.copy(alpha = 0.12f),
                    color = color
                )
            ) {
                append(" ${span.text} ")
            }
        }
        is MdSpan.Link -> {
            pushStringAnnotation(tag = "URL", annotation = span.url)
            withStyle(SpanStyle(color = AccentRed, textDecoration = TextDecoration.Underline)) {
                append(span.label)
            }
            pop()
        }
    }
}

// (End of MarkdownText.kt)
