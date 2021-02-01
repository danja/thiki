package org.thoughtcatchers.thiki;

import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.thoughtcatchers.thiki.sync.SyncDropbox;
import org.thoughtcatchers.thiki.sync.SyncPrefs;
import org.thoughtcatchers.thiki.sync.SyncRunner;
import org.thoughtcatchers.thiki.sync.dropbox.DropboxAuthentication;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.thoughtcatchers.thiki.R;

public class ViewPageActivity extends Activity {

	public static final String TAG = "ViewPageActivity";

	public static final String TITLE_KEY = "ViewPageActivity.Title";
	public static final String SCROLLPOS_KEY = "ViewPageActivity.ScrollPos";

	private static final int EDIT_PAGE = 0;
	private static final int LIST_PAGES = 1;
	private static final int LOGIN = 2;

	private ThikiActivityHelper activityHelper;
	private int activeWebview = 0;
	private boolean hasCustomTitle;
	private boolean ignoreBrowserUpdate;
	private String customWindowTitle;
	private Stack<ViewPageCommand> history;
	
	private boolean inRefresh = false;
	// private boolean notABadTimeForASync = false;
	private SyncPrefs syncPrefs;
	// private boolean syncRequestedByUser = false;
	// private Handler refreshHandler;
	private Lock refreshLock = new ReentrantLock();
	// private DropboxAuthentication dropboxAuthentication;
	private SyncRunner syncRunner  = new SyncRunner(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("ViewPageActivity", "onCreate1");
		super.onCreate(savedInstanceState);
		Log.d("ViewPageActivity syncRunner", " "+syncRunner);
		Log.d("ViewPageActivity", "onCreate2");
		activityHelper = new ThikiActivityHelper(this);
		Log.d("ViewPageActivity activityHelper", " "+activityHelper);
		syncRunner.setActivityHelper(activityHelper);
		if (!activityHelper.isInitialized()) {
			finish();
			return;
		}
		Log.d("ViewPageActivity", "onCreate3");
		 syncPrefs = new SyncPrefs(this);
			Log.d("ViewPageActivity syncPrefs", " "+syncPrefs);
		syncRunner.setSyncPrefs(syncPrefs);

		hasCustomTitle = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		Log.d("ViewPageActivity", "line 84");

		setContentView(R.layout.view_page);

		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_titlebar);

		setTitleCustom(getText(R.string.app_name).toString());

		initializeLocals();
		initializeButtons();
		initializeBrowser();

		tryToPopulateFromSavedState(savedInstanceState);
		startSyncThread();
	}

	private void initializeButtons() {

		setClickListenerOn(R.id.button_back, new View.OnClickListener() {
			public void onClick(View v) {
				onBackPressed();
			}
		});

		setClickListenerOn(R.id.button_edit, new View.OnClickListener() {
			public void onClick(View v) {
				editPage();
			}
		});

		setClickListenerOn(R.id.button_home, new View.OnClickListener() {
			public void onClick(View v) {
				loadUrlWithHistory(WikiPage.DEFAULT_PAGE);
			}
		});

		setClickListenerOn(R.id.button_sync, new View.OnClickListener() {
			public void onClick(View v) {
			//	syncRequestedByUser = true;
				syncRunner.setNotABadTimeForASync(true);
				syncRunner.setSyncRequestedByUser(true);
			}
		});
	}

	private void setClickListenerOn(int buttonId, View.OnClickListener listener) {
		Button b = activityHelper.find(buttonId);
		if (b == null)
			return;

		b.setOnClickListener(listener);
	}

	private void setTitleCustom(String titleText) {
		if (hasCustomTitle) {
			TextView title = activityHelper.find(R.id.title);
			title.setText(titleText);
		} else {
			customWindowTitle = titleText;
			setTitle(titleText);
		}
	}

	public void showStatus(String message, int progressPct) {
		if (hasCustomTitle) {
			TextView titleMessage = activityHelper.find(R.id.title_message);
			if (message.length() > 20) {
				message = message.substring(0, 19);
			}
			titleMessage.setText(message);
			titleMessage.setVisibility(TextView.VISIBLE);

			ProgressBar titleProgressBar = (ProgressBar) findViewById(R.id.title_progressbar);
			titleProgressBar.setVisibility(ProgressBar.VISIBLE);
			titleProgressBar.setMax(100);
			titleProgressBar.setProgress(progressPct);
		} else {
			setTitle(customWindowTitle + " " + message + " " + progressPct
					+ "%");
		}
	}

	public void hideStatus() {
		if (hasCustomTitle) {
			TextView titleMessage = activityHelper.find(R.id.title_message);
			titleMessage.setText("");
			ProgressBar titleProgressBar = activityHelper.find(R.id.title_progressbar);
			titleProgressBar.setVisibility(ProgressBar.GONE);
			titleMessage.setVisibility(TextView.GONE);
		} else {
			setTitle(customWindowTitle);
		}
	}

	private void initializeLocals() {
		history = new Stack<ViewPageCommand>();

	
	}

	private WebView getWebView(int which) {
		if (which == 0) {
			return activityHelper.find(R.id.webview0);
		} else {
			return activityHelper.find(R.id.webview1);
		}
	}

	private WebView getWebView() {
		return getWebView(activeWebview);
	}

	private WebView getInvisibleWebView() {
		int otherView = activeWebview == 0 ? 1 : 0;
		return getWebView(otherView);
	}

	private void initializeBrowser() {
		initializeBrowser(0);
		initializeBrowser(1);
	}

	private void initializeBrowser(int which) {
		final WebView wv = getWebView(which);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setBuiltInZoomControls(true);

		wv.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView webview, String url) {
				return navigateTo(url);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				if (ignoreBrowserUpdate) {
					ignoreBrowserUpdate = false;
				} else {
					restoreScrollPosAndShowBrowser();
				}
			}
		});

		wv.addJavascriptInterface(new JavascriptInterop(
				new JavascriptInterop.Listener() {
					public void sendMessage(String command, String parameters) {
						if (command.equalsIgnoreCase("checkbox")) {
							int whichOne = Integer.parseInt(parameters);
							toggleCheck(whichOne);
						}
					}
				}), "ThikiInterop");
	}

	private void tryToPopulateFromSavedState(Bundle savedInstanceState) {
		boolean populated = populate(savedInstanceState);
		if (!populated) {
			// populate from other activity's intent
			Intent i = getIntent();
			if (i != null) {
				Bundle b = i.getExtras();
				populated = populate(b);
			}
		}

		if (!populated) {
			// do default (=Home)
			showOrRefreshCurrentPage();
		}
	}

	private void startSyncThread() {
		// show sync progress in title bar
		Log.d("ViewPageActivitystartSyncThread", "startSyncThread");
		Log.d("ViewPageActivity.syncPrefs = ", " "+syncPrefs);
		Log.d("ViewPageActivity.syncRunner = ", " "+syncRunner);
		// do the initial sync?
		if (syncPrefs.getOnStartup()) {
			syncRunner.setNotABadTimeForASync(true);
		}

	
		// syncRunner.setNotABadTimeForASync(notABadTimeForASync);
		// start a synchronization thread
		Thread t = new Thread(syncRunner);
				/*
				new Runnable() {
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
							dropboxAuthentication = new DropboxAuthentication(ViewPageActivity.this);
						}

						if (!dropboxAuthentication.isAuthenticated()) {
							if (syncRequestedByUser) {
								syncRequestedByUser = false;
								activityHelper.showToast(getText(R.string.provide_credentials));
							}
							continue;
						}
						syncRequestedByUser = false;

						SyncDropbox sync = new SyncDropbox(activityHelper, dropboxAuthentication
								.getAPI());
						sync.setStatusHandler(syncProgressHandler);
						if (sync.perform()) {
							// -there was a change-
							refreshHandler.sendEmptyMessage(0);
						}
						hideStatusHandler.sendEmptyMessage(0);

					} catch (Exception e) {
						// show sync error
						activityHelper.showToast(getText(R.string.syncError), e);
						Log.w("ViewPageActivity", e.toString());
					} finally {
						notABadTimeForASync = false;
					}
				}
			}
		}
		);
		*/
		t.start();
	}

	private boolean populate(Bundle b) {
		if (b == null) {
			return false;
		}

		String pageTitle = b.getString(TITLE_KEY);
		if (pageTitle == null || pageTitle.length() == 0) {
			return false;
		}

		double scrollPos = b.getDouble(SCROLLPOS_KEY);

		history.push(new ViewPageCommand(pageTitle, scrollPos));

		showOrRefreshCurrentPage();
		return true;
	}

	private ViewPageCommand currentPage() {
		if (history.empty()) {
			history.add(new ViewPageCommand(WikiPage.DEFAULT_PAGE));
		}

		return history.peek();
	}

	private void loadUrlWithHistory(String pageName) {
		if (currentPage().PageName != pageName) {
			rememberScrollPos(); // for current page that very soon is not the
									// current page any longer
			history.push(new ViewPageCommand(pageName));
		}
		showOrRefreshCurrentPage();
	}

	@SuppressWarnings("unused")
	private void log(String message) {
		System.out.println(message);
	}

	

	public void showOrRefreshCurrentPage() {
		// log("Requesting lock...");
		refreshLock.lock();
		// log("in lock");
		if (inRefresh) {
			// log("a refresh action is already taking place, returning.");
			return;
		}
		inRefresh = true;
		refreshLock.unlock();

		final ViewPageCommand currentPage = currentPage();
		setTitleCustom(currentPage.PageName);
		getWebView().setEnabled(false);

		// do time-consuming things asynchronously
		
		 // sticking back in main threa
		//new Thread(new Runnable() {
		//	public void run() {
				WikiPage page = activityHelper.getPagesFiles().fetchByName(
						currentPage.PageName);

				String htmlBody = "";
				try {
					htmlBody = page.getHtmlBody();
				} catch (IOException e) {
					activityHelper.showToast(getText(R.string.page_error), e);
					Log.w("ViewPageActivity", e.getMessage());
					return;
				}

				htmlBody = htmlBody.replaceAll("thikifile\\:",
						"content://org.thoughtcatchers.thiki.localfile/");

				// log("Loading html in view " + activeWebview);
				
				// danny - WTF?
				 getInvisibleWebView().loadDataWithBaseURL("fake://i/will/smack/the/engineer/behind/this/scheme", 
						htmlBody, "text/html",
						FileIO.ENCODING, "");
				
				// will be made visible in the onpageload eventhandler
				 
				 // sticking back in main thread
		//	}
	//	}).start();
	}

	public void rememberScrollPos() {
		final WebView wv = getWebView();
		currentPage().setScrollPos(wv.getContentHeight(), wv.getScrollY());
	}

	private void restoreScrollPosAndShowBrowser() {
		final WebView invisibleWv = getInvisibleWebView();
		final WebView visibleWv = getWebView();

		final int pos = currentPage().getScrollPos(
				invisibleWv.getContentHeight());

		// the code to set scroll pos and show browser, to be executed later in
		// this method
		final Handler restoreScrollPosHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				try {
					// get position again, the previous pos was not reliable
					int posReloaded = currentPage().getScrollPos(
							invisibleWv.getContentHeight());

					invisibleWv.scrollTo(0, posReloaded);

					// log("set invisible: " + activeWebview);
					invisibleWv.setVisibility(View.VISIBLE);
					invisibleWv.setEnabled(true);
					visibleWv.setVisibility(View.GONE);
					ignoreBrowserUpdate = true;
					visibleWv.loadData("", "text/plain", FileIO.ENCODING);
					activeWebview = activeWebview == 0 ? 1 : 0;
					// log("set visible: " + activeWebview);
				} finally {
					refreshLock.lock();
					inRefresh = false;
					refreshLock.unlock();
					// log("released lock");
				}
			}
		};

		if (pos > 0) {
			// the onpageloaded event seems to be triggered to early to set the
			// scrollposition here. So do this after 100 ms from now.
			new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(100);
					} catch (InterruptedException ex) {
						Log.w("ViewPageActivity", ex.getMessage());
					}
					restoreScrollPosHandler.sendEmptyMessage(0);
				}
			}).start();
		} else {
			// no need to delay the code, execute right now.
			restoreScrollPosHandler.sendEmptyMessage(0);
		}
	}

	private void toggleCheck(final int whichOne) {

		// do asynchronously, because it is time-consuming
		final String page = currentPage().PageName;
		new Thread(new Runnable() {
			public void run() {
				try {
					activityHelper.getPagesFiles().toggleCheckbox(page, whichOne);

					// do not refresh the browser here, because this looks very
					// slow
					// instead, the browser will assume a way to show the change
					// itself.

					if (syncPrefs.getAfterEdit()) {
						syncRunner.setNotABadTimeForASync(true);
					}
				} catch (IOException e) {
					activityHelper.showToast(getText(R.string.save_error), e);
					Log.w("ViewPageActivity", e.getMessage());
				}
			}
		}).start();
	}

	@Override
	public void onBackPressed() {
		if (currentPage().PageName == WikiPage.DEFAULT_PAGE) {
			// (home page is shown)
			super.onBackPressed();
			return;
		}

		history.pop();
		showOrRefreshCurrentPage();
	}

	private void editPage() {
		Intent i = new Intent(this, EditPageActivity.class);
		Bundle b = new Bundle();

		b.putString(EditPageActivity.TITLE_KEY, currentPage().PageName);
		i.putExtras(b);

		rememberScrollPos();

		try {
			startActivityForResult(i, EDIT_PAGE);
		} catch (RuntimeException e) {
			Log.e("ViewPageActivity", e.getMessage());
			System.out.println(e.toString());
			throw e;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		switch (requestCode) {
		case EDIT_PAGE:
			showOrRefreshCurrentPage();
			if (syncPrefs.getAfterEdit()) {
				syncRunner.setNotABadTimeForASync(true);
			}
			return;

		case LIST_PAGES:
			if (intent != null) {
				loadUrlWithHistory(intent.getStringExtra(TITLE_KEY));
				return;
			}
			break;

		case LOGIN:
			if (syncRunner.getDropboxAuthentication() != null) {
				syncRunner.getDropboxAuthentication().reInitialize();
			}
			syncRunner.setNotABadTimeForASync(true);
			break;

		}

		super.onActivityResult(requestCode, resultCode, intent);

	}

	private static final Pattern wikiLinkPattern = Pattern.compile("thiki:(.*)");

	private boolean navigateTo(String url) {

		Matcher m = wikiLinkPattern.matcher(url);
		if (m.matches()) {
			// UrlQuerySanitizer().unescape() works incorrect 
			// see http://code.google.com/p/android/issues/detail?id=14437 for details
//			String page = new UrlQuerySanitizer().unescape(m.group(1));
			String page = android.net.Uri.decode(m.group(1));

			loadUrlWithHistory(page);
			return true;
		}

		// normal url, leave it to the system in an external activity
		// replace replaced links
		url = url.replaceAll("content://org.thoughtcatchers.thiki.localfile/",
				"file://" + activityHelper.getPagesFiles().Dir().getAbsolutePath() + "/");

		boolean isFile = url.startsWith("file://");

		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);

		if (isFile) {
			// find out the mimetype in file url's (because this is case
			// sensitive,
			// the default mechanism will not always behave correctly)
			String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
			String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
					ext.toLowerCase());
			intent.setType(type);
		}

		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			activityHelper.showToast(R.string.unknownMimetype);
			Log.w("ViewPageActivity", e.getMessage());
		}
		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		ViewPageCommand currentPage = currentPage();
		outState.putString(TITLE_KEY, currentPage.PageName);
		outState.putDouble(SCROLLPOS_KEY, currentPage.RelativeScrollPosition);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.viewpage_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.about:
			loadUrlWithHistory(WikiPage.ABOUT_PAGE);
			return true;

		case R.id.help:
			navigateTo("http://thoughtcatchers.org/thiki");
			return true;

		case R.id.list_pages:
			listPages();
			return true;

		case R.id.quit:
			finish();
			return true;

		case R.id.dropbox_login:
			showDropboxLoginCredentials();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void listPages() {
		Intent i = new Intent(this, PagesListActivity.class);
		startActivityForResult(i, LIST_PAGES);
	}

	private void showDropboxLoginCredentials() {
		Intent i = new Intent(this,
				org.thoughtcatchers.thiki.sync.EditSyncPreferencesActivity.class);
		startActivityForResult(i, LOGIN);
	}
}
