package it.hyperdata.thiki;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class PagesListActivity extends Activity {

	private static final int REQUEST_NEWPAGENAME = 0;

	private ThikiActivityHelper activityHelper;
	private ListView listView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pageslist);

		activityHelper = new ThikiActivityHelper(this);
		listView = activityHelper.find(R.id.listview);

		registerForContextMenu(listView);
		setTitle(R.string.list_pages_long);

		fillList();

		addListeners();
	}

	private void addListeners() {
		Button addButton = (Button) findViewById(R.id.button_add);
		addButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startCreateNewPage();
			}
		});

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> l, View v, int position,
					long id) {

				@SuppressWarnings("unchecked")
				Map<String, String> item = (Map<String, String>) l
						.getItemAtPosition(position);

				String pageTitle = item.get("Name");
				viewPage(pageTitle);
			}
		});
	}

	/*
	 * get wikipages from dal and fill the list with the names
	 */
	private void fillList() {

		List<Map<String, String>> data = new ArrayList<Map<String, String>>();

		for (WikiPage page : activityHelper.getPagesFiles().fetchAll()) {
			Map<String, String> fileInfo = new HashMap<String, String>();
			fileInfo.put("Name", page.getName());
			data.add(fileInfo);
		}

		TextView t = activityHelper.find(R.id.empty);

		if (data.isEmpty()) {
			t.setVisibility(TextView.VISIBLE);
			listView.setVisibility(ListView.GONE);
			return;
		}

		t.setVisibility(TextView.GONE);

		String[] from = new String[] { "Name" };
		int[] to = new int[] { R.id.text1 };

		SimpleAdapter adapter = new SimpleAdapter(this, data,
				R.layout.pageslist_item, from, to);
		listView.setAdapter(adapter);

	}

	/*
	 * start the activity to get the new page name for creating a new page
	 * (continued in createNewPage)
	 */
	private void startCreateNewPage() {
		Intent i = new Intent(this, NewPageNameActivity.class);
		startActivityForResult(i, REQUEST_NEWPAGENAME);
	}

	/*
	 * create a new page on disk using the pagename that was fetched from the
	 * user with startCreateNewPage
	 */
	private void createNewPage(String pageTitle) {
		try {
			activityHelper.getPagesFiles().createNewPage(pageTitle);
			viewPage(pageTitle);
		} catch (IOException e) {
			activityHelper.showToast(getText(R.string.save_error), e);
			Log.w("PagesListActivity", e.getMessage());
		}
	}

	private void viewPage(String pageTitle) {

		Intent i = new Intent();
		i.putExtra(ViewPageActivity.TITLE_KEY, pageTitle);

		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.pages_contextmenu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		@SuppressWarnings("unchecked")
		Map<String, String> file = (Map<String, String>) listView
				.getItemAtPosition(info.position);
		String pageTitle = file.get("Name");

		switch (item.getItemId()) {
		case R.id.delete:
			try {
				activityHelper.getPagesFiles().savePage(pageTitle, "");
				fillList();
			} catch (IOException e) {
				activityHelper.showToast(R.string.save_error);
				Log.w("PagesLists", e.getMessage());
			}
			return true;

		case R.id.open:
			viewPage(pageTitle);
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		switch (requestCode) {
		case REQUEST_NEWPAGENAME:
			if (intent == null) {
				return;
			}

			Bundle result = intent.getExtras();
			if (result == null) {
				return;
			}

			String pageTitle = result.getString("pageTitle");
			if (pageTitle == null || pageTitle.length() == 0) {
				return;
			}

			createNewPage(pageTitle);
			return;
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}
}