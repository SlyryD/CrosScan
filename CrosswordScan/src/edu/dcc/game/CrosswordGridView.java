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

package edu.dcc.game;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import edu.dcc.crosswordscan.R;
import edu.dcc.game.Grid.OnChangeListener;

/**
 * Crossword grid view
 * 
 * @author Ryan
 */
public class CrosswordGridView extends View {

	public static final int DEFAULT_BOARD_SIZE = 100;

	/**
	 * "Color not set" value. (In relation to {@link Color}, it is in fact black
	 * color with alpha channel set to 0 => that means it is completely
	 * transparent).
	 */
	private static final int NO_COLOR = 0;

	private float mCellWidth;
	private float mCellHeight;
	private Cell mTouchedCell;
	// TODO: should I synchronize access to mSelectedCell?
	private Cell mSelectedCell;

	private CrosswordGame mGame;
	private Grid mGrid;

	private OnCellTappedListener mOnCellTappedListener;
	private OnCellSelectedListener mOnCellSelectedListener;

	private Paint mLinePaint;
	private Paint mCellValuePaint;
	private Paint mClueNumPaint;
	private int mNumberLeft;
	private int mNumberTop;
	private float mClueNumTop;
	private Paint mBackgroundColorBlackCell;
	private Paint mBackgroundColorSelected;
	private Paint mBackgroundColorEntry;

	public CrosswordGridView(Context context) {
		this(context, null);
	}

	public CrosswordGridView(Context context, AttributeSet attrs) {
		super(context, attrs);

		setFocusable(true);
		setFocusableInTouchMode(true);

		mLinePaint = new Paint();
		mCellValuePaint = new Paint();
		mClueNumPaint = new Paint();
		mBackgroundColorBlackCell = new Paint();
		mBackgroundColorSelected = new Paint();
		mBackgroundColorEntry = new Paint();

		mCellValuePaint.setAntiAlias(true);
		mClueNumPaint.setAntiAlias(true);

		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.CrosswordGridView);

		setLineColor(a.getColor(R.styleable.CrosswordGridView_lineColor,
				Color.BLACK));
		setTextColor(a.getColor(R.styleable.CrosswordGridView_textColor,
				Color.BLACK));
		setTextColorClueNum(a.getColor(
				R.styleable.CrosswordGridView_textColorNote, Color.BLACK));
		setBackgroundColor(a.getColor(
				R.styleable.CrosswordGridView_backgroundColor, Color.WHITE));
		setBackgroundColorBlackCell(a.getColor(
				R.styleable.CrosswordGridView_backgroundColorBlackCell,
				Color.BLACK));
		setBackgroundColorEntry(a.getColor(
				R.styleable.CrosswordGridView_backgroundColorEntry,
				Color.rgb(50, 50, 255)));
		setBackgroundColorSelected(a.getColor(
				R.styleable.CrosswordGridView_backgroundColorSelected,
				Color.YELLOW));

		a.recycle();
	}

	public int getLineColor() {
		return mLinePaint.getColor();
	}

	public void setLineColor(int color) {
		mLinePaint.setColor(color);
	}

	public int getTextColor() {
		return mCellValuePaint.getColor();
	}

	public void setTextColor(int color) {
		mCellValuePaint.setColor(color);
	}

	public int getTextColorClueNum() {
		return mClueNumPaint.getColor();
	}

	public void setTextColorClueNum(int color) {
		mClueNumPaint.setColor(color);
	}

	public int getBackgroundColorBlackCell() {
		return mBackgroundColorBlackCell.getColor();
	}

	public void setBackgroundColorBlackCell(int color) {
		mBackgroundColorBlackCell.setColor(color);
	}

	public int getBackgroundColorEntry() {
		return mBackgroundColorEntry.getColor();
	}

	public void setBackgroundColorEntry(int color) {
		mBackgroundColorEntry.setColor(color);
		mBackgroundColorEntry.setAlpha(100);
	}

	public int getBackgroundColorSelected() {
		return mBackgroundColorSelected.getColor();
	}

	public void setBackgroundColorSelected(int color) {
		mBackgroundColorSelected.setColor(color);
		mBackgroundColorSelected.setAlpha(100);
	}

	public void setGame(CrosswordGame game) {
		mGame = game;
		setCells(game.getGrid());
	}

	public void setCells(Grid grid) {
		mGrid = grid;

		if (mGrid != null) {
			// select first cell by default
			mSelectedCell = mGrid.getCell(0, 0);
			onCellSelected(mSelectedCell);

			mGrid.addOnChangeListener(new OnChangeListener() {
				@Override
				public void onChange() {
					postInvalidate();
				}
			});
		}

		postInvalidate();
	}

	public Grid getCells() {
		return mGrid;
	}

	public Cell getSelectedCell() {
		return mSelectedCell;
	}

	/**
	 * Registers callback which will be invoked when user taps the cell.
	 * 
	 * @param l
	 */
	public void setOnCellTappedListener(OnCellTappedListener l) {
		mOnCellTappedListener = l;
	}

	protected void onCellTapped(Cell cell) {
		if (mOnCellTappedListener != null) {
			mOnCellTappedListener.onCellTapped(cell);
		}
	}

	/**
	 * Registers callback which will be invoked when cell is selected. Cell
	 * selection can change without user interaction.
	 * 
	 * @param l
	 */
	public void setOnCellSelectedListener(OnCellSelectedListener l) {
		mOnCellSelectedListener = l;
	}

	protected void onCellSelected(Cell cell) {
		if (mOnCellSelectedListener != null) {
			mOnCellSelectedListener.onCellSelected(cell);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		// Log.d(TAG, "widthMode=" + getMeasureSpecModeString(widthMode));
		// Log.d(TAG, "widthSize=" + widthSize);
		// Log.d(TAG, "heightMode=" + getMeasureSpecModeString(heightMode));
		// Log.d(TAG, "heightSize=" + heightSize);

		int width = -1, height = -1;
		if (widthMode == MeasureSpec.EXACTLY) {
			width = widthSize;
		} else {
			width = DEFAULT_BOARD_SIZE;
			if (widthMode == MeasureSpec.AT_MOST && width > widthSize) {
				width = widthSize;
			}
		}
		if (heightMode == MeasureSpec.EXACTLY) {
			height = heightSize;
		} else {
			height = DEFAULT_BOARD_SIZE;
			if (heightMode == MeasureSpec.AT_MOST && height > heightSize) {
				height = heightSize;
			}
		}

		if (widthMode != MeasureSpec.EXACTLY) {
			width = height;
		}

		if (heightMode != MeasureSpec.EXACTLY) {
			height = width;
		}

		if (widthMode == MeasureSpec.AT_MOST && width > widthSize) {
			width = widthSize;
		}
		if (heightMode == MeasureSpec.AT_MOST && height > heightSize) {
			height = heightSize;
		}
		mCellWidth = (width - getPaddingLeft() - getPaddingRight())
				/ (float) Grid.gridSize;
		mCellHeight = (height - getPaddingTop() - getPaddingBottom())
				/ (float) Grid.gridSize;

		setMeasuredDimension(width, height);

		float cellTextSize = mCellHeight * 0.75f;
		mCellValuePaint.setTextSize(cellTextSize);
		mClueNumPaint.setTextSize(mCellHeight / 3.0f);
		// Compute offsets in each cell to center the rendered number
		mNumberLeft = (int) ((mCellWidth - mCellValuePaint.measureText("M")) / 2);
		mNumberTop = (int) ((mCellHeight - mCellValuePaint.getTextSize()) / 2);

		// Add offset to avoid cutting off clue numbers
		mClueNumTop = mCellHeight / 50.0f;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// Drawable has its own draw() method that takes Canvas as argument

		int width = getWidth() - getPaddingRight();
		int height = getHeight() - getPaddingBottom();

		int paddingLeft = getPaddingLeft();
		int paddingTop = getPaddingTop();

		// Draw cells
		int cellLeft, cellTop;
		if (mGrid != null) {
			float valueAscent = mCellValuePaint.ascent();
			float clueNumAscent = mClueNumPaint.ascent();
			for (int row = 0; row < Grid.gridSize; row++) {
				for (int col = 0; col < Grid.gridSize; col++) {
					Cell cell = mGrid.getCell(row, col);

					cellLeft = Math.round((col * mCellWidth) + paddingLeft);
					cellTop = Math.round((row * mCellHeight) + paddingTop);

					// Draw black cells
					if (!cell.isWhite()) {
						canvas.drawRect(cellLeft, cellTop, cellLeft
								+ mCellWidth, cellTop + mCellHeight,
								mBackgroundColorBlackCell);
					}

					// Draw cell text
					int value = cell.getValue();
					if (value != 0) {
						canvas.drawText(Integer.toString(value), cellLeft
								+ mNumberLeft, cellTop + mNumberTop
								- valueAscent, mCellValuePaint);
					} else {
						int clueNum = cell.getClueNum();
						if (clueNum > 0) {
							canvas.drawText(Integer.toString(clueNum),
									cellLeft + 2, cellTop + mClueNumTop
											- clueNumAscent - 1, mClueNumPaint);
						}
					}

				}
			}

			// Highlight selected cell and entry
			if (mSelectedCell != null) {
				for (Cell cell : mSelectedCell.getEntry().getCells()) {
					cellLeft = Math.round(cell.getColumn() * mCellWidth)
							+ paddingLeft;
					cellTop = Math.round(cell.getRow() * mCellHeight)
							+ paddingTop;
					canvas.drawRect(cellLeft, cellTop, cellLeft + mCellWidth,
							cellTop + mCellHeight,
							cell == mSelectedCell ? mBackgroundColorSelected
									: mBackgroundColorEntry);
				}
			}
		}

		// draw vertical lines
		for (int col = 0; col <= Grid.gridSize; col++) {
			float x = (col * mCellWidth) + paddingLeft;
			canvas.drawLine(x, paddingTop, x, height, mLinePaint);
		}

		// draw horizontal lines
		for (int row = 0; row <= Grid.gridSize; row++) {
			float y = row * mCellHeight + paddingTop;
			canvas.drawLine(paddingLeft, y, width, y, mLinePaint);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		int x = (int) event.getX();
		int y = (int) event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			mTouchedCell = getCellAtPoint(x, y);
			break;
		case MotionEvent.ACTION_UP:
			mSelectedCell = getCellAtPoint(x, y);
			invalidate(); // Update board when selected cell changes

			if (mSelectedCell != null) {
				onCellTapped(mSelectedCell);
				onCellSelected(mSelectedCell);
			}

			break;
		case MotionEvent.ACTION_CANCEL:
			mTouchedCell = null;
			break;
		default:
			postInvalidate();
			return false;
		}

		postInvalidate();
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
			return moveCellSelection(0, -1);
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			return moveCellSelection(1, 0);
		case KeyEvent.KEYCODE_DPAD_DOWN:
			return moveCellSelection(0, 1);
		case KeyEvent.KEYCODE_DPAD_LEFT:
			return moveCellSelection(-1, 0);
		case KeyEvent.KEYCODE_0:
		case KeyEvent.KEYCODE_SPACE:
		case KeyEvent.KEYCODE_DEL:
			// Clear value in selected cell
			if (mSelectedCell != null) {
				setCellValue(mSelectedCell, (char) 0);
				moveCellSelectionRight();
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			if (mSelectedCell != null) {
				onCellTapped(mSelectedCell);
			}
			return true;
		}

		if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
			int selChar = keyCode + 36;
			Cell cell = mSelectedCell;
			// Enter number in cell
			setCellValue(cell, (char) selChar);
			moveCellSelectionRight();
			return true;
		}

		return false;
	}

	/**
	 * Moves selected cell by one cell to the right. If edge is reached,
	 * selection skips on beginning of another line.
	 */
	public void moveCellSelectionRight() {
		if (!moveCellSelection(1, 0)) {
			int selRow = mSelectedCell.getRow();
			selRow++;
			if (!moveCellSelectionTo(selRow, 0)) {
				moveCellSelectionTo(0, 0);
			}
		}
		postInvalidate();
	}

	private void setCellValue(Cell cell, char value) {
		if (cell.isWhite()) {
			if (mGame != null) {
				mGame.setCellValue(cell, value);
			} else {
				cell.setValue(value);
			}
		}
	}

	/**
	 * Moves selected by vx cells right and vy cells down. vx and vy can be
	 * negative. Returns true, if new cell is selected.
	 * 
	 * @param vx
	 *            Horizontal offset, by which move selected cell.
	 * @param vy
	 *            Vertical offset, by which move selected cell.
	 */
	private boolean moveCellSelection(int vx, int vy) {
		int newRow = 0;
		int newCol = 0;

		if (mSelectedCell != null) {
			newRow = mSelectedCell.getRow() + vy;
			newCol = mSelectedCell.getColumn() + vx;
		}

		return moveCellSelectionTo(newRow, newCol);
	}

	/**
	 * Moves selection to the cell given by row and column index.
	 * 
	 * @param row
	 *            Row index of cell which should be selected.
	 * @param col
	 *            Column index of cell which should be selected.
	 * @return True, if cell was successfully selected.
	 */
	private boolean moveCellSelectionTo(int row, int col) {
		if (col >= 0 && col < Grid.gridSize && row >= 0 && row < Grid.gridSize) {
			Cell newCell = mGrid.getCell(row, col);
			if (!newCell.isWhite()) {
				return false;
			}
			mSelectedCell = newCell;
			onCellSelected(mSelectedCell);

			postInvalidate();
			return true;
		}

		return false;
	}

	/**
	 * Returns cell at given screen coordinates or null if not found.
	 * 
	 * @param x
	 * @param y
	 * @return cell at point
	 */
	private Cell getCellAtPoint(int x, int y) {
		// Take into account padding
		int lx = x - getPaddingLeft();
		int ly = y - getPaddingTop();

		int row = (int) (ly / mCellHeight);
		int col = (int) (lx / mCellWidth);

		if (col >= 0 && col < Grid.gridSize && row >= 0 && row < Grid.gridSize) {
			return mGrid.getCell(row, col);
		} else {
			return null;
		}
	}

	/**
	 * Occurs when user tap the cell.
	 * 
	 * @author romario
	 */
	public interface OnCellTappedListener {
		void onCellTapped(Cell cell);
	}

	/**
	 * Occurs when user selects the cell.
	 * 
	 * @author romario
	 */
	public interface OnCellSelectedListener {
		void onCellSelected(Cell cell);
	}
}
