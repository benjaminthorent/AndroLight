package com.benfactory.light;

import java.io.IOException;
import java.util.Locale;
import javax.security.auth.callback.Callback;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LightActivity extends Activity implements Callback, android.view.SurfaceHolder.Callback {

	private Camera cam;
	private ImageButton mainButton;
	private RelativeLayout timerSection;
	private ProgressBar progressBar;
	private TextView remainingTime;
	private ImageButton reloadTimerButton;
	private SurfaceHolder mHolder;

	boolean lightIsOn = false;
	private CustomCountDownTimer timer = null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home); 

		// Set app language as per settings
		setAppLanguage();

		// Set up a surface view on which the camera behavior will be attached to
		SurfaceView preview = (SurfaceView)findViewById(R.id.PREVIEW);
		mHolder = preview.getHolder();
		mHolder.addCallback(this);

		// OnOff Button
		mainButton = (ImageButton) findViewById(R.id.main_button);
		mainButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				lightIsOn=!lightIsOn;
				updateDisplayAndLightStatus();
			}
		});

		// Timer section
		timerSection = (RelativeLayout) findViewById(R.id.timer_section);
		progressBar = (ProgressBar) findViewById(R.id.timer_progressbar);
		remainingTime = (TextView) findViewById(R.id.timer_remaining_time);
		reloadTimerButton = (ImageButton) findViewById(R.id.border);
		reloadTimerButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(timer!=null) {
					timer.cancel();
					timer = new CustomCountDownTimer((new PreferencesHandler(getApplicationContext())).getTimeToSleepAsMs(), 1000, progressBar, remainingTime, LightActivity.this);
					timer.start();
				}
			}
		});

		//Update all display
		updateFullDisplay();
	}

	/**
	 * Menu initialization 
	 * Menu gives access to both settings and battery level
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.light, menu);
		return true;
	}

	/**
	 * Navigation bar items clicks handling
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			// Start dedicated settings activity
			Intent settingsIntent = new Intent(LightActivity.this, SettingsActivity.class);
			startActivity(settingsIntent);
			return true;
		case R.id.action_battery:
			// Display battery information pop-up
			displayBatteryInfoPopup();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@SuppressLint("InflateParams")
	private void displayBatteryInfoPopup() {

		// Set up pop- up basic display
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = LightActivity.this.getLayoutInflater();
		builder.setTitle(R.string.battery_info_popup_title)
		.setView(inflater.inflate(R.layout.battery_status_dialog, null))
		.setCancelable(false)
		.setPositiveButton(R.string.OKoption, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				//do nothing, just close pop-up
			}
		})
		.setIcon(android.R.drawable.ic_dialog_info);
		AlertDialog alert = builder.create();
		alert.show();	

		// Update info inside view inflated in the pop-up view
		Intent i = new ContextWrapper(getApplicationContext()).registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int temp =i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
		int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int health = i.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
		String tech = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
		String healthStatus = (health==BatteryManager.BATTERY_HEALTH_GOOD ? getString(R.string.battery_good_health_info) : getString(R.string.battery_bad_health_info));
		((TextView)alert.findViewById(R.id.battery_level)).setText(getString(R.string.battery_level_info) + " " + level + "%");
		((TextView)alert.findViewById(R.id.battery_temperature)).setText(getString(R.string.battery_temperature_info) + " " + temp/10 + "¡C");
		((TextView)alert.findViewById(R.id.battery_technology)).setText(getString(R.string.battery_technology_info) + " " + tech);
		((TextView)alert.findViewById(R.id.battery_health)).setText(healthStatus);
	}

	/**
	 * Main method to handle behavior when turning light on
	 */
	private void lightOn() {
		//First check if device is supporting flashlight or not
		boolean hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);


		if (!hasFlash) {
			// If device doesn't support flash, show alert message
			displayFlashNotSupportedPopup();	
			// Re update light status since in that case, light could not be turned on
			lightIsOn = false;
		} else {
			// Get battery level
			PreferencesHandler ph = new PreferencesHandler(getApplicationContext());
			Intent i = new ContextWrapper(getApplicationContext()).registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			if(ph.isLowBatteryWarningActivated() && level < ph.getLowBatteryWarningThreshold()){
				// If battery level below threshold defined in settings, display dedicated warning pop-up
				displayLowBatteryWarningPopup(ph.getLowBatteryWarningThreshold(), level);

			} else {
				// If no issue foreseen with flash usage, concretely turn it on
				turnCameraLightOn();
			}

		}

	}

	/**
	 * 
	 * @param thresholdInSettings
	 * @param currentBatteryLevel
	 */
	private void displayLowBatteryWarningPopup(int thresholdInSettings, int currentBatteryLevel) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.low_battery_warning_popup_title))
		.setMessage(getString(R.string.low_battery_warning_popup_message_part1) + currentBatteryLevel 
				+ getString(R.string.low_battery_warning_popup_message_part2) 
				+ thresholdInSettings + getString(R.string.low_battery_warning_popup_message_part3))
				.setCancelable(false)
				.setPositiveButton(getString(R.string.YESoption), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						turnCameraLightOn();
					}
				})
				.setNegativeButton(getString(R.string.NOoption), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						lightIsOn = false;
					}
				})
				.setIcon(android.R.drawable.ic_dialog_info);
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void displayFlashNotSupportedPopup() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.flash_not_supported_popup_title))
		.setMessage(getString(R.string.flash_not_supported_popup_message))
		.setCancelable(false)
		.setPositiveButton(R.string.OKoption, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// closing the application
				finish();
			}
		})
		.setIcon(android.R.drawable.ic_dialog_alert);
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void turnCameraLightOn() {
		if(cam==null || !cam.getParameters().getFlashMode().equals(Parameters.FLASH_MODE_TORCH)){
			cam = Camera.open();   
			try {
				cam.setPreviewDisplay(mHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Parameters params = cam.getParameters();
			params.setFlashMode(Parameters.FLASH_MODE_TORCH);
			cam.setParameters(params);
			cam.startPreview();
			// Start timer when light is on
			startTimer();
			mainButton.setImageResource(R.drawable.button_on);
		}
	}

	private void lightOff() {
		if(cam!=null){
			cam.release();
			cam=null;
		}
		mainButton.setImageResource(R.drawable.button_off);
		// End timer
		finishTimer();
	}

	private void updateDisplayAndLightStatus(){
		if(lightIsOn){
			lightOn();
		} else {
			lightOff();
		}
	}

	private void updateTimerDisplay(){
		PreferencesHandler ph = new PreferencesHandler(getApplicationContext());
		if(ph.isTimeToSleepActivated()){
			timerSection.setVisibility(View.VISIBLE);
			reloadTimerButton.setEnabled(true);
			remainingTime.setText(ph.getTimeToSleepAsText());
			if(timer==null){
				progressBar.setProgress(100);
				reloadTimerButton.setEnabled(false);
			}
		} else {
			reloadTimerButton.setEnabled(false);
			timerSection.setVisibility(View.GONE);
		}
	}

	private void updateFullDisplay(){
		updateDisplayAndLightStatus();
		updateTimerDisplay();
	}

	public void turnLightDownOnTimersUp(){
		lightIsOn = false;
		updateDisplayAndLightStatus();
	}

	/*
	 * Method to be called when starting timer. Handles both timer and screen display.
	 */
	private void startTimer() {
		PreferencesHandler ph = new PreferencesHandler(getApplicationContext());
		if(ph.isTimeToSleepActivated()){
			timer = new CustomCountDownTimer(ph.getTimeToSleepAsMs(), 1000, progressBar, remainingTime, this);
			reloadTimerButton.setEnabled(true);
			timer.start();
		}
	}

	/*
	 * Method to be called when light timer is up. Handles both timer and screen display.
	 */
	private void finishTimer() {
		if(timer!=null){
			timer.cancel();
			timer = null;
			updateTimerDisplay();
		}
	}

	/*
	 * Set application language as per current setting to have proper localizations to be displayed on this screen
	 */
	private void setAppLanguage(){
		PreferencesHandler preferenceshandler = new PreferencesHandler(getApplicationContext());
		Locale locale = new Locale(preferenceshandler.getLanguage());
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
	}


	/*
	 * onResume override to always update screen display when reaching it.
	 * @see android.app.Activity#onResume()
	 */	
	@Override
	public void onResume() {
		super.onResume();
		//Update all display
		updateFullDisplay();
	}

	/*
	 * onPause override to always shut light down when quitting the screen whether temporally or definitively
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public void onPause() {
		lightOff();
		super.onPause();
	}

	/*
	 * onStop override to always shut light down when quitting the screen whether temporally or definitively
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		lightOff();
		super.onStop();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mHolder = holder;
		// Update light status (notably in case, by settings, light should be activated when starting the applciation)
		lightIsOn = new PreferencesHandler(getApplicationContext()).isLightOnStartUp();
		updateFullDisplay();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mHolder = null;
	}

}
