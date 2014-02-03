package edu.dcc.crosswordscan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;

public class ImageActivity extends Activity {

	public static final String TAG = "CrosswordScan/ImageActivity";

	private PinchImageView imageView;
	private GestureDetector mGestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Inflate layout
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image);

		// Fill layout with image view
		imageView = (PinchImageView) findViewById(R.id.imageView);

		// Add image to view
		Intent intent = getIntent();
		String path = intent.getStringExtra(ScanActivity.PHOTO);
		if (path.equals("null")) {
			Bitmap photo = BitmapFactory.decodeResource(getResources(),
					R.drawable.no_photo);
			imageView.setImageBitmap(photo);
		} else {
			Bitmap photo = BitmapFactory.decodeFile(path);
			imageView.setImageBitmap(rotateBitmap(photo, 90));
		}

		// Add gesture listener to view
		mGestureDetector = new GestureDetector(this,
				new GestureDetector.SimpleOnGestureListener() {
					// double tap zoom
					public boolean onDoubleTap(MotionEvent e) {
						if (imageView.getScale() > 1f) {
							imageView.zoomTo(1f);
						} else {
							// MAX_SCALE is a float constant defined in the
							// Activity
							imageView.zoomTo(PinchImageView.MAX_SCALE);
						}

						return true;
					}

					// image scrolling
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						if (imageView.getScale() > 1f) {
							imageView.postTranslate(-distanceX, -distanceY);
							return true;
						}
						return false;
					}
				});
	}

	private Bitmap rotateBitmap(Bitmap source, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(),
				source.getHeight(), matrix, true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.image, menu);
		return true;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		// if the gesture detector didn't handle it, check for another condition
		if (!mGestureDetector.onTouchEvent(event)) {
			// have the PinchImageView handle 2 fingers/pointers for pinch
			// gestures
			if (event.getPointerCount() == 2) {
				return imageView.onTouchEvent(event);
			}
		}
		return super.dispatchTouchEvent(event);
	}

}
