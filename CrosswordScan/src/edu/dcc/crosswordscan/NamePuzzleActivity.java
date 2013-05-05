package edu.dcc.crosswordscan;

import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import edu.dcc.game.CrosswordGridView;
import edu.dcc.game.Grid;

public class NamePuzzleActivity extends Activity {
	public final static String EXTRA_MESSAGE = "edu.dcc.crosswordscan.MESSAGE";
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
		// TODO: Actually get from scan, instead of random
		int[][] cells = new int[10][10];
		for (int i = 0; i < cells.length; i++) {
			for (int j = 0; j < cells[i].length; j++) {
				// 1/8 chance of black square
				if (generator.nextInt(2) == 0 && generator.nextInt(2) == 0) {
					cells[i][j] = 0;
				} else {
					cells[i][j] = 1;
				}
			}
		}
		return new Grid(10, cells, null);
	}

	public void puzzleListTransition(View view) {
		Intent intent = new Intent(this, PuzzleListActivity.class);
		EditText editText = (EditText) findViewById(R.id.crossword_name);
		String message = editText.getText().toString();
		intent.putExtra(EXTRA_MESSAGE, message);
		startActivity(intent);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.name_puzzle, menu);
		return true;
	}

}
