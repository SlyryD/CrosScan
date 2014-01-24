package edu.dcc.crosswordscan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import edu.dcc.game.Puzzle;

public class NamePuzzleActivity extends Activity {
	public final static String TITLE = "title";

	private String grid, photo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_name_puzzle);
		getDataFromScan();
		CrosswordGridView gridView = (CrosswordGridView) findViewById(R.id.crossword_grid);
		gridView.setPuzzle(Puzzle.deserialize(grid));
		gridView.setReadOnly();
	}

	private void getDataFromScan() {
		Intent intent = getIntent();
		grid = intent.getStringExtra(ScanActivity.GRID);
		photo = intent.getStringExtra(ScanActivity.PHOTO);
	}

	public void puzzleListTransition(View view) {
		Intent intent = new Intent(this, PuzzleListActivity.class);
		EditText editText = (EditText) findViewById(R.id.crossword_name);
		String title = editText.getText().toString();
		intent.putExtra(TITLE, title);
		intent.putExtra(ScanActivity.GRID, grid);
		intent.putExtra(ScanActivity.PHOTO, photo);
		startActivity(intent);
		finish();
	}

}
