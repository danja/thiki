package org.thoughtcatchers.thiki.dropbox;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.dropbox.client2.DropboxAPI.Entry;

public class SyncFileInfo {

	// protected static String DROPBOX_FOLDER = "/Apps/ThoughtCatcher";
	private String filename;
	private JSONObject syncInfo;
	private JSONObject syncMetadata;

	private final static String JSON_NAME = "name";
	
	public SyncFileInfo(File f, JSONObject metadata, File localDir) {
		syncMetadata = metadata;
		filename = f.getName();

		// find the sync info of this file
		try {
			JSONArray files = syncMetadata.getJSONArray("files");
			for (int i = 0; i < files.length(); i++) {
				JSONObject file = files.getJSONObject(i);
				if (file.getString(JSON_NAME).equalsIgnoreCase(filename)) {
					syncInfo = file;
					break;
				}
			}
		} catch (JSONException e) {
			Log.e("SyncFileInfo",e+" on "+filename);
		}

		setLocalFile(new File(localDir, filename));
	}

	private File localFile;
	private long localChangeDate;
	private long localRevisionSyncedDate;
	private String localRevision;
	
	private String remotePath;
	private String remoteRevision;
	
	private final static String LOCAL_REVISION_SYNCED_DATE = "localRevisionSynced";
	private final static String LOCAL_REVISION = "localRevision";

	private void setLocalFile(File value) {
		localFile = value;
		if (value.exists()) {
			localChangeDate = localFile.lastModified();

			if (syncInfo != null) {
				localRevisionSyncedDate = syncInfo.optLong(LOCAL_REVISION_SYNCED_DATE);
				localRevision = syncInfo.optString(LOCAL_REVISION);
			}
		}
	}



	public void setRemoteFile(Entry value) {
		remotePath = value.path;
		remoteRevision = value.rev;
	}

	public JSONObject getJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put(JSON_NAME, filename);
			json.put(LOCAL_REVISION, localRevision);
			json.put(LOCAL_REVISION_SYNCED_DATE, localRevisionSyncedDate);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		return json;
	}

	public String getName() {
		return filename;
	}

	public File getFile() {
		return localFile;
	}

	public String getRemotePath() {
		return remotePath;
	}

	public boolean needsDownload() {
		if (localFile == null || !localFile.exists()) {
			return true;
		}
		if (remotePath == null || remotePath.length() == 0) {
			return false;
		}
		
		return (!remoteRevision.equals(localRevision));
	}

	public boolean needsUpload() {
		if (remotePath == null || remotePath.length() == 0) {
			return true;
		}
		if (localFile == null || !localFile.exists()) {
			return false;
		}

		if (!remoteRevision.equals(localRevision)) {
			return false;
		}
		
		return (localChangeDate > localRevisionSyncedDate);
	}

	public void setLocalVersionTo(String rev) {
		localRevision = rev;
	}

	public void updatedLocal() {
		localFile = new File(localFile.getAbsolutePath());
		localRevision = remoteRevision;
		localChangeDate = localFile.lastModified();
		localRevisionSyncedDate = localChangeDate;
	}
	
	public void updatedRemote(Entry e) {
		setRemoteFile(e);
		updatedLocal();
	}
}

