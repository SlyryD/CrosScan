package edu.dcc.crosswordscan;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
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
		try {
			LayoutInflater inflater = LayoutInflater
					.from(PuzzleListActivity.this);
			ListView puzzleActionsView = (ListView) inflater.inflate(
					R.layout.puzzle_actions_list, null);
			ListAdapter la = new ArrayAdapter<String>(
					PuzzleListActivity.this,
					android.R.layout.simple_list_item_1, getResources()
							.getStringArray(R.array.button_list));
			puzzleActionsView.setAdapter(la);
			new AlertDialog.Builder(PuzzleListActivity.this).setTitle(puzzle)
					.setView(puzzleActionsView)
					.setNegativeButton("Cancel", null).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Called when the user clicks the Play Puzzle button */
	public void playTransition(View view) {
		Intent intent = new Intent(this, CompletePuzzleActivity.class);
		startActivity(intent);
	}

	/** Called when the user clicks the Puzzle Info button */
	public void infoTransition(View view) {
		// new AlertDialog.Builder(PuzzleListActivity.this).setTitle(puzzle)
		// .setView(startPuzzleView).setNegativeButton("Cancel", null)
		// .show();
	}

	/** Called when the user clicks the Delete Puzzle button */
	public void deleteTransition(View view) {
		Intent intent = new Intent(this, ScanActivity.class);
		startActivity(intent);
	}

}
