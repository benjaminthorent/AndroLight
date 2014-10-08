package com.benfactory.light;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.benfactory.light.PreferencesHandler.Language;
import com.benfactory.light.TimePicker.TimePickerType;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

	private PreferencesHandler ph;
	private TextView timeToSleepText;
	private long timeToSleepDuration;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.settings);

		// Init preferences handler
		ph = new PreferencesHandler(getApplicationContext());
		timeToSleepDuration= ph.getTimeToSleepAsMs();

		final LinearLayout ll = (LinearLayout)findViewById(R.id.time_to_sleep_details);



		// Set Time To Sleep Duration
		final ImageButton setTimeSleepDuration = (ImageButton)findViewById(R.id.edit_btn);
		setTimeSleepDuration.setEnabled(false);
		setTimeSleepDuration.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				displayDurationPicker();
			}
		});

		Switch s = (Switch)findViewById(R.id.time_to_sleep);
		s.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					ll.setAlpha(1);
					setTimeSleepDuration.setEnabled(true);
				} else {
					ll.setAlpha(0.3f);
					setTimeSleepDuration.setEnabled(false);

				}
			}
		});

		// Set time to sleep text description
		timeToSleepText = (TextView)findViewById(R.id.timeToSleepText);
		String timeText = (ph.getTimeToSleepAsText()==null?getResources().getString(R.string.noTimeSet):ph.getTimeToSleepAsText());
		timeToSleepText.setText(getResources().getString(R.string.timeToSleepSuboptionPart1) 
				+ " " + timeText + " " + getResources().getString(R.string.timeToSleepSuboptionPart2));

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

		// Set info buttons
		ImageButton infoButton1 = (ImageButton) findViewById(R.id.info_button_1);
		infoButton1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Builder alert = new AlertDialog.Builder(SettingsActivity.this);
				alert.setTitle(R.string.ligtOnStartupDescription);
				alert.setMessage(R.string.ligtOnStartupDetails);
				alert.setIcon(android.R.drawable.ic_dialog_info);
				alert.setPositiveButton(R.string.OKoption,null);
				alert.show();
			}
		});

		ImageButton infoButton2 = (ImageButton) findViewById(R.id.info_button_2);
		infoButton2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Builder alert = new AlertDialog.Builder(SettingsActivity.this);
				alert.setTitle(R.string.timeToSleepDescription);
				alert.setMessage(R.string.timeToSleepDetails);
				alert.setIcon(android.R.drawable.ic_dialog_info);
				alert.setPositiveButton(R.string.OKoption,null);
				alert.show();
			}
		});

		ImageButton infoButton3 = (ImageButton) findViewById(R.id.info_button_3);
		infoButton3.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Builder alert = new AlertDialog.Builder(SettingsActivity.this);
				alert.setTitle(R.string.languageSelectionDescription);
				alert.setMessage(R.string.languageSelectionDetails);
				alert.setIcon(android.R.drawable.ic_dialog_info);
				alert.setPositiveButton(R.string.OKoption,null);
				alert.show();
			}
		});


		// Update as per current preferences
		((Switch)findViewById(R.id.light_on_start_up)).setChecked(ph.isLightOnStartUp());
		((Switch)findViewById(R.id.time_to_sleep)).setChecked(ph.isTimeToSleepActivated());

		if(ph.isTimeToSleepActivated()){
			ll.setAlpha(1);
			//et.setEnabled(true);
		} else {
			ll.setAlpha(0.4f);
			//et.setEnabled(false);
		}
		((Spinner)findViewById(R.id.language_spinner)).setSelection(ph.getDefaultLanguageIndex());

		// Define action for Save button
		Button saveSettingsButton = (Button) findViewById(R.id.saveButton);
		saveSettingsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if((!((Switch)findViewById(R.id.time_to_sleep)).isChecked())||timeToSleepDuration>0){
					Toast.makeText(getApplicationContext(), R.string.settingsUpdateOK, Toast.LENGTH_SHORT).show();
					// Save preferences
					ph.saveSettings(((Switch)findViewById(R.id.light_on_start_up)).isChecked(), 
							((Switch)findViewById(R.id.time_to_sleep)).isChecked(), 
							timeToSleepDuration, 
							Language.getLocaleStringFromIndex(((Spinner)findViewById(R.id.language_spinner)).getSelectedItemPosition()));
					// End settings activity
					endActivity();
				} else {
					// raise warning message
					Builder alert = new AlertDialog.Builder(SettingsActivity.this);
					alert.setTitle(R.string.configurationIssue);
					alert.setMessage(R.string.configurationIssueDetails);
					alert.setIcon(android.R.drawable.ic_dialog_alert);
					alert.setPositiveButton(R.string.OKoption,null);
					alert.show();
				}
			}
		});

	}

	protected void endActivity(){
		this.finish();
	}

	private void displayDurationPicker(){
		Calendar defaultTimer = Calendar.getInstance();
		long prefTimerLimistMs = ph.getTimeToSleepAsMs();
		long minute = prefTimerLimistMs/60000;
		long seconds = (prefTimerLimistMs - minute*60000)/1000 ;
		defaultTimer.set(Calendar.HOUR_OF_DAY, 0);
		defaultTimer.set(Calendar.MINUTE, (int) minute);
		defaultTimer.set(Calendar.SECOND, (int) seconds);

		FullTimePickerDialog mTimePicker = new FullTimePickerDialog(SettingsActivity.this, new FullTimePickerDialog.OnTimeSetListener() {

			public void onTimeSet(TimePicker view, int hourOfDay, int minute, int seconds) {
				timeToSleepText.setText(getResources().getString(R.string.timeToSleepSuboptionPart1) 
						+ " " + String.format("%02d", minute) + 
						":" + String.format("%02d", seconds) + 
						" " + getResources().getString(R.string.timeToSleepSuboptionPart2));
				timeToSleepDuration = minute*60000+seconds*1000;
			}
		}, defaultTimer, true, TimePickerType.MS15MAX10);
		mTimePicker.show();
	}

}
