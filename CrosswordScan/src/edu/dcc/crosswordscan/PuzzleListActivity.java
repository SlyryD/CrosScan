package edu.dcc.crosswordscan;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PuzzleListActivity extends ListActivity {

	String[] listItems = { "first puzzle", "second puzzle" };

	// ArrayList<String> listItems = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new ArrayAdapter<String>(PuzzleListActivity.this,
				android.R.layout.simple_list_item_1, listItems));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		String puzzle = listItems[position];
		// String puzzle = listItems.get(position);
		try {
			new AlertDialog.Builder(PuzzleListActivity.this).setTitle(puzzle)
			// TODO: Inflate
			// .setView(findViewById(R.layout.activity_puzzle_list))
					.setNegativeButton("Cancel", null).show();
			findViewById(R.layout.activity_puzzle_list);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
