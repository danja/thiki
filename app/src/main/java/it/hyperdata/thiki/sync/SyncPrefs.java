package it.hyperdata.thiki.sync;

import android.content.Context;
import android.content.SharedPreferences;

public class SyncPrefs {

	private static final String SHARED_PREFERENCES = "Thiki.SyncPreferences";

	private static final String INTERVAL_MINUTES = "Thiki.Sync.IntervalMinutes";
	private static final int INTERVAL_MINUTES_DEFAULT = 10;

	private static final String PERIODICALLY = "Thiki.Sync.Periodically";
	private static final boolean PERIODICALLY_DEFAULT = true;

	private static final String AFTER_EDIT = "Thiki.Sync.AfterEdit";
	private static final boolean AFTER_EDIT_DEFAULT = true;

	private static final String ON_STARTUP = "Thiki.Sync.OnStartup";
	private static final boolean ON_STARTUP_DEFAULT = true;

	private SharedPreferences preferences;

	public SyncPrefs(Context ctx) {
		preferences = ctx.getSharedPreferences(SHARED_PREFERENCES, 0);
	}

	private void commitSetting(String setting, int value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(setting, value);
		editor.commit();
	}

	private void commitSetting(String setting, boolean value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(setting, value);
		editor.commit();
	}
	
	public int getIntervalMinutes() {
		return preferences.getInt(INTERVAL_MINUTES, INTERVAL_MINUTES_DEFAULT);
	}
	
	public void setIntervalMinutes(int value) {
		commitSetting(INTERVAL_MINUTES, value);
	}

	public boolean getPeriodically() {
		return preferences.getBoolean(PERIODICALLY, PERIODICALLY_DEFAULT);
	}
	public void setPeriodically(boolean value) {
		commitSetting(PERIODICALLY, value);
	}

	public boolean getAfterEdit() {
		return preferences.getBoolean(AFTER_EDIT, AFTER_EDIT_DEFAULT);
	}
	
	public void setAfterEdit(boolean value) {
		commitSetting(AFTER_EDIT, value);
	}

	public boolean getOnStartup() {
		return preferences.getBoolean(ON_STARTUP, ON_STARTUP_DEFAULT);
	}
	public void setOnStartup(boolean value) {
		commitSetting(ON_STARTUP, value);
	}
}
