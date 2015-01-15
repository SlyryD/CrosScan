package edu.dcc.game;

import android.os.Bundle;
import android.os.SystemClock;

/**
 * @author Ryan
 */
public class CrosswordGame {

	public static final int GAME_STATE_PLAYING = 0;
	public static final int GAME_STATE_NOT_STARTED = 1;

	private long id;
	private long created;
	private int state;
	private long time;
	private long lastPlayed;
	private Puzzle puzzle;
	private String title;
	private String photo;

	// Keep track of across mode
	private boolean acrossMode;

	// Time when current activity has become active.
	private long mActiveFromTime = -1;

	public CrosswordGame() {
		created = 0;
		state = GAME_STATE_NOT_STARTED;
		time = 0;
		lastPlayed = 0;
		acrossMode = true;
	}

	public void saveState(Bundle outState) {
		outState.putLong("id", id);
		outState.putLong("created", created);
		outState.putInt("state", state);
		outState.putLong("time", time);
		outState.putLong("lastPlayed", lastPlayed);
		outState.putString("puzzle", puzzle.serialize());
		outState.putString("title", title);
		outState.putString("photo", photo);
	}

	public void restoreState(Bundle inState) {
		id = inState.getLong("id");
		created = inState.getLong("created");
		state = inState.getInt("state");
		time = inState.getLong("time");
		lastPlayed = inState.getLong("lastPlayed");
		puzzle = Puzzle.deserialize(inState.getString("puzzle"));
		title = inState.getString("title");
		photo = inState.getString("photo");
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public long getCreated() {
		return created;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}

	/**
	 * Sets time of play in milliseconds.
	 * 
	 * @param time
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * Gets time of game-play in milliseconds.
	 * 
	 * @return
	 */
	public long getTime() {
		if (mActiveFromTime != -1) {
			return time + SystemClock.uptimeMillis() - mActiveFromTime;
		} else {
			return time;
		}
	}

	public void setLastPlayed(long lastPlayed) {
		this.lastPlayed = lastPlayed;
	}

	public long getLastPlayed() {
		return lastPlayed;
	}

	public void setPuzzle(Puzzle puzzle) {
		this.puzzle = puzzle;
	}

	public Puzzle getPuzzle() {
		return puzzle;
	}

	/**
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public String getPhoto() {
		return photo;
	}

	/**
	 * Returns across mode
	 * 
	 * @return acrossMode
	 */
	public boolean isAcrossMode() {
		return acrossMode;
	}

	/**
	 * @param acrossMode
	 */
	public void setAcrossMode(boolean acrossMode) {
		this.acrossMode = acrossMode;
	}

	public float getCompletion() {
		return puzzle.getCompletion();
	}

	/**
	 * Sets value for the given cell.
	 * 
	 * @param cell
	 * @param value
	 */
	public void setCellValue(Cell cell, char value) {
		if (cell == null) {
			throw new IllegalArgumentException("Cell cannot be null");
		}

		if (cell.isWhite()) {
			cell.setValue(value);
		}
	}

	/**
	 * Start game-play.
	 */
	public void start() {
		state = GAME_STATE_PLAYING;
		resume();
	}

	public void resume() {
		// reset time we have spent playing so far, so time when activity was
		// not active will not be part of the game play time
		mActiveFromTime = SystemClock.uptimeMillis();
	}

	/**
	 * Pauses game-play (for example if activity pauses).
	 */
	public void pause() {
		// save time we have spent playing so far - it will be reseted after
		// resuming
		time += SystemClock.uptimeMillis() - mActiveFromTime;
		mActiveFromTime = -1;

		setLastPlayed(System.currentTimeMillis());
	}

	/**
	 * Finishes game-play. Called when puzzle is solved.
	 */
	public void finish() {

	}

	/**
	 * Restarts game.
	 */
	public void restart() {
		state = GAME_STATE_NOT_STARTED;
		setTime(0);
		setLastPlayed(System.currentTimeMillis());
		puzzle.reset();
		acrossMode = true;
	}

}
