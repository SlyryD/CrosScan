package edu.dcc.db;

import edu.dcc.game.CrosswordGame;

public class CrosswordImportParams {
	public long created;
	public long state;
	public long time;
	public long lastPlayed;
	public String data;
	public String title;

	public void clear() {
		created = 0;
		state = CrosswordGame.GAME_STATE_NOT_STARTED;
		time = 0;
		lastPlayed = 0;
		data = null;
		title = null;
	}
}
