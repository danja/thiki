package org.thoughtcatchers.thiki.dropbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcatchers.thiki.Constants;
import org.thoughtcatchers.thiki.ThikiActivityHelper;
import org.thoughtcatchers.thiki.WikiPage;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;

import org.thoughtcatchers.thiki.R;

public class Sync {

	private final static String METADATA_FILE = "thiki-sync-metadata.json";

	private ThikiActivityHelper activityHelper;
	private File localDir;
	private Handler progressUpdateHandler;
	private DropboxWrapper dropboxApi;
	private JSONObject syncMetadata;

	public Sync(ThikiActivityHelper helper, DropboxWrapper api) {
		dropboxApi = api;
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
					Log.e("Sync", e.getMessage()+" on "+syncMetadataFile.getAbsolutePath());
				}
			}
		}

	}

	public void setStatusHandler(Handler progressUpdate) {
		progressUpdateHandler = progressUpdate;
	}

	public boolean perform() throws IOException, DropboxException {
		Entry syncDir = dropboxApi.getOrCreateFolder(Constants.DROPBOX_FOLDER); // SyncFileInfo.DROPBOX_FOLDER

		showProgress(1, 0);

		// create a list of all local files
		Map<String, SyncFileInfo> files = new HashMap<String, SyncFileInfo>();
		for (WikiPage p : activityHelper.getPagesFiles().fetchAll()) {
			SyncFileInfo fileInfo = new SyncFileInfo(p.getFile(), syncMetadata,
					localDir);
			files.put(fileInfo.getName(), fileInfo);
		}

		if (syncDir.contents != null) {
			// add info for remote files
			for (Entry dirEntry : syncDir.contents) {
				if (dirEntry.isDir) {
					// skip dirs
					continue;
				}

				SyncFileInfo fileInfo;
				if (files.containsKey(dirEntry.fileName())) {
					fileInfo = files.get(dirEntry.fileName());
				} else {
					File newFile = new File(localDir, dirEntry.fileName());
					fileInfo = new SyncFileInfo(newFile, syncMetadata, localDir);
					files.put(fileInfo.getName(), fileInfo);
				}
				fileInfo.setRemoteFile(dirEntry);
			}
		}

		int totalFiles = files.size();
		int count = 0;

		boolean syncedSomething = false;
		// do sync for all files
		for (SyncFileInfo fileInfo : files.values()) {
			count++;
			showProgress(totalFiles, count);

			if (fileInfo.needsUpload()) {

				Entry syncedFile = dropboxApi.putFile(syncDir.path, fileInfo.getFile());

				// update local sync info
				fileInfo.updatedRemote(syncedFile);
				syncedSomething = true;

			} else if (fileInfo.needsDownload()) {

				dropboxApi.getFile(syncDir.path, fileInfo.getFile());

				fileInfo.updatedLocal();

				syncedSomething = true;

			}
		}

		updateSyncInfo(files);
		return syncedSomething;
	}

	private void showProgress(int totalFiles, int count) {
		Message updateMessage = new Message();
		updateMessage.obj = activityHelper.getContext()
				.getText(R.string.synchronizing).toString();
		updateMessage.arg1 = (int) ((((double) count) / ((double) totalFiles)) * 100);
		progressUpdateHandler.sendMessage(updateMessage);
	}

	private void updateSyncInfo(Map<String, SyncFileInfo> files)
			throws IOException {

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
						Log.e("Sync", e.getMessage()+" on "+syncMetadataFile.getAbsolutePath());
					}
				}
			}

		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
}