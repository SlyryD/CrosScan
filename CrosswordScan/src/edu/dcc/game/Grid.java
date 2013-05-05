package edu.dcc.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Grid of crossword cells.
 * 
 * @author Ryan
 */
public class Grid {

	// TODO: Get this value somehow
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

	// Cells in this grid.
	private Cell[][] mCells;

	// Clue numbers
	public int clueNum;

	// Helper arrays containing references to crossword entries
	private TreeMap<Integer, Entry> mAcrossEntries;
	private TreeMap<Integer, Entry> mDownEntries;
	private TreeMap<Integer, String> mAcrossClues;
	private TreeMap<Integer, String> mDownClues;

	private boolean mAcrossMode = true;

	private boolean mOnChangeEnabled = true;

	private final List<OnChangeListener> mChangeListeners = new ArrayList<OnChangeListener>();

	/**
	 * Create black and white grid from cells given
	 * 
	 * @param cells
	 */
	public Grid(int[][] cells) {
		mCells = new Cell[gridSize][gridSize];
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				mCells[row][col] = (cells[row][col] == 1) ? new Cell(true)
						: new Cell();
			}
		}
		initGrid();
		initClues();
	}

	/**
	 * Wraps given array in this object.
	 * 
	 * @param cells
	 */
	private Grid(Cell[][] cells) {
		mCells = cells;
		initGrid();
		initClues();
	}

	/**
	 * Initializes grid: Groups of cells that contain entries, row and column
	 * index for each cell.
	 */
	private void initGrid() {
		clueNum = 0;
		mAcrossEntries = new TreeMap<Integer, Entry>();
		mDownEntries = new TreeMap<Integer, Entry>();

		// Traverse grid
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				if (mCells[row][col].isWhite()) {
					// Entries to add white cell to
					Entry acrossEntry, downEntry;
					boolean firstCell = false;
					// Increment clueNum if necessary
					if (cellNeedsNewEntry(row, col)) {
						clueNum++;
						firstCell = true;
					}
					// Create Across and Down entries for white cell
					if (cellNeedsNewDownEntry(row, col)) {
						downEntry = new Entry(clueNum);
						mDownEntries.put(clueNum, downEntry);
					} else {
						downEntry = mCells[row - 1][col].getEntry(false);
					}
					if (cellNeedsNewAcrossEntry(row, col)) {
						acrossEntry = new Entry(clueNum);
						mAcrossEntries.put(clueNum, acrossEntry);
					} else {
						acrossEntry = mCells[row][col - 1].getEntry(true);
					}
					// Initialize cell index and add to entry
					mCells[row][col].initGrid(this, row, col,
							firstCell ? clueNum : 0, acrossEntry, downEntry);
				} else {
					mCells[row][col].initGrid(this, row, col, 0, null, null);
				}
			}
		}
		// TODO: Remove unnecessary print statements
		System.out.println("Across entries");
		for (Entry entry : mAcrossEntries.values()) {
			System.out.println(entry);
		}
		System.out.println("Down entries");
		for (Entry entry : mDownEntries.values()) {
			System.out.println(entry);
		}
	}

	private void initClues() {
		mAcrossClues = new TreeMap<Integer, String>();
		mDownClues = new TreeMap<Integer, String>();
		for (int clueNum : mAcrossEntries.keySet()) {
			mAcrossClues.put(clueNum, clueNum + "a. " + "ACROSS CLUE");
		}
		for (int clueNum : mDownEntries.keySet()) {
			mDownClues.put(clueNum, clueNum + "d. " + "DOWN CLUE");
		}
	}
	
	private boolean cellNeedsNewEntry(int row, int col) {
		return (cellNeedsNewDownEntry(row, col) || cellNeedsNewAcrossEntry(row,
				col));
	}

	private boolean cellNeedsNewDownEntry(int row, int col) {
		return (row == 0 || !mCells[row - 1][col].isWhite())
				&& (row + 1 < gridSize && mCells[row + 1][col].isWhite());
	}

	private boolean cellNeedsNewAcrossEntry(int row, int col) {
		return (col == 0 || !mCells[row][col - 1].isWhite())
				&& (col + 1 < gridSize && mCells[row][col + 1].isWhite());
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
	 * Return true, if no values in any cells.
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

	public int getNumClues() {
		return clueNum;
	}

	public Cell[][] getCells() {
		return mCells;
	}

	public Entry getEntry(int entryNum) {
		return mAcrossMode ? mAcrossEntries.get(entryNum) : mDownEntries.get(entryNum);
	}
	
	public String getClue(int clueNum, boolean acrossMode) {
		return acrossMode ? mAcrossClues.get(clueNum) : mDownClues.get(clueNum);
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
	 * format "1A1B1C00000000...", where each number represents cell color and
	 * each letter its entry.
	 * 
	 * @param data
	 * @return grid
	 */
	public static Grid fromString(String data) {
		Cell[][] cells = new Cell[gridSize][gridSize];

		int pos = 0;
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				char value = 0;
				Cell cell = data.charAt(pos++) == '1' ? new Cell(true)
						: new Cell();
				if ((data.charAt(pos) >= 65 && data.charAt(pos) <= 90)) {
					value = data.charAt(pos);
				}
				pos++;
				cell.setValue(value);
				cells[row][col] = cell;
				System.out.print(cell);
			}
			System.out.println();
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
			.compile("^\\d*$");
	private static Pattern DATA_PATTERN_VERSION_1 = Pattern
			.compile("^version: 1\\n((?#white)[01]\\|(?#value)\\w\\|)*$");

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
