package com.toraleap.collimator.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.toraleap.collimator.R;

public final class FloatingDialog extends Dialog {

	private AbsListView mView;
	private TextView mTextTitle;
	private FloatingDialogAdapter mAdapter;
	private DialogInterface.OnClickListener mListener;
	private int mLayout;
	private int mSubLayout;
	private int mSelected;
	private String mTitle;

	public FloatingDialog(Context context, int theme, int layout, int sublayout, int title, int itemsResource, int[] icons, int selected, DialogInterface.OnClickListener listener) {
		super(context, theme);
		mLayout = layout;
		mSubLayout = sublayout;
		mTitle = context.getResources().getString(title);
		mAdapter = new FloatingDialogAdapter(getLayoutInflater(), context.getResources().getStringArray(itemsResource), icons);
		mListener = listener;
	}
	
	public FloatingDialog(Context context, int theme, int layout, int sublayout, String title, String[] items, int[] icons, int selected, DialogInterface.OnClickListener listener) {
		super(context, theme);
		mLayout = layout;
		mSubLayout = sublayout;
		mTitle = title;
		mAdapter = new FloatingDialogAdapter(getLayoutInflater(), items, icons);
		mListener = listener;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		setContentView(mLayout);
		mTextTitle = (TextView) findViewById(R.id.dialog_title);
		mTextTitle.setText(mTitle);
		mView = (AbsListView) findViewById(R.id.floating_dialog_view);
		mView.setAdapter(mAdapter);
		mView.setSelection(mSelected);
		mView.setOnItemClickListener(new OnItemClickListener());
		mView.setFocusable(true);
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			cancel();
			return true;
		}
		return false;
	}
	
	public void show() {
		super.show();
	}
	
	private class OnItemClickListener implements	AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mListener.onClick(FloatingDialog.this, position);
		}
	}
	
	public class FloatingDialogAdapter extends BaseAdapter {
		
		LayoutInflater mInflater;
		String[] mItems;
		int[] mIcons;
		
		public FloatingDialogAdapter(LayoutInflater inflater, String[] items, int[] icons) {
			mInflater = inflater;
			mItems = items;
			mIcons = icons;
		}

		public int getCount() {
			return mItems.length;
		}

		public Object getItem(int position) {
			return mItems[position];
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			ViewHolder holder;
			if (convertView != null && convertView.getId() == mSubLayout) {
				view = convertView;
				holder = (ViewHolder)view.getTag();
			} else {
				view = mInflater.inflate(mSubLayout, parent, false);
				holder = new ViewHolder();
				holder.icon = (ImageView)view.findViewById(R.id.source_icon);
				holder.label = (TextView)view.findViewById(R.id.source_label);
				view.setTag(holder);
			}
			if (mIcons != null) {
				holder.icon.setImageResource(mIcons[position]);
			} else {
				holder.icon.setImageResource(android.R.drawable.ic_menu_slideshow);
			}
			holder.label.setText(mItems[position]);
			return view;
		}
		
	}
	
	public class ViewHolder
    {
           public ImageView icon;
           public TextView label;
    }

}
