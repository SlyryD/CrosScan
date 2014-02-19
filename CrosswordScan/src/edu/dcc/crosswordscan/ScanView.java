package edu.dcc.crosswordscan;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ScanView extends SurfaceView implements SurfaceHolder.Callback {

	private static final String TAG = "CrosswordScan/ScanView";

	private SurfaceHolder mHolder;
	private Camera mCamera;
	
	private int degrees = 90;

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

	public ScanView(Context context, AttributeSet attr) {
		super(context, attr);
	}
	
	public void releaseCamera() {
		mCamera = null;
	}
	
	public void initCamera(Camera camera) {
		mCamera = camera;
	}

	@SuppressLint("NewApi")
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mCamera == null) {
			return;
		}
		
		// Set parameters
		Camera.Parameters params = mCamera.getParameters();

		// set the focus mode
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

		// set Camera parameters
		mCamera.setParameters(params);

		int degrees = 0;
		switch (getDisplay().getRotation()) {
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
		this.degrees = degrees;
		mCamera.setDisplayOrientation(degrees);

		// Start preview
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}
	
	public int getDegrees() {
		return degrees;
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
		
		if (mCamera == null) {
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
			Log.e(TAG, "Tried to stop non-existent preview");
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// get Camera parameters
		Camera.Parameters params = mCamera.getParameters();

		// set the focus mode
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

		// set Camera parameters
		mCamera.setParameters(params);

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();

		} catch (Exception e) {
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}
}
