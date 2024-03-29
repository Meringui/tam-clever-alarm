package com.vutbr.fit.tam.activity;

import java.util.Calendar;

import com.vutbr.fit.tam.R;
import com.vutbr.fit.tam.alarm.AlarmLauncher;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import com.vutbr.fit.tam.database.*;


public class Ringing extends Activity implements OnClickListener {
	
	final private String DEFAULT_RINGTONE = "content://settings/system/ringtone";
	
	private AudioManager audioManager;
	private Ringtone ringtone;
	private Uri uri;
	private Button stopButton;
	private Button snoozeButton;
	private int systemVolume;
	private int systemRingMode;
	private int ringVolume;
	private int snoozeTime; // in minutes
	private SettingsAdapter settingsAdapter;
	private boolean stop;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.ringing);
        
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        if (!getIntent().getBooleanExtra("SCREEN_OFF", false)) {
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        
        this.settingsAdapter = new SettingsAdapter(this);
        
        this.stopButton = (Button) this.findViewById(R.id.stopButton);
        this.stopButton.setOnClickListener(this);
        
        this.snoozeButton = (Button) this.findViewById(R.id.snoozeButton);
        this.snoozeButton.setOnClickListener(this);
        
        this.load();
        
        if (uri != null) {
        	ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
        }
        
        this.audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        this.systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING); // Backup current volume
        this.systemRingMode = audioManager.getRingerMode();
        
        this.audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        this.audioManager.setStreamVolume(AudioManager.STREAM_RING, ringVolume, 0);
        this.stop = false;
        startRinging();
    }
	
    private void load() {
    	this.settingsAdapter.open();
    	this.ringVolume = Integer.parseInt(settingsAdapter.fetchSetting("volume", "0"));
    	snoozeTime = Integer.parseInt(settingsAdapter.fetchSetting("snoozetime", "300"));
        uri = Uri.parse(settingsAdapter.fetchSetting("uri", DEFAULT_RINGTONE));
        this.settingsAdapter.close();
    }
	
	private void startRinging() {
		if (ringtone != null && ringtone.isPlaying() == false) {
			ringtone.play();
		}
	}
	
	private void snooze() {
		if (ringtone != null && ringtone.isPlaying() == true) {
			ringtone.stop();
		}
		Intent intent = new Intent(Ringing.this, AlarmLauncher.class);
        PendingIntent sender = PendingIntent.getBroadcast(Ringing.this,
                0, intent, 0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, snoozeTime*60);

        // Schedule the alarm!
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
        this.audioManager.setStreamVolume(AudioManager.STREAM_RING, systemVolume, 0); // Restore system volume
        this.audioManager.setRingerMode(systemRingMode);
	}
	
	private void stopRinging() {
		if (ringtone != null && ringtone.isPlaying() == true) {
			ringtone.stop();
		}
		this.audioManager.setStreamVolume(AudioManager.STREAM_RING, systemVolume, 0); // Restore system volume
		this.audioManager.setRingerMode(systemRingMode);
		this.stop = true;
		finish();
	}

	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.stopButton:
			this.stopRinging();
			break;
		case R.id.snoozeButton:
			this.snooze();
			finish();
			break;
		}
	}
	
	public void onStop() {
		if (!stop) snooze();
		finish();
		super.onStop();
	}
}