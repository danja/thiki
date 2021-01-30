package org.thoughtcatchers.thiki;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class ThikiActivityHelper {

	private PagesFiles pagesFiles;
	private Context context;
	private Handler toastHandler;

	public ThikiActivityHelper(Context ctx) {
		context = ctx;
		
		toastHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				CharSequence cs = (CharSequence) msg.obj;
				Toast.makeText(context, cs, Toast.LENGTH_LONG).show();
			}
		};

		try {
			pagesFiles = new PagesFiles(ctx);
		} catch (IOException e) {
			showToast("Error initializing Thiki Wiki", e);
			Log.w("ThikiActivityHelper", "Error initializing "+e.getMessage());
		}

	}

	public boolean isInitialized() {
		return pagesFiles != null;
	}

	public PagesFiles getPagesFiles() {
		return pagesFiles;
	}

	public void showToast(int r) {
		showToast(context.getText(r));
	}
	
	@SuppressWarnings("unchecked")
	public <T extends View> T find(int id) {
		return (T) ((Activity)context).findViewById(id);
	}

	public void showToast(CharSequence message) {
		Message msg = new Message();
		msg.obj = message;
		toastHandler.sendMessage(msg);
	}

	public void showToast(CharSequence message, Exception ex) {
		if (ex.getLocalizedMessage() == null) {
			showToast(message);
		}
		else {
			showToast(message + " \n" + ex.getLocalizedMessage());
		}
	}

	public Context getContext() {
		return context;
	}
}
