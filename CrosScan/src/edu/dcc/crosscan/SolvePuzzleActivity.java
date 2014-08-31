package edu.dcc.crosscan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import edu.dcc.crosscan.CrosswordGridView.OnCellSelectedListener;
import edu.dcc.db.CrosswordDatabase;
import edu.dcc.game.Cell;
import edu.dcc.game.CrosswordGame;
import edu.dcc.game.Entry;
import edu.dcc.game.Puzzle;

// TODO: Design landscape view

public class SolvePuzzleActivity extends Activity {

	public static final int MENU_ITEM_RESTART = Menu.FIRST;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;

	private static final int DIALOG_RESTART = 0;
	private static final int DIALOG_DELETE_PUZZLE = 1;

	private static final int KEYCODE_PHOTO = 273;

	private long mCrosswordGameID;
	private CrosswordGame mGame;

	private CrosswordDatabase mDatabase;

	private CrosswordGridView mCrosswordGrid;

	private TextView mAcrossClue;
	private TextView mDownClue;

	private Keyboard mKeyboard;
	private KeyboardView mKeyboardView;

	private boolean mShowTime = true;
	private GameTimer mGameTimer;
	private GameTimeFormat mGameTimeFormatter = new GameTimeFormat();

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_solve_puzzle);

		mCrosswordGrid = (CrosswordGridView) findViewById(R.id.crossword_grid);

		mAcrossClue = (TextView) findViewById(R.id.across_clue);
		mDownClue = (TextView) findViewById(R.id.down_clue);

		mKeyboard = new Keyboard(this, R.xml.keyboard);
		mKeyboardView = (KeyboardView) findViewById(R.id.keyboard_view);
		mKeyboardView.setPreviewEnabled(false);
		mKeyboardView.setKeyboard(mKeyboard);
		mKeyboardView
				.setOnKeyboardActionListener(new BasicOnKeyboardActionListener(
						this));

		mDatabase = new CrosswordDatabase(getApplicationContext());
		mGameTimer = new GameTimer(this);

		// create crossword game instance
		if (savedInstanceState == null) {
			// activity runs for the first time, read game from database
			mCrosswordGameID = getIntent().getLongExtra(Constants.EXTRA_CROSSWORD_ID, 0);
			System.out.println("GAME_ID: " + mCrosswordGameID + "!!!!");
			mGame = mDatabase.getCrossword(mCrosswordGameID);
		} else {
			// activity has been running before, restore its state
			mGame = new CrosswordGame();
			mGame.restoreState(savedInstanceState);
			mGameTimer.restoreState(savedInstanceState);
		}

		if (mGame.getState() == CrosswordGame.GAME_STATE_NOT_STARTED) {
			mGame.start();
		} else if (mGame.getState() == CrosswordGame.GAME_STATE_PLAYING) {
			mGame.resume();
		}

		mCrosswordGrid
				.setOnCellSelectedListener(new BasicOnCellSelectedListener());
		mCrosswordGrid.setGame(mGame);
	}

	public final void switchAcrossMode(final View view) {
		mCrosswordGrid.switchAcrossMode();
	}

	public final void previousClue(final View view) {
		mCrosswordGrid.nextClue(false);
	}

	public final void nextClue(final View view) {
		mCrosswordGrid.nextClue(true);
	}

	@Override
	protected final void onResume() {
		super.onResume();

		if (mGame.getState() == CrosswordGame.GAME_STATE_PLAYING) {
			mGame.resume();

			if (mShowTime) {
				mGameTimer.start();
			}
		}

		updateTime();
	}

	@Override
	protected final void onPause() {
		super.onPause();

		// we will save game to the database as we might not be able to get back
		mDatabase.updateCrossword(mGame);

		mGameTimer.stop();
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();

		mDatabase.close();
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		mGameTimer.stop();

		if (mGame.getState() == CrosswordGame.GAME_STATE_PLAYING) {
			mGame.pause();
		}

		mGame.saveState(outState);
		mGameTimer.saveState(outState);
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ITEM_RESTART, 0, R.string.restart).setIcon(
				android.R.drawable.ic_menu_rotate);
		menu.add(0, MENU_ITEM_DELETE, 1, R.string.delete_puzzle).setIcon(
				android.R.drawable.ic_delete);

		// Generate any additional actions that can be performed on the
		// overall list. In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
		Intent intent = new Intent(null, getIntent().getData());
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
				new ComponentName(this, SolvePuzzleActivity.class), null,
				intent, 0, null);

		return true;
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_RESTART:
			showDialog(DIALOG_RESTART);
			return true;
		case MENU_ITEM_DELETE:
			showDialog(DIALOG_DELETE_PUZZLE);
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected final Dialog onCreateDialog(final int id) {
		switch (id) {
		case DIALOG_RESTART:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_menu_rotate)
					.setTitle(R.string.restart)
					.setMessage(R.string.restart_confirm)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(final DialogInterface dialog,
										final int whichButton) {
									// Restart game
									mGame.reset();
									mCrosswordGrid.resetView();
									
									mGame.start();
									if (mShowTime) {
										mGameTimer.start();
									}
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
								public void onClick(final DialogInterface dialog,
										final int whichButton) {
									mDatabase.deleteCrossword(mGame
											.getId());
									SolvePuzzleActivity.this.finish();
								}
							}).setNegativeButton(android.R.string.no, null)
					.create();
		default:
			break;
		}
		return null;
	}

	@Override
	public final boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (keyCode == KEYCODE_PHOTO) {
			Intent intent = new Intent(this, ImageActivity.class);
			intent.putExtra(Constants.EXTRA_PHOTO, mGame.getPhoto());
			startActivity(intent);
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Update the time of game-play.
	 */
	private void updateTime() {
		if (mShowTime) {
			setTitle(mGameTimeFormatter.format(mGame.getTime()) + "\t"
					+ mGame.getTitle());
		} else {
			setTitle(mGame.getTitle());
		}

	}

	public class BasicOnKeyboardActionListener implements
			OnKeyboardActionListener {

		private Activity mTargetActivity;

		/**
		 * 
		 * @param targetActivity
		 */
		public BasicOnKeyboardActionListener(final Activity targetActivity) {
			mTargetActivity = targetActivity;
		}

		@Override
		public void swipeUp() {
			// Do nothing
		}

		@Override
		public void swipeRight() {
			// Do nothing
		}

		@Override
		public void swipeLeft() {
			// Do nothing
		}

		@Override
		public void swipeDown() {
			// Do nothing
		}

		@Override
		public void onText(final CharSequence text) {
			// Do nothing
		}

		@Override
		public void onRelease(final int primaryCode) {
			// Do nothing
		}

		@Override
		public void onPress(final int primaryCode) {
			// Do nothing
		}

		@Override
		public final void onKey(final int primaryCode, final int[] keyCodes) {
			long eventTime = System.currentTimeMillis();
			int metaState = KeyEvent.META_SHIFT_MASK;
			if (primaryCode == KeyEvent.KEYCODE_DEL
					|| primaryCode == KEYCODE_PHOTO) {
				metaState = 0;
			}
			KeyEvent event = new KeyEvent(eventTime, eventTime,
					KeyEvent.ACTION_DOWN, primaryCode, 0, metaState, 0, 0,
					KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE);

			mTargetActivity.dispatchKeyEvent(event);
		}
	}

	public class BasicOnCellSelectedListener implements OnCellSelectedListener {

		@Override
		public final boolean onCellSelected(final Cell cell) {
			if (!cell.isWhite()) {
				Log.e("SolvePuzzleActivity", "Attempted to select black cell");
				return false;
			}

			Entry acrossEntry = cell.getEntry(true);
			Entry downEntry = cell.getEntry(false);
			Puzzle puzzle = mGame.getPuzzle();
			boolean acrossMode = mGame.isAcrossMode();

			mAcrossClue.setText(acrossEntry == null ? "" : acrossEntry
					.getClueNum()
					+ "a. "
					+ puzzle.getClue(acrossEntry.getClueNum(), true));
			mDownClue.setText(downEntry == null ? "" : downEntry.getClueNum()
					+ "d. " + puzzle.getClue(downEntry.getClueNum(), false));

			final int blue = Color.rgb(50, 50, 255);
			if (downEntry == null) {
				mAcrossClue.setTextColor(blue);
				mDownClue.setTextColor(Color.BLACK);
			} else if (acrossEntry == null) {
				mDownClue.setTextColor(blue);
				mAcrossClue.setTextColor(Color.BLACK);
			} else if (acrossMode) {
				mAcrossClue.setTextColor(blue);
				mDownClue.setTextColor(Color.BLACK);
			} else if (!acrossMode) {
				mDownClue.setTextColor(blue);
				mAcrossClue.setTextColor(Color.BLACK);
			}
			return true;
		}
	}

	// This class implements the game clock. All it does is update the
	// status each tick.
	private static final class GameTimer extends Timer {

		private final SolvePuzzleActivity activity;

		public GameTimer(final SolvePuzzleActivity activity) {
			super(1000);
			this.activity = activity;
		}

		@Override
		protected boolean step(final int count, final long time) {
			activity.updateTime();

			// Run until explicitly stopped.
			return false;
		}

	}

}
