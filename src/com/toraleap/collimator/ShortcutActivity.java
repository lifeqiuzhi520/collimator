package com.toraleap.collimator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.toraleap.collimator.util.FileInfo;
import com.toraleap.collimator.util.ShortcutHelper;

public final class ShortcutActivity extends Activity {
	
	String mFilename;
	String mDefault;
	Uri mUri;
	int mIconId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shortcut_dialog);
        mUri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
        mFilename = Uri.decode(mUri.toString());
        if (mUri.getScheme().equalsIgnoreCase(SearchActivity.SEARCH_SCHEME)) {
        	mDefault = getIntent().getStringExtra(Intent.EXTRA_TITLE);
        	mIconId = R.drawable.menu_tag;
        }
        else {
        	mDefault = FileInfo.mainName(mFilename);
        	mIconId = R.drawable.menu_filecut;
        }
        showDialog(0);
    }  

	@Override
	protected Dialog onCreateDialog(int id) {
		final View edv = getLayoutInflater().inflate(R.layout.edittext_dialog, null);
		final EditText etv = (EditText)edv.findViewById(R.id.edtext);
		etv.setText(mDefault);
		return new AlertDialog.Builder(this)
			.setView(edv)
			.setIcon(R.drawable.menu_shortcut)
			.setTitle(R.string.dialog_shortcut_title)
			.setMessage(R.string.dialog_shortcut_message)
			.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String name = etv.getText().toString();
					if (null == name || name.length() == 0) {
						Toast.makeText(ShortcutActivity.this, R.string.dialog_shortcut_edit_noname, Toast.LENGTH_SHORT).show();
						ShortcutActivity.this.finish();
						return;
					}
					makeShortcut(etv.getText().toString());
					ShortcutActivity.this.finish();
				}
			})
			.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					ShortcutActivity.this.finish();
				}
			})
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					ShortcutActivity.this.finish();
				}
			})
			.create();
	}

	private void makeShortcut(String name) {
		if (name.length() > 0) {
	        Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
	        shortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
	        shortcutIntent.setDataAndType(mUri, FileInfo.mimeType(mFilename));
			ShortcutHelper shortcut = new ShortcutHelper(this, shortcutIntent);
			shortcut.setName(name)
				.setIconResource(Intent.ShortcutIconResource.fromContext(this, mIconId))
				.install(true);
			finish();
		} else {
			Toast.makeText(this, R.string.dialog_shortcut_edit_noname, Toast.LENGTH_SHORT).show();
		}
	}
}
