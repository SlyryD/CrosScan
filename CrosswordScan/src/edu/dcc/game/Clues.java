package edu.dcc.game;

import java.util.StringTokenizer;
import java.util.TreeMap;

public class Clues {

	private TreeMap<Integer, String> mAcrossClues;
	private TreeMap<Integer, String> mDownClues;

	public Clues(TreeMap<Integer, String> acrossClues,
			TreeMap<Integer, String> downClues) {
		mAcrossClues = acrossClues;
		mDownClues = downClues;
	}

	public static Clues createEmpty() {
		TreeMap<Integer, String> acrossClues = new TreeMap<Integer, String>();
		TreeMap<Integer, String> downClues = new TreeMap<Integer, String>();

		return new Clues(acrossClues, downClues);
	}

	/**
	 * Creates instance from given <code>StringTokenizer</code>.
	 * 
	 * @param data
	 * @return grid
	 */
	public static Clues deserialize(StringTokenizer data) {
		TreeMap<Integer, String> acrossClues = new TreeMap<Integer, String>();
		TreeMap<Integer, String> downClues = new TreeMap<Integer, String>();

		return new Clues(acrossClues, downClues);
	}

	/**
	 * Creates instance from given string (string which has been created by
	 * {@link #serialize(StringBuilder)} or {@link #serialize()} method).
	 * earlier.
	 * 
	 * @param data
	 */
	public static Clues deserialize(String data) {
		String[] lines = data.split("\n");
		if (lines.length == 0) {
			throw new IllegalArgumentException(
					"Cannot deserialize crossword, data corrupted.");
		}

		if (lines[0].equals("version: 1")) {
			StringTokenizer st = new StringTokenizer(lines[1], "|");
			return deserialize(st);
		} else {
			return fromString(data);
		}
	}

	/**
	 * Creates grid instance from given string. String is expected to be in
	 * format "1A1B1C00000000...", where each number represents cell color and
	 * each letter its entry.
	 * 
	 * @param data
	 * @return grid
	 */
	public static Clues fromString(String data) {
		TreeMap<Integer, String> acrossClues = new TreeMap<Integer, String>();
		TreeMap<Integer, String> downClues = new TreeMap<Integer, String>();

		return new Clues(acrossClues, downClues);
	}

	public String serialize() {
		StringBuilder sb = new StringBuilder();
		serialize(sb);
		return sb.toString();
	}

	/**
	 * Writes grid to given StringBuilder. You can later recreate the object
	 * instance by calling {@link #deserialize(String)} method.
	 * 
	 * @return
	 */
	public void serialize(StringBuilder data) {
		data.append("version: 1\n");

	}

}
