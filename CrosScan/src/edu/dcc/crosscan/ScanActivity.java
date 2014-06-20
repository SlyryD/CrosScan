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
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
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
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

/**
 * Activity that allows user to scan crossword puzzles.
 * 
 * @author Ryan D.
 */
public class ScanActivity extends Activity {

	// Request codes
	public static final int CAMERA_REQUEST = 11, CONFIRM_GRID = 22,
			REJECT_GRID = 33;
	public static final String GRID = "grid";
	public static final String PHOTO = "photo";
	private static final String TAG = "CrosswordScan/ScanActivity";

	// Camera
	private Camera mCamera;
	// Camera view
	private ScanView mPreview;
	private FrameLayout preview;

	// Task cancel variable
	private AsyncTask<?, ?, ?> cancelTask;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				/* Enable camera view to start receiving frames */
				Log.i(TAG, "OpenCV loaded successfully");
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public ScanActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Initialize activity layout
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);

		// Create an instance of Camera
		mCamera = getCameraInstance();

		// Create preview and set as content of activity
		mPreview = new ScanView(this);
		mPreview.initCamera(mCamera);
		preview = (FrameLayout) findViewById(R.id.preview);
		preview.addView(mPreview);
	}

	@Override
	protected void onStart() {
		Log.i(TAG, "called onStart");
		super.onStart();
		preview.setOnClickListener(new FocusListener());
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "called onResume");
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this,
				mLoaderCallback);
		if (mCamera == null) {
			mCamera = getCameraInstance();
			mPreview.initCamera(mCamera);
		}
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "called onPause");
		super.onPause();
		releaseCamera();
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "called onStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "called onDestroy");
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && cancelTask != null) {
			cancelTask.cancel(true);
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	private void releaseCamera() {
		mPreview.releaseCamera();
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
			Log.i(TAG, "Camera available");
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			Log.e(TAG, "Camera unavailable");
		}
		return c; // returns null if camera is unavailable
	}

	/**
	 * Respond to button press by taking photo
	 * 
	 * @param view
	 */
	public void takePhoto(View view) {
		// Once photo taken, view no longer clickable
		preview.setOnClickListener(null);
		// Take picture
		mCamera.takePicture(null, null, new SaveAndProcessCallback());
	}

	private class FocusListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Log.i(TAG, "onTouch logged");
			mCamera.autoFocus(null);
		}
	}

	private class SaveAndProcessCallback implements PictureCallback {
		@SuppressLint("NewApi")
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// Release camera
			releaseCamera();

			// Save photo
			SavePhotoTask spTask = new SavePhotoTask();
			cancelTask = spTask;
			spTask.execute(data);
			String result = null;
			try {
				result = spTask.get();
			} catch (CancellationException e) {
				Log.e(TAG, "Take photo cancelled");
				ScanActivity.this.recreate();
				return;
			} catch (InterruptedException e) {
				Log.e(TAG, "Take photo interrupted");
				return;
			} catch (ExecutionException e) {
				Log.e(TAG, "Take photo execution failed");
				return;
			} finally {
				cancelTask = null;
			}

			// Process photo
			ProcessTask pTask = new ProcessTask();
			cancelTask = pTask;
			pTask.execute(result);
			try {
				String grid = pTask.get();
				Intent intent = new Intent(ScanActivity.this,
						NamePuzzleActivity.class);
				intent.putExtra(GRID, grid);
				intent.putExtra(PHOTO, result);
				startActivity(intent);
			} catch (CancellationException e) {
				Log.e(TAG, "Process image cancelled");
				ScanActivity.this.recreate();
				return;
			} catch (InterruptedException e) {
				Log.e(TAG, "Process image interrupted");
				return;
			} catch (ExecutionException e) {
				Log.e(TAG, "Process image execution failed");
				return;
			} finally {
				cancelTask = null;
			}
		}
	}

	private class SavePhotoTask extends AsyncTask<byte[], Void, String> {

		ProgressDialog dialog;

		protected void onPreExecute() {
			dialog = ProgressDialog.show(ScanActivity.this, "Saving photo...",
					"Press back button to cancel.", true, true);
			// TODO: Remove debug
			Log.i(TAG, "Message thread: " + Thread.currentThread());
		}

		@Override
		protected String doInBackground(byte[]... data) {
			// Get path of file directory
			String filePath = getFilePath();

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Write file
			writeFile(filePath, data[0]);

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// TODO: Remove debug
			Log.i(TAG, "Background thread: " + Thread.currentThread());

			// Take photo and return file path
			return filePath;
		}

		/**
		 * Create a File for saving the image
		 * 
		 * @param type
		 * @return
		 */
		private String getFilePath() {
			// Find directory for photo storage
			File mediaStorageDir = new File(
					Environment
							.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
					"edu.dcc.crosscan");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Create the storage directory if it does not exist
			if (!mediaStorageDir.exists()) {
				if (!mediaStorageDir.mkdirs()) {
					Log.d(TAG, "failed to create directory");
					return null;
				}
			}

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Create a media file name
			String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
					.format(new Date());
			return mediaStorageDir.getPath() + File.separator + "IMG_CrosScan_"
					+ timeStamp + ".jpg";
		}

		private void writeFile(String filePath, byte[] data) {
			try {
				// Create stream
				FileOutputStream fos = new FileOutputStream(filePath);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			dialog.dismiss();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dialog.dismiss();
		}
	}

	private class ProcessTask extends AsyncTask<String, Void, String> {

		ProgressDialog dialog;

		protected void onPreExecute() {
			dialog = ProgressDialog.show(ScanActivity.this, "Processing...",
					"Press back button to cancel.", true, true);
			// TODO: Remove debug
			Log.i(TAG, "Message thread: " + Thread.currentThread());
		}

		@Override
		protected String doInBackground(String... filePaths) {
			// TODO: Remove debug
			Log.i(TAG, "Background thread: " + Thread.currentThread());

			// Use OpenCV to recognize and construct grid
			String result = recognizeGrid(filePaths[0]);
			// generateRandomGrid();

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// TODO: Use OCR to recognize clues

			// Return string representation of puzzle
			return result;
		}

		private String recognizeGrid(String filePath) {
			// Read image
			Mat img = Highgui.imread(filePath);
			if (img.empty()) {
				Log.e(TAG, "Cannot open " + filePath);
				System.exit(1);
			}
			Log.i(TAG, "Read " + filePath);

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Rotate image
			Core.flip(img.t(), img, mPreview.getDegrees() / 90);

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
			Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 200, 0, maxGap);
			edges.release();
			Log.i(TAG, "Detected lines");

			// Error handling
			if (lines.cols() < 4) {
				Log.e(TAG, "Not enough lines detected.");
				// TODO: Display "please retake photo"
				cancelTask.cancel(true);
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
			int[] angleCount = new int[180];
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
			int vertical = normalize(angle + 90);

			// Switch horizontal and vertical if necessary
			// Backwards because of rotation?
			if (Math.abs(vertical - 90) > Math.abs(horizontal - 90)) {
				int temp = vertical;
				vertical = horizontal;
				horizontal = temp;
			}
			Log.i(TAG, horizontal + " " + vertical);

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
				cancelTask.cancel(true);
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
				cancelTask.cancel(true);
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
				cancelTask.cancel(true);
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
			double cutoff = findCutoff(cells);

			// Construct data
			StringBuilder data = new StringBuilder();
			data.append("version: 1\n");
			data.append(height + "|").append(width + "|");
			for (int i = 0; i < cells.length; i++) {
				for (int j = 0; j < cells[i].length; j++) {
					if (cells[i][j] >= cutoff) {
						data.append("1").append("|");
					} else {
						data.append("0").append("|");
					}
					data.append((char) 0).append("|");
				}
			}

			// Grid representation
			Log.i(TAG, data.toString());
			return data.toString();
		}

		/**
		 * Find rectangular boundaries of grid
		 * 
		 * @param thresh
		 * @return bounds
		 */
		private Rect findGridBounds(Mat thresh) {
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
		 * Returns line angle between 0 and pi radians (exclusive)
		 * 
		 * @param line
		 * @return angle
		 */
		private int findAngle(Line line) {
			double x = line.x1 - line.x2, y = line.y1 - line.y2;
			if (x == 0) {
				return 90;
			}
			return (int) Math
					.round(180 * normalize(Math.atan(y / x)) / Math.PI);
		}

		/**
		 * Returns equivalent angle between 0 and pi radians (exclusive)
		 * 
		 * @param theta
		 * @return normalized angle
		 */
		private double normalize(double theta) {
			return (theta + (2 * Math.PI)) % Math.PI;
		}

		/**
		 * Returns equivalent angle between 0 and 180 degrees (exclusive)
		 * 
		 * @param theta
		 * @return normalized angle
		 */
		private int normalize(int theta) {
			return (theta + 360) % 180;
		}

		/**
		 * Returns whether given line is close to angle
		 * 
		 * @param line
		 * @return horizontal
		 */
		private boolean closeToAngle(Line line, int angle) {
			int threshold = 4, theta = findAngle(line);
			return Math.abs(theta - angle) < threshold
					|| Math.abs(theta - angle - 180) < threshold;
		}

		/**
		 * Finds largest list of consecutive equally spaced lines
		 * 
		 * @param lines
		 * @param hor
		 * @return result list
		 */
		private ArrayList<Line> findEquallySpacedLines(ArrayList<Line> lines,
				boolean hor, int horizontal, int vertical, int minCellSize,
				int lineError) {
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
		 * Returns vertical or horizontal position of line
		 * 
		 * @param line
		 * @param horizontal
		 * @return position
		 */
		private int getPosition(Line line, boolean horizontal) {
			if (horizontal) {
				// Return vertical position
				return (int) Math.round((line.y1 + line.y2) / 2.0); // Midpoint
			} else {
				// Return horizontal position
				return (int) Math.round((line.x1 + line.x2) / 2.0); // Midpoint
			}
		}

		/**
		 * Find intersection of given lines
		 * 
		 * @param line1
		 * @param line2
		 * @return
		 */
		private int[] findIntersection(Line line1, Line line2) {
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

		/**
		 * Find cutoff value for labeling black and white cells
		 * 
		 * @param cells
		 * @return cutoff
		 */
		private double findCutoff(double[][] cells) {
			double min = cells[0][0], max = cells[0][0];
			int rows = cells.length, cols = cells[0].length;
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < cols; col++) {
					if (cells[row][col] < min) {
						min = cells[row][col];
					} else if (cells[row][col] > max) {
						max = cells[row][col];
					}
				}
			}
			return 0.40 * (min + max);
		}

		private String generateRandomGrid() {
			Random r = new Random(13254);
			StringBuilder sb = new StringBuilder();
			sb.append("13|13|");
			for (int i = 0; i < 13; i++) {
				for (int j = 0; j < 13; j++) {
					sb.append(r.nextBoolean() && r.nextBoolean() ? "0" : "1");
					sb.append("0|");
				}
			}
			return sb.toString();
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			dialog.dismiss();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dialog.dismiss();
		}

		/**
		 * Contains Line data
		 * 
		 * @author Ryan
		 */
		private class Line {

			public int x1, y1, x2, y2;

			private Line(double[] line) {
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
		 * Compares lines based on relative position
		 * 
		 * @author Ryan
		 */
		private class LineComparator implements Comparator<Line> {

			private boolean horizontal;

			private LineComparator(boolean horizontal) {
				this.horizontal = horizontal;
			}

			@Override
			public int compare(Line line1, Line line2) {
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