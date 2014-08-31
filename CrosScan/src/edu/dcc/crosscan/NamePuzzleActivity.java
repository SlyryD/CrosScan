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

	public static final String TAG = "CrosScan/NamePuzzleActivity";
	
	public static final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyyMMdd_HHmmss", Locale.US);

	private String puzzleStr, photo;
	private Puzzle puzzle;

	private CrosswordGridView gridView;
	private EditText editText;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
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
		String[] intentStr = intent.getStringExtra(Constants.EXTRA_PUZZLE).split(
				"\n");
		Log.i(TAG, "Photo: " + intentStr[0] + "\nPuzzle: " + intentStr[1]);
		photo = intentStr[0];
		puzzleStr = intentStr[1];
		puzzle = Puzzle.deserialize(puzzleStr);
	}

	public final void puzzleListTransition(final View view) {
		Intent intent = new Intent(this, PuzzleListActivity.class);
		EditText editText = (EditText) findViewById(R.id.crossword_name);
		String title = editText.getText().toString();
		intent.putExtra(Constants.EXTRA_PHOTO, photo);
		intent.putExtra(Constants.EXTRA_GRID, puzzleStr);
		intent.putExtra(Constants.EXTRA_TITLE, title);
		startActivity(intent);
		finish();
	}

	public class BasicOnCellSelectedListener implements OnCellSelectedListener {

		@Override
		public final boolean onCellSelected(final Cell cell) {
			Log.i(TAG, "Reached on cell selected");
			cell.toggleColor();
			puzzleStr = puzzle.serialize();
			return true;
		}
	}

}
