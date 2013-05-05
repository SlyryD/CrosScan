package edu.dcc.crosswordscan;

import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import edu.dcc.game.CrosswordGridView;
import edu.dcc.game.Grid;

public class NamePuzzleActivity extends Activity {
	public final static String TITLE = "title";
	public static Random generator = new Random(17263849);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_name_puzzle);
		CrosswordGridView gridView = (CrosswordGridView) findViewById(R.id.crossword_grid);
		gridView.setGrid(getGridFromScan());
		gridView.setReadOnly();
	}

	private Grid getGridFromScan() {
		Intent intent = getIntent();
		return Grid.deserialize(intent.getStringExtra(ScanActivity.GRID));
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
