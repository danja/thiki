package org.thoughtcatchers.thiki.sync;

import java.io.IOException;

import android.os.Handler;

import com.dropbox.client2.exception.DropboxException;

public interface Sync {

	public abstract void setStatusHandler(Handler progressUpdate);

	public abstract boolean perform() throws IOException;

}