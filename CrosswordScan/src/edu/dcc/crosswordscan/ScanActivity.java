package edu.dcc.crosswordscan;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import edu.dcc.game.Grid;

/**
 * Activity that allows user to scan crossword puzzles.
 * 
 * @author Ryan D.
 */
public class ScanActivity extends Activity {
	// Camera instance
	private Camera camera;
	// Preview view for camera
	private CameraPreview cameraPreview;
	// Request codes
	public static final int CAMERA_REQUEST = 11, CONFIRM_GRID = 22,
			REJECT_GRID = 33, MEDIA_TYPE_IMAGE = 44;
	public static final String GRID = "grid";
	public static final Random generator = new Random(18293734);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Initialize activity layout
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);

		// Retrieve photo button
		Button photoButton = (Button) this.findViewById(R.id.photo_button);
		// Retrieve instance of Camera
		camera = getCameraInstance();
		// Create camera preview and set it as the content of activity
		cameraPreview = new CameraPreview(this);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(cameraPreview);

		// Collect information from picture that user takes
		final PictureCallback picture = new PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				new SavePhotoTask().execute(data);

			}
		};

		// Add a listener to the Capture button
		photoButton.setOnClickListener(

		new View.OnClickListener() {

			public void onClick(View v) {
				// get an image from the camera
				camera.takePicture(null, null, picture);
				System.out.println("Photo Taken!");
			}
		});

		// photoButton.setOnClickListener(new View.OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// Intent cameraIntent = new Intent(
		// android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		// startActivityForResult(cameraIntent, CAMERA_REQUEST);
		// }
		// });
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // Attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			e.printStackTrace();
		}
		return c; // Returns null if camera is unavailable
	}

	class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

		private SurfaceHolder previewHolder;

		@SuppressWarnings("deprecation")
		public CameraPreview(Context context) {
			super(context);

			// SurfaceView view = new SurfaceView(this);
			// camera.setPreviewDisplay(view.getHolder());
			// camera.startPreview();
			// camera.takePicture(shutterCallback, rawPictureCallback,
			// jpegPictureCallback);

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			previewHolder = getHolder();

			previewHolder.addCallback(this);
			// Deprecated setting, but required on Android versions prior to 3.0
			previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {

			if (previewHolder.getSurface() == null) {
				// Preview surface does not exist
				return;
			}

			// Stop preview before making changes
			try {
				camera.stopPreview();
			} catch (Exception e) {
				// Tried to stop a non-existent preview
				e.printStackTrace();
			}

			// Set preview size and rotate
			Camera.Parameters parameters = camera.getParameters();
			parameters.setPreviewSize(width, height);

			if (Build.VERSION.SDK_INT >= 8) {
				int degrees = 0;
				switch (ScanActivity.this.getWindowManager()
						.getDefaultDisplay().getRotation()) {
				case Surface.ROTATION_0:
					degrees = 90;
					break;
				case Surface.ROTATION_90:
					degrees = 0;
					break;
				case Surface.ROTATION_180:
					degrees = 270;
					break;
				case Surface.ROTATION_270:
					degrees = 180;
					break;
				}
				camera.setDisplayOrientation(degrees);
			} else {
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					parameters.set("orientation", "portrait");
					parameters.set("rotation", 90);
				}
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					parameters.set("orientation", "landscape");
					parameters.set("rotation", 90);
				}
				camera.setParameters(parameters);
			}

			// Start preview with new settings
			try {
				camera.setPreviewDisplay(previewHolder);
				camera.startPreview();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub

		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub

		}

	}

	private class SavePhotoTask extends AsyncTask<byte[], String, String> {

		final private ProgressDialog dialog = new ProgressDialog(
				ScanActivity.this);

		protected void onPreExecute() {
			dialog.setMessage("Processing");
			dialog.show();
		}

		@Override
		protected String doInBackground(byte[]... jpeg) {

			System.out.println("Reached1");

			// Retrieve picture file
			File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

			System.out.println("Reached2");

			// Abort if picture file not found
			if (pictureFile == null) {
				return null;
			}

			System.out.println(pictureFile.getAbsolutePath());

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(jpeg[0]);
				fos.close();
				MediaStore.Images.Media.insertImage(getContentResolver(),
						pictureFile.getAbsolutePath(), pictureFile.getName(),
						pictureFile.getName());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}

		/**
		 * Create a File for saving the image
		 * 
		 * @param type
		 * @return
		 */
		private File getOutputMediaFile(int type) {
			// Find directory for photo storage
			File mediaStorageDir = new File(
					Environment
							.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
					"edu.dcc.crosswordscan");

			// Create the storage directory if it does not exist
			if (!mediaStorageDir.exists()) {
				if (!mediaStorageDir.mkdirs()) {
					return null;
				}
			}

			// Create a media file name IMG_CrosswordScan#.jpg
			int number = 1;
			while (Arrays.binarySearch(mediaStorageDir.list(),
					"IMG_CrosswordScan" + String.valueOf(number) + ".jpg") >= 0) {
				number++;
			}
			File mediaFile = null;
			if (type == MEDIA_TYPE_IMAGE) {
				mediaFile = new File(mediaStorageDir.getPath() + File.separator
						+ "IMG_CrosswordScan" + String.valueOf(number) + ".jpg");
			} else {
				return null;
			}

			return mediaFile;
		}

		@Override
		protected void onPostExecute(String result) {
			dialog.dismiss();
			Intent intent = new Intent(ScanActivity.this,
					NamePuzzleActivity.class);
			// TODO: Actually get from scan, instead of random
			intent.putExtra(GRID, getRandomGrid().serialize());
			startActivity(intent);
		}

		private Grid getRandomGrid() {
			int[][] cells = new int[13][13];
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
			return new Grid(cells.length, cells, null);
		}
	}

	@Override
	protected void onPause() {
		camera.stopPreview();
		releaseCamera(); // release the camera immediately on pause event
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (camera == null) {
			camera = getCameraInstance();
			camera.startPreview();
		}
	}

	private void releaseCamera() {
		if (camera != null) {
			camera.release(); // release the camera for other applications
			camera = null;
		}
	}
}