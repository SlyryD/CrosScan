package edu.dcc.db;

import android.content.Context;
import edu.dcc.crosswordscan.R;

/**
 * Some information about folder, used in FolderListActivity.
 * 
 * @author Ryan
 */
public class FolderInfo {

	/**
	 * Primary key of folder.
	 */
	public long id;

	/**
	 * Name of the folder.
	 */
	public String name;

	/**
	 * Total count of puzzles in the folder.
	 */
	public int puzzleCount;

	/**
	 * Count of puzzles in "playing" state in the folder.
	 */
	public int playingCount;

	public FolderInfo() {

	}

	public FolderInfo(long id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getDetail(Context c) {
		StringBuilder sb = new StringBuilder();

		if (puzzleCount == 0) {
			// no puzzles in folder
			sb.append(c.getString(R.string.no_puzzles));
		} else {
			// there are some puzzles
			sb.append(puzzleCount == 1 ? c.getString(R.string.one_puzzle) : c
					.getString(R.string.n_puzzles, puzzleCount));

			// if there are any playing or unsolved puzzles, add info about them
			if (playingCount != 0) {
				sb.append(" (");

				if (playingCount != 0) {
					sb.append(c.getString(R.string.n_playing, playingCount));
				}

				sb.append(")");
			}

		}

		return sb.toString();

	}

}
