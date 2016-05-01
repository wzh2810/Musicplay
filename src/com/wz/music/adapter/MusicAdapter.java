package com.wz.music.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.wz.music.R;
import com.wz.music.R.id;
import com.wz.music.R.layout;
import com.wz.music.bean.Music;
import com.wz.music.utils.MediaUtils;

public class MusicAdapter extends BaseAdapter{
	//1.����Դ��ʲô�ط�
	private Context context;
	

	public MusicAdapter(Context context) {
		super();
		this.context = context;
	}

	@Override
	public int getCount() {
		if(MediaUtils.songList != null) {
			return MediaUtils.songList.size();
		}
		return 0;
	}

	@Override
	public Object getItem(int position) {
		if(MediaUtils.songList != null) {
			return MediaUtils.songList.get(position);
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		/**---------------��ͼ�ĳ�ʼ��---------------**/
		ViewHolder holder = null;
		if(convertView == null) {
			convertView = View.inflate(context, R.layout.item_music, null);
			holder = new ViewHolder();
			holder.tv_title = (TextView) convertView.findViewById(R.id.tv_title);
			holder.tv_artist = (TextView) convertView.findViewById(R.id.tv_artist);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		/**---------------�õ�����---------------**/
		Music music = MediaUtils.songList.get(position);
		/**---------------��������---------------**/
		holder.tv_artist.setText(music.artist);
		holder.tv_title.setText(music.title);
		
		if(MediaUtils.CURPOSITION == position) {
			holder.tv_title.setTextColor(Color.GREEN);
		} else {
			holder.tv_title.setTextColor(Color.WHITE);
		}
		holder.tv_title.setTag(MediaUtils.CURPOSITION);
		holder.tv_title.setTag(position);//tag�����þ���Ϊ�˷���
		return convertView;
	}

	class ViewHolder {
		TextView tv_title;
		TextView tv_artist;
	}
}
