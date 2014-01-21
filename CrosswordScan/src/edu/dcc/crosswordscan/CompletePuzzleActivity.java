package edu.dcc.crosswordscan;

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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import edu.dcc.crosswordscan.CrosswordGridView.OnCellSelectedListener;
import edu.dcc.db.CrosswordDatabase;
import edu.dcc.game.Cell;
import edu.dcc.game.CrosswordGame;
import edu.dcc.game.Entry;
import edu.dcc.game.Puzzle;

// TODO: Design landscape view

public class CompletePuzzleActivity extends Activity {

	public static final String EXTRA_CROSSWORD_ID = "crossword_id";

	public static final int MENU_ITEM_RESTART = Menu.FIRST;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;

	private static final int DIALOG_RESTART = 0;
	private static final int DIALOG_DELETE_PUZZLE = 1;

	private long mCrosswordGameID;
	private CrosswordGame mCrosswordGame;

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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_complete_puzzle);

		mCrosswordGrid = (CrosswordGridView) findViewById(R.id.crossword_grid);

		mAcrossClue = (TextView) findViewById(R.id.across_clue);
		mDownClue = (TextView) findViewById(R.id.down_clue);

		mKeyboard = new Keyboard(this, R.xml.keyboard);
		mKeyboardView = (KeyboardView) findViewById(R.id.keyboard_view);
		mKeyboardView.setKeyboard(mKeyboard);
		mKeyboardView
				.setOnKeyboardActionListener(new BasicOnKeyboardActionListener(
						this));

		mDatabase = new CrosswordDatabase(getApplicationContext());
		mGameTimer = new GameTimer(this);

		// create crossword game instance
		if (savedInstanceState == null) {
			// activity runs for the first time, read game from database
			mCrosswordGameID = getIntent().getLongExtra(EXTRA_CROSSWORD_ID, 0);
			System.out.println("GAME_ID: " + mCrosswordGameID + "!!!!");
			mCrosswordGame = mDatabase.getCrossword(mCrosswordGameID);
		} else {
			// activity has been running before, restore its state
			mCrosswordGame = new CrosswordGame();
			mCrosswordGame.restoreState(savedInstanceState);
			mGameTimer.restoreState(savedInstanceState);
		}

		if (mCrosswordGame.getState() == CrosswordGame.GAME_STATE_NOT_STARTED) {
			mCrosswordGame.start();
		} else if (mCrosswordGame.getState() == CrosswordGame.GAME_STATE_PLAYING) {
			mCrosswordGame.resume();
		}

		mCrosswordGrid.setOnCellSelectedListener(new OnCellSelectedListener() {

			@Override
			public void onCellSelected(Cell cell) {
				Entry acrossEntry = cell.getEntry(true);
				Entry downEntry = cell.getEntry(false);
				Puzzle puzzle = mCrosswordGame.getPuzzle();
				boolean acrossMode = mCrosswordGame.isAcrossMode();

				mAcrossClue.setText(acrossEntry == null ? "" : acrossEntry
						.getClueNum()
						+ "a. "
						+ puzzle.getClue(acrossEntry.getClueNum(), true));
				mDownClue.setText(downEntry == null ? "" : downEntry
						.getClueNum()
						+ "d. "
						+ puzzle.getClue(downEntry.getClueNum(), false));

				if (downEntry == null) {
					mAcrossClue.setTextColor(Color.rgb(50, 50, 255));
					mDownClue.setTextColor(Color.BLACK);
				} else if (acrossEntry == null) {
					mDownClue.setTextColor(Color.rgb(50, 50, 255));
					mAcrossClue.setTextColor(Color.BLACK);
				} else if (acrossMode) {
					mAcrossClue.setTextColor(Color.rgb(50, 50, 255));
					mDownClue.setTextColor(Color.BLACK);
				} else if (!acrossMode) {
					mDownClue.setTextColor(Color.rgb(50, 50, 255));
					mAcrossClue.setTextColor(Color.BLACK);
				}
			}

		});
		mCrosswordGrid.setGame(mCrosswordGame);
	}

	public void switchAcrossMode(View view) {
		mCrosswordGrid.switchAcrossMode();
	}

	public void previousClue(View view) {
		mCrosswordGrid.nextClue(false);
	}

	public void nextClue(View view) {
		mCrosswordGrid.nextClue(true);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mCrosswordGame.getState() == CrosswordGame.GAME_STATE_PLAYING) {
			mCrosswordGame.resume();

			if (mShowTime) {
				mGameTimer.start();
			}
		}

		updateTime();
	}

	@Override
	protected void onPause() {
		super.onPause();

		// we will save game to the database as we might not be able to get back
		mDatabase.updateCrossword(mCrosswordGame);

		mGameTimer.stop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mDatabase.close();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		mGameTimer.stop();

		if (mCrosswordGame.getState() == CrosswordGame.GAME_STATE_PLAYING) {
			mCrosswordGame.pause();
		}

		mCrosswordGame.saveState(outState);
		mGameTimer.saveState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
				new ComponentName(this, CompletePuzzleActivity.class), null,
				intent, 0, null);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_RESTART:
			showDialog(DIALOG_RESTART);
			return true;
		case MENU_ITEM_DELETE:
			showDialog(DIALOG_DELETE_PUZZLE);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_RESTART:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_menu_rotate)
					.setTitle(R.string.restart)
					.setMessage(R.string.restart_confirm)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Restart game
									mCrosswordGame.reset();
									mCrosswordGrid.resetView();
									mCrosswordGame.start();
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
								public void onClick(DialogInterface dialog,
										int whichButton) {
									mDatabase.deleteCrossword(mCrosswordGame
											.getId());
									CompletePuzzleActivity.this.finish();
								}
							}).setNegativeButton(android.R.string.no, null)
					.create();
		}
		return null;
	}

	/**
	 * Update the time of game-play.
	 */
	private void updateTime() {
		if (mShowTime) {
			setTitle(mGameTimeFormatter.format(mCrosswordGame.getTime()));
			// mTimeLabel.setText(mGameTimeFormatter.format(mCrosswordGame
			// .getTime()));
		} else {
			setTitle(R.string.app_name);
		}

	}

	public class BasicOnKeyboardActionListener implements
			OnKeyboardActionListener {

		private Activity mTargetActivity;

		/**
		 * 
		 * @param targetActivity
		 */
		public BasicOnKeyboardActionListener(Activity targetActivity) {
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
		public void onText(CharSequence text) {
			// Do nothing
		}

		@Override
		public void onRelease(int primaryCode) {
			// Do nothing
		}

		@Override
		public void onPress(int primaryCode) {
			// Do nothing
		}

		@Override
		public void onKey(int primaryCode, int[] keyCodes) {
			long eventTime = System.currentTimeMillis();
			KeyEvent event = new KeyEvent(eventTime, eventTime,
					KeyEvent.ACTION_DOWN, primaryCode, 0,
					primaryCode == KeyEvent.KEYCODE_DEL ? 0 : 193, 0, 0,
					KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE);

			mTargetActivity.dispatchKeyEvent(event);
		}
	}

	// This class implements the game clock. All it does is update the
	// status each tick.
	private static final class GameTimer extends Timer {

		private final CompletePuzzleActivity activity;

		public GameTimer(CompletePuzzleActivity activity) {
			super(1000);
			this.activity = activity;
		}

		@Override
		protected boolean step(int count, long time) {
			activity.updateTime();

			// Run until explicitly stopped.
			return false;
		}

	}

}
