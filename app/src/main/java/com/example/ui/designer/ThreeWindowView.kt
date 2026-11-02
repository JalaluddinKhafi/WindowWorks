package com.example.ui.designer

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
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
fun ThreeWindowView(
  mesh: List<Polygon3D>,
  extentMm: Int,
  yaw: Float,
  pitch: Float,
  zoom: Float,
  perspective: Boolean,
  resetVersion: Int = 0,
  modifier: Modifier = Modifier,
  onUnavailable: () -> Unit
) {
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
  val mainHandler = remember { Handler(Looper.getMainLooper()) }
  val lifecycle = LocalLifecycleOwner.current.lifecycle

  DisposableEffect(lifecycle, webView) {
    val view = webView
    val observer = LifecycleEventObserver { _, event ->
      try {
        when (event) {
          Lifecycle.Event.ON_RESUME -> view?.onResume()
          Lifecycle.Event.ON_PAUSE -> view?.onPause()
          else -> Unit
        }
      } catch (_: Throwable) {}
    }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }
  }

  AndroidView(
    modifier = modifier,
    factory = { context ->
      try {
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
              if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR ||
                  message.message()?.contains("WW_RENDERER_UNAVAILABLE") == true) {
                mainHandler.post { failure() }
              }
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
              try {
                view.evaluateJavascript("window.pendingWindow=$latestPayload;window.applyWindow?.(window.pendingWindow);", null)
              } catch (_: Throwable) {
                mainHandler.post { failure() }
              }
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
              mainHandler.post { failure() }
            }
            @Suppress("DEPRECATION")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
              mainHandler.post { failure() }
            }
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
              android.util.Log.w(
                "ThreeWindowView",
                "Chromium render process gone (didCrash=${detail.didCrash()}), cleanly handling and switching to native fallback."
              )
              try {
                (view.parent as? android.view.ViewGroup)?.removeView(view)
                view.destroy()
              } catch (_: Throwable) {}
              webView = null
              mainHandler.post { failure() }
              return true
            }
          }
          webView = this
          loadUrl("https://designer.windowworks.invalid/index.html")
        }
      } catch (e: Throwable) {
        android.util.Log.e("ThreeWindowView", "WebView creation failed", e)
        mainHandler.post { failure() }
        android.view.View(context)
      }
    },
    update = { view ->
      if (view is WebView) {
        try {
          view.evaluateJavascript("window.pendingWindow=$payload;window.applyWindow?.(window.pendingWindow);", null)
        } catch (_: Throwable) {
          mainHandler.post { failure() }
        }
      }
    },
    onRelease = { view ->
      if (view is WebView) {
        try {
          view.stopLoading()
          view.onPause()
          (view.parent as? android.view.ViewGroup)?.removeView(view)
          view.destroy()
        } catch (_: Throwable) {}
      }
      webView = null
    }
  )
}

