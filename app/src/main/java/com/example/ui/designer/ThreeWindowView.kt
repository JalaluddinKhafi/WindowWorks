package com.example.ui.designer

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.JSONArray
import org.json.JSONObject

/** Local-only WebGL renderer. No JavaScript bridge, network or file-system access. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ThreeWindowView(mesh: List<Polygon3D>, extentMm: Int, yaw: Float, pitch: Float, zoom: Float,
  perspective: Boolean, resetVersion: Int = 0, modifier: Modifier = Modifier, onUnavailable: () -> Unit) {
  val payload = remember(mesh, extentMm, yaw, pitch, zoom, perspective, resetVersion) {
    JSONObject().apply {
      put("extent", extentMm)
      put("yaw", yaw); put("pitch", pitch); put("zoom", zoom); put("perspective", perspective)
      put("resetVersion", resetVersion)
      put("faces", JSONArray().apply {
        mesh.forEach { face -> put(JSONObject().apply {
          put("vertices", JSONArray().apply { face.vertices.forEach { p -> put(JSONArray(listOf(p.x, -p.y, p.z))) } })
          put("color", JSONArray(listOf(face.baseColor.red, face.baseColor.green, face.baseColor.blue)))
          put("glass", face.isGlass)
        }) }
      })
    }.toString()
  }
  var webView by remember { mutableStateOf<WebView?>(null) }
  val latestPayload by rememberUpdatedState(payload)
  val failure by rememberUpdatedState(onUnavailable)
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  DisposableEffect(lifecycle, webView) {
    val view = webView
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> view?.onResume()
        Lifecycle.Event.ON_PAUSE -> view?.onPause()
        else -> Unit
      }
    }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }
  }
  AndroidView(modifier = modifier, factory = { context ->
    WebView(context).apply {
      setBackgroundColor(android.graphics.Color.rgb(15, 23, 42))
      settings.javaScriptEnabled = true
      settings.allowFileAccess = false
      settings.allowContentAccess = false
      settings.domStorageEnabled = false
      settings.blockNetworkLoads = true
      settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
      webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
          if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) post { failure() }
          return true
        }
      }
      webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
          val uri = request.url
          val path = uri.path.orEmpty().removePrefix("/")
          if (uri.scheme != "https" || uri.host != "designer.windowworks.invalid" || path.contains(".."))
            return WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", emptyMap(), "".byteInputStream())
          return try {
            val mime = when {
              path.endsWith(".js") -> "application/javascript"
              path.endsWith(".css") -> "text/css"
              else -> "text/html"
            }
            WebResourceResponse(mime, "UTF-8", context.assets.open("designer/$path"))
          } catch (_: Exception) {
            WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), "".byteInputStream())
          }
        }
        override fun onPageFinished(view: WebView, url: String) {
          view.evaluateJavascript("window.pendingWindow=$latestPayload;window.applyWindow?.(window.pendingWindow);", null)
        }
        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
          view.post { failure() }
        }
      }
      webView = this
      loadUrl("https://designer.windowworks.invalid/index.html")
    }
  }, update = { view ->
    view.evaluateJavascript("window.pendingWindow=$payload;window.applyWindow?.(window.pendingWindow);", null)
  }, onRelease = { view ->
    view.stopLoading(); view.onPause(); view.destroy()
  })
}
