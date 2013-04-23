package edu.dcc.game.command;

import edu.dcc.game.Grid;

/**
 * Generic command acting on one or more cells.
 *
 * @author Ryan
 */
public abstract class AbstractCellCommand extends AbstractCommand {

	private Grid mGrid;

	protected Grid getCells() {
		return mGrid;
	}

	protected void setCells(Grid mCells) {
		this.mGrid = mCells;
	}

}
