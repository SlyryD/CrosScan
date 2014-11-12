package edu.dcc.crosscan;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

/**
 * Activity that allows user to scan crossword puzzles.
 * 
 * @author Ryan D.
 */
public class ScanActivity extends Activity {

	/**
	 * Request codes.
	 */
	public static final int CAMERA_REQUEST = 11, CONFIRM_GRID = 22,
			REJECT_GRID = 33;

	/**
	 * ScanActivity tag.
	 */
	private static final String TAG = "CrosScan/ScanActivity";

	/**
	 * View that shows camera frames.
	 */
	private SurfaceView preview;

	/**
	 * Surface wrapper.
	 */
	private SurfaceHolder previewHolder;

	/**
	 * Android camera hardware.
	 */
	private Camera camera;

	/**
	 * Whether camera is sending frames.
	 */
	private boolean inPreview;

	/**
	 * Whether photo is processing
	 */
	// TODO: Stop preview rotation while processing
	private boolean processing;

	/**
	 * Keeps track of screen rotation when picture is taken.
	 */
	private float rotationWhenTaken;

	/**
	 * Callback for when surface is created, changed, or destroyed.
	 */
	private SurfaceHolder.Callback surfaceCallback = new SurfaceCallback();

	/**
	 * Callback for when autofocus occurs.
	 */
	private Camera.AutoFocusCallback focusCallback = new FocusCallback();

	/**
	 * Callback for when picture is taken.
	 */
	private Camera.PictureCallback photoCallback = new PhotoCallback();

	/**
	 * Loads OpenCV.
	 */
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(final int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				/* Enable camera view to start receiving frames */
				Log.i(TAG, "OpenCV loaded successfully");
				break;
			default:
				super.onManagerConnected(status);
				break;
			}
		}
	};

	/**
	 * Log activity initialization and catch unhandled exceptions.
	 */
	public ScanActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());

		// Make sure camera releases on crash
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(final Thread t, final Throwable ex) {
				Log.wtf(TAG, ex);
				if (camera != null) {
					if (inPreview) {
						camera.stopPreview();
					}
					camera.release();
					camera = null;
					inPreview = false;
				}
			}
		});
	}

	/*
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Initialize activity layout
		Log.i(TAG, "called onCreate");
		setContentView(R.layout.activity_scan);

		// Get surface view and holder
		preview = (SurfaceView) findViewById(R.id.preview);
		previewHolder = preview.getHolder();
		previewHolder.addCallback(surfaceCallback);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
	}

	@Override
	public final void onResume() {
		super.onResume();
		Log.i(TAG, "called onResume");

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this,
				mLoaderCallback);

		// Obtain camera and start preview
		camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
		initPreview();
		startPreview();
	}

	@Override
	public final void onPause() {
		Log.i(TAG, "called onPause");

		if (inPreview) {
			camera.stopPreview();
		}

		camera.release();
		camera = null;
		inPreview = false;

		super.onPause();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "On configuration changed called");

		// Find current camera configuration
		initPreview();
	}

	/**
	 * When photo button pressed, focus and take photo.
	 * 
	 * @param view
	 */
	public void takePhoto(View view) {
		if (inPreview) {
			camera.autoFocus(focusCallback);
		}
	}

	/**
	 * Initialize camera preview.
	 */
	private void initPreview() {
		if (camera != null && previewHolder.getSurface() != null) {
			// Set display to camera preview
			try {
				camera.setPreviewDisplay(previewHolder);
			} catch (Throwable t) {
				Log.e(TAG, "Exception in setPreviewDisplay()", t);
				Toast.makeText(ScanActivity.this, t.getMessage(),
						Toast.LENGTH_LONG).show();
			}

			// Change camera configuration
			camera.setDisplayOrientation(getRotation());
		}

		// Change button configuration
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		int center = RelativeLayout.CENTER_HORIZONTAL;
		int align = RelativeLayout.ALIGN_PARENT_BOTTOM;
		switch (rotation) {
		case Surface.ROTATION_90:
			center = RelativeLayout.CENTER_VERTICAL;
			align = RelativeLayout.ALIGN_PARENT_RIGHT;
			break;
		case Surface.ROTATION_180:
			center = RelativeLayout.CENTER_HORIZONTAL;
			align = RelativeLayout.ALIGN_PARENT_TOP;
			break;
		case Surface.ROTATION_270:
			center = RelativeLayout.CENTER_VERTICAL;
			align = RelativeLayout.ALIGN_PARENT_LEFT;
			break;
		}

		View button = findViewById(R.id.take_photo);
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) button
				.getLayoutParams();
		for (int i = 0; i < layoutParams.getRules().length; i++) {
			layoutParams.addRule(i, 0);
		}

		layoutParams.addRule(center, RelativeLayout.TRUE);
		layoutParams.addRule(align, RelativeLayout.TRUE);
		button.setLayoutParams(layoutParams);
	}

	/**
	 * Get rotation degree of preview based on orientation and degrees.
	 * 
	 * @return rotation degrees
	 */
	private int getRotation() {
		// Find current camera configuration
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 90 * rotation;
		return (info.orientation - degrees + 360) % 360;
	}

	/**
	 * Start camera preview.
	 */
	private void startPreview() {
		if (camera != null) {
			camera.startPreview();
			inPreview = true;
		}
	}

	/**
	 * Handles changes to preview surface.
	 * 
	 * @author Ryan
	 */
	private class SurfaceCallback implements SurfaceHolder.Callback {

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.i(TAG, "Surface created");
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.i(TAG, "Surface changed");
			initPreview();
			startPreview();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.i(TAG, "Surface destroyed");
		}
	}

	private class FocusCallback implements Camera.AutoFocusCallback {

		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			camera.takePicture(null, null, photoCallback);
		}
	}

	/**
	 * Callback for when picture is taken.
	 * 
	 * @author Ryan
	 */
	private class PhotoCallback implements PictureCallback {
		@Override
		public void onPictureTaken(final byte[] data, final Camera camera) {
			// Prepare to rotate photo
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
			rotationWhenTaken = getRotation();

			// Process photo
			ProcessTask pTask = new ProcessTask();
			Log.i(TAG, "Call execute thread: " + Thread.currentThread());
			pTask.execute(data);
			try {
				String puzzle = pTask.get();
				Intent intent = new Intent(ScanActivity.this,
						NamePuzzleActivity.class);
				intent.putExtra(Constants.EXTRA_PUZZLE, puzzle);
				startActivity(intent);
			} catch (CancellationException e) {
				Log.e(TAG, "Processing cancelled", e);
				camera.startPreview();
				inPreview = true;
				return;
			} catch (InterruptedException e) {
				Log.e(TAG, "Processing interrupted", e);
				return;
			} catch (ExecutionException e) {
				Log.e(TAG, "Processing execution failed", e);
				return;
			}
		}
	}

	/**
	 * Handles image processing with OpenCV.
	 * 
	 * @author Ryan
	 */
	private class ProcessTask extends AsyncTask<byte[], Void, String> {

		/**
		 * Progress dialog for tasks.
		 */
		private ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = ProgressDialog.show(ScanActivity.this, "Processing...",
					"Press back button to cancel.", true, true,
					new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							ProcessTask.this.cancel(true);
						}
					});
			// dialog = ProgressDialog.show(getBaseContext(), "Hello", "Hello");
			// TODO: Remove debug
			Log.i(TAG, "Message thread: " + Thread.currentThread());
		}

		@Override
		protected String doInBackground(final byte[]... data) {
			// TODO: Remove debug
			Log.i(TAG, "Background thread: " + Thread.currentThread());

			// Get path of file directory
			String filePath = getFilePath();

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Get bitmap and rotate if necessary
			Bitmap src = BitmapFactory.decodeByteArray(data[0], 0,
					data[0].length);
			Matrix matrix = new Matrix();
			matrix.setRotate(rotationWhenTaken, src.getWidth() / 2.0f,
					src.getHeight() / 2.0f);
			Bitmap target = Bitmap.createBitmap(src, 0, 0, src.getWidth(),
					src.getHeight(), matrix, false);

			// Write file
			writeFile(filePath, target);

			// Use OpenCV to recognize and construct grid
			String result = recognizeGrid(filePath);
			// generateRandomGrid();

			// Check if cancelled
			// if (isCancelled()) {
			// return null;
			// }

			// TODO: Use OCR to recognize clues

			// Return string representation of puzzle
			return filePath + "\n" + result;
		}

		// private String generateRandomGrid() {
		// Random r = new Random(13254);
		// StringBuilder sb = new StringBuilder();
		// sb.append("13|13|");
		// for (int i = 0; i < 13; i++) {
		// for (int j = 0; j < 13; j++) {
		// sb.append(r.nextBoolean() && r.nextBoolean() ? "0" : "1");
		// sb.append(Constants.CHAR_SPACE).append("|");
		// }
		// }
		// return sb.toString();
		// }

		@Override
		protected void onPostExecute(final String result) {
			super.onPostExecute(result);
			dialog.dismiss();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dialog.dismiss();
		}

		/**
		 * Create a File for saving the image
		 * 
		 * @param type
		 * @return file path
		 */
		private String getFilePath() {
			// Find directory for photo storage
			File mediaStorageDir = new File(
					Environment
							.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
					"edu.dcc.crosscan");

			// Create the storage directory if it does not exist
			if (!mediaStorageDir.exists()) {
				if (!mediaStorageDir.mkdirs()) {
					Log.e(TAG, "failed to create directory");
					return null;
				}
			}

			// Create a media file name
			String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
					.format(new Date());
			return mediaStorageDir.getPath() + File.separator + "IMG_CrosScan_"
					+ timeStamp + ".jpg";
		}

		/**
		 * Writes image file.
		 * 
		 * @param filePath
		 * @param data
		 */
		private void writeFile(final String filePath, final Bitmap data) {
			try {
				// Write data to stream
				FileOutputStream fos = new FileOutputStream(filePath);
				data.compress(Bitmap.CompressFormat.JPEG, 100, fos);
				fos.flush();
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
		}

		/**
		 * Uses computer vision to recognize crossword grid.
		 * 
		 * @param filePath
		 * @return string representation of grid
		 */
		private String recognizeGrid(final String filePath) {
			// Read image
			Mat img = Highgui.imread(filePath);
			if (img.empty()) {
				Log.e(TAG, "Cannot open " + filePath);
				cancel(true);
			}
			Log.i(TAG, "Read " + filePath);

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Matrices
			Mat gray = new Mat();
			Mat edges = new Mat();
			Mat lines = new Mat();

			// Convert image to grayscale
			Imgproc.cvtColor(img, gray, Imgproc.COLOR_RGB2GRAY);
			Log.i(TAG, "Converted to grayscale");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Find edges using adaptive threshold
			Imgproc.adaptiveThreshold(gray, edges, 255,
					Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV,
					15, 15);
			Log.i(TAG, "Detected edges");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Crop to grid bounds
			Rect rect = findGridBounds(edges);
			edges = new Mat(edges, rect);
			gray = new Mat(gray, rect);
			Log.i(TAG, "Found grid bounds");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Find lines using Hough transform
			int maxGap = Math.min(rect.height, rect.width);
			Imgproc.HoughLinesP(edges, lines, 1, Math.PI
					/ Constants.DEGREES_180, 200, 0, maxGap);
			edges.release();
			Log.i(TAG, "Detected lines");

			// Error handling
			if (lines.cols() < 4) {
				Log.e(TAG, "Not enough lines detected.");
				// TODO: Display "please retake photo"
				cancel(true);
			}

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Collect lines
			HashSet<Line> lineSet = new HashSet<Line>();
			for (int index = 0; index < lines.cols(); index++) {
				Line line = new Line(lines.get(0, index));
				lineSet.add(line);
			}
			lines.release();
			Log.i(TAG, "Collected lines");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Find most frequent line angle
			int angle = 0;
			int[] angleCount = new int[Constants.DEGREES_180];
			for (Line line : lineSet) {
				int theta = findAngle(line);
				angleCount[theta] += 1;
				if (angleCount[theta] > angleCount[angle]) {
					angle = theta;
				}
			}
			System.out.println("Found most frequent angle");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Base orientation on most frequent angle
			int horizontal = angle;
			int vertical = normalize(angle + Constants.DEGREES_90);

			// Switch horizontal and vertical if necessary
			// Backwards because of rotation?
			if (Math.abs(vertical - Constants.DEGREES_90) > Math.abs(horizontal
					- Constants.DEGREES_90)) {
				int temp = vertical;
				vertical = horizontal;
				horizontal = temp;
			}
			Log.i(TAG, horizontal + " " + vertical);
			
			// TODO: Rotate image (perspective) along with lines

			// Keep horizontal and vertical lines
			ArrayList<Line> hLines = new ArrayList<Line>();
			ArrayList<Line> vLines = new ArrayList<Line>();
			for (Line line : lineSet) {
				if (closeToAngle(line, horizontal)) {
					hLines.add(line);
				} else if (closeToAngle(line, vertical)) {
					vLines.add(line);
				}
			}
			lineSet.clear();
			Log.i(TAG, "Created lists of horizontal and vertical lines");

			// Error handling
			if (hLines.size() < 2 || vLines.size() < 2) {
				Log.e(TAG, "Not enough lines detected.");
				// TODO: Display "please retake photo"
				cancel(true);
			}

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Sort horizontal lines
			Collections.sort(hLines, new LineComparator(true));
			Log.i(TAG, "Sorted horizontal lines");

			// Sort vertical lines
			Collections.sort(vLines, new LineComparator(false));
			Log.i(TAG, "Sorted vertical lines");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Threshold values
			int minCellSize = Math
					.round(Math.min(rect.height, rect.width) / 30f);
			int lineError = Math.round(minCellSize / 4f);

			// Compute largest set of equally spaced horizontal lines
			ArrayList<Line> hList = findEquallySpacedLines(hLines, true,
					horizontal, vertical, minCellSize, lineError);
			Log.i(TAG, "Found equally spaced horizontal lines");

			// Error handling
			if (hList.size() < 2) {
				System.err.println("Not enough horizontal lines found.");
				// TODO: Display "please retake photo"
				cancel(true);
			}

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Compute largest set of equally spaced vertical lines
			ArrayList<Line> vList = findEquallySpacedLines(vLines, false,
					horizontal, vertical, minCellSize, lineError);
			Log.i(TAG, "Found equally spaced vertical lines");

			// Error handling
			if (vList.size() < 2) {
				System.err.println("Not enough vertical lines found.");
				// TODO: Display "please retake photo"
				cancel(true);
			}

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Get puzzle size
			int height = hList.size() - 1, width = vList.size() - 1;
			Log.i(TAG, height + "x" + width);

			// Construct grid representation
			double[][] cells = new double[height][width];
			for (int row = 0; row < height; row++) {
				for (int col = 0; col < width; col++) {
					// Find bounds of cell
					int[] topLeft = findIntersection(hList.get(row),
							vList.get(col));
					int[] topRight = findIntersection(hList.get(row),
							vList.get(col + 1));
					int[] botLeft = findIntersection(hList.get(row + 1),
							vList.get(col));
					int[] botRight = findIntersection(hList.get(row + 1),
							vList.get(col + 1));
					int[] coords = { Math.max(topLeft[0], botLeft[0]),
							Math.min(topRight[0], botRight[0]),
							Math.max(topLeft[1], topRight[1]),
							Math.min(botLeft[1], botRight[1]) };

					// Calculate average intensity of cell
					for (int x = Math.max(0, coords[0]); x <= Math.min(
							coords[1], rect.width); x++) {
						for (int y = Math.max(0, coords[2]); y <= Math.min(
								coords[3], rect.height); y++) {
							try {
								cells[row][col] += gray.get(y, x)[0];
							} catch (Exception e) {
								System.err.println("Unable to get pixel data");
								break;
							}
						}
					}
					cells[row][col] /= ((coords[1] - coords[0]) * (coords[3] - coords[2]));
				}
			}
			img.release();
			gray.release();

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Decide whether cells are black or white
			adjustColors(cells);

			// Construct data
			StringBuilder data = new StringBuilder();
			data.append(height + "|").append(width + "|");
			for (int i = 0; i < cells.length; i++) {
				for (int j = 0; j < cells[i].length; j++) {
					if (cells[i][j] != 0) {
						data.append("1");
					} else {
						data.append("0");
					}
					data.append(Constants.CHAR_SPACE).append("|");
				}
			}

			// Grid representation
			Log.i(TAG, data.toString());
			return data.toString();
		}

		/**
		 * Find rectangular boundaries of grid.
		 * 
		 * @param thresh
		 * @return bounds
		 */
		private Rect findGridBounds(final Mat thresh) {
			// Keep track of largest area
			double largestArea = 0;
			int largestIndex = 0;
			Rect bounds = new Rect();

			// List for storing contours
			Mat gray = thresh.clone();
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_CCOMP,
					Imgproc.CHAIN_APPROX_SIMPLE);

			// Iterate through each contour
			for (int i = 0; i < contours.size(); i++) {
				// Find the area of contour
				double area = Imgproc.contourArea(contours.get(i), false);
				if (area > largestArea) {
					largestArea = area;
					System.out.println(i + " area  " + area);

					// Store the index of largest contour
					largestIndex = i;

					// Find the bounding rectangle for biggest contour
					bounds = Imgproc.boundingRect(contours.get(i));
				}
			}

			// Draw the contour and rectangle
			Scalar color = new Scalar(255, 0, 0);
			Imgproc.drawContours(gray, contours, largestIndex, color, 2);
			Core.rectangle(gray, new Point(bounds.x, bounds.y), new Point(
					bounds.x + bounds.width, bounds.y + bounds.height), color,
					2, 8, 0);
			return bounds;
		}

		/**
		 * Returns line angle between 0 and pi radians (exclusive).
		 * 
		 * @param line
		 * @return angle
		 */
		private int findAngle(final Line line) {
			double x = line.x1 - line.x2, y = line.y1 - line.y2;
			if (x == 0) {
				return Constants.DEGREES_90;
			}
			return (int) Math.round(Constants.DEGREES_180
					* normalize(Math.atan(y / x)) / Math.PI);
		}

		/**
		 * Returns equivalent angle between 0 and pi radians (exclusive).
		 * 
		 * @param theta
		 * @return normalized angle
		 */
		private double normalize(final double theta) {
			return (theta + (2 * Math.PI)) % Math.PI;
		}

		/**
		 * Returns equivalent angle between 0 and 180 degrees (exclusive).
		 * 
		 * @param theta
		 * @return normalized angle
		 */
		private int normalize(final int theta) {
			return (theta + 360) % Constants.DEGREES_180;
		}

		/**
		 * Returns whether given line is close to angle.
		 * 
		 * @param line
		 * @return horizontal
		 */
		private boolean closeToAngle(final Line line, final int angle) {
			int threshold = 4, theta = findAngle(line);
			return Math.abs(theta - angle) < threshold
					|| Math.abs(theta - angle - Constants.DEGREES_180) < threshold;
		}

		/**
		 * Finds largest list of consecutive equally spaced lines.
		 * 
		 * @param lines
		 * @param hor
		 * @return result list
		 */
		@SuppressLint("UseSparseArrays")
		private ArrayList<Line> findEquallySpacedLines(
				final ArrayList<Line> lines, final boolean hor,
				final int horizontal, final int vertical,
				final int minCellSize, final int lineError) {
			// Create map of positions of each line
			HashMap<Integer, Line> positions = new HashMap<Integer, Line>();
			int target = hor ? horizontal : vertical;
			for (Line line : lines) {
				int position = getPosition(line, hor);
				if (!positions.containsKey(position)
						|| Math.abs(findAngle(line) - target) < Math
								.abs(findAngle(positions.get(position))
										- target)) {
					positions.put(position, line);
				}
			}
			lines.retainAll(positions.values());

			// List of equally spaced lines
			ArrayList<Line> resultList = new ArrayList<Line>();
			for (int i = 0; i < lines.size(); i++) {
				for (int j = i + 1; j < lines.size(); j++) {
					// Construct temporary list of equally spaced lines
					ArrayList<Line> tempList = new ArrayList<Line>();
					Line line1 = lines.get(i), line2 = lines.get(j);
					// Keep track of position of last line
					int currentPos = getPosition(line2, hor);
					// Calculate distance
					int distance = currentPos - getPosition(line1, hor);
					// Rule out small distances between lines
					if (distance < minCellSize) {
						continue;
					}
					// Add first couple of lines to set
					tempList.add(line1);
					tempList.add(line2);
					// Get last key
					int lastKey = getPosition(lines.get(lines.size() - 1), hor);
					while (currentPos < lastKey) {
						int nextPos = currentPos + distance;
						int candidate = nextPos;
						if (!positions.containsKey(nextPos)) {
							// Find lines immediately before and after next
							// position
							Integer floor = null, ceil = null;
							for (int k = j + 1; k < lines.size(); k++) {
								int pos = getPosition(lines.get(k), hor);
								if (pos <= nextPos) {
									floor = pos;
								}
								if (pos > nextPos) {
									ceil = pos;
									break;
								}
							}
							if (ceil == null) {
								// Accept floor if in range
								if (floor == null || floor == currentPos) {
									break;
								} else if (nextPos - floor < lineError) {
									candidate = floor;
								} else {
									break;
								}
							} else if (floor == null) {
								// Accept ceiling if in range
								if (ceil - nextPos < lineError) {
									candidate = ceil;
								} else {
									break;
								}
							} else {
								// Accept floor then ceiling if in range
								if (nextPos - floor < lineError) {
									candidate = floor;
								} else if (ceil - nextPos < lineError) {
									candidate = ceil;
								} else {
									break;
								}
							}
						}
						// TODO: Remove? Angle difference natural
						// Rule out lines with different angles
						// if (Math.abs(findAngle(positions.get(currentPos))
						// - findAngle(positions.get(candidate))) > DEGREE) {
						// break;
						// } else {
						tempList.add(positions.get(candidate));
						currentPos = candidate;
						// }
					}
					// Update result list if temporary list is longer
					if (tempList.size() > resultList.size()) {
						resultList = tempList;
					}
				}
			}
			return resultList;
		}

		/**
		 * Returns vertical or horizontal position of line.
		 * 
		 * @param line
		 * @param horizontal
		 * @return position
		 */
		private int getPosition(final Line line, final boolean horizontal) {
			if (horizontal) {
				// Return vertical position
				return (int) Math.round((line.y1 + line.y2) / 2.0); // Midpoint
			} else {
				// Return horizontal position
				return (int) Math.round((line.x1 + line.x2) / 2.0); // Midpoint
			}
		}

		/**
		 * Find intersection of given lines.
		 * 
		 * @param line1
		 * @param line2
		 * @return
		 */
		private int[] findIntersection(final Line line1, final Line line2) {
			int[] coords = new int[2];
			float[] x = { line1.x1, line1.x2, line2.x1, line2.x2 };
			float[] y = { line1.y1, line1.y2, line2.y1, line2.y2 };
			coords[0] = Math.round((x[0] * x[2] * y[1] - x[1] * x[2] * y[0]
					- x[0] * x[3] * y[1] + x[1] * x[3] * y[0] - x[0] * x[2]
					* y[3] + x[0] * x[3] * y[2] + x[1] * x[2] * y[3] - x[1]
					* x[3] * y[2])
					/ (x[0] * y[2] - x[2] * y[0] - x[0] * y[3] - x[1] * y[2]
							+ x[2] * y[1] + x[3] * y[0] + x[1] * y[3] - x[3]
							* y[1]));
			coords[1] = Math.round((x[0] * y[1] * y[2] - x[1] * y[0] * y[2]
					- x[0] * y[1] * y[3] + x[1] * y[0] * y[3] - x[2] * y[0]
					* y[3] + x[3] * y[0] * y[2] + x[2] * y[1] * y[3] - x[3]
					* y[1] * y[2])
					/ (x[0] * y[2] - x[2] * y[0] - x[0] * y[3] - x[1] * y[2]
							+ x[2] * y[1] + x[3] * y[0] + x[1] * y[3] - x[3]
							* y[1]));
			return coords;
		}
		
		private void adjustColors(final double[][] cells) {
			// Initialize grayscale mat
			int rows = cells.length, cols = cells[0].length;
			Mat colorMat = new Mat(cells.length, cells[0].length, CvType.CV_8UC1);

			// Put average cell colors into mat
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < cols; col++) {
					colorMat.put(row, col, cells[row][col]);
				}
			}

			// Find colors using adaptive threshold
			Imgproc.adaptiveThreshold(colorMat, colorMat, 255,
					Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 3, 15);

			// Put values back into array
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < cols; col++) {
					cells[row][col] = colorMat.get(row, col)[0];
				}
			}
			
			// Release mat
			System.out.println("Got colors");
			Highgui.imwrite("color-mat.jpg", colorMat);
			colorMat.release();
		}

		// private String generateRandomGrid() {
		// Random r = new Random(13254);
		// StringBuilder sb = new StringBuilder();
		// sb.append("13|13|");
		// for (int i = 0; i < 13; i++) {
		// for (int j = 0; j < 13; j++) {
		// sb.append(r.nextBoolean() && r.nextBoolean() ? "0" : "1");
		// sb.append(Constants.CHAR_SPACE).append("|");
		// }
		// }
		// return sb.toString();
		// }

		/**
		 * Contains Line data.
		 * 
		 * @author Ryan
		 */
		private final class Line {

			public int x1, y1, x2, y2;

			/**
			 * Constructs line from coordinate double array.
			 * 
			 * @param line
			 */
			private Line(final double[] line) {
				this.x1 = (int) line[0];
				this.y1 = (int) line[1];
				this.x2 = (int) line[2];
				this.y2 = (int) line[3];
			}

			@Override
			public String toString() {
				return "[" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "]";
			}
		}

		/**
		 * Compares lines based on relative position.
		 * 
		 * @author Ryan
		 */
		private final class LineComparator implements Comparator<Line> {

			/**
			 * Whether to compare horizontal or vertical lines.
			 */
			private boolean horizontal;

			/**
			 * Constructs line comparator.
			 * 
			 * @param horizontal
			 */
			private LineComparator(final boolean horizontal) {
				this.horizontal = horizontal;
			}

			@Override
			public int compare(final Line line1, final Line line2) {
				int[] comp = { getPosition(line1, horizontal),
						getPosition(line2, horizontal),
						getPosition(line1, !horizontal),
						getPosition(line2, !horizontal) };
				if (comp[0] == comp[1]) {
					return comp[2] - comp[3];
				} else {
					return comp[0] - comp[1];
				}
			}
		}
	}

}