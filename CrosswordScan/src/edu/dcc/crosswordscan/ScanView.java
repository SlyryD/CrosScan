package edu.dcc.crosswordscan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ScanView extends SurfaceView implements SurfaceHolder.Callback,
		PictureCallback {

	private static final String TAG = "Sample/ScanView";
	private String filePath;

	private SurfaceHolder mHolder;
	private Camera mCamera;

	@SuppressWarnings("deprecation")
	public ScanView(Context context, Camera camera) {
		super(context);
		mCamera = camera;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		Log.i(TAG, "ScanView instantiated");
	}

	public ScanView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.i(TAG, "ScanView instantiated");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the
		// preview.
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();

		} catch (Exception e) {
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

	public void focus() {
		mCamera.autoFocus(null);
	}

	public String takePicture() {
		Log.i(TAG, "Taking picture");
		// Retrieve picture filename
		getOutputMediaFile();

		// Postview and jpeg are sent in the same buffers if the queue is not
		// empty when performing a capture.
		// Clear up buffers to avoid mCamera.takePicture to be stuck because of
		// a memory issue
		mCamera.setPreviewCallback(null);

		// PictureCallback is implemented by the current class
		mCamera.takePicture(null, null, this);

		return filePath;
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		Log.i(TAG, "Saving a bitmap to file");

		// Write image to file (in jpeg format)
		try {
			FileOutputStream fos = new FileOutputStream(filePath);
			fos.write(data);
			fos.close();
			// MediaStore.Images.Media.insertImage(getContentResolver(),
			// pictureFile.getAbsolutePath(), pictureFile.getName(),
			// pictureFile.getName());
		} catch (IOException e) {
			Log.e(TAG, "Could not write image to file");
		}
	}

	/**
	 * Create a File for saving the image
	 * 
	 * @param type
	 * @return
	 */
	private void getOutputMediaFile() {
		// Find directory for photo storage
		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"edu.dcc.crosswordscan");

		System.out.println("Media storage directory: "
				+ mediaStorageDir.getName());
		System.out.println("Media storage directory: "
				+ mediaStorageDir.getAbsolutePath());

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				filePath = null;
				return;
			}
		}

		// Use unique identifier for file
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
		String currentDateandTime = sdf.format(new Date());
		filePath = mediaStorageDir.getPath() + File.separator
				+ "IMG_CrosswordScan_" + currentDateandTime + ".jpg";
	}
}
