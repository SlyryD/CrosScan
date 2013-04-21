package edu.dcc.grid;

/**
 * Represents group of cells that correspond to clue.
 * <p/>
 * This includes Across and Down clues.
 * 
 * @author Ryan
 */
public class Entry {
	private Cell[] mCells;
	private int mPos = 0, clueNum;
	private String clue;

	public Entry(int clueNum, String clue, int size) {
		this.mCells = new Cell[size];
		this.clueNum = clueNum;
		this.clue = clue;
	}

	public void addCell(Cell cell) {
		mCells[mPos] = cell;
		mPos++;
	}
	
	public Cell[] getCells() {
		return mCells;
	}

}
