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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Grid of crossword cells.
 * 
 * @author Ryan
 */
public class Grid {

	// TODO: Get these values somehow
	public static final int gridSize = 13;

	/**
	 * String is expected to be in format "00002343243202...", where each number
	 * represents cell value, no other information can be set using this method.
	 */
	public static int DATA_VERSION_PLAIN = 0;

	/**
	 * See {@link #DATA_PATTERN_VERSION_1} and {@link #serialize()}.
	 */
	public static int DATA_VERSION_1 = 1;

	public static int acrossClueNum = 1, downClueNum = 1;

	// Cells in this grid.
	private Cell[][] mCells;

	// Helper arrays containing references to crossword entries
	private Entry[] mAcross;
	private Entry[] mDown;

	private boolean mAcrossMode;

	private boolean mOnChangeEnabled = true;

	private final List<OnChangeListener> mChangeListeners = new ArrayList<OnChangeListener>();

	public Grid(int size, int[][] cells) {
		// this.gridSize = size;
		mCells = new Cell[size][size];
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				mCells[row][col] = (cells[row][col] == 1) ? new Cell(true)
						: new Cell();
			}
		}
	}

	/**
	 * Creates empty crossword.
	 * 
	 * @return empty grid
	 */
	public static Grid createEmpty() {
		Cell[][] cells = new Cell[gridSize][gridSize];

		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				cells[row][col] = new Cell();
			}
		}

		return new Grid(cells);
	}

	/**
	 * Return true, if no value is entered in any of cells.
	 * 
	 * @return empty
	 */
	public boolean isEmpty() {
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				Cell cell = mCells[row][col];
				if (cell.getValue() != 0)
					return false;
			}
		}
		return true;
	}

	/**
	 * Generates debug game.
	 * 
	 * @return
	 */
	public static Grid createDebugGame() {
		Grid debugGame = new Grid(new Cell[][] {
				{ new Cell(true), new Cell(true), new Cell(true), new Cell(),
						new Cell(), new Cell(true), new Cell(true),
						new Cell(true), new Cell(true), },
				{ new Cell(true), new Cell(true), new Cell(true),
						new Cell(true), new Cell(), new Cell(true),
						new Cell(true), new Cell(true), new Cell(true), },
				{ new Cell(true), new Cell(true), new Cell(true),
						new Cell(true), new Cell(), new Cell(true),
						new Cell(true), new Cell(true), new Cell(true), },
				{ new Cell(true), new Cell(true), new Cell(true),
						new Cell(true), new Cell(true), new Cell(true),
						new Cell(true), new Cell(), new Cell(true), },
				{ new Cell(), new Cell(), new Cell(), new Cell(true),
						new Cell(true), new Cell(true), new Cell(),
						new Cell(true), new Cell(true), },
				{ new Cell(true), new Cell(true), new Cell(true),
						new Cell(true), new Cell(true), new Cell(),
						new Cell(true), new Cell(true), new Cell(true), },
				{ new Cell(true), new Cell(true), new Cell(true), new Cell(),
						new Cell(), new Cell(true), new Cell(true),
						new Cell(true), new Cell(), },
				{ new Cell(true), new Cell(true), new Cell(true),
						new Cell(true), new Cell(true), new Cell(true),
						new Cell(true), new Cell(), new Cell(), },
				{ new Cell(), new Cell(), new Cell(), new Cell(true),
						new Cell(true), new Cell(true), new Cell(), new Cell(),
						new Cell(), } });
		return debugGame;
	}

	public Cell[][] getCells() {
		return mCells;
	}

	/**
	 * @return the acrossMode
	 */
	public boolean isAcrossMode() {
		return mAcrossMode;
	}

	/**
	 * @param acrossMode
	 *            the acrossMode to set
	 */
	public void setAcrossMode(boolean acrossMode) {
		this.mAcrossMode = acrossMode;
	}

	/**
	 * Wraps given array in this object.
	 * 
	 * @param cells
	 */
	private Grid(Cell[][] cells) {
		mCells = cells;
		initGrid();
	}

	/**
	 * Gets cell at given position.
	 * 
	 * @param row
	 * @param col
	 * @return cell
	 */
	public Cell getCell(int row, int col) {
		return mCells[row][col];
	}

	/**
	 * Returns how many times each value is used in <code>Grid</code>. Returns
	 * map with entry for each value.
	 * 
	 * @return valueCount
	 */
	public Map<Character, Integer> getValueCount() {
		Map<Character, Integer> valueCount = new HashMap<Character, Integer>();
		for (char value = 65; value <= 90; value++) {
			valueCount.put(value, 0);
		}

		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				char value = getCell(row, col).getValue();
				if (value != 0) {
					valueCount.put(value, valueCount.get(value) + 1);
				}
			}
		}

		return valueCount;
	}

	/**
	 * Initializes grid: 1) 2) Groups of cells which must contain unique numbers
	 * are created. 3) Row and column index for each cell is set.
	 */
	private void initGrid() {

		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				mCells[row][col].initGrid(this, row, col, mAcross[col],
						mDown[row]);
			}
		}

		// TODO: Num of Across and down clues not dependent on grid size
		mAcross = new Entry[gridSize];
		mDown = new Entry[gridSize];

		for (int i = 0; i < gridSize; i++) {
			mAcross[i] = new Entry(acrossClueNum++);
		}

		for (int i = 0; i < gridSize; i++) {
			mDown[i] = new Entry(downClueNum++);
		}

		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				mCells[row][col].initGrid(this, row, col, mAcross[col],
						mDown[row]);
			}
		}
	}

	/**
	 * Creates instance from given <code>StringTokenizer</code>.
	 * 
	 * @param data
	 * @return grid
	 */
	public static Grid deserialize(StringTokenizer data) {
		Cell[][] cells = new Cell[gridSize][gridSize];

		int row = 0, col = 0;
		while (data.hasMoreTokens() && row < gridSize) {
			cells[row][col] = Cell.deserialize(data);
			col++;

			if (col == gridSize) {
				row++;
				col = 0;
			}
		}

		return new Grid(cells);
	}

	/**
	 * Creates instance from given string (string which has been created by
	 * {@link #serialize(StringBuilder)} or {@link #serialize()} method).
	 * earlier.
	 * 
	 * @param data
	 */
	public static Grid deserialize(String data) {
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
	 * format "00002343243202...", where each number represents cell value, no
	 * other information can be set using this method.
	 * 
	 * @param data
	 * @return grid
	 */
	public static Grid fromString(String data) {
		// TODO: validate

		Cell[][] cells = new Cell[gridSize][gridSize];

		int pos = 0;
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				char value = 0;
				while (pos < data.length()) {
					pos++;
					if (data.charAt(pos - 1) >= '0'
							&& data.charAt(pos - 1) <= '9') {
						// value=Integer.parseInt(data.substring(pos-1, pos));
						value = data.charAt(pos - 1);
						break;
					}
				}
				Cell cell = new Cell();
				cell.setValue(value);
				cell.setWhite(value == 0);
				cells[row][col] = cell;
			}
		}

		return new Grid(cells);
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

		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				mCells[row][col].serialize(data);
			}
		}
	}

	private static Pattern DATA_PATTERN_VERSION_PLAIN = Pattern
			.compile("^\\d{81}$");
	private static Pattern DATA_PATTERN_VERSION_1 = Pattern
			.compile("^version: 1\\n((?#value)\\d\\|(?#note)((\\d,)+|-)\\|(?#editable)[01]\\|){0,81}$");

	/**
	 * Returns true, if given <code>data</code> conform to format of given data
	 * version.
	 * 
	 * @param data
	 * @param dataVersion
	 * @return
	 */
	public static boolean isValid(String data, int dataVersion) {
		if (dataVersion == DATA_VERSION_PLAIN) {
			return DATA_PATTERN_VERSION_PLAIN.matcher(data).matches();
		} else if (dataVersion == DATA_VERSION_1) {
			return DATA_PATTERN_VERSION_1.matcher(data).matches();
		} else {
			throw new IllegalArgumentException("Unknown version: "
					+ dataVersion);
		}
	}

	public void addOnChangeListener(OnChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("The listener is null.");
		}
		synchronized (mChangeListeners) {
			if (mChangeListeners.contains(listener)) {
				throw new IllegalStateException("Listener " + listener
						+ "is already registered.");
			}
			mChangeListeners.add(listener);
		}
	}

	public void removeOnChangeListener(OnChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("The listener is null.");
		}
		synchronized (mChangeListeners) {
			if (!mChangeListeners.contains(listener)) {
				throw new IllegalStateException("Listener " + listener
						+ " was not registered.");
			}
			mChangeListeners.remove(listener);
		}
	}

	/**
	 * Returns whether change notification is enabled.
	 * 
	 * If true, change notifications are distributed to the listeners registered
	 * by {@link #addOnChangeListener(OnChangeListener)}.
	 * 
	 * @return
	 */
	// public boolean isOnChangeEnabled() {
	// return mOnChangeEnabled;
	// }
	//
	// /**
	// * Enables or disables change notifications, that are distributed to the
	// listeners
	// * registered by {@link #addOnChangeListener(OnChangeListener)}.
	// *
	// * @param onChangeEnabled
	// */
	// public void setOnChangeEnabled(boolean onChangeEnabled) {
	// mOnChangeEnabled = onChangeEnabled;
	// }

	/**
	 * Notify all registered listeners that something has changed.
	 */
	protected void onChange() {
		if (mOnChangeEnabled) {
			synchronized (mChangeListeners) {
				for (OnChangeListener l : mChangeListeners) {
					l.onChange();
				}
			}
		}
	}

	public interface OnChangeListener {
		/**
		 * Called when anything in the collection changes (cell's value, note,
		 * etc.)
		 */
		void onChange();
	}
}
