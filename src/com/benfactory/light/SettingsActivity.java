package com.benfactory.light;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.benfactory.light.PreferencesHandler.Language;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class SettingsActivity extends Activity {

	private PreferencesHandler ph;
	private TextView timeToSleepText;
	private long timeToSleepDuration;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_settings); 

		// Init preferences handler
		ph = new PreferencesHandler(getApplicationContext());


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
		/*((Switch)findViewById(R.id.time_to_sleep)).setChecked(ph.isTimeToSleepActivated());

		if(ph.isTimeToSleepActivated()){
			ll.setAlpha(1);
			//et.setEnabled(true);
		} else {
			ll.setAlpha(0.4f);
			//et.setEnabled(false);
		}*/
		((Spinner)findViewById(R.id.language_spinner)).setSelection(ph.getDefaultLanguageIndex());
	}
	
	private void saveSettings(){
		if((!((CheckBox)findViewById(R.id.time_to_sleep_checkbox)).isChecked())||timeToSleepDuration>0){
			Toast.makeText(getApplicationContext(), R.string.settingsUpdateOK, Toast.LENGTH_SHORT).show();
			// Save preferences
			ph.saveSettings(((CheckBox)findViewById(R.id.light_on_start_up_checkbox)).isChecked(), 
					((CheckBox)findViewById(R.id.time_to_sleep_checkbox)).isChecked(), 
					timeToSleepDuration, 
					Language.getLocaleStringFromIndex(((Spinner)findViewById(R.id.language_spinner)).getSelectedItemPosition()));
			// End settings activity
			this.finish();
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

}
