package edu.dcc.crosswordscan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class ConfirmGridActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_confirm_grid);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.confirm_grid, menu);
		return true;
	}

	/** Called when the user clicks the Yes button */
	public void confirmGrid(View view) {
		finishActivity(ScanActivity.CONFIRM_GRID);
		Intent intent = new Intent(this, NamePuzzleActivity.class);
		startActivity(intent);
	}

	/** Called when the user clicks the No button */
	public void rejectGrid(View view) {
		finishActivity(ScanActivity.REJECT_GRID);
	}
}
