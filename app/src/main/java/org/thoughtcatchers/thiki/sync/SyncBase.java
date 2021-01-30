package org.thoughtcatchers.thiki.sync;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcatchers.thiki.R;
import org.thoughtcatchers.thiki.ThikiActivityHelper;
import org.thoughtcatchers.thiki.sync.dropbox.DropboxWrapper;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SyncBase {

	protected static final String METADATA_FILE = "thiki-sync-metadata.json";
	protected ThikiActivityHelper activityHelper;
	private Handler progressUpdateHandler;
	protected JSONObject syncMetadata;
	private File localDir;

	public SyncBase(ThikiActivityHelper helper) {
		activityHelper = helper;

		localDir = activityHelper.getPagesFiles().Dir();

		File syncMetadataFile = new File(localDir, METADATA_FILE);

		if (!syncMetadataFile.exists()) {
			syncMetadata = new JSONObject();
			return;
		}

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(syncMetadataFile), 4096);
			char[] buffer = new char[(int) syncMetadataFile.length()];
			br.read(buffer);

			syncMetadata = new JSONObject(new String(buffer));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					Log.e("SyncDropbox", e.getMessage()+" on "+syncMetadataFile.getAbsolutePath());
				}
			}
		}

	}
	
	public File getLocalDir() {
		return localDir;
	}

	public void setLocalDir(File localDir) {
		this.localDir = localDir;
	}

	public SyncBase() {
		super();
	}

	public void setStatusHandler(Handler progressUpdate) {
		progressUpdateHandler = progressUpdate;
	}

	protected void showProgress(int totalFiles, int count) {
		Message updateMessage = new Message();
		updateMessage.obj = activityHelper.getContext()
				.getText(R.string.synchronizing).toString();
		updateMessage.arg1 = (int) ((((double) count) / ((double) totalFiles)) * 100);
		progressUpdateHandler.sendMessage(updateMessage);
	}

	protected void updateSyncInfo(Map<String, SyncFileInfo> files) throws IOException {
	
		try {
			syncMetadata = new JSONObject();
			JSONArray filesArray = new JSONArray();
	
			for (SyncFileInfo file : files.values()) {
				filesArray.put(file.getJSON());
			}
	
			syncMetadata.put("files", filesArray);
	
			File syncMetadataFile = new File(localDir, METADATA_FILE);
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter(syncMetadataFile), 4096);
				bw.write(syncMetadata.toString(2));
			} finally {
				if (bw != null) {
					try {
						bw.close();
					} catch (Exception e) {
						Log.e("SyncDropbox", e.getMessage()+" on "+syncMetadataFile.getAbsolutePath());
					}
				}
			}
	
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

}