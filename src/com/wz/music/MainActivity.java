package com.wz.music;

import java.io.File;
import java.util.Random;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Messenger;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.wz.music.adapter.MusicAdapter;
import com.wz.music.conf.Constants;
import com.wz.music.service.MusicService;
import com.wz.music.utils.LrcUtil;
import com.wz.music.utils.MediaUtils;
import com.wz.music.views.LyricShow;
import com.wz.music.views.ScrollableViewGroup;
import com.wz.music.views.ScrollableViewGroup.OnCurrentViewChangedListener;



public class MainActivity extends Activity implements OnClickListener {

	private TextView mTv_curduration;
	private TextView mTv_minilrc;
	private TextView mTv_totalduration;
	private SeekBar mSk_duration;
	private ImageView mIv_bottom_model;
	private ImageView mIv_bottom_play;
	private ListView mLv_list;
	private ScrollableViewGroup mSvg_main;
	private MainActivity mInstance;
	private LrcUtil mLrcUtil;
	private Handler handler = new Handler() {//���ս��,ˢ��ui

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case Constants.MSG_ONPREPARED:
				int currentPosition = msg.arg1;
				int totalDuration = msg.arg2;
				mTv_curduration.setText(MediaUtils.duration2Str(currentPosition));
				mTv_totalduration.setText(MediaUtils.duration2Str(totalDuration));
				mSk_duration.setMax(totalDuration);
				mSk_duration.setProgress(currentPosition);
				if (mLrcUtil == null) {
					mLrcUtil = new LrcUtil(mInstance);
				}
				//���л����
				File f = MediaUtils.getLrcFile(MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
				mLrcUtil.ReadLRC(f);
				//ʹ�ù���
				mLrcUtil.RefreshLRC(currentPosition);
				//1. ���ü���
				mTv_lyricShow.SetTimeLrc(LrcUtil.lrclist);
				//2. ���¹������
				mTv_lyricShow.SetNowPlayIndex(currentPosition);
				break;
			case Constants.MSG_ONCOMPLETION:
				//���ӵ�ǰ�Ĳ���model����Ӧ�Ĵ���
				if (MediaUtils.CURMODEL == Constants.MODEL_NORMAL) {//��ǰ��˳�򲥷�
					if (MediaUtils.CURPOSITION < MediaUtils.songList.size() - 1) {
						changeColorWhite();
						MediaUtils.CURPOSITION++;//MediaUtils.CURPOSITION���ֵ����MediaUtils.songList.size() - 1
						changeColorGreen();
						startMediaService("����", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
					} else {
						startMediaService("ֹͣ");
					}
				} else if (MediaUtils.CURMODEL == Constants.MODEL_RANDOM) {//��ǰ���������
					Random random = new Random();
					int position = random.nextInt(MediaUtils.songList.size());
					changeColorWhite();
					MediaUtils.CURPOSITION = position;
					changeColorGreen();
					startMediaService("����", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
				} else if (MediaUtils.CURMODEL == Constants.MODEL_REPEAT) {//��ǰ���ظ�����
					if (MediaUtils.CURPOSITION < MediaUtils.songList.size() - 1) {
						changeColorWhite();
						MediaUtils.CURPOSITION++;//MediaUtils.CURPOSITION���ֵ����MediaUtils.songList.size() - 1
						changeColorGreen();
					} else {
						changeColorWhite();
						MediaUtils.CURPOSITION = 0;
						changeColorGreen();
						startMediaService("����", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
					}
				} else if (MediaUtils.CURMODEL == Constants.MODEL_SINGLE) {//��ǰ�ǵ���ѭ��
					startMediaService("����", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
				}
				break;
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		mInstance = this;//��ǰactivity������
		initView();
		initData();
		initListener();
	}

	/**
	 * ��ʼ���ؼ�
	 */
	private void initView() {
		mTv_curduration = (TextView) findViewById(R.id.tv_curduration);
		mTv_minilrc = (TextView) findViewById(R.id.tv_minilrc);
		mTv_totalduration = (TextView) findViewById(R.id.tv_totalduration);
		mSk_duration = (SeekBar) findViewById(R.id.sk_duration);
		mIv_bottom_model = (ImageView) findViewById(R.id.iv_bottom_model);
		mIv_bottom_play = (ImageView) findViewById(R.id.iv_bottom_play);
		mLv_list = (ListView) findViewById(R.id.lv_list);
		mSvg_main = (ScrollableViewGroup) findViewById(R.id.svg_main);
		mTv_lyricShow = (LyricShow) findViewById(R.id.tv_lrc);
		//Ĭ��ѡ�е�һ��
		findViewById(R.id.ib_top_play).setSelected(true);
	}

	/**
	 * ���ݵļ���
	 */
	private void initData() {
		MediaUtils.initSongList(this);
		mAdapter = new MusicAdapter(this);
		mLv_list.setAdapter(mAdapter);
	}

	/**
	 * ��ʼ������
	 */
	private void initListener() {
		findViewById(R.id.ib_top_play).setOnClickListener(this);
		findViewById(R.id.ib_top_list).setOnClickListener(this);
		findViewById(R.id.ib_top_lrc).setOnClickListener(this);
		findViewById(R.id.ib_top_volumn).setOnClickListener(this);
		findViewById(R.id.ib_bottom_model).setOnClickListener(this);
		findViewById(R.id.ib_bottom_last).setOnClickListener(this);
		findViewById(R.id.ib_bottom_play).setOnClickListener(this);
		findViewById(R.id.ib_bottom_next).setOnClickListener(this);
		findViewById(R.id.ib_bottom_update).setOnClickListener(this);

		mSvg_main.setOnCurrentViewChangedListener(new OnCurrentViewChangedListener() {

			@Override
			public void onCurrentViewChanged(View view, int currentview) {
				
				setTopSelected(topArr[currentview]);

			}

		});

		mLv_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// 1.�޸�curposition
				changeColorWhite();
				MediaUtils.CURPOSITION = position;
				changeColorGreen();
				// 2.����
				startMediaService("play", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
				// 3.�޸�ͼ��
				mIv_bottom_play.setImageResource(R.drawable.appwidget_pause);

			}
		});

		mSk_duration.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { // ֹͣ��ק
				mSk_duration.setProgress(seekBar.getProgress());
				startMediaService("progress", seekBar.getProgress());
				// ���ֲ�����,��ת��ָ����λ�ò���
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {// ��������ק��ť

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {// ���ȸı�

			}
		});

	}

	private int[] topArr = { R.id.ib_top_play, R.id.ib_top_list, R.id.ib_top_lrc, R.id.ib_top_volumn };
	private LyricShow mTv_lyricShow;

	/**
	 * ������ť��ѡ��Ч��
	 * @param selectedId
	 */
	private void setTopSelected(int selectedId) {
		//1.��ԭ���пؼ���Ч��,��top�����4����ť��ʾЧ������δѡ��
		findViewById(R.id.ib_top_play).setSelected(false);
		findViewById(R.id.ib_top_list).setSelected(false);
		findViewById(R.id.ib_top_lrc).setSelected(false);
		findViewById(R.id.ib_top_volumn).setSelected(false);

		//2.�ô��ݽ����Ŀؼ���ѡ��Ч��
		findViewById(selectedId).setSelected(true);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ib_top_play:
			mSvg_main.setCurrentView(0);
			setTopSelected(R.id.ib_top_play);
			break;
		case R.id.ib_top_list:
			mSvg_main.setCurrentView(1);
			setTopSelected(R.id.ib_top_list);
			break;
		case R.id.ib_top_lrc:
			mSvg_main.setCurrentView(2);
			setTopSelected(R.id.ib_top_lrc);
			break;
		case R.id.ib_bottom_play:// ���Ű�ť,���ͬһ����ť.����������.��Ҫ����һ���������п���
			// �������񣬶����÷��񲥷�����
			if (MediaUtils.CURPOSITION == Constants.STATE_STOP) {// Ĭ����ֹͣ,����ͱ䲥��
				startMediaService("play", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
				// �޸�ͼ��
				mIv_bottom_play.setImageResource(R.drawable.appwidget_pause);
			} else if (MediaUtils.CURSTATE == Constants.STATE_PLAY) { // �ڶ��ε����ʱ�򣬵�ǰ��״̬�ǲ���
				startMediaService("pause");
				mIv_bottom_play.setImageResource(R.drawable.img_playback_bt_play);
			} else if (MediaUtils.CURSTATE == Constants.STATE_PAUSE) { // �����ε����ʱ�򣬵�ǰ��״̬����ͣ
				startMediaService("continuePlay");
				mIv_bottom_play.setImageResource(R.drawable.appwidget_pause);
			}

			break;
		case R.id.ib_bottom_last:
			if (MediaUtils.CURPOSITION > 0) {
				changeColorWhite();
				MediaUtils.CURPOSITION--;
				changeColorGreen();
				// ����
				startMediaService("play", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);

				mIv_bottom_play.setImageResource(R.drawable.appwidget_pause);

			}

			break;

		case R.id.ib_bottom_next:
			if (MediaUtils.CURPOSITION < MediaUtils.songList.size() - 1) {
				changeColorWhite();
				MediaUtils.CURPOSITION++;
				changeColorGreen();
				// ����
				startMediaService("play", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);

				mIv_bottom_play.setImageResource(R.drawable.appwidget_pause);

			}

		case R.id.ib_bottom_model:
			if (MediaUtils.CURMODEL == Constants.MODEL_NORMAL) {// ��ǰ��˳�򲥷�
				MediaUtils.CURMODEL = Constants.MODEL_RANDOM;// �л����������
				// ����ui
				mIv_bottom_model.setImageResource(R.drawable.icon_playmode_shuffle);
				// ��ʾ
				Toast.makeText(getApplicationContext(), "�������", 0).show();
			} else if (MediaUtils.CURMODEL == Constants.MODEL_RANDOM) {// ��ǰ���������
				MediaUtils.CURMODEL = Constants.MODEL_REPEAT;// �л����ظ�����
				// ����ui
				mIv_bottom_model.setImageResource(R.drawable.icon_playmode_repeat);
				// ��ʾ
				Toast.makeText(getApplicationContext(), "�ظ�����", 0).show();
			} else if (MediaUtils.CURMODEL == Constants.MODEL_REPEAT) {// ��ǰ���ظ�����
				MediaUtils.CURMODEL = Constants.MODEL_SINGLE;// �л��ɵ���ѭ��
				// ����ui
				mIv_bottom_model.setImageResource(R.drawable.icon_playmode_single);
				// ��ʾ
				Toast.makeText(getApplicationContext(), "����ѭ��", 0).show();
			} else if (MediaUtils.CURMODEL == Constants.MODEL_SINGLE) {// ��ǰ�ǵ���ѭ��
				MediaUtils.CURMODEL = Constants.MODEL_NORMAL;// �л���˳�򲥷�
				// ����ui
				mIv_bottom_model.setImageResource(R.drawable.icon_playmode_normal);
				// ��ʾ
				Toast.makeText(getApplicationContext(), "˳�򲥷�", 0).show();
			}
			break;
		case R.id.ib_bottom_update:
			reflash();
			break;
		case R.id.ib_top_volumn:
			AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			// ���ֿ������õ��������
			int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			// ��������
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 2, AudioManager.FLAG_PLAY_SOUND);
			break;
			

		default:
			break;
		}
	}

	public void startMediaService(String option) {
		Intent service = new Intent(MainActivity.this, MusicService.class);
		service.putExtra("messenger", new Messenger(handler));
		service.putExtra("option", option);
		startService(service);
	}

	public void startMediaService(String option, String path) {
		Intent service = new Intent(MainActivity.this, MusicService.class);
		service.putExtra("option", option);
		service.putExtra("messenger", new Messenger(handler));
		service.putExtra("path", path);
		startService(service);
	}

	public void startMediaService(String option, int progress) {
		Intent service = new Intent(MainActivity.this, MusicService.class);
		service.putExtra("messenger", new Messenger(handler));
		service.putExtra("option", option);
		service.putExtra("progress", progress);
		startService(service);
	}

	/**
	 * �޸���ɫ.ֻҪ���ǵ�curPostion�޸���.��ô��ɫֵ����Ҫ�޸�
	 * @param color
	 */
	public void changeColorWhite() {
		TextView tv = (TextView) mLv_list.findViewWithTag(MediaUtils.CURPOSITION);
		if (tv != null) {
			tv.setTextColor(Color.WHITE);
		}
	}

	public void changeColorGreen() {
		TextView tv = (TextView) mLv_list.findViewWithTag(MediaUtils.CURPOSITION);
		if (tv != null) {
			tv.setTextColor(Color.GREEN);
		}
	}

	/**
	 * �޸�minilrc���ı�
	 * @param lrcString
	 */
	public void setMiniLrc(String lrcString) {
		mTv_minilrc.setText(lrcString);
	}

	/**
	 * 1.�����ض��Ĺ㲥,�ò���ϵͳ���¶�ý������
	 * 2.ϵͳɨ�����,�ᷢ��һ���ض��ĵĹ㲥.����ֻ��Ҫȥ�����ض��Ĺ㲥
	 */
		public void reflash() {
			/**---------------����ϵͳɨ����ɵĹ㲥---------------**/
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
			filter.addDataScheme("file");
			//ע��㲥
			registerReceiver(receiver, filter);

			/**---------------���͹㲥,��ϵͳ����ý�����ݿ�---------------**/
			String  file = "file://" + Environment.getExternalStorageDirectory();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//�����4.4�����ϰ汾
                Intent mediaScanIntent = new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(new File(file)); 
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
            } else {
                sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_MOUNTED,
                        Uri.parse("file://"
                                + Environment.getExternalStorageDirectory())));
            }
			
			
//			Intent intent = new Intent();
//			intent.setAction(Intent.ACTION_MEDIA_MOUNTED);
//			String  file = "file://" + Environment.getExternalStorageDirectory();
//			intent.setData(Uri.fromFile(new File(file)));
//			//intent.setData(Uri.parse("file://" + Environment.getExternalStorageDirectory()));
//			sendBroadcast(intent);
		}
		
		MyBroadcastReceiver receiver = new MyBroadcastReceiver();
		private MusicAdapter mAdapter;

		class MyBroadcastReceiver extends BroadcastReceiver {
			@Override
			public void onReceive(Context context, Intent intent) {//onReceive����������治Ӧ��ִ�к�ʱ�Ĳ���
				//��ע��㲥
				unregisterReceiver(receiver);
				//ִ��task
				new MyScanTask().execute();
			}
		}
		
		
		class MyScanTask extends AsyncTask<Void, Void, Void> {

			private ProgressDialog mDialog;

			@Override
			protected Void doInBackground(Void... params) {
				//���¸���songList
				MediaUtils.initSongList(MainActivity.this);//contentProvider-->sqlite
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				mDialog.dismiss();
				//listviewˢ��
				mAdapter.notifyDataSetChanged();
				super.onPostExecute(result);
			}
			

			@Override
			protected void onPreExecute() {
				mDialog = ProgressDialog.show(MainActivity.this, "��ʾ", "����������...");
				super.onPreExecute();
			}
		}
		
		@Override
		public void onBackPressed() {
			//1.�ص�����-->��������-->����һ��������ʽ��ͼ
			// ֱ�ӿ����ֻ�����  
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_MAIN);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addCategory(Intent.CATEGORY_HOME);
			startActivity(intent);
			//2.��ʾnotification
			showNotification();
		}

		private void showNotification() {

			NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			Notification notification = new Notification();
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.icon = R.drawable.ic_launcher;

			Intent intent = new Intent(this, MainActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			notification.setLatestEventInfo(this, "�ƽ�����ֲ�����", null, contentIntent);

			notifManager.notify(0, notification);
		}

	}
