package edu.dcc.crosswordscan;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

public class PuzzleInfoActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_puzzle_info);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.puzzle_info, menu);
		return true;
	}

}
