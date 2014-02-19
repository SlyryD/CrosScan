package edu.dcc.game;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import android.util.Log;

/**
 * Consists of grid and clues
 * 
 * @author Ryan
 */
public class Puzzle {

	// Default values
	public static final int DEFAULT_SIZE = 13;

	// Puzzle grid
	private final Cell[][] grid; // Cells in grid
	private final int height, width; // Grid size
	private Cell firstWhiteCell; // First white cell

	// Puzzle clues
	private TreeMap<Integer, String> acrossClues;
	private TreeMap<Integer, String> downClues;

	// Puzzle entries (groups of cells)
	private TreeMap<Integer, Entry> acrossEntries;
	private TreeMap<Integer, Entry> downEntries;
	private int numEntries; // Number of entries

	// Puzzle change listeners
	private final List<OnChangeListener> changeListeners = new ArrayList<OnChangeListener>();

	// Data validation fields
	/**
	 * String is expected to be in format "00|00|10...", where each number
	 * represents cell color or value
	 */
	public static int DATA_VERSION_PLAIN = 0;
	/**
	 * See {@link #DATA_PATTERN_VERSION_1} and {@link #serialize()}.
	 */
	public static int DATA_VERSION_1 = 1;
	private static Pattern DATA_PATTERN_VERSION_PLAIN = Pattern
			.compile("^\\d*$");
	private static Pattern DATA_PATTERN_VERSION_1 = Pattern
			.compile("^version: 1\\n(?#grid_size)\\d\\|((?#white)[01]\\|(?#value)\\w\\|)*$");

	/* Constructors */

	/**
	 * Construct puzzle from given grid and clues
	 * 
	 * @param size
	 * @param grid
	 * @param clues
	 */
	public Puzzle(int height, int width, Cell[][] grid, List<String> clues) {
		// Initialize grid and entries
		this.height = height;
		this.width = width;
		this.grid = grid;
		initGrid();

		// Initialize clues
		initClues(clues);

		Log.i("CrosswordScan/Puzzle", "Puzzle instantiated");
	}

	/**
	 * Construct puzzle from given integer array and clues
	 * 
	 * @param gridSize
	 * @param cells
	 * @param clues
	 */
	public Puzzle(int height, int width, int[][] cells, List<String> clues) {
		this(height, width, convertIntsToCells(height, width, cells), clues);
	}

	/**
	 * Converts int array to cell array
	 * 
	 * @param gridSize
	 * @param ints
	 * @return cells
	 */
	private static Cell[][] convertIntsToCells(int height, int width,
			int[][] ints) {
		// Initialize cell array
		Cell[][] cells = new Cell[height][width];
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				cells[row][col] = new Cell(ints[row][col] == 1);
			}
		}
		return cells;
	}

	/**
	 * Initializes grid: Groups of cells that contain entries, row and column
	 * index for each cell.
	 * 
	 * @return number of entries
	 */
	private void initGrid() {
		acrossEntries = new TreeMap<Integer, Entry>();
		downEntries = new TreeMap<Integer, Entry>();

		// Track entry number
		int entryNum = 0;
		// Traverse grid
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (grid[row][col].isWhite()) {
					// Entries to add white cell to
					Entry acrossEntry = null, downEntry = null;
					boolean firstCell = false;
					// Increment numClues if necessary
					if (cellNeedsNewEntry(row, col)) {
						entryNum++;
						firstCell = true;
					}
					// Create Across and Down entries for white cell
					if (cellNeedsNewDownEntry(row, col)) {
						downEntry = new Entry(entryNum);
						downEntries.put(entryNum, downEntry);
					} else if (row != 0 && grid[row - 1][col].isWhite()) {
						downEntry = grid[row - 1][col].getEntry(false);
					}
					if (cellNeedsNewAcrossEntry(row, col)) {
						acrossEntry = new Entry(entryNum);
						acrossEntries.put(entryNum, acrossEntry);
					} else if (col != 0 && grid[row][col - 1].isWhite()) {
						acrossEntry = grid[row][col - 1].getEntry(true);
					}
					// Initialize cell index and add to entry
					grid[row][col].initCell(this, row, col,
							firstCell ? entryNum : 0, acrossEntry, downEntry);
				} else {
					grid[row][col].initCell(this, row, col, 0, null, null);
				}
			}
		}
		numEntries = entryNum;
		firstWhiteCell = findFirstWhiteCell();
	}

	/**
	 * Initializes clues from given list
	 * 
	 * @param clues
	 */
	private void initClues(List<String> clues) {
		acrossClues = new TreeMap<Integer, String>();
		downClues = new TreeMap<Integer, String>();

		if (clues == null) {
			for (int clueNum : acrossEntries.keySet()) {
				acrossClues.put(clueNum, "ACROSS CLUE");
			}
			for (int clueNum : downEntries.keySet()) {
				downClues.put(clueNum, "DOWN CLUE");
			}
		} else {
			int i = 0;
			for (int clueNum : acrossEntries.keySet()) {
				acrossClues.put(clueNum, clues.get(i++));
			}
			for (int clueNum : downEntries.keySet()) {
				downClues.put(clueNum, clues.get(i++));
			}
		}
	}

	/**
	 * Returns whether cell is start of new entry
	 * 
	 * @param row
	 * @param col
	 * @return needs new entry
	 */
	private boolean cellNeedsNewEntry(int row, int col) {
		return (cellNeedsNewDownEntry(row, col) || cellNeedsNewAcrossEntry(row,
				col));
	}

	/**
	 * Returns whether cell is start of new down entry
	 * 
	 * @param row
	 * @param col
	 * @return needs new down entry
	 */
	private boolean cellNeedsNewDownEntry(int row, int col) {
		return (row == 0 || !grid[row - 1][col].isWhite())
				&& (row + 1 < height && grid[row + 1][col].isWhite());
	}

	/**
	 * Returns whether cell is start of new across entry
	 * 
	 * @param row
	 * @param col
	 * @return needs new across entry
	 */
	private boolean cellNeedsNewAcrossEntry(int row, int col) {
		return (col == 0 || !grid[row][col - 1].isWhite())
				&& (col + 1 < width && grid[row][col + 1].isWhite());
	}

	/**
	 * Returns grid height
	 * 
	 * @return height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Returns grid width
	 * 
	 * @return width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Returns array of grid cells
	 * 
	 * @return grid
	 */
	public Cell[][] getGrid() {
		return grid;
	}

	/**
	 * Returns cell from grid with given indices
	 * 
	 * @param row
	 * @param col
	 * @return cell
	 */
	public Cell getCell(int row, int col) {
		return grid[row][col];
	}

	/**
	 * Returns first white cell
	 * 
	 * @return firstWhiteCell
	 */
	public Cell getFirstWhiteCell() {
		return firstWhiteCell;
	}

	/**
	 * Finds first white cell
	 * 
	 * @return firstWhiteCell
	 */
	private Cell findFirstWhiteCell() {
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (grid[row][col].isWhite()) {
					return grid[row][col];
				}
			}
		}
		return null;
	}

	/**
	 * Returns number of entries in grid
	 * 
	 * @return numEntries
	 */
	public int getNumEntries() {
		return numEntries;
	}

	/**
	 * Returns entry based on across mode
	 * 
	 * @param entryNum
	 * @return entry
	 */
	public Entry getEntry(int entryNum, boolean acrossMode) {
		return acrossMode ? acrossEntries.get(entryNum) : downEntries
				.get(entryNum);
	}

	/**
	 * Returns across entries
	 * 
	 * @return acrossEntries
	 */
	public TreeMap<Integer, Entry> getAcrossEntries() {
		return acrossEntries;
	}

	/**
	 * Returns down entries +**
	 * 
	 * @return downEntries
	 */
	public TreeMap<Integer, Entry> getDownEntries() {
		return downEntries;
	}

	public String getClue(int clueNum, boolean acrossMode) {
		return acrossMode ? acrossClues.get(clueNum) : downClues.get(clueNum);
	}

	/**
	 * Return percentage complete
	 * 
	 * @return completion
	 */
	public float getCompletion() {
		return ((float) getValueCount()) / ((float) getWhiteCount());
	}

	/**
	 * Returns number white squares with values
	 * 
	 * @return valueCount
	 */
	private int getValueCount() {
		int valueCount = 0;

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				char value = grid[row][col].getValue();
				if (value != 0) {
					valueCount++;
				}
			}
		}

		return valueCount;
	}

	// TODO: Make whiteCount final instance variable?
	/**
	 * Returns number of white cells in grid
	 * 
	 * @return whiteCount
	 */
	private int getWhiteCount() {
		int whiteCount = 0;

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (grid[row][col].isWhite()) {
					whiteCount++;
				}
			}
		}

		return whiteCount;
	}

	/**
	 * Resets puzzle grid
	 */
	public void reset() {
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				grid[row][col].setValue((char) 0);
			}
		}

	}

	/*
	 * Serialization Methods
	 */

	/**
	 * Creates instance from given string (string which has been created by
	 * {@link #serialize(StringBuilder)} or {@link #serialize()} method).
	 * earlier.
	 * 
	 * @param data
	 * @return puzzle
	 */
	public static Puzzle deserialize(String data) {
		Log.i("Puzzle", data.toString());
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
	 * Creates instance from given <code>StringTokenizer</code>.
	 * 
	 * @param data
	 * @return puzzle
	 */
	public static Puzzle deserialize(StringTokenizer data) {
		// Get grid size
		int height = Integer.parseInt(data.nextToken());
		int width = Integer.parseInt(data.nextToken());

		// Get grid
		Cell[][] grid = new Cell[height][width];
		int row = 0, col = 0;
		while (data.hasMoreTokens() && row < height) {
			grid[row][col++] = Cell.deserialize(data);

			if (col == width) {
				row++;
				col = 0;
			}
		}

		// Get clues
		if (!data.hasMoreTokens()) {
			return new Puzzle(height, width, grid, null);
		}
		ArrayList<String> clues = new ArrayList<String>();
		while (data.hasMoreTokens()) {
			clues.add(data.nextToken());
		}

		return new Puzzle(height, width, grid, clues);
	}

	/**
	 * Creates grid instance from given string. String is expected to be in
	 * format "1A|1B|1C|00|00|00|00|...|", where each number represents cell
	 * color and each letter its entry.
	 * 
	 * @param data
	 * @return grid
	 */
	public static Puzzle fromString(StringTokenizer data) {
		// Get grid size
		int height = Integer.parseInt(data.nextToken());
		int width = Integer.parseInt(data.nextToken());

		// Get grid
		Cell[][] grid = new Cell[height][width];
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				String token = data.nextToken();
				char color = token.charAt(0), value = 0;
				Cell cell = new Cell(color == '1');
				if ((token.charAt(1) >= 65 && token.charAt(1) <= 90)) {
					value = token.charAt(1);
				}
				cell.setValue(value);
				grid[row][col] = cell;
			}
		}

		// Get clues
		if (!data.hasMoreTokens()) {
			return new Puzzle(height, width, grid, null);
		}
		ArrayList<String> clues = new ArrayList<String>();
		while (data.hasMoreTokens()) {
			clues.add(data.nextToken());
		}

		return new Puzzle(height, width, grid, clues);
	}

	/**
	 * Serialize puzzle
	 * 
	 * @return string
	 */
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
		data.append(height + "|");
		data.append(width + "|");
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				grid[row][col].serialize(data);
			}
		}
		for (String clue : acrossClues.values()) {
			data.append(clue).append("|");
		}
		for (String clue : downClues.values()) {
			data.append(clue).append("|");
		}
	}

	/*
	 * Change listener methods
	 */

	/**
	 * Add listener to list
	 * 
	 * @param listener
	 */
	public void addOnChangeListener(OnChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("The listener is null.");
		}
		synchronized (changeListeners) {
			if (changeListeners.contains(listener)) {
				throw new IllegalStateException("Listener " + listener
						+ "is already registered.");
			}
			changeListeners.add(listener);
		}
	}

	/**
	 * Remove listener from list
	 * 
	 * @param listener
	 */
	public void removeOnChangeListener(OnChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("The listener is null.");
		}
		synchronized (changeListeners) {
			if (!changeListeners.contains(listener)) {
				throw new IllegalStateException("Listener " + listener
						+ " was not registered.");
			}
			changeListeners.remove(listener);
		}
	}

	/**
	 * Notify all registered listeners that something has changed.
	 */
	protected void onChange() {
		synchronized (changeListeners) {
			for (OnChangeListener l : changeListeners) {
				l.onChange();
			}
		}
	}

	/**
	 * Notify all registered listeners that something has changed.
	 */
	protected void onColorChange() {
		synchronized (changeListeners) {
			initGrid();
			onChange();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				sb.append(grid[row][col]);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Listener interface
	 * 
	 * @author Ryan
	 */
	public interface OnChangeListener {
		/**
		 * Called when anything in the collection changes (cell's value, note,
		 * etc.)
		 */
		void onChange();
	}

	/*
	 * Data validation methods
	 */

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

}
