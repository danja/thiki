package org.thoughtcatchers.thiki.sync.dropbox;

import org.thoughtcatchers.thiki.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class DropboxAuthentication {

	final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;
	
	final static public String ACCOUNT_PREFS_NAME = "Thiki.DropboxAccount";
	final static public String ACCESS_KEY_NAME = "Thiki.Dropbox.Key";
	final static public String ACCESS_SECRET_NAME = "Thiki.Dropbox.Secret";

//	final static public String ACCOUNT_PREFS_NAME = "Ema.DropboxAccount";
//	final static public String ACCESS_KEY_NAME = "Ema.Dropbox.Key";
//	final static public String ACCESS_SECRET_NAME = "Ema.Dropbox.Secret";
	
	private DropboxAPI<AndroidAuthSession> dropboxApiSession;
	private Context context;
	private boolean isAuthenticated;

	public DropboxAuthentication(Context ctx) {
		context = ctx;
		
		initialize();
	}
	
	private void initialize() { 
		AppKeyPair appKeys = new AppKeyPair(Constants.APP_KEY, Constants.APP_SECRET);
		AccessTokenPair access = getStoredKeys();
		
		AndroidAuthSession session;
		if (access != null) {
			session = new AndroidAuthSession(appKeys, ACCESS_TYPE, access);
		} else {
			session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
		}
		
		dropboxApiSession = new DropboxAPI<AndroidAuthSession>(session);
		isAuthenticated = dropboxApiSession.getSession().isLinked();
	}

	public void reInitialize() {
		initialize();
	}
	
	// start authentication; will return to onResume on the context
	public void startAuthentication() {
		dropboxApiSession.getSession().startAuthentication(context);
	}

	public DropboxWrapper getAPI() {
		return new DropboxWrapper(dropboxApiSession);
	}

	private AccessTokenPair getStoredKeys() {
		SharedPreferences prefs = context.getSharedPreferences(
				ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key != null && secret != null) {
			return new AccessTokenPair(key, secret);
		} else {
			return null;
		}
	}

	private void storeKeys(String key, String secret) {
		// Save the access key for later
		SharedPreferences prefs = context.getSharedPreferences(
				ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.putString(ACCESS_KEY_NAME, key);
		edit.putString(ACCESS_SECRET_NAME, secret);
		edit.commit();
	}

	public void logout() {
		clearKeys();
		isAuthenticated = false;
		dropboxApiSession.getSession().unlink();
	}

	private void clearKeys() {
		SharedPreferences prefs = context.getSharedPreferences(
				ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}

	public boolean isAuthenticated() {
		return isAuthenticated;
	}

	public void authenticationFinished() {
		isAuthenticated = false;

		if (dropboxApiSession.getSession().authenticationSuccessful()) {
			try {
				// MANDATORY call to complete auth.
				// Sets the access token on the session
				dropboxApiSession.getSession().finishAuthentication();

				AccessTokenPair tokens = dropboxApiSession.getSession().getAccessTokenPair();

				isAuthenticated = true;
				storeKeys(tokens.key, tokens.secret);

			} catch (IllegalStateException e) {
				isAuthenticated = false;
			}
		}

	}

}
