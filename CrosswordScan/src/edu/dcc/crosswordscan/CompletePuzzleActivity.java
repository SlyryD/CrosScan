package edu.dcc.crosswordscan;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

public class CompletePuzzleActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_complete_puzzle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.complete_puzzle, menu);
		return true;
	}

}
