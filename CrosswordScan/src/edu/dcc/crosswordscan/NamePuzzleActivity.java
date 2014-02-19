package edu.dcc.crosswordscan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import edu.dcc.crosswordscan.CrosswordGridView.OnCellSelectedListener;
import edu.dcc.game.Cell;
import edu.dcc.game.Puzzle;

public class NamePuzzleActivity extends Activity {

	public final static String TITLE = "title";

	private String puzzleStr, photo;
	private Puzzle puzzle;

	private CrosswordGridView gridView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_name_puzzle);
		getDataFromScan();
		gridView = (CrosswordGridView) findViewById(R.id.crossword_grid);
		gridView.setPuzzle(puzzle);
		gridView.setOnCellSelectedListener(new BasicOnCellSelectedListener());
		gridView.setReadOnly();
	}

	private void getDataFromScan() {
		Intent intent = getIntent();
		puzzleStr = intent.getStringExtra(ScanActivity.GRID);
		photo = intent.getStringExtra(ScanActivity.PHOTO);
		puzzle = Puzzle.deserialize(puzzleStr);
	}

	public void puzzleListTransition(View view) {
		Intent intent = new Intent(this, PuzzleListActivity.class);
		EditText editText = (EditText) findViewById(R.id.crossword_name);
		String title = editText.getText().toString();
		intent.putExtra(TITLE, title);
		intent.putExtra(ScanActivity.GRID, puzzleStr);
		intent.putExtra(ScanActivity.PHOTO, photo);
		startActivity(intent);
		finish();
	}

	public class BasicOnCellSelectedListener implements OnCellSelectedListener {

		@Override
		public void onCellSelected(Cell cell) {
			cell.toggleColor();
			puzzleStr = puzzle.serialize();
		}
	}

}
