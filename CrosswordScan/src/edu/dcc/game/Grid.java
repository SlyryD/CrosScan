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
	public int acrossClueNum, downClueNum;

	// Helper arrays containing references to crossword entries
	private ArrayList<Entry> mAcross;
	private ArrayList<Entry> mDown;

	private boolean mAcrossMode;

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
	}

	/**
	 * Initializes grid: Groups of cells that contain entries, row and column
	 * index for each cell.
	 */
	private void initGrid() {
		acrossClueNum = downClueNum = 0;
		mAcross = new ArrayList<Entry>();
		mDown = new ArrayList<Entry>();

		// Traverse grid
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				if (mCells[row][col].isWhite()) {
					// Create Across and Down entries for white cell
					if (row == 0 || !mCells[row - 1][col].isWhite()) {
						downClueNum++;
						mDown.add(new Entry(downClueNum));
					}
					if (col == 0 || !mCells[row][col - 1].isWhite()) {
						acrossClueNum++;
						mAcross.add(new Entry(acrossClueNum));
					}
					// Initialize cell index and add to entry
					mCells[row][col].initGrid(this, row, col,
							mAcross.get(acrossClueNum), mDown.get(downClueNum));
				} else {
					mCells[row][col].initGrid(this, row, col, null, null);
				}
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
		Cell[][] cells = new Cell[gridSize][gridSize];

		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				char value = 0;
				for (int pos = 0; pos < data.length(); pos++) {
					if ((data.charAt(pos) >= 65 && data.charAt(pos) <= 90)
							|| data.charAt(pos) == 0) {
						value = data.charAt(pos);
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
