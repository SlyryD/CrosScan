package edu.dcc.crosscan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

public class ImageActivity extends Activity {

	public static final String TAG = "CrosScan/ImageActivity";

	private PinchImageView imageView;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		// Inflate layout
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image);

		// Fill layout with image view
		imageView = (PinchImageView) findViewById(R.id.imageView);

		// Add image to view
		Intent intent = getIntent();
		String path = intent.getStringExtra(Constants.EXTRA_PHOTO);
		Bitmap photo;
		if (path.equals("null")) {
			photo = BitmapFactory.decodeResource(getResources(),
					R.drawable.no_photo);
			imageView.setImageBitmap(photo);
		} else {
			photo = BitmapFactory.decodeFile(path);
			imageView.setImageBitmap(photo);
		}
		imageView.setImageBitmap(photo);
	}
}
