package edu.dcc.crosswordscan;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import edu.dcc.db.CrosswordColumns;
import edu.dcc.db.CrosswordDatabase;
import edu.dcc.db.DatabaseHelper;
import edu.dcc.db.FolderDetailLoader;
import edu.dcc.game.CrosswordGame;
import edu.dcc.game.Grid;

public class PuzzleListActivity extends ListActivity {

	public static final int MENU_ITEM_PLAY = Menu.FIRST;
	public static final int MENU_ITEM_INFO = Menu.FIRST + 1;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 2;

	private static final int DIALOG_DELETE_PUZZLE = 0;
	private static final int DIALOG_RESET_PUZZLE = 1;

	private long mFolderID;

	// input parameters for dialogs
	private long mDeletePuzzleID;
	private long mResetPuzzleID;

	private SimpleCursorAdapter mAdapter;
	private Cursor mCursor;
	private CrosswordDatabase mDatabase;
	private FolderDetailLoader mFolderDetailLoader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_puzzle_list);

		getListView().setOnCreateContextMenuListener(this);
		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

		mDatabase = new CrosswordDatabase(getApplicationContext());

		Intent intent = getIntent();
		String title = intent.getStringExtra(NamePuzzleActivity.TITLE);
		String grid = intent.getStringExtra(ScanActivity.GRID);
		if (title != null && grid != null) {
			CrosswordGame crossword = new CrosswordGame();
			crossword.setId(DatabaseHelper.getNextId());
			crossword.setTitle(title);
			crossword.setGrid(Grid.deserialize(grid));
			mDatabase.insertCrossword(1, crossword);
		}

		mFolderDetailLoader = new FolderDetailLoader(getApplicationContext());

		mFolderID = 1;

		mAdapter = new SimpleCursorAdapter(this, R.layout.puzzle_list_item,
				null, new String[] { CrosswordColumns.TITLE },
				new int[] { R.id.title });

		// mAdapter.setViewText((TextView) findViewById(R.id.title),
		// CrosswordColumns.TITLE);
		updateList();
		setListAdapter(mAdapter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mDatabase.close();
		mFolderDetailLoader.destroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putLong("mDeletePuzzleID", mDeletePuzzleID);
		outState.putLong("mResetPuzzleID", mResetPuzzleID);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);

		mDeletePuzzleID = state.getLong("mDeletePuzzleID");
		mResetPuzzleID = state.getLong("mResetPuzzleID");
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DELETE_PUZZLE:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_delete)
					.setTitle(CrosswordColumns.TITLE)
					.setMessage(R.string.delete_puzzle_confirm)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									mDatabase.deleteCrossword(mDeletePuzzleID);
									updateList();
								}
							}).setNegativeButton(android.R.string.no, null)
					.create();
		case DIALOG_RESET_PUZZLE:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_menu_rotate)
					.setTitle("Puzzle")
					.setMessage(R.string.reset_puzzle_confirm)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									CrosswordGame game = mDatabase
											.getCrossword(mResetPuzzleID);
									if (game != null) {
										game.reset();
										mDatabase.updateCrossword(game);
									}
									updateList();
								}
							}).setNegativeButton(android.R.string.no, null)
					.create();
		}
		return null;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			e.printStackTrace();
			return;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			// For some reason the requested item isn't available, do nothing
			return;
		}

		menu.setHeaderTitle(CrosswordColumns.TITLE);

		// Add menu items
		menu.add(0, MENU_ITEM_PLAY, 0, R.string.play_puzzle);
		menu.add(0, MENU_ITEM_INFO, 1, R.string.puzzle_info);
		menu.add(0, MENU_ITEM_DELETE, 2, R.string.delete_puzzle);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			e.printStackTrace();
			return false;
		}

		switch (item.getItemId()) {
		case MENU_ITEM_PLAY:
			playTransition(info.id);
			return true;
		case MENU_ITEM_INFO:
			Intent intent = new Intent(this, PuzzleInfoActivity.class);
			intent.setAction(Intent.ACTION_EDIT);
			// intent.putExtra(PuzzleInfoActivity.EXTRA_CROSSWORD_ID, info.id);
			startActivity(intent);
			return true;
		case MENU_ITEM_DELETE:
			mDeletePuzzleID = info.id;
			showDialog(DIALOG_DELETE_PUZZLE);
			return true;
		}
		return false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		playTransition(id);
		// String puzzle = listItems[position];
		// try {
		// LayoutInflater inflater = LayoutInflater
		// .from(PuzzleListActivity.this);
		// View puzzleActionsView = inflater.inflate(
		// R.layout.puzzle_actions_list, null);
		// ListAdapter la = new ArrayAdapter<String>(PuzzleListActivity.this,
		// android.R.layout.simple_list_item_1, getResources()
		// .getStringArray(R.array.button_list));
		// la.getItemId(0);
		// // puzzleActionsView.setAdapter(la);
		// new AlertDialog.Builder(PuzzleListActivity.this)
		// // .setTitle(puzzle)
		// .setView(puzzleActionsView)
		// .setNegativeButton("Cancel", null).show();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * Updates whole list.
	 */
	private void updateList() {
		if (mCursor != null) {
			stopManagingCursor(mCursor);
		}
		mCursor = mDatabase.getCrosswordList(mFolderID);
		startManagingCursor(mCursor);
		mAdapter.changeCursor(mCursor);
	}

	/** Called when the user clicks the Play Puzzle button */
	public void playTransition(long id) {
		Intent intent = new Intent(this, CompletePuzzleActivity.class);
		intent.putExtra(CompletePuzzleActivity.EXTRA_CROSSWORD_ID, id);
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
		// Intent intent = new Intent(this, DeleteActivity.class);
		// startActivity(intent);
	}

}
