/**
 * 
 */
package org.thoughtcatchers.thiki;

import org.thoughtcatchers.thiki.dropbox.DropboxAuthentication;
import org.thoughtcatchers.thiki.dropbox.Sync;
import org.thoughtcatchers.thiki.dropbox.SyncPrefs;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author danny
 *
 */
public class SyncRunner implements Runnable {

	private boolean notABadTimeForASync = false;
	private SyncPrefs syncPrefs;
	private DropboxAuthentication dropboxAuthentication;
	private ViewPageActivity viewPageActivity;
	private boolean syncRequestedByUser;
	private ThikiActivityHelper activityHelper;
	final Handler syncProgressHandler;
	private Handler refreshHandler;
	final Handler hideStatusHandler;

	public SyncRunner(final ViewPageActivity viewPageActivity) {
		this.viewPageActivity = viewPageActivity;
		syncProgressHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String message = (String) msg.obj;
				int pct = msg.arg1;
				viewPageActivity.showStatus(message, pct);
			}
		};
		hideStatusHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				viewPageActivity.hideStatus();
			}
		};
		refreshHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				viewPageActivity.rememberScrollPos();
				viewPageActivity.showOrRefreshCurrentPage();
			}
		};
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (true) {
			try {
				// sleep in batches of 1000 milliseconds, so it is
				// easier to interrupt by the "Not A Bad Time" trigger
				int seconds = 0;
				while (seconds < syncPrefs.getIntervalMinutes() * 60) {
					Thread.sleep(1000);

					// can loop be broken by the timer?
					if (syncPrefs.getPeriodically()) {
						seconds++;
					}

					if (notABadTimeForASync) {
						break;
					}
				}

				if (dropboxAuthentication == null) {
					dropboxAuthentication = new DropboxAuthentication(viewPageActivity); // is ok??
				}

				if (!dropboxAuthentication.isAuthenticated()) {
					if (syncRequestedByUser) {
						syncRequestedByUser = false;
						activityHelper.showToast(viewPageActivity.getText(R.string.provide_credentials));
					}
					continue;
				}
				syncRequestedByUser = false;

				Sync sync = new Sync(activityHelper, dropboxAuthentication
						.getAPI());
				sync.setStatusHandler(syncProgressHandler);
				if (sync.perform()) {
					// -there was a change-
					refreshHandler.sendEmptyMessage(0);
				}
				hideStatusHandler.sendEmptyMessage(0);

			} catch (Exception e) {
				// show sync error
				activityHelper.showToast(viewPageActivity.getText(R.string.syncError), e);
				Log.w("ViewPageActivity", e.toString());
			} finally {
				notABadTimeForASync = false;
			}
		}
	}

	public void setNotABadTimeForASync(boolean notABadTimeForASync) {
		this.notABadTimeForASync  = notABadTimeForASync;
		
	}

	public void setSyncPrefs(SyncPrefs syncPrefs) {
		this.syncPrefs = syncPrefs;
	}

	public DropboxAuthentication getDropboxAuthentication() {
		return dropboxAuthentication;
	}

	public void setSyncRequestedByUser(boolean syncRequestedByUser) {
		this.syncRequestedByUser = syncRequestedByUser;
		
	}

	public void setActivityHelper(ThikiActivityHelper thikiActivityHelper) {
		this.activityHelper = thikiActivityHelper;
		
	}
}
