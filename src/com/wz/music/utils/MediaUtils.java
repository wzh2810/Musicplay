package com.wz.music.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.wz.music.bean.Music;
import com.wz.music.conf.Constants;

public class MediaUtils {
	public static List<Music> songList = new ArrayList<Music>();
	public static int CURSTATE = Constants.STATE_STOP;
	public static int CURPOSITION = 0;
	public static int CURMODEL = Constants.MODEL_NORMAL;

	//�����ֻ�����ı�������-->sqlite-->contentProvider
	/**
	 * ���ر��ص�����
	 * @param context
	 */
	public static void initSongList(Context context) {
		songList.clear();
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String[] projection = {MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.DATA};
		Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
		while(cursor.moveToNext()) {
			String title = cursor.getString(0);
			String artist = cursor.getString(1);
			String path = cursor.getString(2);
			Music music = new Music(title, artist, path);
			songList.add(music);
		}
		cursor.close();
	}
	
	
	public static String duration2Str(int duration) {
		//		"00:11" "11:11"
		String result = "";
		int i = duration / 1000;
		int min = i / 60;//1 2  3 
		int sec = i % 60;// 0-59  
		if (min > 9) {
			if (sec > 9) {
				result = min + ":" + sec;
			} else {
				result = min + ":0" + sec;
			}
		} else {
			if (sec > 9) {
				result = "0" + min + ":" + sec;
			} else {
				result = "0" + min + ":0" + sec;
			}
		}
		return result;
	}
	
	/**
	 * ���ݸ�����·��,�õ���Ӧ��lrc
	 * @param path
	 * @return
	 */
	public static File getLrcFile(String path) {
		File file;
		String lrcName = path.replace(".mp3", ".lrc");//�Ҹ���������ͬ��.lrc�ļ�
		file = new File(lrcName);
		if (!file.exists()) {
			lrcName = path.replace(".mp3", ".krc");//��ʿ�����.txt��β
			file = new File(lrcName);
			if (!file.exists()) {
				lrcName = path.replace(".mp3", ".qrc");//��ʿ�����.txt��β
				file = new File(lrcName);
				if(!file.exists()) {
					return null;
				}
			}
		}
		return file;

	}
	
}
