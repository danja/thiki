package it.hyperdata.thiki.sync;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import it.hyperdata.thiki.Constants;
import it.hyperdata.thiki.ThikiActivityHelper;
import it.hyperdata.thiki.WikiPage;
import it.hyperdata.thiki.sync.dropbox.DropboxWrapper;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;

public class SyncDropbox extends SyncBase implements Sync {

	
	private DropboxWrapper dropboxApi;
	
	public SyncDropbox(ThikiActivityHelper helper, DropboxWrapper api) {
		dropboxApi = api;
	
	}

	/* (non-Javadoc)
	 * @see it.hyperdata.thiki.sync.Sync#perform()
	 */
	public boolean perform() throws IOException {
		Entry syncDir = null;
		try {
			syncDir = dropboxApi.getOrCreateFolder(Constants.DROPBOX_FOLDER);
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // SyncFileInfo.DROPBOX_FOLDER

		showProgress(1, 0);

		// create a list of all local files
		Map<String, SyncFileInfo> files = new HashMap<String, SyncFileInfo>();
		for (WikiPage p : activityHelper.getPagesFiles().fetchAll()) {
			SyncFileInfo fileInfo = new SyncFileInfo(p.getFile(), syncMetadata,
					super.getLocalDir());
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
					File newFile = new File(super.getLocalDir(), dirEntry.fileName());
					fileInfo = new SyncFileInfo(newFile, syncMetadata, super.getLocalDir());
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

				Entry syncedFile = null;
				try {
					syncedFile = dropboxApi.putFile(syncDir.path, fileInfo.getFile());
				} catch (DropboxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// update local sync info
				fileInfo.updatedRemote(syncedFile);
				syncedSomething = true;

			} else if (fileInfo.needsDownload()) {

				try {
					dropboxApi.getFile(syncDir.path, fileInfo.getFile());
				} catch (DropboxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				fileInfo.updatedLocal();

				syncedSomething = true;

			}
		}

		updateSyncInfo(files);
		return syncedSomething;
	}
}