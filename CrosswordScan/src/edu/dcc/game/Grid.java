package edu.dcc.game;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Grid of crossword cells.
 * 
 * @author Ryan
 */
public class Grid {

	/**
	 * String is expected to be in format "00|00|2343243202...", where each
	 * number represents cell value, no other information can be set using this
	 * method.
	 */
	public static int DATA_VERSION_PLAIN = 0;

	/**
	 * See {@link #DATA_PATTERN_VERSION_1} and {@link #serialize()}.
	 */
	public static int DATA_VERSION_1 = 1;

	// Grid size
	private int mGridSize = 13;

	private Cell mFirstWhiteCell;

	// Cells in this grid
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
	public Grid(int gridSize, int[][] cells, List<String> clues) {
		this.mGridSize = gridSize;
		mCells = new Cell[gridSize][gridSize];
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				mCells[row][col] = (cells[row][col] == 1) ? new Cell(true)
						: new Cell();
			}
		}
		System.out.println(this);
		initGrid();
		initClues(clues);
	}

	/**
	 * Wraps given array in this object.
	 * 
	 * @param cells
	 */
	private Grid(int gridSize, Cell[][] cells, List<String> clues) {
		this.mGridSize = gridSize;
		mCells = cells;
		initGrid();
		initClues(clues);
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
		for (int row = 0; row < mGridSize; row++) {
			for (int col = 0; col < mGridSize; col++) {
				if (mCells[row][col].isWhite()) {
					// Entries to add white cell to
					Entry acrossEntry = null, downEntry = null;
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
					} else if (row != 0 && mCells[row - 1][col].isWhite()) {
						downEntry = mCells[row - 1][col].getEntry(false);
					}
					if (cellNeedsNewAcrossEntry(row, col)) {
						acrossEntry = new Entry(clueNum);
						mAcrossEntries.put(clueNum, acrossEntry);
					} else if (col != 0 && mCells[row][col - 1].isWhite()) {
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

	private void initClues(List<String> clues) {
		mAcrossClues = new TreeMap<Integer, String>();
		mDownClues = new TreeMap<Integer, String>();
		if (clues == null) {
			for (int clueNum : mAcrossEntries.keySet()) {
				mAcrossClues.put(clueNum, "ACROSS CLUE");
			}
			for (int clueNum : mDownEntries.keySet()) {
				mDownClues.put(clueNum, "DOWN CLUE");
			}
		} else {
			int i = 0;
			for (int clueNum : mAcrossEntries.keySet()) {
				mAcrossClues.put(clueNum, clues.get(i++));
			}
			for (int clueNum : mDownEntries.keySet()) {
				mDownClues.put(clueNum, clues.get(i++));
			}
		}
	}

	private boolean cellNeedsNewEntry(int row, int col) {
		return (cellNeedsNewDownEntry(row, col) || cellNeedsNewAcrossEntry(row,
				col));
	}

	private boolean cellNeedsNewDownEntry(int row, int col) {
		return (row == 0 || !mCells[row - 1][col].isWhite())
				&& (row + 1 < mGridSize && mCells[row + 1][col].isWhite());
	}

	private boolean cellNeedsNewAcrossEntry(int row, int col) {
		return (col == 0 || !mCells[row][col - 1].isWhite())
				&& (col + 1 < mGridSize && mCells[row][col + 1].isWhite());
	}

	public int getGridSize() {
		return mGridSize;
	}

	public Cell getFirstWhiteCell() {
		if (mFirstWhiteCell != null) {
			return mFirstWhiteCell;
		}
		for (int i = 0; i < mGridSize; i++) {
			for (int j = 0; j < mGridSize; j++) {
				if (mCells[i][j].isWhite()) {
					return (mFirstWhiteCell = mCells[i][j]);
				}
			}
		}
		return null;
	}

	public Cell[][] getCells() {
		return mCells;
	}

	public int getNumClues() {
		return clueNum;
	}

	public Entry getEntry(int entryNum) {
		return mAcrossMode ? mAcrossEntries.get(entryNum) : mDownEntries
				.get(entryNum);
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
	 * Returns number white squares with values
	 * 
	 * @return valueCount
	 */
	public int getValueCount() {
		int valueCount = 0;

		for (int row = 0; row < mGridSize; row++) {
			for (int col = 0; col < mGridSize; col++) {
				char value = getCell(row, col).getValue();
				if (value != 0) {
					valueCount++;
				}
			}
		}

		return valueCount;
	}

	public int getWhiteCount() {
		int whiteCount = 0;

		for (int row = 0; row < mGridSize; row++) {
			for (int col = 0; col < mGridSize; col++) {
				if (getCell(row, col).isWhite()) {
					whiteCount++;
				}
			}
		}

		return whiteCount;
	}

	public float getComplete() {
		return ((float) getValueCount()) / ((float) getWhiteCount());
	}

	/**
	 * Creates instance from given <code>StringTokenizer</code>.
	 * 
	 * @param data
	 * @return grid
	 */
	public static Grid deserialize(StringTokenizer data) {
		int gridSize = Integer.parseInt(data.nextToken());
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

		if (!data.hasMoreTokens()) {
			return new Grid(gridSize, cells, null);
		}

		ArrayList<String> clues = new ArrayList<String>();
		while (data.hasMoreTokens()) {
			clues.add(data.nextToken());
		}

		return new Grid(gridSize, cells, clues);
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
			return deserialize(new StringTokenizer(lines[1], "|"));
		} else {
			return fromString(new StringTokenizer(lines[0], "|"));
		}
	}

	/**
	 * Creates grid instance from given string. String is expected to be in
	 * format "1A|1B|1C|00|00|00|00|...|", where each number represents cell
	 * color and each letter its entry.
	 * 
	 * @param data
	 * @return grid
	 */
	public static Grid fromString(StringTokenizer data) {
		int gridSize = Integer.parseInt(data.nextToken());
		Cell[][] cells = new Cell[gridSize][gridSize];

		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				String datum = data.nextToken();
				char color = datum.charAt(0), value = 0;
				Cell cell = color == '1' ? new Cell(true) : new Cell();
				if ((datum.charAt(1) >= 65 && datum.charAt(1) <= 90)) {
					value = datum.charAt(1);
				}
				cell.setValue(value);
				cells[row][col] = cell;
				System.out.print(cell);
			}
			System.out.println();
		}

		if (!data.hasMoreTokens()) {
			return new Grid(gridSize, cells, null);
		}

		ArrayList<String> clues = new ArrayList<String>();
		while (data.hasMoreTokens()) {
			clues.add(data.nextToken());
		}

		return new Grid(gridSize, cells, clues);
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
		data.append(mGridSize + "|");
		for (int row = 0; row < mGridSize; row++) {
			for (int col = 0; col < mGridSize; col++) {
				mCells[row][col].serialize(data);
			}
		}
		for (String clue : mAcrossClues.values()) {
			data.append(clue).append("|");
		}
		for (String clue : mDownClues.values()) {
			data.append(clue).append("|");
		}
	}

	private static Pattern DATA_PATTERN_VERSION_PLAIN = Pattern
			.compile("^\\d*$");
	private static Pattern DATA_PATTERN_VERSION_1 = Pattern
			.compile("^version: 1\\n(?#grid_size)\\d\\|((?#white)[01]\\|(?#value)\\w\\|)*$");

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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int row = 0; row < mGridSize; row++) {
			for (int col = 0; col < mGridSize; col++) {
				sb.append(mCells[row][col]);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public interface OnChangeListener {
		/**
		 * Called when anything in the collection changes (cell's value, note,
		 * etc.)
		 */
		void onChange();
	}
}
