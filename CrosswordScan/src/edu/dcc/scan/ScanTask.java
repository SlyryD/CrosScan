package edu.dcc.scan;

import android.app.ProgressDialog;
import android.os.AsyncTask;

public class ScanTask extends AsyncTask<Void, Void, Void> {

	ProgressDialog progressDialog;
	
	public ScanTask(ProgressDialog progressDialog) {
		this.progressDialog = progressDialog;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressDialog.setTitle("Processing");
		progressDialog.setMessage("Please Wait...");
		progressDialog.show();
	}
	
	@Override
	protected void onPostExecute(Void result) {
//		super.onPostExecute(result);
		progressDialog.dismiss();
	}
	
	@Override
	protected Void doInBackground(Void... arg0) {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}
