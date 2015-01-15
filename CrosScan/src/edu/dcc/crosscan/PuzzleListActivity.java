package edu.dcc.crosscan;

import java.util.ArrayList;

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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import edu.dcc.db.CrosswordColumns;
import edu.dcc.db.CrosswordDatabase;
import edu.dcc.db.DatabaseHelper;
import edu.dcc.db.FolderDetailLoader;
import edu.dcc.game.CrosswordGame;
import edu.dcc.game.Puzzle;

public class PuzzleListActivity extends ListActivity {

	public static final int MENU_ITEM_PLAY = Menu.FIRST;
	public static final int MENU_ITEM_INFO = Menu.FIRST + 1;
	public static final int MENU_ITEM_RESTART = Menu.FIRST + 2;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 3;

	private static final int DIALOG_RESTART_PUZZLE = 0;
	private static final int DIALOG_DELETE_PUZZLE = 1;

	private long mFolderID;

	// input parameters for dialogs
	private long mRestartPuzzleID;
	private long mDeletePuzzleID;

	private ArrayAdapter<String> mAdapter;
	private Cursor mCursor;
	private CrosswordDatabase mDatabase;
	private FolderDetailLoader mFolderDetailLoader;

	/**
	 * Updates whole list.
	 */
	private void updateList() {
		// Query database
		mCursor = mDatabase.getCrosswordList(mFolderID);

		// Create list of results
		ArrayList<String> sData = new ArrayList<String>();

		// Check if our result was valid.
		if (mCursor.moveToFirst()) {
			int titleCol = mCursor.getColumnIndex(CrosswordColumns.TITLE);
			// cursor left as it came from the database because it starts at the
			// row before the first row
			do {
				sData.add(mCursor.getString(titleCol));
			} while (mCursor.moveToNext());
		}
		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, sData);
		setListAdapter(mAdapter);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_puzzle_list);

		getListView().setOnCreateContextMenuListener(this);
		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

		mDatabase = new CrosswordDatabase(getApplicationContext());

		Intent intent = getIntent();
		String title = intent.getStringExtra(Constants.EXTRA_TITLE);
		String puzzle = intent.getStringExtra(Constants.EXTRA_GRID);
		String photo = intent.getStringExtra(Constants.EXTRA_PHOTO);
		if (title != null && puzzle != null) {
			CrosswordGame crossword = new CrosswordGame();
			crossword.setId(DatabaseHelper.getNextId());
			crossword.setTitle(title);
			crossword.setPuzzle(Puzzle.deserialize(puzzle));
			crossword.setPhoto(photo);
			mDatabase.insertCrossword(1, crossword);
		}

		mFolderDetailLoader = new FolderDetailLoader(getApplicationContext());

		mFolderID = 1;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Update list
		updateList();
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();

		mDatabase.close();
		mFolderDetailLoader.destroy();
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putLong("mRestartPuzzleID", mRestartPuzzleID);
		outState.putLong("mDeletePuzzleID", mDeletePuzzleID);
	}

	@Override
	protected final void onRestoreInstanceState(final Bundle state) {
		super.onRestoreInstanceState(state);

		mRestartPuzzleID = state.getLong("mRestartPuzzleID");
		mDeletePuzzleID = state.getLong("mDeletePuzzleID");
	}

	@Override
	protected final Dialog onCreateDialog(final int id) {
		switch (id) {
		case DIALOG_RESTART_PUZZLE:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_menu_rotate)
					.setTitle(R.string.restart)
					.setMessage(R.string.restart_puzzle_confirm)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int whichButton) {
									CrosswordGame game = mDatabase
											.getCrossword(mRestartPuzzleID);
									game.restart();
									mDatabase.updateCrossword(game);
								}
							}).setNegativeButton(android.R.string.no, null)
					.create();
		case DIALOG_DELETE_PUZZLE:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_delete)
					.setTitle(R.string.delete_puzzle)
					.setMessage(R.string.delete_puzzle_confirm)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int whichButton) {
									try {
										mDatabase
												.deleteCrossword(mDeletePuzzleID);

										// TODO: Delete photo
									} finally {
										updateList();
									}
								}
							}).setNegativeButton(android.R.string.no, null)
					.create();
		default:
			break;
		}
		return null;
	}

	@Override
	public final void onCreateContextMenu(final ContextMenu menu,
			final View view, final ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			e.printStackTrace();
			return;
		}

		if (getListAdapter().getItem(info.position) == null) {
			// For some reason the requested item isn't available, do nothing
			return;
		}

		// Add menu items
		menu.add(0, MENU_ITEM_PLAY, 0, R.string.play_puzzle);
		menu.add(0, MENU_ITEM_INFO, 1, R.string.puzzle_info);
		menu.add(0, MENU_ITEM_RESTART, 3, R.string.restart);
		menu.add(0, MENU_ITEM_DELETE, 4, R.string.delete_puzzle);
	}

	@Override
	public final boolean onContextItemSelected(final MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			e.printStackTrace();
			return false;
		}

		mCursor.moveToPosition((int) info.id);

		long puzzleId = mCursor.getLong(mCursor
				.getColumnIndex(CrosswordColumns._ID));
		switch (item.getItemId()) {
		case MENU_ITEM_PLAY:
			playTransition(puzzleId);
			return true;
		case MENU_ITEM_INFO:
			infoTransition(puzzleId);
			return true;
		case MENU_ITEM_RESTART:
			mRestartPuzzleID = puzzleId;
			restartTransition(info.id);
			return true;
		case MENU_ITEM_DELETE:
			mDeletePuzzleID = puzzleId;
			deleteTransition(info.id);
			return true;
		default:
			break;
		}
		return false;
	}

	@Override
	protected final void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		super.onListItemClick(l, v, position, id);
		mCursor.moveToPosition((int) id);
		playTransition(mCursor.getLong(mCursor
				.getColumnIndex(CrosswordColumns._ID)));
	}

	/** Called when the user clicks the Play Puzzle button */
	public final void playTransition(final long id) {
		Intent intent = new Intent(this, SolvePuzzleActivity.class);
		intent.putExtra(Constants.EXTRA_CROSSWORD_ID, id);
		startActivity(intent);
	}

	/** Called when the user clicks the Puzzle Info menu item */
	public final void infoTransition(final long id) {
		Intent intent = new Intent(this, PuzzleInfoActivity.class);
		intent.putExtra(Constants.EXTRA_CROSSWORD_ID, id);
		startActivity(intent);
	}

	public final void deleteTransition(final long id) {
		showDialog(DIALOG_DELETE_PUZZLE);
	}

	public final void restartTransition(final long id) {
		showDialog(DIALOG_RESTART_PUZZLE);
	}
}
