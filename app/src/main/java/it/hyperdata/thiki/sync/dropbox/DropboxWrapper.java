package it.hyperdata.thiki.sync.dropbox;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

public class DropboxWrapper {

	private DropboxAPI<AndroidAuthSession> dropboxAPI;

	public DropboxWrapper(DropboxAPI<AndroidAuthSession> api) {
		dropboxAPI = api;
	}

	public Entry getOrCreateFolder(String name) throws DropboxException {
		try {
			Entry retval = dropboxAPI.metadata(name, 0, null, true, null);
			if (retval.isDir) {
				return retval;
			}
		} catch (DropboxException ex) {
			Log.e("DropboxWrapper", ex.toString()+" on "+name);
		}
		return dropboxAPI.createFolder(name);
	}

	public Entry putFile(String path, File file) throws DropboxException {

		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(file));
			path = path + "/" + file.getName();
			return dropboxAPI.putFileOverwrite(path, is,
					file.length(), null);
		} catch (FileNotFoundException e) {
			Log.e("DropboxWrapper", e.getMessage()+" on "+path);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					Log.e("DropboxWrapper", e.getMessage());
				}
		}
		return null;
	}

	public void getFile(String path, File file) throws DropboxException {

		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(file));
			path = path + "/" + file.getName();
			dropboxAPI.getFile(path, null, os, null);
		} catch (FileNotFoundException e) {
			Log.e("DropboxWrapper", e.getMessage()+" on "+path);
			if (os != null) {
				try {
					os.close();
				} catch (IOException e1) {
					Log.e("DropboxWrapper", e1.getMessage());
				}
			}

		}

	}

}
