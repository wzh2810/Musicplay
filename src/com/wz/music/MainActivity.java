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
	private Handler handler = new Handler() {//接收结果,刷新ui

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
				//序列化歌词
				File f = MediaUtils.getLrcFile(MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
				mLrcUtil.ReadLRC(f);
				//使用功能
				mLrcUtil.RefreshLRC(currentPosition);
				//1. 设置集合
				mTv_lyricShow.SetTimeLrc(LrcUtil.lrclist);
				//2. 更新滚动歌词
				mTv_lyricShow.SetNowPlayIndex(currentPosition);
				break;
			case Constants.MSG_ONCOMPLETION:
				//更加当前的播放model做对应的处理
				if (MediaUtils.CURMODEL == Constants.MODEL_NORMAL) {//当前是顺序播放
					if (MediaUtils.CURPOSITION < MediaUtils.songList.size() - 1) {
						changeColorWhite();
						MediaUtils.CURPOSITION++;//MediaUtils.CURPOSITION最大值就是MediaUtils.songList.size() - 1
						changeColorGreen();
						startMediaService("播放", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
					} else {
						startMediaService("停止");
					}
				} else if (MediaUtils.CURMODEL == Constants.MODEL_RANDOM) {//当前是随机播放
					Random random = new Random();
					int position = random.nextInt(MediaUtils.songList.size());
					changeColorWhite();
					MediaUtils.CURPOSITION = position;
					changeColorGreen();
					startMediaService("播放", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
				} else if (MediaUtils.CURMODEL == Constants.MODEL_REPEAT) {//当前是重复播放
					if (MediaUtils.CURPOSITION < MediaUtils.songList.size() - 1) {
						changeColorWhite();
						MediaUtils.CURPOSITION++;//MediaUtils.CURPOSITION最大值就是MediaUtils.songList.size() - 1
						changeColorGreen();
					} else {
						changeColorWhite();
						MediaUtils.CURPOSITION = 0;
						changeColorGreen();
						startMediaService("播放", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
					}
				} else if (MediaUtils.CURMODEL == Constants.MODEL_SINGLE) {//当前是单曲循环
					startMediaService("播放", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
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
		mInstance = this;//当前activity的引用
		initView();
		initData();
		initListener();
	}

	/**
	 * 初始化控件
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
		//默认选中第一个
		findViewById(R.id.ib_top_play).setSelected(true);
	}

	/**
	 * 数据的加载
	 */
	private void initData() {
		MediaUtils.initSongList(this);
		mAdapter = new MusicAdapter(this);
		mLv_list.setAdapter(mAdapter);
	}

	/**
	 * 初始化监听
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
				// 1.修改curposition
				changeColorWhite();
				MediaUtils.CURPOSITION = position;
				changeColorGreen();
				// 2.播放
				startMediaService("play", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
				// 3.修改图标
				mIv_bottom_play.setImageResource(R.drawable.appwidget_pause);

			}
		});

		mSk_duration.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { // 停止拖拽
				mSk_duration.setProgress(seekBar.getProgress());
				startMediaService("progress", seekBar.getProgress());
				// 音乐播放器,跳转到指定的位置播放
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {// 触摸到拖拽按钮

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {// 进度改变

			}
		});

	}

	private int[] topArr = { R.id.ib_top_play, R.id.ib_top_list, R.id.ib_top_lrc, R.id.ib_top_volumn };
	private LyricShow mTv_lyricShow;

	/**
	 * 顶部按钮的选中效果
	 * @param selectedId
	 */
	private void setTopSelected(int selectedId) {
		//1.还原所有控件的效果,让top上面的4个按钮显示效果都是未选中
		findViewById(R.id.ib_top_play).setSelected(false);
		findViewById(R.id.ib_top_list).setSelected(false);
		findViewById(R.id.ib_top_lrc).setSelected(false);
		findViewById(R.id.ib_top_volumn).setSelected(false);

		//2.让传递进来的控件有选中效果
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
		case R.id.ib_bottom_play:// 播放按钮,点击同一个按钮.有两个操作.需要定义一个变量进行控制
			// 启动服务，而且让服务播放音乐
			if (MediaUtils.CURPOSITION == Constants.STATE_STOP) {// 默认是停止,点击就变播放
				startMediaService("play", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);
				// 修改图标
				mIv_bottom_play.setImageResource(R.drawable.appwidget_pause);
			} else if (MediaUtils.CURSTATE == Constants.STATE_PLAY) { // 第二次点击的时候，当前的状态是播放
				startMediaService("pause");
				mIv_bottom_play.setImageResource(R.drawable.img_playback_bt_play);
			} else if (MediaUtils.CURSTATE == Constants.STATE_PAUSE) { // 第三次点击的时候，当前的状态是暂停
				startMediaService("continuePlay");
				mIv_bottom_play.setImageResource(R.drawable.appwidget_pause);
			}

			break;
		case R.id.ib_bottom_last:
			if (MediaUtils.CURPOSITION > 0) {
				changeColorWhite();
				MediaUtils.CURPOSITION--;
				changeColorGreen();
				// 播放
				startMediaService("play", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);

				mIv_bottom_play.setImageResource(R.drawable.appwidget_pause);

			}

			break;

		case R.id.ib_bottom_next:
			if (MediaUtils.CURPOSITION < MediaUtils.songList.size() - 1) {
				changeColorWhite();
				MediaUtils.CURPOSITION++;
				changeColorGreen();
				// 播放
				startMediaService("play", MediaUtils.songList.get(MediaUtils.CURPOSITION).path);

				mIv_bottom_play.setImageResource(R.drawable.appwidget_pause);

			}

		case R.id.ib_bottom_model:
			if (MediaUtils.CURMODEL == Constants.MODEL_NORMAL) {// 当前是顺序播放
				MediaUtils.CURMODEL = Constants.MODEL_RANDOM;// 切换成随机播放
				// 更新ui
				mIv_bottom_model.setImageResource(R.drawable.icon_playmode_shuffle);
				// 提示
				Toast.makeText(getApplicationContext(), "随机播放", 0).show();
			} else if (MediaUtils.CURMODEL == Constants.MODEL_RANDOM) {// 当前是随机播放
				MediaUtils.CURMODEL = Constants.MODEL_REPEAT;// 切换成重复播放
				// 更新ui
				mIv_bottom_model.setImageResource(R.drawable.icon_playmode_repeat);
				// 提示
				Toast.makeText(getApplicationContext(), "重复播放", 0).show();
			} else if (MediaUtils.CURMODEL == Constants.MODEL_REPEAT) {// 当前是重复播放
				MediaUtils.CURMODEL = Constants.MODEL_SINGLE;// 切换成单曲循环
				// 更新ui
				mIv_bottom_model.setImageResource(R.drawable.icon_playmode_single);
				// 提示
				Toast.makeText(getApplicationContext(), "单曲循环", 0).show();
			} else if (MediaUtils.CURMODEL == Constants.MODEL_SINGLE) {// 当前是单曲循环
				MediaUtils.CURMODEL = Constants.MODEL_NORMAL;// 切换成顺序播放
				// 更新ui
				mIv_bottom_model.setImageResource(R.drawable.icon_playmode_normal);
				// 提示
				Toast.makeText(getApplicationContext(), "顺序播放", 0).show();
			}
			break;
		case R.id.ib_bottom_update:
			reflash();
			break;
		case R.id.ib_top_volumn:
			AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			// 音乐可以设置的最大音量
			int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			// 设置音量
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
	 * 修改颜色.只要我们的curPostion修改了.那么颜色值就需要修改
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
	 * 修改minilrc的文本
	 * @param lrcString
	 */
	public void setMiniLrc(String lrcString) {
		mTv_minilrc.setText(lrcString);
	}

	/**
	 * 1.发送特定的广播,让操作系统更新多媒体数据
	 * 2.系统扫描完成,会发出一个特定的的广播.我们只需要去监听特定的广播
	 */
		public void reflash() {
			/**---------------接收系统扫描完成的广播---------------**/
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
			filter.addDataScheme("file");
			//注册广播
			registerReceiver(receiver, filter);

			/**---------------发送广播,让系统更新媒体数据库---------------**/
			String  file = "file://" + Environment.getExternalStorageDirectory();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//如果是4.4及以上版本
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
			public void onReceive(Context context, Intent intent) {//onReceive这个方法里面不应该执行耗时的操作
				//反注册广播
				unregisterReceiver(receiver);
				//执行task
				new MyScanTask().execute();
			}
		}
		
		
		class MyScanTask extends AsyncTask<Void, Void, Void> {

			private ProgressDialog mDialog;

			@Override
			protected Void doInBackground(Void... params) {
				//重新更新songList
				MediaUtils.initSongList(MainActivity.this);//contentProvider-->sqlite
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				mDialog.dismiss();
				//listview刷新
				mAdapter.notifyDataSetChanged();
				super.onPostExecute(result);
			}
			

			@Override
			protected void onPreExecute() {
				mDialog = ProgressDialog.show(MainActivity.this, "提示", "玩命更新中...");
				super.onPreExecute();
			}
		}
		
		@Override
		public void onBackPressed() {
			//1.回到桌面-->跳到桌面-->开启一个桌面隐式意图
			// 直接开启手机桌面  
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_MAIN);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addCategory(Intent.CATEGORY_HOME);
			startActivity(intent);
			//2.显示notification
			showNotification();
		}

		private void showNotification() {

			NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			Notification notification = new Notification();
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.icon = R.drawable.ic_launcher;

			Intent intent = new Intent(this, MainActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			notification.setLatestEventInfo(this, "黄金版音乐播放器", null, contentIntent);

			notifManager.notify(0, notification);
		}

	}
