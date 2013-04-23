package edu.dcc.crosswordscan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import edu.dcc.db.CrosswordDatabase;
import edu.dcc.game.CrosswordGame;
import edu.dcc.game.CrosswordGridView;

public class CompletePuzzleActivity extends Activity {

	public static final String EXTRA_CROSSWORD_ID = "crossword_id";

	public static final int MENU_ITEM_RESTART = Menu.FIRST;

	private static final int DIALOG_RESTART = 1;

	private long mCrosswordGameID;
	private CrosswordGame mCrosswordGame;

	private CrosswordDatabase mDatabase;

	private Handler mGuiHandler;

	private ViewGroup mRootLayout;
	private CrosswordGridView mCrosswordGrid;
//	private TextView mTimeLabel;

	// private IMControlPanel mIMControlPanel;
	// private IMControlPanelStatePersister mIMControlPanelStatePersister;
	// private IMPopup mIMPopup;
	// private IMSingleNumber mIMSingleNumber;
	// private IMNumpad mIMNumpad;

	private boolean mShowTime = true;
	private GameTimer mGameTimer;
	private GameTimeFormat mGameTimeFormatter = new GameTimeFormat();
	private boolean mFullScreen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_complete_puzzle);

		// go fullscreen for devices with QVGA screen (only way I found
		// how to fit UI on the screen)
		Display display = getWindowManager().getDefaultDisplay();
		if ((display.getWidth() == 240 || display.getWidth() == 320)
				&& (display.getHeight() == 240 || display.getHeight() == 320)) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			mFullScreen = true;
		}

		mRootLayout = (ViewGroup) findViewById(R.id.root_layout);
		mCrosswordGrid = (CrosswordGridView) findViewById(R.id.crossword_grid);
		// mTimeLabel = (TextView) findViewById(R.id.time_label);

		mDatabase = new CrosswordDatabase(getApplicationContext());
		mGameTimer = new GameTimer();

		mGuiHandler = new Handler();

		// create crossword game instance
		if (savedInstanceState == null) {
			// activity runs for the first time, read game from database
			mCrosswordGameID = getIntent().getLongExtra(EXTRA_CROSSWORD_ID, 0);
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

		mCrosswordGrid.setGame(mCrosswordGame);

		// mIMControlPanel = (IMControlPanel) findViewById(R.id.input_methods);
		// mIMControlPanel.initialize(mCrosswordGrid, mCrosswordGame,
		// mHintsQueue);
		//
		// mIMControlPanelStatePersister = new
		// IMControlPanelStatePersister(this);
		//
		// mIMPopup = mIMControlPanel
		// .getInputMethod(IMControlPanel.INPUT_METHOD_POPUP);
		// mIMSingleNumber = mIMControlPanel
		// .getInputMethod(IMControlPanel.INPUT_METHOD_SINGLE_NUMBER);
		// mIMNumpad = mIMControlPanel
		// .getInputMethod(IMControlPanel.INPUT_METHOD_NUMPAD);
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
//		mTimeLabel.setVisibility(mFullScreen && mShowTime ? View.VISIBLE
//				: View.GONE);

		// mIMPopup.setEnabled(gameSettings.getBoolean("im_popup", true));
		// mIMSingleNumber.setEnabled(gameSettings.getBoolean("im_single_number",
		// true));
		// mIMNumpad.setEnabled(gameSettings.getBoolean("im_numpad", true));
		// mIMNumpad.setMoveCellSelectionOnPress(gameSettings.getBoolean(
		// "im_numpad_move_right", false));
		// mIMPopup.setHighlightCompletedValues(gameSettings.getBoolean(
		// "highlight_completed_values", true));
		// mIMPopup.setShowNumberTotals(gameSettings.getBoolean(
		// "show_number_totals", false));
		// mIMSingleNumber.setHighlightCompletedValues(gameSettings.getBoolean(
		// "highlight_completed_values", true));
		// mIMSingleNumber.setShowNumberTotals(gameSettings.getBoolean(
		// "show_number_totals", false));
		// mIMNumpad.setHighlightCompletedValues(gameSettings.getBoolean(
		// "highlight_completed_values", true));
		// mIMNumpad.setShowNumberTotals(gameSettings.getBoolean(
		// "show_number_totals", false));
		//
		// mIMControlPanel.activateFirstInputMethod(); // make sure that some
		// input
		// // method is activated
		// mIMControlPanelStatePersister.restoreState(mIMControlPanel);

		updateTime();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (hasFocus) {
			// FIXME: When activity is resumed, title isn't sometimes hidden
			// properly (there is black
			// empty space at the top of the screen). This is desperate
			// workaround.
			if (mFullScreen) {
				mGuiHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						getWindow()
								.clearFlags(
										WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
						mRootLayout.requestLayout();
					}
				}, 1000);
			}

		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		// we will save game to the database as we might not be able to get back
		mDatabase.updateCrossword(mCrosswordGame);

		mGameTimer.stop();
		// mIMControlPanel.pause();
		// mIMControlPanelStatePersister.saveState(mIMControlPanel);
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

		menu.add(0, MENU_ITEM_RESTART, 1, R.string.restart)
				.setShortcut('7', 'r')
				.setIcon(android.R.drawable.ic_menu_rotate);

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

		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Restarts whole activity.
	 */
	private void restartActivity() {
		startActivity(getIntent());
		finish();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_RESTART:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_menu_rotate)
					.setTitle(R.string.app_name)
					.setMessage(R.string.restart_confirm)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Restart game
									mCrosswordGame.reset();
									mCrosswordGame.start();
									if (mShowTime) {
										mGameTimer.start();
									}
								}
							}).setNegativeButton(android.R.string.no, null)
					.create();

		}
		return null;
	}

	/**
	 * Update the time of game-play.
	 */
	void updateTime() {
		if (mShowTime) {
			setTitle(mGameTimeFormatter.format(mCrosswordGame.getTime()));
			// mTimeLabel.setText(mGameTimeFormatter.format(mCrosswordGame
			// .getTime()));
		} else {
			setTitle(R.string.app_name);
		}

	}

	// This class implements the game clock. All it does is update the
	// status each tick.
	private final class GameTimer extends Timer {

		public GameTimer() {
			super(1000);
		}

		@Override
		protected boolean step(int count, long time) {
			updateTime();

			// Run until explicitly stopped.
			return false;
		}

	}

}
