package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Code preview rendered inside a WebView.
 *
 * IMPORTANT (fix v1.2.0):
 * The previous implementation wrapped the AndroidView with:
 *     .pointerInput(Unit) { detectTransformGestures { _, _, _, _ -> } }
 * That detector only reliably consumes *multi-touch* transforms (pinch / rotate).
 * A single-pointer horizontal swipe was NOT consumed, so the touch event bubbled
 * up to the parent [androidx.compose.material3.ModalNavigationDrawer], which
 * interpreted it as an "open drawer" edge gesture. The empty lambda also stole
 * touch events from the WebView, which is why the preview only moved a tiny bit
 * when the user tried to scroll it horizontally.
 *
 * Fix: drop the broken pointerInput entirely so the WebView owns its touch
 * stream, and disable the drawer's swipe gesture while preview mode is active
 * (handled in MainScreen via `gesturesEnabled = !isPreviewMode`).
 */
@Composable
fun CodePreviewer(
    code: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Make the WebView scrollable in both directions inside the
                // preview pane so the user can pan around the rendered HTML.
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = true
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
