package org.thoughtcatchers.thiki;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.thoughtcatchers.thiki.R;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
// import android.provider.SyncStateContract.Constants;
import android.util.Log;

public class PagesFiles {

	private File filesDir;
	private Context context;

	public PagesFiles(Context ctx) throws IOException {
		context = ctx;

		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			throw new IOException(context.getText(R.string.storage_not_accessible)
					.toString());
		}

		filesDir = new File(Environment.getExternalStorageDirectory(),
				Constants.FILES_DIR); // context.getExternalFilesDir(null);
		
		boolean isNewInstallation = !filesDir.exists();
		filesDir.mkdir();
		if (!filesDir.exists()) {
			throw new IOException(context.getText(
					R.string.storage_dir_creation_failed).toString());
		}

		//createBackup("2", isNewInstallation);

		createAndReadCss();

		// set default content for default page
		WikiPage.defaultPageDefaultContent = context.getText(
				R.string.default_page_text).toString();
		WikiPage.aboutPageContent = context.getText(R.string.about_text).toString();
	}

	private void createBackup(String version, boolean isNewInstallation) throws IOException {
		File signalFile = new File(filesDir, "v" + version + ".flg");
		if (signalFile.exists()) {
			return;
		}

		if (isNewInstallation) {
			return;
		}
		
		File newDir = new File(filesDir.getAbsolutePath() + "-backup-v"
				+ version);
		if (newDir.exists()) {
			return;
		}

		newDir.mkdir();

		FileUtils.copyDirectory(filesDir, newDir);
		signalFile.createNewFile();
		
		AlertDialog ad = new AlertDialog.Builder(context).create();
		ad.setTitle(R.string.v2_update_title);
		ad.setMessage(context.getText(R.string.v2_update_text));
		ad.show();
	}

	public File Dir() {
		return filesDir;
	}

	private void createAndReadCss() {
		// create default stylesheet if not present
		File css = new File(filesDir, "style.css");

		if (!css.exists()) {
			try {
				FileIO
						.writeContents(
								css,
								"body {\npadding-bottom: 50px; \n}\n.thiki-task-finished {\n  text-decoration: line-through;\n}");
			} catch (IOException e) {
				Log.w("PagesFiles", e.getMessage());
			}
		}

		if (css.exists()) {
			try {
				WikiPage.css = FileIO.readContents(css);
			} catch (IOException e) {
				Log.w("PagesFiles", e.getMessage());
			}
		}
	}

	public Collection<WikiPage> fetchAll() {
		File[] files = filesDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {

				if (filename.toLowerCase().endsWith(WikiPage.EXT) || filename.toLowerCase().endsWith(RdfRepresentation.EXT)) {
					File f = new File(dir, filename);
					if (f.length() > 0) {
						return true;
					}
				}

				return false;
			}
		});

		SortedMap<String, WikiPage> pages = new TreeMap<String, WikiPage>();

		for (File f : files) {
			WikiPage page = new WikiPage(f);
			pages.put(page.getName(), page);
		}
		return pages.values();
	}

	public WikiPage fetchByName(String pageName) {
		return new WikiPage(getFileForPage(pageName));
	}

	private File getFileForPage(String pageName) {
		pageName = pageName.replaceAll("[^\\w\\.\\-]", "_");
		return new File(filesDir, pageName + WikiPage.EXT);
	}

	/*
	 * create page. If it already exists, function will return false.
	 */
	public boolean createNewPage(String pageName) throws IOException {
		File newPageFile = getFileForPage(pageName);
		if (newPageFile.exists()) {
			return false;
		}

		newPageFile.createNewFile();
	//	RdfRepresentation.newPage(pageName);
		return true;
	}

	public void savePage(String name, String body) throws IOException {
		Log.d("PagesFiles.savePage : ", name);
		File page = getFileForPage(name);
		FileIO.writeContents(page, body);
		RdfRepresentation.newPage(name);
		RdfRepresentation.saveMeta();
	}

	public void toggleCheckbox(String pageTitle, int whichOne)
			throws IOException {
		WikiPage page = fetchByName(pageTitle);

		String body = page.getBody();

		StringBuffer sb = new StringBuffer();
		Matcher m = WikiPage.checkBoxes.matcher(body);
		int checkboxIx = 0;
		while (m.find()) {
			String replacement = m.group();
			if (checkboxIx == whichOne) {
				// found the checkbox that should be toggled
				replacement = replacement.replace("[ ]", "[!]")
						.replace("[x]", "[ ]").replace("[!]", "[x]");
			}

			m.appendReplacement(sb, replacement);
			checkboxIx++;
		}
		m.appendTail(sb);

		body = sb.toString();

		savePage(pageTitle, body);
	}

}
