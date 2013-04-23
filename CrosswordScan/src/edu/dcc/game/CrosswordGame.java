/* 
 * Copyright (C) 2009 Roman Masek
 * 
 * This file is part of OpenSudoku.
 * 
 * OpenSudoku is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OpenSudoku is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OpenSudoku.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package edu.dcc.game;

import android.os.Bundle;
import android.os.SystemClock;
import edu.dcc.game.command.AbstractCommand;
import edu.dcc.game.command.CommandStack;
import edu.dcc.game.command.SetCellValueCommand;

public class CrosswordGame {

	public static final int GAME_STATE_PLAYING = 0;
	public static final int GAME_STATE_NOT_STARTED = 1;

	private long mId;
	private String mTitle;
	private long mCreated;
	private int mState;
	private long mTime;
	private long mLastPlayed;
	private Grid mGrid;

	private CommandStack mCommandStack;
	// Time when current activity has become active.
	private long mActiveFromTime = -1;

	public static CrosswordGame createEmptyGame() {
		CrosswordGame game = new CrosswordGame();
		game.setGrid(Grid.createEmpty());
		// set creation time
		game.setCreated(System.currentTimeMillis());
		return game;
	}

	public CrosswordGame() {
		mTime = 0;
		mLastPlayed = 0;
		mCreated = 0;

		mState = GAME_STATE_NOT_STARTED;
	}

	public void saveState(Bundle outState) {
		outState.putLong("id", mId);
		outState.putLong("created", mCreated);
		outState.putInt("state", mState);
		outState.putLong("time", mTime);
		outState.putLong("lastPlayed", mLastPlayed);
		outState.putString("cells", mGrid.serialize());
		outState.putString("title", mTitle);

		mCommandStack.saveState(outState);
	}

	public void restoreState(Bundle inState) {
		mId = inState.getLong("id");
		mCreated = inState.getLong("created");
		mState = inState.getInt("state");
		mTime = inState.getLong("time");
		mLastPlayed = inState.getLong("lastPlayed");
		mGrid = Grid.deserialize(inState.getString("cells"));
		mTitle = inState.getString("title");

		mCommandStack = new CommandStack(mGrid);
		mCommandStack.restoreState(inState);
	}

	public void setCreated(long created) {
		mCreated = created;
	}

	public long getCreated() {
		return mCreated;
	}

	public void setState(int state) {
		mState = state;
	}

	public int getState() {
		return mState;
	}

	/**
	 * Sets time of play in milliseconds.
	 * 
	 * @param time
	 */
	public void setTime(long time) {
		mTime = time;
	}

	/**
	 * Gets time of game-play in milliseconds.
	 * 
	 * @return
	 */
	public long getTime() {
		if (mActiveFromTime != -1) {
			return mTime + SystemClock.uptimeMillis() - mActiveFromTime;
		} else {
			return mTime;
		}
	}

	public void setLastPlayed(long lastPlayed) {
		mLastPlayed = lastPlayed;
	}

	public long getLastPlayed() {
		return mLastPlayed;
	}

	public void setGrid(Grid cells) {
		mGrid = cells;
		mCommandStack = new CommandStack(mGrid);
	}

	public Grid getGrid() {
		return mGrid;
	}

	public void setId(long id) {
		mId = id;
	}

	public long getId() {
		return mId;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return mTitle;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.mTitle = title;
	}

	/**
	 * Sets value for the given cell.
	 * 
	 * @param cell
	 * @param value
	 */
	public void setCellValue(Cell cell, char value) {
		if (cell == null) {
			throw new IllegalArgumentException("Cell cannot be null.");
		}
		if ((value != 0 && value < 65) || value > 90) {
			throw new IllegalArgumentException("Value must be a character.");
		}

		if (cell.isWhite()) {
			executeCommand(new SetCellValueCommand(cell, value));
		}
	}

	private void executeCommand(AbstractCommand c) {
		mCommandStack.execute(c);
	}

	/**
	 * Undo last command.
	 */
	public void undo() {
		mCommandStack.undo();
	}

	public boolean hasSomethingToUndo() {
		return mCommandStack.hasSomethingToUndo();
	}

	public void setUndoCheckpoint() {
		mCommandStack.setCheckpoint();
	}

	public void undoToCheckpoint() {
		mCommandStack.undoToCheckpoint();
	}

	public boolean hasUndoCheckpoint() {
		return mCommandStack.hasCheckpoint();
	}

	/**
	 * Start game-play.
	 */
	public void start() {
		mState = GAME_STATE_PLAYING;
		resume();
	}

	public void resume() {
		// reset time we have spent playing so far, so time when activity was
		// not active
		// will not be part of the game play time
		mActiveFromTime = SystemClock.uptimeMillis();
	}

	/**
	 * Pauses game-play (for example if activity pauses).
	 */
	public void pause() {
		// save time we have spent playing so far - it will be reseted after
		// resuming
		mTime += SystemClock.uptimeMillis() - mActiveFromTime;
		mActiveFromTime = -1;

		setLastPlayed(System.currentTimeMillis());
	}

	/**
	 * Finishes game-play. Called when puzzle is solved.
	 */
	private void finish() {
		pause();
	}

	/**
	 * Resets game.
	 */
	public void reset() {
		for (int row = 0; row < Grid.gridSize; row++) {
			for (int col = 0; col < Grid.gridSize; col++) {
				Cell cell = mGrid.getCell(row, col);
				if (cell.isWhite()) {
					cell.setValue((char) 0);
				}
			}
		}
		setTime(0);
		setLastPlayed(0);
		mState = GAME_STATE_NOT_STARTED;
	}

}
