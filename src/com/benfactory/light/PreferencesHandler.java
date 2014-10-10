package com.benfactory.light;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PreferencesHandler {

	private SharedPreferences sharedPrefs;
	private static final String defaultLanguage = "en";

	public enum Settings {
		LIGHT_ON_START_UP,
		TIME_TO_SLEEP_ACTIVATED,
		TIME_TO_SLEEP_DURATION,
		LOW_BATTERY_WARNING_ACTIVATED,
		LOW_BATTERY_WARNING_THRESHOLD,
		LANGUAGE_LOCALE;
	}
	
	public enum Language {
		ENGLISH(R.string.languageEnglish, "en"),
		FRENCH(R.string.languageFrench, "fr");
		
		private String stringFileFolder;
		private int ressourceID;
		
		Language(int ri, String sff){
			stringFileFolder = sff;
			ressourceID = ri;
		}
		
		public String getStringFileFolder() {
			return stringFileFolder;
		}
		
		public int getRessourceID() {
			return ressourceID;
		}

		public static List<Integer> getSuppportedLanguagesListAsRessources(){
			List<Integer> languagesAsRessources = new ArrayList<Integer>();
			for(Language l: Language.values()){
				languagesAsRessources.add(l.getRessourceID());
			}
			return languagesAsRessources;
		}
		
		public static String getLocaleStringFromIndex(int index){
			return Language.values()[index].getStringFileFolder();
		}
		
		public static int getDefaultItemPositionForLocale(String locale){
			int index = 0;
			for(Language l: Language.values()){
				if(l.getStringFileFolder().equals(locale)){
					return index;
				}
				index++;
			}
			return index;
		}
	
	}

	public PreferencesHandler(Context applicationContext){
		// Get app preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
	}

	public void saveSettings(
			boolean lightOnStartUp, 
			boolean timeToSleepActivated, 
			long timeToSleep, 
			boolean lowBatteryWarningActivated,
			int lowBatteryWarningThreshold,
			String locale) 
	{
		Editor edit = sharedPrefs.edit();
		// Set changes to perform
		edit.putBoolean(Settings.LIGHT_ON_START_UP.toString(), lightOnStartUp);
		edit.putBoolean(Settings.TIME_TO_SLEEP_ACTIVATED.toString(), timeToSleepActivated);
		edit.putLong(Settings.TIME_TO_SLEEP_DURATION.toString(), timeToSleep);
		edit.putBoolean(Settings.LOW_BATTERY_WARNING_ACTIVATED.toString(), lowBatteryWarningActivated);
		edit.putInt(Settings.LOW_BATTERY_WARNING_THRESHOLD.toString(), lowBatteryWarningThreshold);
		edit.putString(Settings.LANGUAGE_LOCALE.toString(), locale);
		// Save preferences
		edit.apply();
	}

	public boolean isLightOnStartUp(){
		return sharedPrefs.getBoolean(Settings.LIGHT_ON_START_UP.toString(), false);
	}
	
	public boolean isTimeToSleepActivated(){
		return sharedPrefs.getBoolean(Settings.TIME_TO_SLEEP_ACTIVATED.toString(), false);
	}
		
	public String getTimeToSleepAsText(){
		long timeToSleep = sharedPrefs.getLong(Settings.TIME_TO_SLEEP_DURATION.toString(), -1);
		if(timeToSleep!=-1){
			long minute = timeToSleep/60000;
			long seconds = (timeToSleep - minute*60000)/1000 ;
			return String.format("%02d", minute) + ":" + String.format("%02d", seconds);
		} else {
			return null;
		}
	}
	
	public long getTimeToSleepAsMs(){
		return sharedPrefs.getLong(Settings.TIME_TO_SLEEP_DURATION.toString(), 0);
	}
	
	public String getLanguage(){
		return sharedPrefs.getString(Settings.LANGUAGE_LOCALE.toString(), defaultLanguage);
	}
	
	public boolean isLowBatteryWarningActivated(){
		return sharedPrefs.getBoolean(Settings.LOW_BATTERY_WARNING_ACTIVATED.toString(), false);
	}
	
	public int getLowBatteryWarningThreshold(){
		return sharedPrefs.getInt(Settings.LOW_BATTERY_WARNING_THRESHOLD.toString(), 0);
	}
	
	public int getDefaultLanguageIndex(){
		return Language.getDefaultItemPositionForLocale(sharedPrefs.getString(Settings.LANGUAGE_LOCALE.toString(), defaultLanguage));
	}

}
