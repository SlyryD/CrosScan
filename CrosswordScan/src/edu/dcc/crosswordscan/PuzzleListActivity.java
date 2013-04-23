package edu.dcc.crosswordscan;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import edu.dcc.db.CrosswordColumns;
import edu.dcc.db.CrosswordDatabase;

public class PuzzleListActivity extends ListActivity {

	public static final int MENU_ITEM_PLAY = Menu.FIRST;
	public static final int MENU_ITEM_INFO = Menu.FIRST + 1;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 2;

	String[] listItems = { "first puzzle", "second puzzle" };

	private long mFolderID;

	// input parameters for dialogs
	private long mDeletePuzzleID;

	private CrosswordDatabase mDatabase;
	private SimpleCursorAdapter mAdapter;

	// ArrayList<String> listItems = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_puzzle_list);

		mDatabase = new CrosswordDatabase(getApplicationContext());

		mAdapter = new SimpleCursorAdapter(this, R.layout.crossword_list_item,
				null, new String[] { CrosswordColumns.DATA,
						CrosswordColumns.STATE, CrosswordColumns.TIME,
						CrosswordColumns.LAST_PLAYED, CrosswordColumns.CREATED,
						CrosswordColumns.PUZZLE_NOTE }, new int[] {
						R.id.crossword_board, R.id.state, R.id.time,
						R.id.last_played, R.id.created, R.id.note }, 0);
		
		setListAdapter(mAdapter);
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
			ListAdapter la = new ArrayAdapter<String>(PuzzleListActivity.this,
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
