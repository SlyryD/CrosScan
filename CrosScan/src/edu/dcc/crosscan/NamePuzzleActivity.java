package edu.dcc.crosscan;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import edu.dcc.crosscan.CrosswordGridView.OnCellSelectedListener;
import edu.dcc.game.Cell;
import edu.dcc.game.Puzzle;

public class NamePuzzleActivity extends Activity {

	public static final String TITLE = "title";

	public static final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyyMMdd_HHmmss", Locale.US);

	private String puzzleStr, photo;
	private Puzzle puzzle;

	private CrosswordGridView gridView;
	private EditText editText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_name_puzzle);
		getDataFromScan();
		editText = (EditText) findViewById(R.id.crossword_name);
		editText.setText("Crossword_" + sdf.format(System.currentTimeMillis()));
		gridView = (CrosswordGridView) findViewById(R.id.crossword_grid);
		gridView.setPuzzle(puzzle);
		gridView.setOnCellSelectedListener(new BasicOnCellSelectedListener());
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
		public boolean onCellSelected(Cell cell) {
			Log.i(TITLE, "Reached on cell selected");
			cell.toggleColor();
			puzzleStr = puzzle.serialize();
			return true;
		}
	}

}
