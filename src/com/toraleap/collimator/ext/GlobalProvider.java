package com.toraleap.collimator.ext;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.toraleap.collimator.R;

public final class GlobalProvider extends ContentProvider {
    public final static String AUTHORITY = "collimator";
    private static final int SEARCH_SUGGEST = 0;
    private static final int SHORTCUT_REFRESH = 1;
    private static final UriMatcher sURIMatcher = buildUriMatcher();
 
    private static final String[] COLUMNS = {
    	BaseColumns._ID,
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA,
        SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
    };
    
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher =  new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, SHORTCUT_REFRESH);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SHORTCUT_REFRESH);
        return matcher;
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (sURIMatcher.match(uri)) {
        case SEARCH_SUGGEST:
            String query = null;
            if (uri.getPathSegments().size() > 1) {
                query = uri.getLastPathSegment();
            }
            return getSuggestions(query);
        case SHORTCUT_REFRESH:
            String shortcutId = null;
            if (uri.getPathSegments().size() > 1) {
                shortcutId = uri.getLastPathSegment();
            }
            return refreshShortcut(shortcutId);
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }
    
    private Cursor getSuggestions(String query) {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        boolean isMakeShortcut = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("global_makeshortcut", true);
        cursor.addRow(new Object[] { 0, getContext().getString(R.string.search_title), getContext().getString(R.string.search_hint_format, query), query, isMakeShortcut ? null : SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT });
        return cursor;
    }
    
    private Cursor refreshShortcut(String shortcutId) {
    	Log.e("shortcutid", shortcutId);
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        cursor.addRow(new Object[] { 0, getContext().getString(R.string.search_title), getContext().getString(R.string.search_hint_format, shortcutId), shortcutId, null });
        return cursor;
    }

    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

	@Override
	public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
        case SEARCH_SUGGEST:
            return SearchManager.SUGGEST_MIME_TYPE;
        case SHORTCUT_REFRESH:
            return SearchManager.SHORTCUT_MIME_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
	}

	@Override
	public boolean onCreate() {
        return true;
	}

}
