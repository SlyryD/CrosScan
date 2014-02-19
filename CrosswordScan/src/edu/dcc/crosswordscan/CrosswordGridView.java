package edu.dcc.crosswordscan;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import edu.dcc.game.Cell;
import edu.dcc.game.CrosswordGame;
import edu.dcc.game.Entry;
import edu.dcc.game.Puzzle;
import edu.dcc.game.Puzzle.OnChangeListener;

/**
 * Crossword grid view
 * 
 * @author Ryan
 */
public class CrosswordGridView extends View {

	public static final int DEFAULT_BOARD_SIZE = 240;

	private float mCellWidth;

	private Cell mSelectedCell;

	private CrosswordGame mGame;
	private Puzzle puzzle;

	private OnCellTappedListener mOnCellTappedListener;
	private OnCellSelectedListener mOnCellSelectedListener;

	private Paint mLinePaint;
	private Paint mCellValuePaint;
	private Paint mClueNumPaint;
	private Paint mBackgroundColorBlackCell;
	private Paint mBackgroundColorSelected;
	private Paint mBackgroundColorEntry;

	private int mNumberLeft;
	private int mNumberTop;
	private float mClueNumTop;

	// PointF downPt = new PointF(); // Record position on down
	// PointF startPt = new PointF(); // Record start position of view

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

		mLinePaint.setStyle(Paint.Style.STROKE);
		mLinePaint.setStrokeWidth(3);
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

	public Cell getSelectedCell() {
		return mSelectedCell;
	}

	public void setGame(CrosswordGame game) {
		mGame = game;
		setPuzzle(game.getPuzzle());
	}

	public Puzzle getPuzzle() {
		return puzzle;
	}

	public void setPuzzle(Puzzle puzzle) {
		this.puzzle = puzzle;

		if (this.puzzle != null) {
			if (mGame != null) {
				// select first cell by default
				mSelectedCell = puzzle.getFirstWhiteCell();
				onCellSelected(mSelectedCell);
			}
			this.puzzle.addOnChangeListener(new OnChangeListener() {
				@Override
				public void onChange() {
					postInvalidate();
				}
			});
		}

		postInvalidate();
	}

	/**
	 * Registers callback which will be invoked when user taps the cell.
	 * 
	 * @param l
	 */
	public void setOnCellTappedListener(OnCellTappedListener l) {
		mOnCellTappedListener = l;
	}

	public OnCellSelectedListener getOnCellSelectedListener() {
		return mOnCellSelectedListener;
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

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int width = getWidth() - getPaddingLeft() - getPaddingRight();
		int height = getHeight() - getPaddingTop() - getPaddingBottom();

		// Draw cells
		int cellLeft, cellTop;
		if (puzzle != null) {
			float valueAscent = mCellValuePaint.ascent();
			float clueNumAscent = mClueNumPaint.ascent();
			for (int row = 0; row < puzzle.getHeight(); row++) {
				for (int col = 0; col < puzzle.getWidth(); col++) {
					Cell cell = puzzle.getCell(row, col);

					cellLeft = Math
							.round((col * mCellWidth) + getPaddingLeft());
					cellTop = Math.round((row * mCellWidth) + getPaddingTop());

					if (!cell.isWhite()) {
						// Draw black cells
						canvas.drawRect(cellLeft, cellTop, cellLeft
								+ mCellWidth, cellTop + mCellWidth,
								mBackgroundColorBlackCell);
					} else {
						// Draw clue numbers
						if ((cell.getEntry(true) != null && cell.getEntry(true)
								.getCell(0) == cell)
								|| (cell.getEntry(false) != null && cell
										.getEntry(false).getCell(0) == cell)) {
							canvas.drawText(
									Integer.toString(cell.getClueNum()),
									cellLeft + 2, cellTop + mClueNumTop
											- clueNumAscent + 2, mClueNumPaint);
						}
					}

					// Draw cell text
					char value = cell.getValue();
					if (value != 0) {
						canvas.drawText(String.valueOf(value), cellLeft
								+ mNumberLeft + 1, cellTop + mNumberTop
								- valueAscent + 4, mCellValuePaint);
					}

				}
			}

			// Highlight selected cell and entry
			if (mSelectedCell != null && mGame != null) {
				boolean acrossMode = mGame.isAcrossMode();
				Entry entry = mSelectedCell.getEntry(acrossMode);

				if (entry == null) {
					mGame.setAcrossMode(!acrossMode);
					entry = mSelectedCell.getEntry(!acrossMode);
				}

				for (Cell cell : entry.getCells()) {
					cellLeft = Math.round(cell.getColumn() * mCellWidth)
							+ getPaddingLeft();
					cellTop = Math.round(cell.getRow() * mCellWidth)
							+ getPaddingTop();
					canvas.drawRect(cellLeft, cellTop, cellLeft + mCellWidth,
							cellTop + mCellWidth,
							cell == mSelectedCell ? mBackgroundColorSelected
									: mBackgroundColorEntry);
				}
			}

			// draw vertical lines
			for (int col = 0; col <= puzzle.getWidth(); col++) {
				float x = (col * mCellWidth) + getPaddingLeft();
				canvas.drawLine(x, getPaddingTop(), x, height, mLinePaint);
			}

			// draw horizontal lines
			for (int row = 0; row <= puzzle.getHeight(); row++) {
				float y = (row * mCellWidth) + getPaddingTop();
				canvas.drawLine(getPaddingLeft(), y, width, y, mLinePaint);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

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

		if (puzzle == null) {
			mCellWidth = (width - getPaddingLeft() - getPaddingRight())
					/ ((float) Puzzle.DEFAULT_SIZE);
		} else {
			mCellWidth = (width - getPaddingLeft() - getPaddingRight())
					/ ((float) puzzle.getWidth());
		}

		setMeasuredDimension(width, height);

		float cellTextSize = mCellWidth * 0.75f;
		mCellValuePaint.setTextSize(cellTextSize);
		mClueNumPaint.setTextSize(mCellWidth / 3.0f);
		// Compute offsets in each cell to center the rendered number
		mNumberLeft = (int) ((mCellWidth - mCellValuePaint.measureText("M")) / 2);
		mNumberTop = (int) ((mCellWidth - mCellValuePaint.getTextSize()) / 2);

		// Add offset to avoid cutting off clue numbers
		mClueNumTop = mCellWidth / 50.0f;
	}

	protected void onCellTapped(Cell cell) {
		if (mOnCellTappedListener != null) {
			mOnCellTappedListener.onCellTapped(cell);
		}
	}

	protected void onCellSelected(Cell cell) {
		if (mOnCellSelectedListener != null) {
			mOnCellSelectedListener.onCellSelected(cell);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		int x = (int) event.getX();
		int y = (int) event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// downPt.x = event.getX();
			// downPt.y = event.getY();
			// startPt = new PointF(getX(), getY());
			break;
		case MotionEvent.ACTION_MOVE:
			// TODO: Move view?
			// PointF mv = new PointF(event.getX() - downPt.x, event.getY()
			// - downPt.y);
			// setX((int) (startPt.x + mv.x));
			// setY((int) (startPt.y + mv.y));
			// startPt = new PointF(getX(), getY());
			break;
		case MotionEvent.ACTION_UP:
			Cell selected = getCellAtPoint(x, y);
			if (selected == null) {
				return false;
			}

			if (mGame != null && selected == mSelectedCell) {
				mGame.setAcrossMode(!mGame.isAcrossMode());
			}

			mSelectedCell = selected;
			invalidate(); // Update board when selected cell changes
			if (mSelectedCell != null) {
				onCellTapped(mSelectedCell);
				onCellSelected(mSelectedCell);
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			// TODO: Do nothing?
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
		if (mGame == null) {
			return false;
		}

		if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
			// Enter letter in cell
			setCellValue(mSelectedCell, (char) (keyCode + 36));
			moveCellSelection();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DEL) {
			// Clear value in selected cell
			if (mSelectedCell != null) {
				if (mSelectedCell.getValue() == 0) {
					if (mGame.isAcrossMode() ? !moveCellSelection(-1, 0)
							: !moveCellSelection(0, -1)) {
						Entry previousEntry = getPreviousEntry();
						Cell previousCell = previousEntry.getCell(previousEntry
								.getLength() - 1);
						moveCellSelectionTo(previousCell.getRow(),
								previousCell.getColumn());
					}
				}
				setCellValue(mSelectedCell, (char) 0);
			}
		}

		return false;
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
	 * Moves selected cell by one. If cell is full, move to next empty white
	 * cell. If edge or black cell is reached, selection moves to next entry.
	 */
	public void moveCellSelection() {
		boolean acrossMode = mGame.isAcrossMode();
		Cell original = mSelectedCell;
		do {
			if (!moveCellSelection(acrossMode ? 1 : 0, acrossMode ? 0 : 1)) {
				// Move to next entry
				System.out.println("Current entry: "
						+ mSelectedCell.getEntry(acrossMode));
				System.out.println("Next entry: " + getNextEntry());
				Cell nextCell = getNextEntry().getCell(0);
				moveCellSelectionTo(nextCell.getRow(), nextCell.getColumn());
			}
		} while (mSelectedCell.getValue() != 0 && mSelectedCell != original);
		postInvalidate();
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
		if (col >= 0 && col < puzzle.getWidth() && row >= 0
				&& row < puzzle.getHeight()) {
			Cell newCell = puzzle.getCell(row, col);
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

	public void switchAcrossMode() {
		mGame.setAcrossMode(!mGame.isAcrossMode());
		invalidate();
		if (mSelectedCell != null) {
			onCellSelected(mSelectedCell);
		}
	}

	public void nextClue(boolean next) {
		// Get next or previous entry
		Entry entry = next ? getNextEntry() : getPreviousEntry();

		// Find first empty cell of entry
		boolean found = false;
		int index = 0;
		while (index < entry.getLength() && !found) {
			found = entry.getCell(index).getValue() == 0;
			index++;
		}

		// Get cell of entry
		Cell cell = entry.getCell(found ? index - 1 : 0);
		moveCellSelectionTo(cell.getRow(), cell.getColumn());
	}

	public void resetView() {
		mSelectedCell = puzzle.getFirstWhiteCell();
	}

	private Entry getNextEntry() {
		int entryNum = mSelectedCell.getEntry(mGame.isAcrossMode()).getCell(0)
				.getClueNum();
		Entry entry = null;
		for (int num = entryNum; entry == null; num = (num + 1)
				% (puzzle.getNumEntries() + 1)) {
			entry = puzzle.getEntry(num + 1, mGame.isAcrossMode());
		}
		return entry;
	}

	private Entry getPreviousEntry() {
		int entryNum = mSelectedCell.getEntry(mGame.isAcrossMode()).getCell(0)
				.getClueNum();
		Entry entry = null;
		for (int num = entryNum; entry == null; num = (num == 1) ? puzzle
				.getNumEntries() + 1 : (num - 1)) {
			entry = puzzle.getEntry(num - 1, mGame.isAcrossMode());
		}
		return entry;
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

		int row = (int) (ly / mCellWidth);
		int col = (int) (lx / mCellWidth);

		if (col >= 0 && col < puzzle.getWidth() && row >= 0
				&& row < puzzle.getHeight()) {
			return puzzle.getCell(row, col);
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
