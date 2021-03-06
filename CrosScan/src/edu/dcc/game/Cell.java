package edu.dcc.game;

import java.util.StringTokenizer;

import android.util.Log;
import edu.dcc.crosscan.Constants;

/**
 * A Crossword cell. Every cell is black or white, can contain a value, and can
 * contain a clue number. Cells belong to a Grid and have (row, col) indices and
 * entries associated with them.
 * 
 * @author Ryan
 */
public class Cell {
	// Information about grid and cell's position
	private Puzzle puzzle; // Final: puzzle
	private final Object mGridLock = new Object();
	private int mRow = -1; // Final: Row column
	private int mColumn = -1; // Final: Cell column
	private Entry mAcross; // Final: Row containing this cell
	private Entry mDown; // Final: Column containing this cell

	// Information about cell
	private int mClueNum = -1; // Final: number in cell
	private boolean mWhite; // Color of cell
	private char mValue;

	/**
	 * Creates black or white cell.
	 */
	public Cell(boolean white) {
		this(white, Constants.CHAR_SPACE);
	}

	private Cell(boolean white, char value) {
		if (value != Constants.CHAR_SPACE
				&& (value < Constants.CHAR_A || value > Constants.CHAR_Z)) {
			Log.e("Cell", "Value " + value + " (" + Integer.toString(value)
					+ ") not accepted");
			throw new IllegalArgumentException("Value" + value
					+ "must be a capital letter");
		}

		mValue = value;
		mWhite = white;
	}

	/**
	 * Gets cell's row index within {@link Grid}.
	 * 
	 * @return Cell's row index within Grid.
	 */
	public int getRow() {
		return mRow;
	}

	/**
	 * Gets cell's column index within {@link Grid}.
	 * 
	 * @return Cell's column index within Grid.
	 */
	public int getColumn() {
		return mColumn;
	}

	/**
	 * Called when <code>Cell</code> is added to {@link Grid}.
	 * 
	 * @param row
	 *            Cell's row index within collection.
	 * @param col
	 *            Cell's column index within collection.
	 * @param sector
	 *            Reference to sector group in which cell is included.
	 * @param across
	 *            Reference to row group in which cell is included.
	 * @param down
	 *            Reference to column group in which cell is included.
	 */
	public void initCell(Puzzle puzzle, int row, int col, int clueNum,
			Entry across, Entry down) {
		synchronized (mGridLock) {
			this.puzzle = puzzle;
		}

		mRow = row;
		mColumn = col;
		mClueNum = clueNum;
		mAcross = across;
		mDown = down;

		if (across != null) {
			across.addCell(this);
		}
		if (down != null) {
			down.addCell(this);
		}
	}

	/**
	 * Sets cell's value. Value must be a character or 0 if cell is empty.
	 * 
	 * @param value
	 *            Character or 0 if cell is empty.
	 */
	public void setValue(char value) {
		if ((value != Constants.CHAR_SPACE && value < Constants.CHAR_A)
				|| value > Constants.CHAR_Z) {
			throw new IllegalArgumentException("Value must be a character.");
		}
		mValue = value;
		onValueChange();
	}

	/**
	 * Gets cell's value. Value must be a character or 0 if cell is empty.
	 * 
	 * @return Cell's value. Character or 0 if cell is empty.
	 */
	public char getValue() {
		return mValue;
	}

	public Entry getEntry(boolean acrossMode) {
		if (acrossMode) {
			return mAcross;
		} else {
			return mDown;
		}
	}

	/**
	 * @return the mClueNum
	 */
	public int getClueNum() {
		return mClueNum;
	}

	/**
	 * @param clueNum
	 *            the clueNum to set
	 */
	public void setClueNum(int clueNum) {
		this.mClueNum = clueNum;
	}

	/**
	 * Returns whether cell is white.
	 * 
	 * @return True if cell is white.
	 */
	public boolean isWhite() {
		return mWhite;
	}

	public void toggleColor() {
		mWhite = !mWhite;
		onColorChange();
	}

	/**
	 * Creates instance from given <code>StringTokenizer</code>.
	 * 
	 * @param data
	 * @return
	 */
	public static Cell deserialize(StringTokenizer data) {
		String token = data.nextToken();
		return new Cell(token.charAt(0) == '1', token.charAt(1));
	}

	/**
	 * Creates instance from given string (string which has been created by
	 * {@link #serialize(StringBuilder)} or {@link #serialize()} method).
	 * earlier.
	 * 
	 * @param cellData
	 */
	public static Cell deserialize(String cellData) {
		StringTokenizer data = new StringTokenizer(cellData, "|");
		return deserialize(data);
	}

	/**
	 * Appends string representation of this object to the given
	 * <code>StringBuilder</code>. You can later recreate object from this
	 * string by calling {@link #deserialize}.
	 * 
	 * @param data
	 */
	public void serialize(StringBuilder data) {
		data.append(mWhite ? "1" : "0");
		data.append(mValue).append("|");
	}

	public String serialize() {
		StringBuilder sb = new StringBuilder();
		serialize(sb);
		return sb.toString();
	}

	/**
	 * Notify Grid that something has changed.
	 */
	private void onValueChange() {
		synchronized (mGridLock) {
			if (puzzle != null) {
				puzzle.onChange();
			}

		}
	}

	private void onColorChange() {
		synchronized (mGridLock) {
			if (puzzle != null) {
				puzzle.onColorChange();
			}

		}
	}

	public String toString() {
		return mWhite ? "(" + mValue + ")" : " X ";
	}
}
