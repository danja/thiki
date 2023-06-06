package it.hyperdata.thiki.sync;

import java.io.IOException;

import android.os.Handler;

public interface Sync {

	public abstract void setStatusHandler(Handler progressUpdate);

	public abstract boolean perform() throws IOException;

}