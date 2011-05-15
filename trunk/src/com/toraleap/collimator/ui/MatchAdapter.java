package com.toraleap.collimator.ui;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.toraleap.collimator.R;
import com.toraleap.collimator.data.Match;
import com.toraleap.collimator.util.FileInfo;

/**
 * 连接 ListView 及其匹配结果集的适配器。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1104
 */
public final class MatchAdapter extends BaseAdapter {//implements SectionIndexer {
	
	private static final int[] LAYOUT_DEFAULT = { R.layout.listitem_tile_default, R.layout.listitem_icon_default, R.layout.listitem_digest_default };
	private static final int[] LAYOUT_SELECTED = { R.layout.listitem_tile_selected, R.layout.listitem_icon_selected, R.layout.listitem_digest_selected };
	private static final int[] IMAGE_FRAME_THUMBNAIL = { R.drawable.frame_undefined, R.drawable.frame_audio, R.drawable.frame_image, R.drawable.frame_video, R.drawable.frame_text };
	private static final int IMAGE_FRAME_NONE = 0;
	private static final int IMAGE_FRAME_AUDIO = 1;
	private static final int IMAGE_FRAME_IMAGE = 2;
	private static final int IMAGE_FRAME_VIDEO = 3;
	private static final int IMAGE_FRAME_TEXT = 4;
	public static final int LAYOUT_TILE = 0;
	public static final int LAYOUT_ICON = 1;
	public static final int LAYOUT_DIGEST = 2;
	
    private final LayoutInflater mInflater;  
    private final List<Match> mItems;
    private int mSelected = -1;
    private int mLayout = 0;
    
    public MatchAdapter(LayoutInflater inflater, List<Match> items) {
        mInflater = inflater;
        mItems = items;
    }
    
    /**
     * 设置显示条目所用的布局。
     * @param layout	布局类型(LAYOUT打头的常数)
     */
    public void setLayout(int layout) {
    	mLayout = layout;
    }
    
    /**
     * 设置显示为选中样式的条目的编号。
     * @param position	条目编号
     */
    public void setSelected(int position) {
    	mSelected = position;
    }
    
    public boolean areAllItemsEnabled()
    {
        return true;
    }

    public boolean isEnabled(int position)
    {
        return true;
    }
    
	public int getCount() {
		return mItems.size();
	}

	public Object getItem(int position) {
		if (position < mItems.size()) {
			return mItems.get(position);
		} else {
			return null;
		}
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		final Match matchEntry = mItems.get(position);
		View view;
		ViewHolder holder;
		int viewid = position == mSelected ? LAYOUT_SELECTED[mLayout] : LAYOUT_DEFAULT[mLayout]; 
		if (convertView != null && ((ViewHolder)convertView.getTag()).viewId == viewid) {
			view = convertView;
			holder = (ViewHolder)view.getTag();
		} else {
			view = mInflater.inflate(viewid, parent, false);
			holder = new ViewHolder(view, viewid);
			view.setTag(holder);
		}
		holder.updateView(matchEntry);
		return view;
	}
	
//	@Override
//	public int getItemViewType(int position) {
//		return position == mSelected ? 0 : 1; 
//	}
//
//	@Override
//	public int getViewTypeCount() {
//		return 2;
//	}

	private int getFrameType(Match match) {
		String mime = FileInfo.mimeType(match.name());
		if (mime.startsWith("audio/")) return IMAGE_FRAME_AUDIO;
		else if (mime.startsWith("image/")) return IMAGE_FRAME_IMAGE;
		else if (mime.startsWith("video/")) return IMAGE_FRAME_VIDEO;
		else if (mime.startsWith("text/plain")) return IMAGE_FRAME_TEXT;
		else return IMAGE_FRAME_NONE;
	}

//	public int getPositionForSection(int section) {
//		// TODO Auto-generated method stub
//		Log.e("section", String.valueOf(section));
//		return section;
//	}
//
//	public int getSectionForPosition(int position) {
//		Log.e("position", String.valueOf(position));
//		return position;
//	}
//
//	public Object[] getSections() {
//		Log.e("getsections", "getsections");
//		return new Object[]{ "A", "B", "C", "D", "E", "F", "G" };
//	}
//	
	public final class ViewHolder
    {
		public final int viewId;
	    public final ImageView thumbnail;
	    public final TextView filename;
	    public final TextView filepath;
	    public final TextView filesize;
	    public final TextView filetime;
	    public final TextView digest;
	    
	    public ViewHolder(View v, int viewid) {
			viewId = viewid;
			thumbnail = (ImageView)v.findViewById(R.id.thumbnail);
			filename = (TextView)v.findViewById(R.id.filename);
			filepath = (TextView)v.findViewById(R.id.filepath);
			filesize = (TextView)v.findViewById(R.id.filesize);
			filetime = (TextView)v.findViewById(R.id.filetime);
			digest = (TextView)v.findViewById(R.id.digest);
	    }
	    
	    public void updateView(Match matchEntry) {
			if (null != thumbnail) {
				thumbnail.setImageBitmap(matchEntry.thumbnail());
				thumbnail.setBackgroundResource(IMAGE_FRAME_THUMBNAIL[getFrameType(matchEntry)]);
			}
			if (null != filename) filename.setText(matchEntry.highlightedName());
			if (null != filepath) filepath.setText(matchEntry.path());
			if (null != filesize) filesize.setText(matchEntry.sizeString());
			if (null != filetime) filetime.setText(matchEntry.timeString());
			if (null != digest) digest.setText(matchEntry.digest());	    	
	    }
    }
}
	
