package pl.glitch.pocket;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.FrameLayout;
import java.io.OutputStream;
import java.util.Arrays;

public class MainActivity extends Activity {
  private static final String HOST = "appassets.androidplatform.net";
  private static final String HOME = "https://" + HOST + "/assets/index.html";
  private static final int CAMERA_PERMISSION = 100, FILE_PICKER = 101;
  private WebView web;
  private PermissionRequest cameraRequest;
  private ValueCallback<Uri[]> fileCallback;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(android.graphics.Color.rgb(13,16,16));
    root.setOnApplyWindowInsetsListener((view, insets) -> {
      view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
      return insets;
    });
    web = new WebView(this); root.addView(web, new FrameLayout.LayoutParams(-1, -1)); setContentView(root);
    root.requestApplyInsets();
    WebSettings settings = web.getSettings();
    settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false); settings.setAllowContentAccess(true);
    settings.setMediaPlaybackRequiresUserGesture(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    web.addJavascriptInterface(new DownloadBridge(), "AndroidDownloads");
    web.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri u = request.getUrl(); return !"https".equals(u.getScheme()) || !HOST.equals(u.getHost());
      }
      @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        Uri u = request.getUrl();
        // Android's document picker returns content:// URIs with a temporary read grant.
        // Let WebView read these locally; all other non-local origins remain blocked.
        if ("content".equals(u.getScheme()) || "blob".equals(u.getScheme())) return null;
        if (!"https".equals(u.getScheme()) || !HOST.equals(u.getHost())) return new WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", null, null);
        if (!"/assets/index.html".equals(u.getPath())) return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", null, null);
        try { return new WebResourceResponse("text/html", "UTF-8", getAssets().open("index.html")); }
        catch (Exception e) { return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", null, null); }
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override public void onPermissionRequest(PermissionRequest req) {
        runOnUiThread(() -> {
          Uri origin = req.getOrigin();
          if (!"https".equals(origin.getScheme()) || !HOST.equals(origin.getHost()) || !Arrays.asList(req.getResources()).contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) { req.deny(); return; }
          if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) req.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
          else { if (cameraRequest != null) cameraRequest.deny(); cameraRequest = req; requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION); }
        });
      }
      @Override public void onPermissionRequestCanceled(PermissionRequest req) { if (cameraRequest == req) cameraRequest = null; }
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
        if (fileCallback != null) fileCallback.onReceiveValue(null); fileCallback = cb;
        try { Intent intent = params.createIntent(); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivityForResult(intent, FILE_PICKER); return true; }
        catch (Exception e) { fileCallback = null; cb.onReceiveValue(null); return true; }
      }
    });
    web.loadUrl(HOME);
  }
  @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(code, permissions, results);
    if (code == CAMERA_PERMISSION && cameraRequest != null) {
      PermissionRequest req = cameraRequest; cameraRequest = null;
      if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) req.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE}); else req.deny();
    }
  }
  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == FILE_PICKER && fileCallback != null) {
      ValueCallback<Uri[]> callback = fileCallback; fileCallback = null;
      Uri[] selected = null;
      if (result == RESULT_OK && data != null) {
        if (data.getClipData() != null) {
          int count = data.getClipData().getItemCount(); selected = new Uri[count];
          for (int i = 0; i < count; i++) selected[i] = data.getClipData().getItemAt(i).getUri();
        } else if (data.getData() != null) selected = new Uri[]{data.getData()};
        else selected = WebChromeClient.FileChooserParams.parseResult(result, data);
      }
      callback.onReceiveValue(selected);
    }
  }
  @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
  @Override protected void onDestroy() { if (cameraRequest != null) cameraRequest.deny(); if (fileCallback != null) fileCallback.onReceiveValue(null); web.destroy(); super.onDestroy(); }
  private final class DownloadBridge {
    @JavascriptInterface public void save(String encoded, String name, String mime) {
      try {
        byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
        String safe = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        ContentValues values = new ContentValues(); values.put(MediaStore.Downloads.DISPLAY_NAME, safe);
        values.put(MediaStore.Downloads.MIME_TYPE, mime); values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GLITCH_MACHINE"); values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new Exception("Nie można utworzyć pliku");
        try (OutputStream out = getContentResolver().openOutputStream(uri)) { if (out == null) throw new Exception("Nie można zapisać pliku"); out.write(bytes); }
        catch (Exception error) { getContentResolver().delete(uri, null, null); throw error; }
        values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0); getContentResolver().update(uri, values, null, null);
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Zapisano w Pobrane/GLITCH_MACHINE", Toast.LENGTH_LONG).show());
      } catch (Exception e) { runOnUiThread(() -> Toast.makeText(MainActivity.this, "Błąd zapisu: " + e.getMessage(), Toast.LENGTH_LONG).show()); }
    }
  }
}
