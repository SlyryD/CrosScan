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

package edu.dcc.grid;

import java.util.StringTokenizer;

/**
 * Crossword cell. Every cell has value, some notes attached to it and some
 * basic state (whether it is white and valid).
 * 
 * @author Ryan
 */
public class Cell {
	// Information about grid and cell's position
	private Grid mGrid;
	private final Object mGridLock = new Object();
	private int mRow = -1;
	private int mColumn = -1;
	private Entry mAcross; // row containing this cell
	private Entry mDown; // column containing this cell
	// Information about cell
	private char mValue;
	private int mClueNum; // TODO: Implement clue num
	private boolean mWhite;

	/**
	 * Creates black cell.
	 */
	public Cell() {
		this((char) 0, 0, false);
	}

	/**
	 * Creates black or white cell.
	 */
	public Cell(boolean white) {
		this((char) 0, 0, white);
	}

	/**
	 * Creates white cell containing given value.
	 * 
	 * @param value
	 *            Value of the cell.
	 */
	public Cell(char value) {
		this(value, 0, true);
	}

	/**
	 * Creates white cell containing given value and clue number.
	 * 
	 * @param value
	 *            Value of the cell.
	 * @param clueNum
	 *            Clue number of cell.
	 */
	public Cell(char value, int clueNum) {
		this(value, clueNum, true);
	}

	private Cell(char value, int clueNum, boolean white) {
		if (value != 0 || value < 65 || value > 90) {
			throw new IllegalArgumentException("Value must be a character.");
		}

		mValue = value;
		mClueNum = clueNum;
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
	protected void initGrid(Grid grid, int row, int col, Entry across,
			Entry down) {
		synchronized (mGridLock) {
			mGrid = grid;
		}

		mRow = row;
		mColumn = col;
		mAcross = across;
		mDown = down;

		across.addCell(this);
		down.addCell(this);
	}

	/**
	 * Returns row containing this cell.
	 * 
	 * @return Row containing this cell.
	 */
	public Entry getAcross() {
		return mAcross;
	}

	/**
	 * Returns column containing this cell.
	 * 
	 * @return Column containing this cell.
	 */
	public Entry getDown() {
		return mDown;
	}

	/**
	 * Sets cell's value. Value must be a character or 0 if cell is empty.
	 * 
	 * @param value
	 *            Character or 0 if cell is empty.
	 */
	public void setValue(char value) {
		if (value != 0 || value < 65 || value > 90) {
			throw new IllegalArgumentException("Value must be a character.");
		}
		mValue = value;
		onChange();
	}

	/**
	 * Gets cell's value. Value must be a character or 0 if cell is empty.
	 * 
	 * @return Cell's value. Character or 0 if cell is empty.
	 */
	public char getValue() {
		return mValue;
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

	/**
	 * Sets whether cell is white.
	 * 
	 * @param white
	 *            True, if cell is white.
	 */
	public void setWhite(boolean white) {
		mWhite = white;
		onChange();
	}

	/**
	 * Creates instance from given <code>StringTokenizer</code>.
	 * 
	 * @param data
	 * @return
	 */
	public static Cell deserialize(StringTokenizer data) {
		Cell cell = new Cell();
		cell.setValue(data.nextToken().charAt(0));
		cell.setClueNum(Integer.parseInt(data.nextToken()));
		cell.setWhite(data.nextToken().equals("1"));

		return cell;
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
		data.append(mValue).append("|");
		if (mClueNum == 0) {
			data.append("-").append("|");
		} else {
			data.append(mClueNum).append("|");
		}
		data.append(mWhite ? "1" : "0").append("|");
	}

	public String serialize() {
		StringBuilder sb = new StringBuilder();
		serialize(sb);
		return sb.toString();
	}

	/**
	 * Notify Grid that something has changed.
	 */
	private void onChange() {
		synchronized (mGridLock) {
			if (mGrid != null) {
				mGrid.onChange();
			}

		}
	}
}
