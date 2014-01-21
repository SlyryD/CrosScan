package edu.dcc.crosswordscan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import edu.dcc.game.Puzzle;

public class NamePuzzleActivity extends Activity {
	public final static String TITLE = "title";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_name_puzzle);
		CrosswordGridView gridView = (CrosswordGridView) findViewById(R.id.crossword_grid);
		gridView.setPuzzle(getGridFromScan());
		gridView.setReadOnly();
	}

	private Puzzle getGridFromScan() {
		Intent intent = getIntent();
		return Puzzle.deserialize(intent.getStringExtra(ScanActivity.GRID));
	}

	public void puzzleListTransition(View view) {
		Intent intent = new Intent(this, PuzzleListActivity.class);
		EditText editText = (EditText) findViewById(R.id.crossword_name);
		String title = editText.getText().toString();
		intent.putExtra(TITLE, title);
		intent.putExtra(ScanActivity.GRID,
				getIntent().getStringExtra(ScanActivity.GRID));
		startActivity(intent);
		finish();
	}

}
