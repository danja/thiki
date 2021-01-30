package org.thoughtcatchers.thiki;

public class JavascriptInterop {

	private Listener listener;
	public JavascriptInterop(Listener l) {
		listener= l;
	}
	public void sendMessage(String command, String parameters)  {
		listener.sendMessage(command, parameters);
	}
	
	public interface Listener {
		public void sendMessage(String command, String parameters);
	}
}
