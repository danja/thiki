package org.thoughtcatchers.thiki.sync;

import org.thoughtcatchers.thiki.ThikiActivityHelper;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

import org.thoughtcatchers.thiki.R;
import org.thoughtcatchers.thiki.sync.dropbox.DropboxAuthentication;

public class EditSyncPreferencesActivity extends Activity {

	private DropboxAuthentication dropboxAuth;
	private ThikiActivityHelper activityHelper;
	private boolean isLoggedIn;
	private SyncPrefs syncPreferences;
	private Button submitButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sync);

		activityHelper = new ThikiActivityHelper(this);
		syncPreferences = new SyncPrefs(this);

		submitButton = activityHelper.find(R.id.login_submit);

		initializeCommonSettings();
		initializeSpinner();
		initializeDropboxControls();
	}

	private void initializeCommonSettings() {
		CheckBox checkBox = activityHelper.find(R.id.after_edit);
		checkBox.setChecked(syncPreferences.getAfterEdit());
		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				syncPreferences.setAfterEdit(isChecked);
			}
		});

		checkBox = activityHelper.find(R.id.when_starting_app);
		checkBox.setChecked(syncPreferences.getOnStartup());
		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				syncPreferences.setOnStartup(isChecked);
			}
		});

		checkBox = activityHelper.find(R.id.periodically);
		checkBox.setChecked(syncPreferences.getPeriodically());
		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				syncPreferences.setPeriodically(isChecked);
			}
		});
	}

	private void initializeDropboxControls() {
		dropboxAuth = new DropboxAuthentication(this);

		submitButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isLoggedIn) {
					//logout
					setLoggedIn(false);
					dropboxAuth.logout();
				} else {
					// Try to log in
					doLogin();
				}
			}
		});

		setLoggedIn(dropboxAuth.isAuthenticated());
	}

	private void initializeSpinner() {
		final Integer[] items = new Integer[] { 1, 2, 5, 10, 15, 20, 30, 45, 60 };
		Spinner spinner = activityHelper.find(R.id.minutes_spinner);
		ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this,
				android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		// find position of setting in items
		final int minutes = syncPreferences.getIntervalMinutes();
		int position = 3; // 10
		for (int ix = 0; ix < items.length; ix++) {
			if (items[ix] == minutes) {
				position = ix;
				break;
			}
		}
		spinner.setSelection(position);

		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				syncPreferences.setIntervalMinutes(items[arg2]);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	private void doLogin() {
		dropboxAuth.startAuthentication();
	}

	protected void onResume() {
		super.onResume();

		dropboxAuth.authenticationFinished();
		setLoggedIn(dropboxAuth.isAuthenticated());
	}

	public void setLoggedIn(boolean loggedIn) {
		isLoggedIn = loggedIn;
		if (loggedIn) {
			submitButton.setText(getText(R.string.do_dropbox_logout));
		} else {
			submitButton.setText(getText(R.string.do_dropbox_login));
		}
	}

	public void showToast(String msg) {
		Toast message = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		message.show();
	}

}