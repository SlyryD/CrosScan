package edu.dcc.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents group of cells that correspond to clue.
 * <p/>
 * This includes Across and Down clues.
 * 
 * @author Ryan
 */
public class Entry {
	private ArrayList<Cell> mCells = new ArrayList<Cell>();
	private int clueNum, size;
	private String clue = null;

	public Entry(int clueNum) {
		this.clueNum = clueNum;
		this.size = 0;
	}

	public Entry(int clueNum, String clue) {
		this.clueNum = clueNum;
		this.clue = clue;
		this.size = 0;
	}

	public void addCell(Cell cell) {
		mCells.add(cell);
		size++;
	}

	public List<Cell> getCells() {
		return mCells;
	}

	public int getClueNum() {
		return clueNum;
	}

	/**
	 * @return size
	 */
	public int getSize() {
		return size;
	}

}
