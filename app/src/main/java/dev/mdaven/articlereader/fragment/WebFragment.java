package dev.mdaven.articlereader.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import dev.mdaven.articlereader.R;

public class WebFragment extends Fragment {

    private String url;

    private WebListener listener;

    private WebView webView;

    private boolean enableJavascript;

    public static WebFragment newInstance() {
        return new WebFragment();
    }

    public void initialize(String url, WebListener listener, boolean enableJavascript) {
        this.url = url;
        this.listener = listener;
        this.enableJavascript = enableJavascript;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_web, container, false);

        webView = view.findViewById(R.id.web_view);

        webView.setWebChromeClient(new WebChromeClient());
//        webView.addJavascriptInterface();
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        clearCache();
        webView.loadUrl(url);
        if(!enableJavascript) {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    runFindContents();
                }
            });
        }
        return view;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void runFindContents() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.evaluateJavascript("""
                (function() {
                    const pageContent = document.body.innerText;
                    const articles = document.getElementsByTagName("article");
                    if(articles.length == 1) {
                        const articleContent = articles[0].innerText;
                        return [pageContent, articleContent];
                    }
                    return [pageContent];
                } )()
                """, s -> {
            webView.getSettings().setJavaScriptEnabled(enableJavascript);
            String[] returnedValues = new Gson().fromJson(s, String[].class);
            listener.pageContent(returnedValues[0]);
            if (returnedValues.length >= 2) {
                listener.articleContent(returnedValues[1]);
            }
        });
    }

    private void clearCache() {
        // Clear all the Application Cache, Web SQL Database and the HTML5 Web Storage
        WebStorage.getInstance().deleteAllData();

        // Clear all the cookies
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        webView.loadUrl("javascript:document.open();document.close();");
        webView.loadUrl("about:blank");
        webView.clearCache(true);
        webView.clearFormData();
        webView.clearHistory();
        webView.clearSslPreferences();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void scrollToText(String text) {
        requireActivity().runOnUiThread(() -> {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.evaluateJavascript(String.format("""
                            (function () {
                                function getElementWithText(el, text) {
                                    for(const c of el.children) {
                                        if(c.innerText && c.innerText.includes(text)) {
                                            return getElementWithText(c, text);
                                        }
                                    }
                                    return el;
                                }
                            
                                const text = '%s';
                                const el = getElementWithText(document.body, text);

                                const diffHeight = window.innerHeight - el.offsetHeight;
                                let scrollTop = el.getBoundingClientRect().top + window.scrollY;
                                if(scrollTop < window.scrollY) scrollTop = window.scrollY;
                                else if(diffHeight > 0) scrollTop -= diffHeight / 2;
                                window.scroll({top: scrollTop, behavior: "smooth"});
                             } )()
                            """, text),
                    s -> webView.getSettings().setJavaScriptEnabled(enableJavascript));
        });
    }

    public interface WebListener {
        void articleContent(String content);

        void pageContent(String content);
    }
}