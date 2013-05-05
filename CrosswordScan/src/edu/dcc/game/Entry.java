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

	public Entry(int clueNum) {
		this.clueNum = clueNum;
		this.size = 0;
	}

	public void addCell(Cell cell) {
		mCells.add(cell);
		size++;
	}

	public List<Cell> getCells() {
		return mCells;
	}

	public Cell getCell(int index) {
		return mCells.get(index);
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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(clueNum);
		for (Cell cell : mCells) {
			sb.append(cell.getValue());
		}
		return sb.toString();
	}

}
