package com.benfactory.light;

import android.os.CountDownTimer;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CustomCountDownTimer extends CountDownTimer{
	
	private ProgressBar progressBar;
	private TextView remainingTime;
	private long timerPeriod;
	private LightActivity lightActivity;

	public CustomCountDownTimer(long millisInFuture, long countDownInterval, ProgressBar pb, TextView rt, LightActivity la) {
		super(millisInFuture, countDownInterval);
		progressBar = pb;
		remainingTime = rt;
		timerPeriod = millisInFuture;
		lightActivity = la;
	}

	@Override
	public void onFinish() {
		progressBar.setProgress(0);
		remainingTime.setText("done!");
		lightActivity.turnLightDownOnTimersUp();
	}

	@Override
	public void onTick(long millisUntilFinished) {
		progressBar.setProgress((int)(millisUntilFinished*100/timerPeriod));
		int remainingMinutes = (int)(millisUntilFinished/60000);
		int remainingSecondes = (int)((millisUntilFinished-remainingMinutes*60000)/1000);
		String toDisplay = String.format("%02d", remainingMinutes) + ":" + String.format("%02d", remainingSecondes);
		remainingTime.setText(toDisplay);
	}

}
