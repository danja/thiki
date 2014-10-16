package org.thoughtcatchers.thiki;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.thoughtcatchers.thiki.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EditPage extends Activity {

	public final static String TITLE_KEY = "EditPage.Title";
	private final static int SAVE_INTERVAL_SECONDS = 10;

	private ThikiActivityHelper  activityHelper;
	private String pageTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_page);
		
		activityHelper = new ThikiActivityHelper(this);

		// try to populate from saved state
		if (!populate(savedInstanceState)) {
			// no saved state, populate from intent extras
			Intent i = getIntent();
			if (i != null) {
				populate(i.getExtras());
			}
		}

		Button saveButton = (Button) findViewById(R.id.save);
		saveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				okResult();
			}
		});

	}

	@Override
	protected void onStart() {
		super.onStart();
		startTimer();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		autosaveTimer.cancel();
	}
	
	Timer autosaveTimer;
	/*
	 * start timer that saves the note periodically
	 */
	private void startTimer() {
		autosaveTimer = new Timer();
		autosaveTimer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				save();
			}
		}, 1000 * SAVE_INTERVAL_SECONDS, 1000 * SAVE_INTERVAL_SECONDS);
	}

	private boolean populate(Bundle b) {
		if (b == null) {
			return false;
		}

		pageTitle = b.getString(TITLE_KEY);
		if (pageTitle == null || pageTitle.length() == 0) {
			return false;
		}
		return populate();
	}

	private boolean populate() {
		String pageBody;

		WikiPage page = activityHelper.getPagesFiles().fetchByName(pageTitle);
		try {
			pageBody = page.getBody();
		} catch (IOException e) {
			Toast.makeText(
					this,
					getText(R.string.page_error) + "\n"
							+ e.getLocalizedMessage(), Toast.LENGTH_LONG);
			Log.w("EditPage", e.getMessage());
			return false;
		}

		TextView title = (TextView) findViewById(R.id.page_title);
		EditText body = (EditText) findViewById(R.id.page_body);

		title.setText(pageTitle);
		if (pageBody != null) {
			body.setText(pageBody);
		}

		return true;
	}

	private void okResult() {
		save();
		setResult(RESULT_OK);
		finish();
	}
	
	@Override
	public void onBackPressed() {
		okResult();
	}

	private synchronized void save() {
		EditText body = (EditText) findViewById(R.id.page_body);

		try {
			activityHelper.getPagesFiles().savePage(pageTitle, body.getText().toString());
		} catch (IOException e) {
			Toast.makeText(
					this,
					getText(R.string.save_error) + "\n"
							+ e.getLocalizedMessage(), Toast.LENGTH_LONG);
			Log.w("EditPage", e.getMessage());
		}
	}
	

	@Override
	protected void onPause() {
		save();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		populate();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		save();
		outState.putString(TITLE_KEY, pageTitle);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (!populate(savedInstanceState)) {
			finish();
		}
	}
	
	

}
