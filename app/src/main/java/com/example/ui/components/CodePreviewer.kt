package com.example.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CodePreviewer(
    code: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, _, _, _ -> } },
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            val isMarkdown = fileName.endsWith(".md")
            if (isMarkdown) {
                // simple markdown rendering using marked.js via CDN
                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
                        <style>
                            body { font-family: sans-serif; padding: 16px; background: #0F0E13; color: #DFDCE3; }
                            pre { background: #16151B; padding: 16px; overflow-x: auto; }
                            code { font-family: monospace; }
                            a { color: #8A6C9C; }
                        </style>
                    </head>
                    <body>
                        <div id="content"></div>
                        <script>
                            document.getElementById('content').innerHTML = marked.parse(`${code.replace("`", "\\`").replace("$", "\\$")}`);
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            } else {
                webView.loadDataWithBaseURL(null, code, "text/html", "UTF-8", null)
            }
        }
    )
}
