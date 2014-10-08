package com.benfactory.light;

import java.io.IOException;
import java.util.Locale;

import javax.security.auth.callback.Callback;

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
import android.widget.Toast;

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
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.home); 

		/*
		 * Perform actions to be checked when activity is started
		 */
		// Update light status (notably in case, by settings, light should be activated when starting the applciation)
		lightIsOn = new PreferencesHandler(getApplicationContext()).isLightOnStartUp();
		// Set app language as per settings
		setAppLanguage();

		/*
		 * 
		 */
		SurfaceView preview = (SurfaceView)findViewById(R.id.PREVIEW);
		mHolder = preview.getHolder();
		mHolder.addCallback(this);
		
		
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
		/*settingsButton = (ImageButton) findViewById(R.id.settings_button);
		settingsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(LightActivity.this, SettingsActivity.class);
				startActivity(intent);
			}
		});*/

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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.light, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent settingsIntent = new Intent(LightActivity.this, SettingsActivity.class);
			startActivity(settingsIntent);
			return true;
		case R.id.action_battery:
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			LayoutInflater inflater = LightActivity.this.getLayoutInflater();
			builder.setTitle("Battery Information")
			       .setView(inflater.inflate(R.layout.battery_status_dialog, null))
			       .setCancelable(false)
			       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                //do things
			           }
			       })
			       .setIcon(android.R.drawable.ic_dialog_info);
			AlertDialog alert = builder.create();
			
			alert.show();	
			
			//Updates
			Intent i = new ContextWrapper(getApplicationContext()).registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			int temp =i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
			int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			int health = i.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
			String tech = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
			String healthStatus = (health==BatteryManager.BATTERY_HEALTH_GOOD ? "Good battery health !" : "You might face some issues with your battery :(...");
			
			((TextView)alert.findViewById(R.id.battery_level)).setText("Battery level : " + level + "%");
			((TextView)alert.findViewById(R.id.battery_temperature)).setText("Temperature : " + temp/10 + "°C");
			((TextView)alert.findViewById(R.id.battery_technology)).setText("Technology : " + tech);
			((TextView)alert.findViewById(R.id.battery_health)).setText(healthStatus);

			return true;
		case R.id.action_more_info:
			Intent moreInfoIntent = new Intent(LightActivity.this, Settings2Activity.class);
			startActivity(moreInfoIntent);
			return true;
		case R.id.action_give_me_a_beer:
			Intent supportUsIntent = new Intent(LightActivity.this, SupportMeActivity.class);
			startActivity(supportUsIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
				try {
					cam.setPreviewDisplay(mHolder);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mHolder = holder;
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
