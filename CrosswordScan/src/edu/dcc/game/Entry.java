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
	private int mPos = 0, clueNum;
	private String clue;

	public Entry(int clueNum) {
		this.clueNum = clueNum;
	}

	public Entry(int clueNum, String clue) {
		this.clueNum = clueNum;
		this.clue = clue;
	}

	public void addCell(Cell cell) {
		mCells.add(cell);
		mPos++;
	}

	public List<Cell> getCells() {
		return mCells;
	}
	
	public int getClueNum() {
		return clueNum;
	}

}
