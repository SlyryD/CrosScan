package edu.dcc.crosswordscan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class NamePuzzleActivity extends Activity {
	public final static String EXTRA_MESSAGE = "edu.dcc.crosswordscan.MESSAGE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_name_puzzle);
	}

	public void completeTransition(View view) {
		Intent intent = new Intent(this, PuzzleListActivity.class);
		EditText editText = (EditText) findViewById(R.id.crossword_name);
		String message = editText.getText().toString();
		intent.putExtra(EXTRA_MESSAGE, message);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.name_puzzle, menu);
		return true;
	}

}
