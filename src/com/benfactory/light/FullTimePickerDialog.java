package com.benfactory.light;

import java.util.Calendar;

import com.benfactory.light.TimePicker.OnTimeChangedListener;
import com.benfactory.light.TimePicker.TimePickerType;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.content.DialogInterface.OnClickListener;

public class FullTimePickerDialog extends AlertDialog implements OnClickListener, OnTimeChangedListener {

	/**
	 * The callback interface used to indicate the user is done filling in
	 * the time (they clicked on the 'Set' button).
	 */
	public interface OnTimeSetListener {

		/**
		 * @param view The view associated with this listener.
		 * @param hourOfDay The hour that was set.
		 * @param minute The minute that was set.
		 */
		void onTimeSet(TimePicker view, int hourOfDay, int minute, int seconds);
	}

	private static final String HOUR = "hour";
	private static final String MINUTE = "minute";
	private static final String SECONDS = "seconds";
	private static final String IS_24_HOUR = "is24hour";

	private final TimePicker mTimePicker;
	private final OnTimeSetListener mCallback;
	private final Calendar mCalendar;
	@SuppressWarnings("unused")
	private Calendar mDefaultCalendar;
	@SuppressWarnings("unused")
	private final java.text.DateFormat mDateFormat;

	int mInitialHourOfDay;
	int mInitialMinute;
	int mInitialSeconds;    
	boolean mIs24HourView;

	/**
	 * @param context Parent.
	 * @param callBack How parent is notified.
	 * @param hourOfDay The initial hour.
	 * @param minute The initial minute.
	 * @param is24HourView Whether this is a 24 hour view, or AM/PM.
	 */
	public FullTimePickerDialog(Context context, OnTimeSetListener callBack,
			Calendar cal, boolean is24HourView, TimePickerType pickerType) {

		this(context, R.style.Theme_Dialog_Alert, callBack, cal, is24HourView, pickerType);
		
	}

	/**
	 * @param context Parent.
	 * @param theme the theme to apply to this dialog
	 * @param callBack How parent is notified.
	 * @param hourOfDay The initial hour.
	 * @param minute The initial minute.
	 * @param is24HourView Whether this is a 24 hour view, or AM/PM.
	 */
	@SuppressWarnings("deprecation")
	public FullTimePickerDialog(Context context,
			int theme,
			OnTimeSetListener callBack,
			Calendar cal,
			boolean is24HourView,
			TimePickerType pickerType
		) {
		super(context, theme);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mCallback = callBack;
		mInitialHourOfDay = cal.get(Calendar.HOUR_OF_DAY);
		mInitialMinute = cal.get(Calendar.MINUTE);
		mInitialSeconds = cal.get(Calendar.SECOND);
		mIs24HourView = is24HourView;
		mDefaultCalendar = cal;

		mDateFormat = DateFormat.getTimeFormat(context);
		mCalendar = Calendar.getInstance();
		updateTitle(mInitialHourOfDay, mInitialMinute, mInitialSeconds);

		setButton(context.getText(R.string.time_set), this);
		setButton2(context.getText(R.string.cancel), (OnClickListener) null);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.time_picker_dialog, null);
		setView(view);
		mTimePicker = (TimePicker) view.findViewById(R.id.timePicker);
		mTimePicker.setPickerType(pickerType);

		// initialize state
		mTimePicker.setCurrentHour(mInitialHourOfDay);
		mTimePicker.setCurrentMinute(mInitialMinute);
		mTimePicker.setCurrentSecond(mInitialSeconds);
		mTimePicker.setIs24HourView(mIs24HourView);
		mTimePicker.setOnTimeChangedListener(this); 
		
		// set title
		this.setTitle(context.getResources().getString(R.string.timeToSleepDialogTitle));
	}

	public void onClick(DialogInterface dialog, int which) {
		if (mCallback != null) {
			mTimePicker.clearFocus();
			mCallback.onTimeSet(mTimePicker, mTimePicker.getCurrentHour(), 
					mTimePicker.getCurrentMinute(), mTimePicker.getCurrentSeconds());
		}
	}

	public void onTimeChanged(TimePicker view, int hourOfDay, int minute, int seconds) {
		updateTitle(hourOfDay, minute, seconds);
	}

	public void updateTime(int hourOfDay, int minutOfHour, int seconds) {
		mTimePicker.setCurrentHour(hourOfDay);
		mTimePicker.setCurrentMinute(minutOfHour);
		mTimePicker.setCurrentSecond(seconds);
	}

	private void updateTitle(int hour, int minute, int seconds) {
		mCalendar.set(Calendar.HOUR_OF_DAY, hour);
		mCalendar.set(Calendar.MINUTE, minute);
		mCalendar.set(Calendar.SECOND, seconds);
	}

	@Override
	public Bundle onSaveInstanceState() {
		Bundle state = super.onSaveInstanceState();
		state.putInt(HOUR, mTimePicker.getCurrentHour());
		state.putInt(MINUTE, mTimePicker.getCurrentMinute());
		state.putInt(SECONDS, mTimePicker.getCurrentSeconds());
		state.putBoolean(IS_24_HOUR, mTimePicker.is24HourView());
		return state;
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		int hour = savedInstanceState.getInt(HOUR);
		int minute = savedInstanceState.getInt(MINUTE);
		int seconds = savedInstanceState.getInt(SECONDS);
		mTimePicker.setCurrentHour(hour);
		mTimePicker.setCurrentMinute(minute);
		mTimePicker.setCurrentSecond(seconds);
		mTimePicker.setIs24HourView(savedInstanceState.getBoolean(IS_24_HOUR));
		mTimePicker.setOnTimeChangedListener(this);
		updateTitle(hour, minute, seconds);
	}


}
