package edu.dcc.crosscan;

import edu.dcc.crosscan.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.menu_help) {
			Toast.makeText(MainActivity.this, "Select a button to begin",
					Toast.LENGTH_SHORT).show();
		}
		return super.onOptionsItemSelected(item);
	}

	/** Called when the user clicks the Scan button */
	public final void scanTransition(final View view) {
		Intent intent = new Intent(this, ScanActivity.class);
		startActivity(intent);
	}

	/** Called when the user clicks the Complete Puzzle button */
	public final void gridTransition(final View view) {
		Intent intent = new Intent(this, PuzzleListActivity.class);
		startActivity(intent);
	}

}
