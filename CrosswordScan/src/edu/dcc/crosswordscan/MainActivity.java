package edu.dcc.crosswordscan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_help:
			Toast.makeText(MainActivity.this, "Select a button to begin",
					Toast.LENGTH_SHORT).show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/** Called when the user clicks the Scan button */
	public void scanTransition(View view) {
		Intent intent = new Intent(this, ScanActivity.class);
		startActivity(intent);
	}

	/** Called when the user clicks the Complete Puzzle button */
	public void gridTransition(View view) {
		Intent intent = new Intent(this, PuzzleListActivity.class);
		startActivity(intent);
	}

}
