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
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
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

	// OpenCV fields
	private static final String TAG = "CrosswordScan/ScanActivity";
	public static double horizontal = 0, vertical = Math.PI / 2;
	public static final double DEGREE = Math.PI / 180;
	public static final double DEGREE_90 = Math.PI / 2;
	public static final double DEGREE_180 = Math.PI;
	public static final double DEGREE_360 = 2 * Math.PI;
	public static final double THRESHOLD = 4 * DEGREE;
	public static final double MIN_CELL_SIZE = 50;
	public static final double LINE_ERROR = 5;

	// Camera
	private Camera mCamera;
	// Camera view
	private ScanView scanView;
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

		// Set content view
		setContentView(R.layout.activity_scan);

		preview = (FrameLayout) findViewById(R.id.preview);

		mCamera = getCameraInstance();

		scanView = new ScanView(this, mCamera);

		preview.addView(scanView);
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "called onPause");
		super.onPause();
		releaseCamera();
		scanView.releaseCamera();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "called onResume");
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this,
				mLoaderCallback);
		if (mCamera == null) {
			mCamera = getCameraInstance();
			scanView.initCamera(mCamera);
		}
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "called onDestroy");
		super.onDestroy();
		releaseCamera();
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "called onStop");
		super.onStop();
	}

	@Override
	protected void onStart() {
		Log.i(TAG, "called onStart");
		super.onStart();
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
		if (mCamera != null) {
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

	public void focus(View view) {
		Log.i(TAG, "onTouch logged");
		mCamera.autoFocus(null);
	}

	private class SaveAndProcessCallback implements PictureCallback {
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

	/**
	 * Respond to button press by taking photo
	 * 
	 * @param view
	 */
	public void takePhoto(View view) {
		// Auto focus before photo
		mCamera.autoFocus(new AutoFocusCallback() {
			@Override
			public void onAutoFocus(boolean success, Camera camera) {
				mCamera.takePicture(null, null, new SaveAndProcessCallback());
			}
		});
	}

	private class SavePhotoTask extends AsyncTask<byte[], Void, String> {

		final private ProgressDialog dialog = new ProgressDialog(
				ScanActivity.this);

		protected void onPreExecute() {
			dialog.setMessage("Saving photo");
			dialog.show();
		}

		@Override
		protected String doInBackground(byte[]... data) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
			return mediaStorageDir.getPath() + File.separator
					+ "IMG_CrosScan_" + timeStamp + ".jpg";
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
			dialog.dismiss();
		}

		@Override
		protected void onCancelled() {
			dialog.dismiss();
			super.onCancelled();
		}
	}

	private class ProcessTask extends AsyncTask<String, Void, String> {

		final private ProgressDialog dialog = new ProgressDialog(
				ScanActivity.this);

		protected void onPreExecute() {
			dialog.setMessage("Processing");
			dialog.show();
		}

		@Override
		protected String doInBackground(String... filePaths) {
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

		private class Line {

			public double x1, y1, x2, y2;

			public Line(double[] line) {
				this.x1 = line[0];
				this.y1 = line[1];
				this.x2 = line[2];
				this.y2 = line[3];
			}

			@Override
			public String toString() {
				return "[" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "]";
			}
		}

		/**
		 * Returns equivalent angle between 0 and pi radians (exclusive)
		 * 
		 * @param theta
		 * @return normalized angle
		 */
		private double normalize(double theta) {
			return (theta + DEGREE_360) % DEGREE_180;
		}

		/**
		 * Returns line angle between 0 and pi radians (exclusive)
		 * 
		 * @param line
		 * @return angle
		 */
		private double findAngle(Line line) {
			double x = line.x1 - line.x2, y = line.y1 - line.y2;
			if (x == 0) {
				return DEGREE_90;
			}
			return normalize(Math.atan(y / x));
		}

		/**
		 * Returns whether given line is vertical
		 * 
		 * @param line
		 * @return vertical
		 */
		private boolean isVertical(Line line) {
			double theta = findAngle(line);
			return Math.abs(theta - vertical) < THRESHOLD
					|| Math.abs(theta - vertical - DEGREE_180) < THRESHOLD;
			// theta == vertical;
		}

		/**
		 * Returns whether given line is horizontal
		 * 
		 * @param line
		 * @return horizontal
		 */
		private boolean isHorizontal(Line line) {
			double theta = findAngle(line);
			return Math.abs(theta - horizontal) < THRESHOLD
					|| Math.abs(theta - horizontal - DEGREE_180) < THRESHOLD;
			// theta == horizontal;
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
				double[] comp = { getPosition(line1, horizontal),
						getPosition(line2, horizontal),
						getPosition(line1, !horizontal),
						getPosition(line2, !horizontal) };
				if (comp[0] == comp[1]) {
					return comp[2] < comp[3] ? -1 : 1;
				} else {
					return comp[0] < comp[1] ? -1 : 1;
				}
			}
		}

		/**
		 * Returns vertical or horizontal position of line
		 * 
		 * @param line
		 * @param horizontal
		 * @return position
		 */
		private double getPosition(Line line, boolean horizontal) {
			if (horizontal) {
				// Return vertical position
				return Math.round((line.y1 + line.y2) / 2); // Midpoint
			} else {
				// Return horizontal position
				return Math.round((line.x1 + line.x2) / 2); // Midpoint
			}
		}

		/**
		 * Finds largest list of consecutive equally spaced lines
		 * 
		 * @param lines
		 * @param hor
		 * @return result list
		 */
		// TODO: Find workaround for suppression
		@SuppressLint("NewApi")
		private ArrayList<Line> findEquallySpacedLines(ArrayList<Line> lines,
				boolean hor) {
			// Create map of positions of each line
			TreeMap<Double, Line> positions = new TreeMap<Double, Line>();
			double target = hor ? horizontal : vertical;
			for (Line line : lines) {
				double position = getPosition(line, hor);
				if (!positions.containsKey(position)
						|| Math.abs(findAngle(line) - target) < Math
								.abs(findAngle(positions.get(position))
										- target)) {
					positions.put(position, line);
				}
				if (isCancelled()) {
					return null;
				}
			}

			// List of equally spaced lines
			ArrayList<Line> resultList = new ArrayList<Line>();
			for (double i : positions.keySet()) {
				for (double j : positions.tailMap(i + 1).keySet()) {
					// Construct temporary list of equally spaced lines
					ArrayList<Line> tempList = new ArrayList<Line>();
					double distance = j - i;
					// Rule out small distances between lines
					if (distance < MIN_CELL_SIZE) {
						continue;
					}
					// Add first couple of lines to set
					tempList.add(positions.get(i));
					tempList.add(positions.get(j));
					// Rule out lines with different angles
					if (Math.abs(findAngle(tempList.get(0))
							- findAngle(tempList.get(1))) > DEGREE) {
						continue;
					}
					// Keep track of position of last line
					double currentPos = j;
					// Loop until last key
					while (currentPos < positions.lastKey()) {
						double nextPos = currentPos + distance;
						double candidate = nextPos;
						if (!positions.containsKey(nextPos)) {
							Double floor = positions.lowerKey(nextPos), ceil = positions
									.higherKey(nextPos);
							if (ceil == null) {
								// Accept floor if in range
								if (floor == null || floor == currentPos) {
									break;
								} else if (nextPos - floor < LINE_ERROR) {
									candidate = floor;
								} else {
									break;
								}
							} else if (floor == null || floor == currentPos) {
								// Accept ceiling if in range
								if (ceil - nextPos < LINE_ERROR) {
									candidate = ceil;
								} else {
									break;
								}
							} else {
								// Accept floor then ceiling if in range
								if (nextPos - floor < LINE_ERROR) {
									candidate = floor;
								} else if (ceil - nextPos < LINE_ERROR) {
									candidate = ceil;
								} else {
									break;
								}
							}
						}
						// Rule out lines with different angles
						if (Math.abs(findAngle(positions.get(currentPos))
								- findAngle(positions.get(candidate))) > DEGREE) {
							break;
						} else {
							tempList.add(positions.get(candidate));
							currentPos = candidate;
						}
						if (isCancelled()) {
							return null;
						}
					}
					// Update result list if temporary list is longer
					if (tempList.size() > resultList.size()) {
						resultList = tempList;
					}
					if (isCancelled()) {
						return null;
					}
				}
			}
			return resultList;
		}

		private double[] findIntersection(Line line1, Line line2) {
			double[] coords = new double[2];
			double x[] = { line1.x1, line1.x2, line2.x1, line2.x2 };
			double y[] = { line1.y1, line1.y2, line2.y1, line2.y2 };
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
			return (min + max) / 2;
		}
		
		private boolean isAllOneColor(double[] cells, double cutoff) {
			boolean white = false, black = false;
			for (double d : cells) {
				if (white && black) {
					return false;
				}
				if (d >= cutoff) {
					white = true;
				} else {
					black = true;
				}
			}
			return true;
		}

		private boolean isAllOneColor(double[][] cells, int col,
				double cutoff) {
			boolean white = false, black = false;
			for (int i = 0; i < cells.length; i++) {
				if (white && black) {
					return false;
				}
				if (cells[i][col] >= cutoff) {
					white = true;
				} else {
					black = true;
				}
			}
			return true;
		}

		private String recognizeGrid(String filePath) {
			// Read image
			Mat img = Highgui.imread(filePath);
			Log.i(TAG, "Read " + filePath);
			if (img.empty()) {
				Log.e(TAG, "Cannot open " + filePath);
				System.exit(1);
			}

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Rotate image
			Mat imgT = img.t();
			int amount = scanView.getDegrees() / 90;
			Core.flip(img.t(), imgT, amount);
			Imgproc.resize(imgT, img, amount % 2 == 0 ? img.size() : new Size(
					img.rows(), img.cols()));
			imgT.release();

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Convert image to grayscale
			Mat gray = new Mat();
			Imgproc.cvtColor(img, gray, Imgproc.COLOR_RGB2GRAY);
			Log.i(TAG, "Converted to grayscale");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Find edges using Canny detector
			Mat edges = new Mat();
			Imgproc.Canny(img, edges, 50, 200, 3, false);
			Log.i(TAG, "Computed edges");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Find lines using Hough transform
			Mat lines = new Mat();
			Imgproc.HoughLinesP(edges, lines, 1, DEGREE, 150, MIN_CELL_SIZE, MIN_CELL_SIZE);
			Log.i(TAG, "Computed lines");
			edges.release();

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Detect lines
			HashSet<Line> lineSet = new HashSet<Line>();
			for (int index = 0; index < lines.cols(); index++) {
				Line line = new Line(lines.get(0, index));
				lineSet.add(line);
			}
			lines.release();
			Log.i(TAG, "Detected lines");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Find most frequent line angle
			double angle = 0;
			HashMap<Double, Integer> angleCount = new HashMap<Double, Integer>();
			for (Line line : lineSet) {
				double theta = findAngle(line);
				if (angleCount.containsKey(theta)) {
					angleCount.put(theta, angleCount.get(theta) + 1);
				} else {
					angleCount.put(theta, 1);
				}
				if (!angleCount.containsKey(angle)
						|| angleCount.get(theta) > angleCount.get(angle)) {
					angle = theta;
				}
				// Check if cancelled
				if (isCancelled()) {
					return null;
				}
			}
			Log.i(TAG, "Found most frequent angle");

			// Base orientation on most frequent angle
			horizontal = angle;
			vertical = normalize(angle + DEGREE_90);

			// Switch horizontal and vertical if necessary
			if (Math.abs(horizontal - DEGREE_90) < Math.abs(vertical
					- DEGREE_90)) {
				double temp = vertical;
				vertical = horizontal;
				horizontal = temp;
			}
			Log.i(TAG, horizontal + " " + vertical);

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Keep horizontal and vertical lines
			ArrayList<Line> hLines = new ArrayList<Line>();
			ArrayList<Line> vLines = new ArrayList<Line>();
			for (Line line : lineSet) {
				if (isHorizontal(line)) {
					hLines.add(line);
				} else if (isVertical(line)) {
					vLines.add(line);
				}
				// Check if cancelled
				if (isCancelled()) {
					return null;
				}
			}
			lineSet.clear();
			Log.i(TAG, "Created lists of horizontal and vertical lines");

			// Sort horizontal lines
			Collections.sort(hLines, new LineComparator(true));
			Log.i(TAG, "Sorted horizontal lines");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Sort vertical lines
			Collections.sort(vLines, new LineComparator(false));
			Log.i(TAG, "Sorted vertical lines");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Compute largest set of equally spaced horizontal lines
			ArrayList<Line> hList = findEquallySpacedLines(hLines, true);
			Log.i(TAG, "Found equally spaced horizontal lines");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Compute largest set of equally spaced vertical lines
			ArrayList<Line> vList = findEquallySpacedLines(vLines, false);
			Log.i(TAG, "Found equally spaced vertical lines");

			// Check if cancelled
			if (isCancelled()) {
				return null;
			}

			// Get puzzle size
			int height = hList.size() - 1, width = vList.size() - 1;
			Log.i(TAG, height + "x" + width);

			// Print differences
			double diff = 0;
			for (int i = 1; i < hList.size(); i++) {
				diff += (getPosition(hList.get(i), true) - getPosition(
						hList.get(i - 1), true));
				// Check if cancelled
				if (isCancelled()) {
					return null;
				}
			}
			Log.i(TAG, "Avg hDistance: " + (diff / hList.size()));
			diff = 0;
			for (int i = 1; i < vList.size(); i++) {
				diff += (getPosition(vList.get(i), false) - getPosition(
						vList.get(i - 1), false));
				// Check if cancelled
				if (isCancelled()) {
					return null;
				}
			}
			Log.i(TAG, "Avg hDistance: " + (diff / vList.size()));

			// Construct grid representation
			double[][] cells = new double[height][width];
			for (int row = 0; row < height; row++) {
				for (int col = 0; col < width; col++) {
					// Find bounds of cell
					double[] point1 = findIntersection(vList.get(col),
							hList.get(row));
					double[] point2 = findIntersection(vList.get(col + 1),
							hList.get(row + 1));
					double[] coords = { Math.min(point1[0], img.cols() - 1),
							Math.min(point2[0], img.cols() - 1),
							Math.min(point1[1], img.rows() - 1),
							Math.min(point2[1], img.rows() - 1) };

					// Calculate average intensity of cell
					for (int x = (int) coords[2]; x <= coords[3]; x++) {
						for (int y = (int) coords[0]; y <= coords[1]; y++) {
							try {
								cells[row][col] += gray.get(x, y)[0];
							} catch (Exception e) {
								Log.e(TAG, "Unable to get pixel data");
								break;
							}
							// Check if cancelled
							if (isCancelled()) {
								return null;
							}
						}
					}
					cells[row][col] /= ((coords[1] - coords[0]) * (coords[3] - coords[2]));
					// Check if cancelled
					if (isCancelled()) {
						return null;
					}
				}
			}
			img.release();
			gray.release();

			// Decide whether cells are black or white
			double cutoff = findCutoff(cells);
			// Rows and columns to remove
			int firstRows = 0, firstCols = 0, lastRows = 0, lastCols = 0;
			// Count first rows
			for (int i = 0; i < cells.length && isAllOneColor(cells[i], cutoff); i++) {
				firstRows++;
			}
			// Count last rows
			for (int i = cells.length - 1; i >= firstRows
					&& isAllOneColor(cells[i], cutoff); i--) {
				lastRows++;
			}
			// Count first cols
			for (int j = 0; j < cells[0].length && isAllOneColor(cells, j, cutoff); j++) {
				firstCols++;
			}
			// Count first cols
			for (int j = cells[0].length - 1; j >= firstCols
					&& isAllOneColor(cells, j, cutoff); j--) {
				lastCols++;
			}

			// Construct new cells
			height = cells.length - firstRows - lastRows;
			width = cells[0].length - firstCols - lastCols;
			double[][] newCells = new double[height][width];
			// Remove rows
			if (firstRows > 0 || lastRows > 0) {
				int index = 0;
				for (int i = Math.max(firstRows, 0); i < height + firstRows; i++) {
					newCells[index++] = cells[i];
				}
			}
			// Remove cols
			if (firstCols > 0 || lastCols > 0) {
				for (int i = 0; i < newCells.length; i++) {
					int index = 0;
					for (int j = Math.max(firstCols, 0); j < height + firstCols; j++) {
						newCells[i][index++] = cells[i + firstRows][j];
					}
				}
			}
			System.out.println(height + "x" + width);

			// Construct data
			StringBuilder data = new StringBuilder();
			data.append("version: 1\n");
			data.append(height + "|").append(width + "|");
			for (int i = 0; i < newCells.length; i++) {
				for (int j = 0; j < newCells[i].length; j++) {
					if (newCells[i][j] >= cutoff) {
						data.append("1");
						System.out.print("O ");
					} else {
						data.append("0");
						System.out.print("X ");
					}
					data.append("0").append("|");
				}
				System.out.println();
			}
			for (int i = 0; i < cells.length; i++) {
				for (int j = 0; j < cells[i].length; j++) {
					if (cells[i][j] >= cutoff) {
						data.append("1").append("|");
						System.out.print("O ");
					} else {
						data.append("0").append("|");
						System.out.print("X ");
					}
					data.append((char) 0).append("|");
					// Check if cancelled
					if (isCancelled()) {
						return null;
					}
				}
				System.out.println();
			}

			// Grid representation
			Log.i(TAG, data.toString());
			return data.toString();
		}

		@Override
		protected void onPostExecute(String result) {
			dialog.dismiss();
		}

		@Override
		protected void onCancelled() {
			dialog.dismiss();
			super.onCancelled();
		}
	}

}