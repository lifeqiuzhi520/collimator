package com.toraleap.collimator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TouchDelegate;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.toraleap.collimator.bll.FileScannerService;
import com.toraleap.collimator.data.Expression;
import com.toraleap.collimator.data.Index;
import com.toraleap.collimator.data.Match;
import com.toraleap.collimator.data.Matcher;
import com.toraleap.collimator.ext.Playlist;
import com.toraleap.collimator.ui.FloatingDialog;
import com.toraleap.collimator.ui.MatchAdapter;
import com.toraleap.collimator.util.FileInfo;
import com.toraleap.collimator.util.SoftCache;

/**
 * Collimator 主活动，负责搜索处理，结果显示等操作。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2011.0515
 */
public class SearchActivity extends Activity implements OnClickListener, OnItemLongClickListener, OnItemSelectedListener, OnItemClickListener {

	public static final String SEARCH_SCHEME = "collimator";
	
	private static final int DIALOG_OPENAS = 1;
	private static final int DIALOG_RESULT = 2;
	private static final int DIALOG_SORT = 3;
	private static final int DIALOG_LAYOUT = 4;
	private static final int DIALOG_PICK = 5;
	private static final int DIALOG_RESULT_STATICS = 7;
	private static final int DIALOG_RESULT_PLAYLIST_CREATE = 8;
	private static final int DIALOG_RESULT_DELETE = 9;
	private static final int DIALOG_RESULT_DELETE_FORBIDDEN = 10;
	private static final int DIALOG_FILTER_RANGE = 11;
	private static final int DIALOG_OPENAS_DELETE = 12;
	private static final int DIALOG_INDEX_OBSOLETE = 100;
	private static final int DIALOG_FIRST_LAUNCH = 101;
	private static final int ITEM_OPEN_VIEW = 0;
	private static final int ITEM_OPEN_LOCATE = 1;
	private static final int ITEM_OPEN_EDIT = 2;
	private static final int ITEM_OPEN_CHOOSE = 3;
	private static final int ITEM_OPEN_SEND = 4;
	private static final int ITEM_OPEN_DELETE = 5;
	private static final int ITEM_RESULT_STATICS = 0;
	private static final int ITEM_RESULT_SORT = 1;
	private static final int ITEM_RESULT_SHORTCUT = 2;
	private static final int ITEM_RESULT_PLAYLIST = 3;
	private static final int ITEM_RESULT_DELETE = 4;
	private static final int ITEM_LAYOUT_TILE = 0;
	private static final int ITEM_LAYOUT_ICON = 1;
	private static final int ITEM_LAYOUT_DIGEST = 2;
	private static final int REQUEST_PREFERENCE = 1;
	private static final int REQUEST_HELP = 2;
	private static final int MENU_RESULT = Menu.FIRST;
	private static final int MENU_SORT = Menu.FIRST + 1;
	private static final int MENU_RANGE = Menu.FIRST + 2;
	private static final int MENU_RELOAD = Menu.FIRST + 3;
	private static final int MENU_PREFERENCES = Menu.FIRST + 4;
	private static final int MENU_HELP = Menu.FIRST + 5;
	private static final int MENU_LAYOUT = Menu.FIRST + 6;
	
	private static final int[] ICON_FILTER_RANGE = {R.drawable.menu_all, R.drawable.menu_image, R.drawable.menu_audio, R.drawable.menu_video, R.drawable.menu_document, R.drawable.menu_executable, R.drawable.menu_known};
	private static final int[] ICON_BUTTON_RANGE = {R.drawable.button_search_all, R.drawable.button_search_image, R.drawable.button_search_audio, R.drawable.button_search_video, R.drawable.button_search_document, R.drawable.button_search_executable, R.drawable.button_search_known};
	private static final int[] ICON_RESULT = {R.drawable.menu_result, R.drawable.menu_sort, R.drawable.menu_shortcut, R.drawable.menu_playlist, R.drawable.menu_delete};
	private static final int[] ICON_PICK = {R.drawable.menu_result, R.drawable.menu_sort};
	private static final int[] ICON_RESULT_AMOUNT = {R.drawable.button_star_empty, R.drawable.button_star_half, R.drawable.button_star_full};
	private static final int[] STRING_PICK_TOAST = {R.string.status_pick_file, R.string.status_pick_image, R.string.status_pick_audio, R.string.status_pick_video, R.string.status_pick_file, R.string.status_pick_file, R.string.status_pick_file};

	SharedPreferences mPreferences;
	NotificationManager mNotificationManager;
	Notification mReloadNotification;
	final EventHandler mEventHandler = new EventHandler();
	final List<Match> mSearchResult = new ArrayList<Match>();
	MatchAdapter mListAdapter;
	// 当前状态变量
	Match mSelectedMatch;
	Expression mExpression;
	boolean isSearching = false;
	boolean isPickMode = false;
	boolean isRangeLocked = false;
	long startTick = 0;
	// 首选项参数
	boolean isTapView = true;
	boolean isDeletePermitted = false;
	boolean isReloadWithoutPrompt = false;
	boolean isRefreshingInstant = false;
	boolean isFirstLaunch = true;
	int displayLayout = MatchAdapter.LAYOUT_TILE;
	// 界面控件
	ImageButton mButtonRange;
	ImageButton mButtonStar;
	EditText mEditSearch;
	GridView mListEntries;
	TextView mTextStatus;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent();
        intent.setClass(this, FileScannerService.class);
        startService(intent);
       	setContentView(R.layout.search_activity);
       	initUtils();
       	initViews();
       	Index.deserialization();
    }
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
       	updatePreferences();
		super.onResume();
	}

	private void initUtils() {
       	Intent intent = new Intent();
       	intent.setClass(SearchActivity.this, SearchActivity.class);
       	intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
       	final PendingIntent appIntent = PendingIntent.getActivity(SearchActivity.this, 0, intent, 0);
        mNotificationManager = (NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);
        mReloadNotification = new Notification();
        mReloadNotification.icon = R.drawable.stat_notify_reloading;
        mReloadNotification.flags = Notification.FLAG_ONGOING_EVENT;
        mReloadNotification.contentIntent = appIntent;
        mReloadNotification.when = 0;		
    	mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
       	Index.init(getApplicationContext(), mEventHandler);
	}

	private void initViews() {
       	Intent intent = getIntent();
		mEditSearch = (EditText)findViewById(R.id.EditSearch);
       	mButtonRange = (ImageButton)findViewById(R.id.ButtonRange);
       	mButtonRange.setOnClickListener(this);
       	// 增大范围选择按钮的触摸范围
       	final View parent = (View)mButtonRange.getParent();
       	parent.post(new Runnable() {
			public void run() {
				final Rect r = new Rect();
				mButtonRange.getHitRect(r);
				r.left = 0;
				r.top = 0;
				r.bottom = parent.getHeight();
				parent.setTouchDelegate(new TouchDelegate(r, mButtonRange));
			}
       	});
       	mButtonStar = (ImageButton)findViewById(R.id.ButtonStar);
       	mButtonStar.setOnClickListener(this);
       	mListAdapter = new MatchAdapter(getLayoutInflater(), mSearchResult);
       	mListEntries = (GridView)findViewById(R.id.ListEntries);
       	mListEntries.setAdapter(mListAdapter);
       	mListEntries.setOnItemClickListener(this);
       	mListEntries.setOnItemSelectedListener(this);
       	mListEntries.setOnItemLongClickListener(this);
       	mTextStatus = (TextView)findViewById(R.id.TextStatus);
       	mTextStatus.setOnClickListener(this);
       	
       	String action = intent.getAction();
       	if (Intent.ACTION_VIEW.equals(action) && SEARCH_SCHEME.equalsIgnoreCase(intent.getData().getScheme())) {
       		mExpression = new Expression(getApplicationContext(), intent.getData().getFragment());
           	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
       		updateUI();
       	} else if (Intent.ACTION_SEARCH.equals(action)) {
       		Log.e("extras", intent.getStringExtra(SearchManager.USER_QUERY));
       		mExpression = new Expression(getApplicationContext());
       		String query = intent.getStringExtra(SearchManager.QUERY);
       		if (query == null) query = intent.getDataString();
       		if (query == null) query = intent.getStringExtra(SearchManager.USER_QUERY);
       		if (query == null) query = "";
       		mExpression.setKey(query);
           	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
       		updateUI();
       	} else if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
       		String type = intent.getType();
       		if (type == null) {
       			enterPickMode(0, false);
       		} else if (type.startsWith("image/")) {
       			// 由于系统 GMail 软件的一个 BUG，添加附件时，发送的是 image/* 类型请求，因此为了兼容，不能锁定范围，只能默认到图片选择。
       			enterPickMode(1, false);
       		} else if (type.startsWith("audio/")) {
       			enterPickMode(2, true);
       		} else if (type.startsWith("video/")) {
       			enterPickMode(3, true);
       		} else {
       			enterPickMode(0, false);
       		}
       		updateUI();
       	} else if (RingtoneManager.ACTION_RINGTONE_PICKER.equals(action)) {
   			enterPickMode(2, true);
       	} else {
       		Log.e("else", action);
       		mExpression = new Expression(getApplicationContext());
       	}
       	
       	mEditSearch.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable arg0) { }
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				mExpression.setKey(s.toString());
				doSearch();
			}});
	}

	private void updatePreferences() {
		SharedPreferences prefs = mPreferences;
		try {
			Index.init(getApplicationContext(), mEventHandler);
			Matcher.init(prefs, mEventHandler);
	       	FileInfo.init(prefs, getApplicationContext());
	       	isTapView = prefs.getBoolean("operation_tap", true);
	       	isDeletePermitted = prefs.getBoolean("operation_delete", false);
	       	isReloadWithoutPrompt = prefs.getBoolean("index_without_prompt", false);
	    	isRefreshingInstant = prefs.getBoolean("display_instant", false);
	    	isFirstLaunch = prefs.getBoolean("display_firstlaunch", true);
	       	displayLayout = Integer.parseInt(prefs.getString("display_layout", "0"));
	       	setLayout(displayLayout);
	       	mTextStatus.setVisibility(prefs.getBoolean("display_statusbar", true) ? View.VISIBLE : View.GONE);
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), R.string.status_error_preferences, Toast.LENGTH_SHORT).show();
		}
		if (isFirstLaunch) showDialog(DIALOG_FIRST_LAUNCH);
		else tryReloadIndex();
	}
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_RANGE, 0, R.string.menu_range).setIcon(R.drawable.ic_menu_range);
		menu.add(0, MENU_LAYOUT, 0, R.string.menu_layout).setIcon(android.R.drawable.ic_menu_view);
		menu.add(0, MENU_RESULT, 0, R.string.menu_result).setIcon(android.R.drawable.ic_menu_slideshow);
		menu.add(0, MENU_RELOAD, 0, R.string.menu_reload).setIcon(R.drawable.ic_menu_reload);
		menu.add(0, MENU_PREFERENCES, 0, R.string.menu_preferences).setIcon(android.R.drawable.ic_menu_preferences);
    	menu.add(0, MENU_HELP, 0, R.string.menu_help).setIcon(android.R.drawable.ic_menu_help);
    	return true;
    }

    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(MENU_RANGE).setEnabled(!isRangeLocked);
		menu.findItem(MENU_RELOAD).setEnabled(Index.getStatus() != Index.STATUS_DESERIALIZING && Index.getStatus() != Index.STATUS_RELOADING);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case MENU_RESULT:
    		if (isPickMode)
    			showDialog(DIALOG_PICK);
    		else
    			showDialog(DIALOG_RESULT);
    		break;
    	case MENU_SORT:
    		showDialog(DIALOG_SORT);
    		break;
    	case MENU_RANGE:
    		showDialog(DIALOG_FILTER_RANGE);
    		break;
    	case MENU_LAYOUT:
    		showDialog(DIALOG_LAYOUT);
    		break;
    	case MENU_RELOAD:
    		reloadIndex();
    		break;
    	case MENU_PREFERENCES:
            startActivityForResult(new Intent().setClass(this, PrefsActivity.class), REQUEST_PREFERENCE);
    		break;
    	case MENU_HELP:
            startActivityForResult(new Intent().setClass(this, HelpActivity.class), REQUEST_HELP);
    		break;
    	}
    	return false;
    }

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_OPENAS:
			return new AlertDialog.Builder(this).setTitle(R.string.dialog_openas_title)
				.setIcon(R.drawable.menu_openas)
				.setItems(R.array.dialog_openas_entries, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent();
						switch (which) {
						case ITEM_OPEN_VIEW:
							intent.setAction(Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.fromFile(new File(mSelectedMatch.path(), mSelectedMatch.name())), FileInfo.mimeType(mSelectedMatch.name()));
							break;
						case ITEM_OPEN_LOCATE:
							intent.setAction(Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.fromFile(new File(mSelectedMatch.path())), "resource/folder");
							break;
						case ITEM_OPEN_EDIT:
							intent.setAction(Intent.ACTION_EDIT);
							intent.setDataAndType(Uri.fromFile(new File(mSelectedMatch.path(), mSelectedMatch.name())), FileInfo.mimeType(mSelectedMatch.name()));
							break;
						case ITEM_OPEN_SEND:
							intent.setAction(Intent.ACTION_SEND);
							intent.setType(FileInfo.mimeType(mSelectedMatch.name()));
							intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mSelectedMatch.path(), mSelectedMatch.name())));
							break;
						case ITEM_OPEN_CHOOSE:
							intent.setAction(Intent.ACTION_VIEW);
							intent.setType("*/*");
							intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mSelectedMatch.path(), mSelectedMatch.name())));
							intent = Intent.createChooser(intent, getString(R.string.dialog_openas_choose));
							break;
						case ITEM_OPEN_DELETE:
							if (isDeletePermitted)
								showDialog(DIALOG_OPENAS_DELETE);
							else
								showDialog(DIALOG_RESULT_DELETE_FORBIDDEN);
							return;
						}
						if (isIntentAvailable(intent)) {
							startActivity(intent);
						} else {
							Toast.makeText(SearchActivity.this, getResources().getString(R.string.dialog_openas_unavailable), Toast.LENGTH_SHORT).show();
						}
					}})
				.create();
		case DIALOG_OPENAS_DELETE:
			return new AlertDialog.Builder(this).setTitle(R.string.dialog_openas_delete_title)
				.setIcon(R.drawable.menu_warning)
				.setMessage(getString(R.string.dialog_openas_delete_format, mSelectedMatch.path() + "/" + mSelectedMatch.name()))
				.setPositiveButton(R.string.dialog_delete_button_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						boolean result = Index.delete(mSelectedMatch.index());
						if (result) {
							mSearchResult.remove(mSelectedMatch);
							mSelectedMatch = null;
							mListAdapter.notifyDataSetChanged();
							mPreferences.edit().putBoolean("index_is_obsolete", true).commit();
							Index.checkObsolete();
							Toast.makeText(SearchActivity.this, R.string.dialog_openas_delete_success, Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(SearchActivity.this, R.string.dialog_openas_delete_failed, Toast.LENGTH_SHORT).show();
						}
					}})
				.setNegativeButton(R.string.dialog_cancel, null)
				.create();
		case DIALOG_RESULT:
			return new FloatingDialog(this, R.style.Theme_FloatingDialog_List, R.layout.floating_dialog_list, R.layout.floating_dialog_list_item, R.string.dialog_result_title, R.array.dialog_result_entries, 
				ICON_RESULT, 0, 
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case ITEM_RESULT_STATICS:
						showDialog(DIALOG_RESULT_STATICS);
						break;
					case ITEM_RESULT_SORT:
						showDialog(DIALOG_SORT);
						break;
					case ITEM_RESULT_SHORTCUT:
						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.setClass(SearchActivity.this, ShortcutActivity.class);
						intent.setType("*.*");
						intent.putExtra(Intent.EXTRA_TITLE, mExpression.getName());
						intent.putExtra(Intent.EXTRA_STREAM, Uri.fromParts("collimator", "search", mExpression.toJSON()));
						startActivity(intent);
						break;
					case ITEM_RESULT_PLAYLIST:
						showDialog(DIALOG_RESULT_PLAYLIST_CREATE);
						break;
					case ITEM_RESULT_DELETE:
						if (isDeletePermitted)
							showDialog(DIALOG_RESULT_DELETE);
						else
							showDialog(DIALOG_RESULT_DELETE_FORBIDDEN);
						break;
					}
					dialog.dismiss();
				}});
		case DIALOG_RESULT_STATICS:
			return new AlertDialog.Builder(this).setTitle(R.string.dialog_result_title)
				.setIcon(R.drawable.menu_result)
				.setMessage(getStatistics())
				.create();
		case DIALOG_RESULT_PLAYLIST_CREATE:
    		final View edv = getLayoutInflater().inflate(R.layout.edittext_dialog, null);
    		((EditText)edv.findViewById(R.id.edtext)).setText(mExpression.getName());
			return new AlertDialog.Builder(this)
				.setView(edv)
				.setIcon(R.drawable.menu_playlist)
				.setTitle(R.string.dialog_playlist_title)
				.setMessage(R.string.dialog_playlist_message)
				.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						EditText edittext = (EditText)edv.findViewById(R.id.edtext);
						String name = edittext.getText().toString();
						if (null == name || name.length() == 0) {
							name = getString(R.string.dialog_playlist_default);
						}
			    		mExpression.setName(name);
						String[] list = new String[mSearchResult.size()];
						for (int i = 0; i < list.length; i++) {
							Match match = mSearchResult.get(i);
							list[i] = match.path() + "/" + match.name();
						}
						Playlist playlist = new Playlist(getContentResolver(), list);
						int inserted = playlist.createNew(name);
						Toast.makeText(SearchActivity.this, getString(R.string.dialog_playlist_inserted_format, inserted, name), Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(R.string.dialog_cancel, null)
				.create();
		case DIALOG_RESULT_DELETE:
			return new AlertDialog.Builder(this).setTitle(R.string.dialog_result_delete_title)
				.setIcon(R.drawable.menu_warning)
				.setMessage(getString(R.string.dialog_result_delete_format, mSearchResult.size()))
				.setPositiveButton(R.string.dialog_delete_button_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						int count = 0;
						boolean result;
						for (int i = mSearchResult.size() - 1; i >= 0; i--) {
							result = Index.delete(mSearchResult.get(i).index());
							if (result) {
								count++;
								mSearchResult.remove(i);
							}
						}
						if (count > 0) {
							mPreferences.edit().putBoolean("index_is_obsolete", true).commit();
							Index.checkObsolete();
							mListAdapter.notifyDataSetChanged();
						}
						Toast.makeText(SearchActivity.this, getString(R.string.dialog_result_delete_message_format, count), Toast.LENGTH_SHORT).show();
					}})
				.setNegativeButton(R.string.dialog_cancel, null)
				.create();
		case DIALOG_RESULT_DELETE_FORBIDDEN:
			return new AlertDialog.Builder(this).setTitle(R.string.dialog_openas_delete_title)
				.setIcon(R.drawable.menu_delete)
				.setMessage(R.string.dialog_result_delete_forbidden)
				.setPositiveButton(R.string.dialog_ok, null)
				.create();
		case DIALOG_SORT:
			return new AlertDialog.Builder(this).setTitle(R.string.dialog_sort_title)
				.setIcon(R.drawable.menu_sort)
				.setSingleChoiceItems(R.array.dialog_sort_entries, mExpression.getSortMode(), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mExpression.setSort(which);
					}})
				.setPositiveButton(R.string.dialog_sort_ascend, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mExpression.setSort(false);
						doSort();
						dialog.dismiss();
					}})
				.setNeutralButton(R.string.dialog_sort_descend, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mExpression.setSort(true);
						doSort();
						dialog.dismiss();
					}})
				.create();
		case DIALOG_FILTER_RANGE:
			return new FloatingDialog(this, R.style.Theme_FloatingDialog_Grid, R.layout.floating_dialog_grid, R.layout.floating_dialog_grid_item, R.string.dialog_filter_range_title, R.array.dialog_filter_range_entries, 
				ICON_FILTER_RANGE, mExpression.getRange(), 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mExpression.setRange(which);
						mButtonRange.setImageResource(ICON_BUTTON_RANGE[which]);
						doSearch();
						dialog.dismiss();
					}});
		case DIALOG_PICK:
			return new FloatingDialog(this, R.style.Theme_FloatingDialog_List, R.layout.floating_dialog_list, R.layout.floating_dialog_list_item, R.string.dialog_pick_title, R.array.dialog_pick_entries, 
				ICON_PICK, 0, 
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case ITEM_RESULT_STATICS:
						showDialog(DIALOG_RESULT_STATICS);
						break;
					case ITEM_RESULT_SORT:
						showDialog(DIALOG_SORT);
						break;
					}
					dialog.dismiss();
				}});
		case DIALOG_LAYOUT:
			return new AlertDialog.Builder(this).setTitle(R.string.dialog_layout_title)
			.setIcon(R.drawable.menu_layout)
			.setSingleChoiceItems(R.array.dialog_layout_entries, displayLayout, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					setLayout(which);
					mPreferences.edit().putString("display_layout", String.valueOf(which)).commit();
					dialog.dismiss();
				}})
			.create();
		case DIALOG_INDEX_OBSOLETE:
			return new AlertDialog.Builder(this).setTitle(R.string.dialog_index_obsolete_title)
				.setIcon(R.drawable.menu_obsolete)
				.setMessage(R.string.dialog_index_obsolete_message)
				.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						reloadIndex();
					}})
				.setNeutralButton(R.string.dialog_skip, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mPreferences.edit().putBoolean("index_without_prompt", true).commit();
						reloadIndex();
					}})
				.setNegativeButton(R.string.dialog_cancel, null)
				.create();
		case DIALOG_FIRST_LAUNCH:
    		final View flv = getLayoutInflater().inflate(R.layout.first_launch_dialog, null);
			return new AlertDialog.Builder(this).setTitle(R.string.dialog_first_launch_title)
				.setView(flv)
				.setIcon(R.drawable.menu_firstlaunch)
				.setNeutralButton(R.string.dialog_skip, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mPreferences.edit().putBoolean("display_firstlaunch", false).commit();
					}})
				.setNegativeButton(R.string.dialog_cancel, null)
				.create();
	}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_OPENAS_DELETE:
			((AlertDialog)dialog).setMessage(getString(R.string.dialog_openas_delete_format, mSelectedMatch.path() + "/" + mSelectedMatch.name()));
			break;
		case DIALOG_RESULT_STATICS:
			((AlertDialog)dialog).setMessage(getStatistics());
			break;
		case DIALOG_RESULT_PLAYLIST_CREATE:
    		final View edv = getLayoutInflater().inflate(R.layout.edittext_dialog, null);
    		((EditText)edv.findViewById(R.id.edtext)).setText(mExpression.getName());
    		((AlertDialog)dialog).setView(edv);
    		break;
		case DIALOG_RESULT_DELETE:
			((AlertDialog)dialog).setMessage(getString(R.string.dialog_result_delete_format, mSearchResult.size()));
			break;
		}
		super.onPrepareDialog(id, dialog);
	}
	
	@Override
	public boolean onSearchRequested() {
		mExpression = new Expression(getApplicationContext());
		updateUI();
		mEditSearch.requestFocus();
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_PREFERENCE) {
			updatePreferences();
		} else if (requestCode == REQUEST_HELP) {
			if (resultCode == RESULT_OK) {
				mExpression.setRange(0);
				mExpression.setSort(0);
				mExpression.setKey(data.getStringExtra(Intent.EXTRA_TEXT));
				mButtonRange.setImageResource(ICON_BUTTON_RANGE[0]);
				mEditSearch.setText(mExpression.getKey());
				doSearch();
			}
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration config) {
	    super.onConfigurationChanged(config);
	}
	
	public void onClick(View v) {
		if (mButtonStar == v) {
			if (isSearching) return;
			if (isPickMode) 
				showDialog(DIALOG_PICK);
			else
				showDialog(DIALOG_RESULT);
		} else if (mButtonRange == v) {
			if (isRangeLocked) {
				Toast.makeText(this, R.string.status_range_locked, Toast.LENGTH_SHORT).show();
				return;
			}
			showDialog(DIALOG_FILTER_RANGE);
		} else if (mTextStatus == v) {
			this.openOptionsMenu();
		}
	}

	public void onItemSelected(AdapterView<?> l, View v, int position, long id) {
		mListAdapter.setSelected(position);
		mListAdapter.notifyDataSetChanged();
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		mListAdapter.setSelected(-1);
		mListAdapter.notifyDataSetChanged();
	}

	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		if (mSelectedMatch == mSearchResult.get((int)id)) {
			if (isPickMode) {
				Intent intent = new Intent();
				intent.setData(Uri.fromFile(new File(mSelectedMatch.path(), mSelectedMatch.name())));
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.fromFile(new File(mSelectedMatch.path(), mSelectedMatch.name())));
				this.setResult(RESULT_OK, intent);
				finish();
			} else {
				if (isTapView) {
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(new File(mSelectedMatch.path(), mSelectedMatch.name())), FileInfo.mimeType(mSelectedMatch.name()));
					if (isIntentAvailable(intent)) {
						startActivity(intent);
					} else {
						Toast.makeText(SearchActivity.this, getResources().getString(R.string.dialog_openas_unavailable), Toast.LENGTH_SHORT).show();
					}
				} else {
					showDialog(DIALOG_OPENAS);
				}
			}
		} else {
			mListAdapter.setSelected(position);
			mListAdapter.notifyDataSetChanged();
			mSelectedMatch = mSearchResult.get((int)id);
		}
	}

	public boolean onItemLongClick(AdapterView<?> l, View v, int position, long id) {
		mListAdapter.setSelected(position);
		mListAdapter.notifyDataSetChanged();
		mSelectedMatch = mSearchResult.get((int)id);
    	showDialog(DIALOG_OPENAS);
		return false;
	}
	
	private void doSearch() {
		if (Index.getStatus() == Index.STATUS_READY || Index.getStatus() == Index.STATUS_OBSOLETE) {
			isSearching = true;
			Index.interrupt();
			mSearchResult.clear();
			mExpression.matchAsync();
		}
	}
	
	private void doSort() {
		Comparator<Match> comparator = mExpression.getSorter();
		if (comparator != null && mSearchResult != null && mSearchResult.size() > 0) {
			Collections.sort(mSearchResult, comparator);
			mListAdapter.notifyDataSetChanged();
		}
	}
	
	private void tryReloadIndex() {
		if (!isSearching && (Index.getStatus() == Index.STATUS_FAILED || Index.getStatus() == Index.STATUS_OBSOLETE)) {
			if (isReloadWithoutPrompt) {
				reloadIndex();
			} else {
				mButtonStar.setImageResource(R.drawable.button_warning);
				showDialog(DIALOG_INDEX_OBSOLETE);
			}
		}
	}
	
	private void reloadIndex() {
		mSearchResult.clear();
		mTextStatus.setText(SearchActivity.this.getString(R.string.status_reload_start));
		mButtonStar.setImageResource(R.drawable.button_reloading);
		mReloadNotification.setLatestEventInfo(SearchActivity.this, getResources().getString(R.string.app_name), getResources().getString(R.string.status_reload_message), mReloadNotification.contentIntent);
		mNotificationManager.notify(0, mReloadNotification);
		Matcher.stopAsyncMatch();
        Index.reloadEntriesAsync();
	}
	
	private void enterPickMode(int type, boolean lock) {
		isPickMode = true;
   		mExpression = new Expression(getApplicationContext());
		mExpression.setRange(type);
		isRangeLocked = lock;
		Toast.makeText(this, STRING_PICK_TOAST[type], Toast.LENGTH_LONG).show();
	}
	
	private void setLayout(int layout) {
		switch (layout) {
		case ITEM_LAYOUT_TILE:
			mListAdapter.setLayout(MatchAdapter.LAYOUT_TILE);
			mListEntries.setNumColumns(1);
			mListAdapter.notifyDataSetChanged();
			break;
		case ITEM_LAYOUT_ICON:
			mListAdapter.setLayout(MatchAdapter.LAYOUT_ICON);
			mListEntries.setNumColumns(3);
			mListAdapter.notifyDataSetChanged();
			break;
		case ITEM_LAYOUT_DIGEST:
			mListAdapter.setLayout(MatchAdapter.LAYOUT_DIGEST);
			mListEntries.setNumColumns(1);
			mListAdapter.notifyDataSetChanged();
			break;
		}
	}
	
	private void updateUI() {
		mButtonRange.setImageResource(ICON_BUTTON_RANGE[mExpression.getRange()]);
		mEditSearch.setText(mExpression.getKey());
	}
	
	private String getStatistics() {
		long size = 0;
		HashMap<String, Integer> where = new HashMap<String, Integer>();
		for (Match m : mSearchResult) {
			size += m.size();
			if (where.containsKey(m.path())) {
				Integer count = where.get(m.path());
				count = count.intValue() + 1;
			} else {
				where.put(m.path(), 1);
			}
		}
		return getString(R.string.dialog_statistics_message_format, 
			mSearchResult.size(), 
			FileInfo.sizeString(size), 
			where.size(),
			(Index.getStatus() == Index.STATUS_READY || Index.getStatus() == Index.STATUS_OBSOLETE ? 
				getString(R.string.dialog_statistics_index_details, Index.length(), new Date(Index.reloadTime()).toLocaleString()) : 
				getString(R.string.dialog_statistics_index_none)));
	}
    
	/**
	 * 判断一个 Intent 在系统中是否有对应的活动可以处理。
	 * @param intent	要进行判断的 Intent。
	 * @return	该 Intent 是否可以被处理
	 */
	private boolean isIntentAvailable(Intent intent) { 
	    final PackageManager packageManager = getPackageManager(); 
	    List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY); 
	    return list.size() > 0; 
	}
	
	public class EventHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Matcher.MATCHER_START:
				startTick = System.currentTimeMillis();
				mSearchResult.clear();
				mTextStatus.setText(SearchActivity.this.getString(R.string.status_match_start));
				mButtonStar.setImageResource(R.drawable.button_search_progressing);
				break;
			case Matcher.MATCHER_ENTRY:
				Match matchEntry = (Match)msg.obj;
				mSearchResult.add(matchEntry);
				mListAdapter.notifyDataSetChanged();
				break;
			case Matcher.MATCHER_FINISHED:
				isSearching = false;
				mListAdapter.notifyDataSetChanged();
				mTextStatus.setText(getString(R.string.status_result_format, mSearchResult.size(), FileInfo.timeString(System.currentTimeMillis() - startTick)));
				if (isPickMode)
					mButtonStar.setImageResource(R.drawable.button_pick);
				else
					mButtonStar.setImageResource(ICON_RESULT_AMOUNT[mSearchResult.size() == 0 ? 0 : (mSearchResult.size() < 20 ? 1 : 2)]);
				doSort();
				break;
			case Matcher.MATCHER_NODATA:
				mTextStatus.setText(SearchActivity.this.getString(R.string.status_match_nodata));
				mButtonStar.setImageResource(R.drawable.button_warning);
				break;
			case SoftCache.MESSAGE_CACHE_GOT:
				if (isRefreshingInstant) mListAdapter.notifyDataSetChanged();
				break;
			case SoftCache.MESSAGE_QUEUE_FINISHED:
				mListAdapter.notifyDataSetChanged();
				break;
			case Index.MESSAGE_NOSDCARD:
	            mNotificationManager.cancel(0);
				mTextStatus.setText(SearchActivity.this.getString(R.string.status_nosdcard));
				mButtonStar.setImageResource(R.drawable.button_warning);
				break;
			case Index.MESSAGE_RELOAD_SUCCESS:
	            mNotificationManager.cancel(0);
				mTextStatus.setText(getString(R.string.status_reload_result_format, FileInfo.timeSpanString(System.currentTimeMillis() - Index.reloadTime()), Index.length()));
				doSearch();
				break;
			case Index.MESSAGE_RELOAD_FAILED:
	            mNotificationManager.cancel(0);
				mTextStatus.setText(SearchActivity.this.getString(R.string.status_reload_failed));
				mButtonStar.setImageResource(R.drawable.button_warning);
				break;
			case Index.MESSAGE_SERIALIZING_FAILED:
	            mNotificationManager.cancel(0);
				mTextStatus.setText(SearchActivity.this.getString(R.string.status_serializing_failed));
				mButtonStar.setImageResource(R.drawable.button_warning);
				break;
			case Index.MESSAGE_DESERIALIZING_SUCCESS:
				mTextStatus.setText(getString(R.string.status_reload_result_format, FileInfo.timeSpanString(System.currentTimeMillis() - Index.reloadTime()), Index.length()));
	            tryReloadIndex();
				doSearch();
				break;
			case Index.MESSAGE_DESERIALIZING_FAILED:
				mTextStatus.setText(SearchActivity.this.getString(R.string.status_deserializing_failed));
				mButtonStar.setImageResource(R.drawable.button_warning);
	            if (isFirstLaunch) reloadIndex();
	            else tryReloadIndex();
				break;
			case Index.MESSAGE_DESERIALIZING_DIFFERENT_VERSION:
				mTextStatus.setText(SearchActivity.this.getString(R.string.status_deserializing_different_version));
				mButtonStar.setImageResource(R.drawable.button_warning);
	            tryReloadIndex();
				break;
			}
		}
	}
}