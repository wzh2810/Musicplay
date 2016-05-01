package com.wz.music.service;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.widget.Toast;

import com.wz.music.conf.Constants;
import com.wz.music.utils.MediaUtils;

public class MusicService extends Service implements OnErrorListener, OnPreparedListener, OnCompletionListener, OnAudioFocusChangeListener {

	private MediaPlayer mPlayer;
	private Messenger mMessenger;
	private Timer mTimer;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() { // �������serve��ִ��һ��
		mPlayer = new MediaPlayer();
		// ���ü�����
		mPlayer.setOnErrorListener(this);// ������Դ��ʱ�������
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnCompletionListener(this);
		//����audioManger
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {// ÿ���������������˷���
		String option = intent.getStringExtra("option");
		if (mMessenger == null) {
			mMessenger = (Messenger) intent.getExtras().get("messenger");
		}

		if ("play".equals(option)) {
			String path = intent.getStringExtra("path");
			play(path);
		} else if ("pause".equals(option)) {
			pause();
		} else if ("continuePlay".equals(option)) {
			continuePlay();
		} else if ("stop".equals(option)) {
			stop();
		} else if ("progress".equals(option)) {
			int progress = intent.getIntExtra("progress", -1);
			seekPlay(progress);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		super.onDestroy();
	}

	/** ---------------��װ���ֲ��ų����ķ��� begin--------------- **/

	// activity��Ҫ����service�з���
	// 1.bindService��������
	// 2.aidl
	// 3.���͹㲥
	// 4.��Ϣ����
	// 5.����service�������,���ظ����� onStartCommd()����������

	private void play(String path) {
		try {
			mPlayer.reset();// idle
			mPlayer.setDataSource(path);// ���ø�����·��
			mPlayer.prepare();// ��ʼ׼������������ʹ��ͬ��׼���Ϳ�����
			mPlayer.start();// ��ʼ����
			MediaUtils.CURSTATE = Constants.STATE_PLAY;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * ��ͣ
	 */
	private void pause() {
		if (mPlayer != null && mPlayer.isPlaying()) {
			mPlayer.pause();
			MediaUtils.CURSTATE = Constants.STATE_PAUSE;
		}

	}

	/**
	 * ��������
	 */
	private void continuePlay() {
		if (mPlayer != null && !mPlayer.isPlaying()) {
			mPlayer.start();
			MediaUtils.CURSTATE = Constants.STATE_PLAY;
		}

	}

	/**
	 * ֹͣ����
	 */
	private void stop() {
		if (mPlayer != null) {
			mPlayer.stop();
			MediaUtils.CURSTATE = Constants.STATE_STOP;
			if (mTimer != null) {
				mTimer.cancel();
				mTimer = null;
			}
		}

	}

	/**
	 * ���Ȳ���
	 * 
	 * @param progress
	 */
	private void seekPlay(int progress) {
		if (mPlayer != null && mPlayer.isPlaying()) {
			mPlayer.seekTo(progress);
		}

	}

	/** ---------------��װ���ֲ��ų����ķ��� end--------------- **/

	/** ---------------��صĻص�����--------------- **/

	@Override
	public void onCompletion(MediaPlayer mp) {
		try {
			//service������Ϣ������Activity����ǰ�ĸ�����������
			Message msg = Message.obtain();
			msg.what = Constants.MSG_ONCOMPLETION;
			//������Ϣ
			mMessenger.send(msg);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		if (mTimer == null) {
			mTimer = new Timer();
		}
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				try {
					// 1.׼���õ��ʱ�򣬸���Activity����ǰ��������ʱ��
					int currentPaosition = mPlayer.getCurrentPosition();
					int totalDuration = mPlayer.getDuration();
					Message msg = Message.obtain();
					msg.what = Constants.MSG_ONPREPARED;
					msg.arg1 = currentPaosition;
					msg.arg2 = totalDuration;
					// ������Ϣ
					mMessenger.send(msg);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 0, 1000);

	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Toast.makeText(getApplicationContext(), "��,������Դ������", Toast.LENGTH_LONG).show();
		return true;
	}

	/**---------------��Ƶ���㴦����صķ���---------------**/
	@Override
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_GAIN://���Ѿ��õ�����Ƶ���㡣 
			System.out.println("-------------AUDIOFOCUS_GAIN---------------");
			// resume playback
			mPlayer.start();
			mPlayer.setVolume(1.0f, 1.0f);
			break;
		case AudioManager.AUDIOFOCUS_LOSS://���Ѿ�ʧȥ����Ƶ����ܳ�ʱ���ˡ������ֹͣ���е���Ƶ����
			System.out.println("-------------AUDIOFOCUS_LOSS---------------");
			// Lost focus for an unbounded amount of time: stop playback and release media player
			if (mPlayer.isPlaying())
				mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://����ʱʧȥ����Ƶ����
			System.out.println("-------------AUDIOFOCUS_LOSS_TRANSIENT---------------");
			// Lost focus for a short time, but we have to stop
			// playback. We don't release the media player because playback
			// is likely to resume
			if (mPlayer.isPlaying())
				mPlayer.pause();
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://����ʱʧȥ����Ƶ���㣬�������С���ؼ���������Ƶ������������������ȫ��ɱ��Ƶ��
			System.out.println("-------------AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK---------------");
			// Lost focus for a short time, but it's ok to keep playing
			// at an attenuated level
			if (mPlayer.isPlaying())
				mPlayer.setVolume(0.1f, 0.1f);
			break;
		}
		
	}

}
