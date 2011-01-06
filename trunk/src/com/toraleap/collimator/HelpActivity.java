package com.toraleap.collimator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;

public final class HelpActivity extends Activity {

	private Handler mHandler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON); 
		setContentView(R.layout.help_activity);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.dialog_help);
		WebView webView = (WebView) this.findViewById(R.id.HelpView);
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(false); 
		webSettings.setSavePassword(false);
		webView.addJavascriptInterface(new Object() {  //添加javascript可调用的接口
            @SuppressWarnings("unused")
			public void doSearch(final String key) {
                mHandler.post(new Runnable() {
                    public void run() {
                    	Intent intent = new Intent();
                    	intent.putExtra(Intent.EXTRA_TEXT, key);
                    	setResult(RESULT_OK, intent);
                        finish();
				    }
				});
			}
		}, "collimator");
		webView.loadUrl("file:///android_asset/" + getString(R.string.dialog_help_file));
	} 
}
