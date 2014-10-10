package com.benfactory.light;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import com.benfactory.light.PreferencesHandler.Language;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class SettingsActivity extends Activity {

	private PreferencesHandler ph;
	private long timeToSleepDuration;
	private int lowBatteryWarningThreshold;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_settings); 

		// Init preferences handler
		ph = new PreferencesHandler(getApplicationContext());
		lowBatteryWarningThreshold = ph.getLowBatteryWarningThreshold();
		timeToSleepDuration = ph.getTimeToSleepAsMs();

		// Set language spinner
		Spinner languageSpinner = (Spinner) findViewById(R.id.language_spinner);
		List<String> list = new ArrayList<String>();
		for(Language l: Language.values()){
			list.add(getResources().getString(l.getRessourceID()));
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		languageSpinner.setAdapter(dataAdapter);

		languageSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				Locale locale = new Locale(Language.getLocaleStringFromIndex(position));
				Locale.setDefault(locale);
				Configuration config = new Configuration();
				config.locale = locale;
				getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// Do nothing
			}
		});

		// Update as per current preferences
		((CheckBox)findViewById(R.id.light_on_start_up_checkbox)).setChecked(ph.isLightOnStartUp());
		((Spinner)findViewById(R.id.language_spinner)).setSelection(ph.getDefaultLanguageIndex());

		LinearLayout emailMeSection =(LinearLayout) findViewById(R.id.email_me_section);
		emailMeSection.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				startSendEmailActivity();
			}
		});

		LinearLayout moreInfoSection =(LinearLayout) findViewById(R.id.more_info_section);
		moreInfoSection.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				displayMoreInfoPopup();
			}
		});

		CheckBox timeToSleepCheckbox = (CheckBox) findViewById (R.id.time_to_sleep_checkbox);
		timeToSleepCheckbox.setChecked(ph.isTimeToSleepActivated());
		timeToSleepCheckbox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleAutoShutDownSubDetailsDisplay();
			}});

		CheckBox lowBatteryWarningCheckbox = (CheckBox) findViewById (R.id.low_battery_warning_checkbox);
		lowBatteryWarningCheckbox.setChecked(ph.isLowBatteryWarningActivated());
		lowBatteryWarningCheckbox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleLowBatteryWarningSubDetailsDisplay();
			}});

		Button timeToSleepTimesetBtn = (Button) findViewById (R.id.time_to_sleep_timeset_btn);
		timeToSleepTimesetBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showAutoShutDownPopup();
			}
		});

		Button lowBatteryWarningThresholdBtn = (Button) findViewById (R.id.low_battery_warning_threshold_btn);
		lowBatteryWarningThresholdBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showLowBatteryWarningPopup();
			}
		});

		// Update display
		handleAutoShutDownSubDetailsDisplay();
		handleLowBatteryWarningSubDetailsDisplay();
		updateLowBatteryWarningThresholdDisplay();
		updateTimeToSleepTimerDisplay();

	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_save:
			saveSettings();
			return true;
		case android.R.id.home:
			showPopupSettingsUnsaved();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
    public void onBackPressed()
    {
		showPopupSettingsUnsaved();
    }

	private void saveSettings(){

		StringBuilder errorSettingsDetails = new StringBuilder();
		/*sb.append("select id1, ");
		sb.append(id2);
		sb.append(" from ");
		sb.append(table);*/

		if(performSettingsPreCheck(errorSettingsDetails)) {
			if(!ph.getLanguage().equalsIgnoreCase(Language.getLocaleStringFromIndex(((Spinner)findViewById(R.id.language_spinner)).getSelectedItemPosition()))){
				Builder alert = new AlertDialog.Builder(SettingsActivity.this);
				alert.setTitle("App will restart");
				alert.setMessage("Please note that your app will restart since your performed a language change in order to take this one into consideration. This is completely expected behaviour :).");
				alert.setIcon(android.R.drawable.ic_dialog_alert);
				alert.setPositiveButton(R.string.OKoption,new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Toast.makeText(getApplicationContext(), R.string.settingsUpdateOK, Toast.LENGTH_SHORT).show();
						// Save preferences
						saveDataIntoPreferenceHandler();
						// Force app restart to force language update
						forceAppRestart();
					}
				});
				alert.show();
			} else {
				Toast.makeText(getApplicationContext(), R.string.settingsUpdateOK, Toast.LENGTH_SHORT).show();
				// Save preferences
				saveDataIntoPreferenceHandler();
				// Go back to home page
				this.finish();
			}			
		} else {
			// raise warning message
			Builder alert = new AlertDialog.Builder(SettingsActivity.this);
			alert.setTitle(R.string.configurationIssue);
			alert.setMessage("Your current settings contain some impossible associations. Please update them.\n" + errorSettingsDetails.toString());
			alert.setIcon(android.R.drawable.ic_dialog_alert);
			alert.setPositiveButton(R.string.OKoption,null);
			alert.show();
		}

	}

	private void showPopupSettingsUnsaved(){
		if(ph.isSettingsWarningOnBackToBeShown()){
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final View view = SettingsActivity.this.getLayoutInflater().inflate(R.layout.settings_not_saved_dialog, null);
			builder.setTitle("You will loose your settings !")
			.setView(view)
			.setCancelable(false)
			.setPositiveButton(R.string.OKoption, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if(((CheckBox)view.findViewById(R.id.deactivate_warning_checkbox)).isChecked()){
						PreferencesHandler preferenceshandler = new PreferencesHandler(getApplicationContext());
						preferenceshandler.deActivateShowSettingsWarningOnBack();
					}
					SettingsActivity.this.finish();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					//do nothing
				}
			})
			.setIcon(android.R.drawable.ic_dialog_alert);
			final AlertDialog alert = builder.create();
			alert.show();	
		} else {
			this.finish();
		}
	}



	private void saveDataIntoPreferenceHandler() {
		ph.saveSettings(((CheckBox)findViewById(R.id.light_on_start_up_checkbox)).isChecked(), 
				((CheckBox)findViewById(R.id.time_to_sleep_checkbox)).isChecked(), 
				timeToSleepDuration, 
				((CheckBox)findViewById(R.id.low_battery_warning_checkbox)).isChecked(),
				lowBatteryWarningThreshold,
				Language.getLocaleStringFromIndex(((Spinner)findViewById(R.id.language_spinner)).getSelectedItemPosition()));
	}



	private void forceAppRestart() {
		Intent mStartActivity = new Intent(getApplicationContext(), LightActivity.class);
		int mPendingIntentId = 123456;
		PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager mgr = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				System.exit(0);
			}
		}, 100);

	}

	private boolean performSettingsPreCheck(StringBuilder errorSettingsDetails) {
		boolean preChecksResult = true;

		// Auto Shut Down feature prechecks
		// Settings are not correct if feature activated and corresponding time not >0
		if(((CheckBox)findViewById(R.id.time_to_sleep_checkbox)).isChecked() && timeToSleepDuration==0){
			errorSettingsDetails.append("\n" + getString(R.string.configurationTimeToSleepIssueDetails));
			preChecksResult = false;
		}

		// Low Battery Warning feature prechecks
		// Settings are not correct (or do not make sense) if feature activated and corresponding threshold not >0
		if(((CheckBox)findViewById(R.id.low_battery_warning_checkbox)).isChecked() && lowBatteryWarningThreshold==0){
			errorSettingsDetails.append("\n" + getString(R.string.configurationLowBatteryWarningIssueDetails));
			preChecksResult = false;
		}
		return preChecksResult;
	}



	private void startSendEmailActivity() {
		Intent email = new Intent(Intent.ACTION_SEND);
		email.putExtra(Intent.EXTRA_EMAIL, new String[] { "b.thorent@gmail.com" });
		email.putExtra(Intent.EXTRA_SUBJECT, "[AndroLight] Support");
		email.putExtra(Intent.EXTRA_TEXT, "");
		email.setType("message/rfc822");
		startActivity(Intent.createChooser(email, "Choose an Email client"));
	}

	private void displayMoreInfoPopup() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("More Info")
		.setMessage("Very interesting information...")
		.setCancelable(false)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				//do things
			}
		})
		.setIcon(android.R.drawable.ic_dialog_info);
		AlertDialog alert = builder.create();

		alert.show();
	}

	private void handleAutoShutDownSubDetailsDisplay(){

		CheckBox timeToSleepCheckbox = (CheckBox) findViewById(R.id.time_to_sleep_checkbox);
		TextView timeToSleepTimeSetText = (TextView) findViewById(R.id.time_to_sleep_timeset_text);
		Button timeToSleepTimeSetBtn = (Button) findViewById(R.id.time_to_sleep_timeset_btn);

		if(timeToSleepCheckbox.isChecked()){
			timeToSleepTimeSetBtn.setEnabled(true);
			timeToSleepTimeSetBtn.setAlpha((float) 1);
			timeToSleepTimeSetText.setAlpha((float) 1);
		} else {
			timeToSleepTimeSetBtn.setEnabled(false);
			timeToSleepTimeSetBtn.setAlpha((float) 0.2);
			timeToSleepTimeSetText.setAlpha((float) 0.2);
		}

	}

	private void handleLowBatteryWarningSubDetailsDisplay(){

		CheckBox timeToSleepCheckbox = (CheckBox) findViewById(R.id.low_battery_warning_checkbox);
		TextView timeToSleepTimeSetText = (TextView) findViewById(R.id.low_battery_warning_threshold_text);
		Button timeToSleepTimeSetBtn = (Button) findViewById(R.id.low_battery_warning_threshold_btn);

		if(timeToSleepCheckbox.isChecked()){
			timeToSleepTimeSetBtn.setEnabled(true);
			timeToSleepTimeSetBtn.setAlpha((float) 1);
			timeToSleepTimeSetText.setAlpha((float) 1);
		} else {
			timeToSleepTimeSetBtn.setEnabled(false);
			timeToSleepTimeSetBtn.setAlpha((float) 0.2);
			timeToSleepTimeSetText.setAlpha((float) 0.2);
		}

	}

	private void updateLowBatteryWarningThresholdDisplay() {
		if(lowBatteryWarningThreshold==0){
			((TextView) findViewById(R.id.low_battery_warning_threshold_text)).setText(
					getString(R.string.low_battery_warning_threshold_text) + " -- %");
		} else {
			((TextView) findViewById(R.id.low_battery_warning_threshold_text)).setText(
					getString(R.string.low_battery_warning_threshold_text) + " " + lowBatteryWarningThreshold + " %");
		}	
	}

	private void updateTimeToSleepTimerDisplay() {
		SimpleDateFormat formatter = new SimpleDateFormat("mm.ss");
		if(timeToSleepDuration==0){
			((TextView) findViewById(R.id.time_to_sleep_timeset_text)).setText(
					getString(R.string.time_to_sleep_timeset_text_part1) 
					+ " --.-- " 
					+ getString(R.string.time_to_sleep_timeset_text_part2));
		} else {
			((TextView) findViewById(R.id.time_to_sleep_timeset_text)).setText(
					getString(R.string.time_to_sleep_timeset_text_part1) 
					+ " " 
					+  formatter.format(new Date(timeToSleepDuration)) 
					+ " " + getString(R.string.time_to_sleep_timeset_text_part2));
		}	
	}

	private void showLowBatteryWarningPopup() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final View view = SettingsActivity.this.getLayoutInflater().inflate(R.layout.threshold_setting_dialog, null);
		builder.setTitle("Threshold" +" (" + lowBatteryWarningThreshold + "%)")
		.setView(view)
		.setCancelable(false)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				lowBatteryWarningThreshold = ((SeekBar)view.findViewById(R.id.seekbar)).getProgress()*10;
				updateLowBatteryWarningThresholdDisplay();
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				//do things
			}
		});
		final AlertDialog alert = builder.create();
		alert.show();	

		SeekBar seekBar = (SeekBar)alert.findViewById(R.id.seekbar);
		seekBar.setMax(10);
		seekBar.setProgress(ph.getLowBatteryWarningThreshold()/10);
		seekBar.setSecondaryProgress(ph.getLowBatteryWarningThreshold()/10);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ 
			@Override 
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { 
				alert.setTitle("Threshold (" + String.valueOf(progress*10) + "%)");
			} 
			@Override 
			public void onStartTrackingTouch(SeekBar seekBar) { 
			} 
			@Override 
			public void onStopTrackingTouch(SeekBar seekBar) { 
			} 
		}); 

	}

	private void showAutoShutDownPopup() {

		final SimpleDateFormat formatter = new SimpleDateFormat("mm.ss");
		//Date dateStr = formatter.parse(strDate);
		//String formattedDate = formatter.format(dateStr);

		final List<String> possibleTimers = new ArrayList<String>();
		possibleTimers.add("00.00");
		possibleTimers.add("00.15");
		possibleTimers.add("00.30");
		possibleTimers.add("00.45");
		possibleTimers.add("01.00");
		possibleTimers.add("01.15");
		possibleTimers.add("01.30");
		possibleTimers.add("01.45");
		possibleTimers.add("02.00");
		possibleTimers.add("02.15");
		possibleTimers.add("02.30");
		possibleTimers.add("02.45");
		possibleTimers.add("03.00");

		// Current saved setting
		Date currentSetting = new Date(ph.getTimeToSleepAsMs());
		String formattedDate = formatter.format(currentSetting);
		int pos = possibleTimers.indexOf(formattedDate);

		// Current session setting
		Date currentSessionSetting = new Date(timeToSleepDuration);
		String formattedSessionDate = formatter.format(currentSessionSetting);
		int posSession = possibleTimers.indexOf(formattedSessionDate);


		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final View view = SettingsActivity.this.getLayoutInflater().inflate(R.layout.time_to_sleep_setting_dialog, null);
		builder.setTitle("Time to sleep " +" (" + formattedSessionDate + " mins)")
		.setView(view)
		.setCancelable(false)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				int pos = ((SeekBar)view.findViewById(R.id.seekbar)).getProgress();
				String dateStr = possibleTimers.get(pos);
				Date date = null;
				try {
					date = formatter.parse(dateStr);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Calendar calendar = GregorianCalendar.getInstance();
				calendar.setTime(date); 
				long minute = calendar.get(Calendar.MINUTE);
				long seconds = calendar.get(Calendar.SECOND);
				timeToSleepDuration = minute*60000+seconds*1000;
				updateTimeToSleepTimerDisplay();
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				//do nothing
			}
		});
		final AlertDialog alert = builder.create();
		alert.show();	

		SeekBar seekBar = (SeekBar)alert.findViewById(R.id.seekbar);
		seekBar.setMax(possibleTimers.size()-1);
		seekBar.setProgress(posSession);
		seekBar.setSecondaryProgress(pos);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ 
			@Override 
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { 
				alert.setTitle("Time to sleep (" + possibleTimers.get(progress) + " mins)");
			} 
			@Override 
			public void onStartTrackingTouch(SeekBar seekBar) { 
			} 
			@Override 
			public void onStopTrackingTouch(SeekBar seekBar) { 
			} 
		}); 

	}




}
