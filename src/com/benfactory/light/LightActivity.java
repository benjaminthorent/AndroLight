package com.benfactory.light;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LightActivity extends Activity {

	private Camera cam;
	private ImageButton mainButton;
	private ImageButton settingsButton;
	private RelativeLayout timerSection;
	private ProgressBar progressBar;
	private TextView remainingTime;
	private ImageButton reloadTimerButton;

	boolean lightIsOn = false;
	private CustomCountDownTimer timer = null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.home); 

		/*
		 * Perform actions to be checked when activity is started
		 */
		// Update light status (notably in case, by settings, light should be activated when starting the applciation)
		lightIsOn = new PreferencesHandler(getApplicationContext()).isLightOnStartUp();
		// Set app language as per settings
		setAppLanguage();

		/*
		 * Get and set all graphical elements 
		 */

		// OnOff Button
		mainButton = (ImageButton) findViewById(R.id.main_button);
		mainButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				lightIsOn=!lightIsOn;
				updateDisplayAndLightStatus();
			}
		});

		// Settings Button
		settingsButton = (ImageButton) findViewById(R.id.settings_button);
		settingsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(LightActivity.this, SettingsActivity.class);
				startActivity(intent);
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

		/*
		 * Update all display
		 */
		updateFullDisplay();
	}

	@SuppressWarnings("deprecation")
	private void lightOn() {
		/*
		 * First check if device is supporting flashlight or not
		 */
		boolean hasFlash = getApplicationContext().getPackageManager()
				.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

		if (!hasFlash) {
			// device doesn't support flash
			// Show alert message and close the application
			AlertDialog alert = new AlertDialog.Builder(LightActivity.this)
					.create();
			alert.setTitle("Error");
			alert.setMessage("Sorry, your device doesn't support flash light!");
			alert.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// closing the application
					finish();
				}
			});
			alert.show();
			return;
		} else {
			if(cam==null || !cam.getParameters().getFlashMode().equals(Parameters.FLASH_MODE_TORCH)){
				cam = Camera.open();     
				Parameters params = cam.getParameters();
				params.setFlashMode(Parameters.FLASH_MODE_TORCH);
				cam.setParameters(params);
				cam.startPreview();
				/*cam.autoFocus(new AutoFocusCallback() {
					public void onAutoFocus(boolean success, Camera camera) {
					}
				});*/
				// Start timer when light is on
				startTimer();
			}
		}
		
	}

	private void lightOff() {
		if(cam!=null){
			cam.release();
			cam=null;
		}
		// End timer
		finishTimer();
	}

	private void updateDisplayAndLightStatus(){
		if(lightIsOn){
			lightOn();
			mainButton.setImageResource(R.drawable.button_on);

		} else {
			lightOff();
			mainButton.setImageResource(R.drawable.button_off);
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

}
