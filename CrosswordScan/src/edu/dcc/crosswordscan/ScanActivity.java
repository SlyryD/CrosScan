package edu.dcc.crosswordscan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import edu.dcc.game.Puzzle;

/**
 * Activity that allows user to scan crossword puzzles.
 * 
 * @author Ryan D.
 */
public class ScanActivity extends Activity implements CvCameraViewListener,
		View.OnTouchListener {

	// Request codes
	public static final int CAMERA_REQUEST = 11, CONFIRM_GRID = 22,
			REJECT_GRID = 33;
	public static final String GRID = "grid";
	public static final Random generator = new Random(18293734);

	// OpenCV fields
	private static final String TAG = "CrosswordScan/ScanActivity";
	public static double horizontal = 0, vertical = Math.PI / 2;
	public static final double DEGREE = Math.PI / 180;
	public static final double DEGREE90 = Math.PI / 2;
	public static final double DEGREE180 = Math.PI;
	public static final double DEGREE360 = 2 * Math.PI;
	public static final double THRESHOLD = 4 * DEGREE;

	// Camera view
	private ScanView scanView;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				/* Enable camera view to start receiving frames */
				Log.i(TAG, "OpenCV loaded successfully");
				scanView.enableView();
				scanView.setOnTouchListener(ScanActivity.this);
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
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_scan);

		scanView = (ScanView) findViewById(R.id.scan_activity_surface_view);

		scanView.setVisibility(SurfaceView.VISIBLE);

		scanView.setCvCameraViewListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (scanView != null)
			scanView.disableView();
	}

	@Override
	protected void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this,
				mLoaderCallback);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (scanView != null)
			scanView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
	}

	public void onCameraViewStopped() {
	}

	public Mat onCameraFrame(Mat inputFrame) {
		Mat ift = inputFrame.t();
		Core.flip(inputFrame.t(), ift, 1);
		Imgproc.resize(ift, ift, new Size(scanView.getWidth(), scanView.getHeight()));
		return ift;
	}

	public boolean onTouch(View view, MotionEvent event) {
		scanView.focus();
		return false;
	}

	/**
	 * Respond to button press by taking photo
	 * 
	 * @param view
	 */
	public void takePhoto(View view) {
		// Save photo
		SavePhotoTask spTask = new SavePhotoTask();
		spTask.execute();
		String result = null;
		try {
			result = spTask.get();
		} catch (InterruptedException e) {
			Log.e(TAG, "Take photo interrupted");
		} catch (ExecutionException e) {
			Log.e(TAG, "Take photo execution failed");
		}

		// Process photo
		ProcessTask pTask = new ProcessTask();
		pTask.execute(result);
		try {
			String grid = pTask.get();
			Intent intent = new Intent(this, NamePuzzleActivity.class);
			intent.putExtra(GRID, grid);
			startActivity(intent);
		} catch (InterruptedException e) {
			Log.e(TAG, "Process image interrupted");
		} catch (ExecutionException e) {
			Log.e(TAG, "Process image execution failed");
		}
	}

	private class SavePhotoTask extends AsyncTask<Void, Void, String> {

		final private ProgressDialog dialog = new ProgressDialog(
				ScanActivity.this);

		protected void onPreExecute() {
			dialog.setMessage("Saving photo");
			dialog.show();
		}

		@Override
		protected String doInBackground(Void... params) {
			// Take photo and return file path
			return scanView.takePicture();
		}

		@Override
		protected void onPostExecute(String result) {
			dialog.dismiss();
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
			getRandomPuzzle();

			// TODO: Use OCR to recognize clues

			// TODO: Add image URL to string representation

			// Return string representation of puzzle
			return result;
		}

		private class Line {

			public double x1, x2, y1, y2;

			public Line(double[] line) {
				this.x1 = line[0];
				this.x2 = line[1];
				this.y1 = line[2];
				this.y2 = line[3];
			}
		}

		/**
		 * Returns equivalent angle between 0 and pi radians (exclusive)
		 * 
		 * @param theta
		 * @return normalized angle
		 */
		private double normalize(double theta) {
			return (theta + DEGREE360) % DEGREE180;
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
				return DEGREE90;
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
			return Math.abs(theta - vertical) <= THRESHOLD
					|| Math.abs(theta - vertical - DEGREE180) <= THRESHOLD;
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
			return Math.abs(theta - horizontal) <= THRESHOLD
					|| Math.abs(theta - horizontal - DEGREE180) <= THRESHOLD;
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
				// return line.y1; // First endpoint
				// return line.y2; // Second endpoint
				return (line.y1 + line.y2) / 2; // Midpoint
			} else {
				// Return horizontal position
				// return line.x1; // First endpoint
				// return line.x2; // Second endpoint
				return (line.x1 + line.x2) / 2; // Midpoint
			}
		}

		private double findLength(Line line) {
			return Math.sqrt(Math.pow(line.x1 - line.x2, 2)
					+ Math.pow(line.y1 - line.y2, 2));
		}

		/**
		 * Finds largest list of consecutive equally spaced lines
		 * 
		 * @param lines
		 * @param horizontal
		 * @return result list
		 */
		// TODO: Find workaround for suppression
		@SuppressLint("NewApi")
		private ArrayList<Line> findEquallySpacedLines(ArrayList<Line> lines,
				boolean horizontal) {
			// Create map of positions of each line
			TreeMap<Double, Line> positions = new TreeMap<Double, Line>();
			for (Line line : lines) {
				double position = getPosition(line, horizontal);
				if (!positions.containsKey(position)
						|| findLength(line) > findLength(positions
								.get(position))) {
					positions.put(position, line);
				}
			}

			// List of equally spaced lines
			ArrayList<Line> resultList = new ArrayList<Line>();
			for (double i : positions.keySet()) {
				for (double j : positions.tailMap(i, false).keySet()) {
					// Construct temporary list of equally spaced lines
					ArrayList<Line> tempList = new ArrayList<Line>();
					double distance = j - i;
					// Rule out small distances between lines
					if (distance < 15) {
						continue;
					}
					// Add first couple of lines to set
					tempList.add(positions.get(i));
					tempList.add(positions.get(j));
					// Keep track of position of last line
					double currentPos = j;
					// Loop until last key
					while (currentPos < positions.lastKey()) {
						double nextPos = currentPos + distance;
						if (positions.containsKey(nextPos)) {
							tempList.add(positions.get(nextPos));
							currentPos = nextPos;
						} else {
							Double floor = positions.lowerKey(nextPos), ceil = positions
									.higherKey(nextPos);
							if (ceil == null) {
								// Accept floor if in range
								if (floor == null || floor == currentPos) {
									break;
								} else if (nextPos - floor <= 10) {
									tempList.add(positions.get(floor));
									currentPos = floor;
								} else {
									break;
								}
							} else if (floor == null || floor == currentPos) {
								// Accept ceiling if in range
								if (ceil - nextPos <= 10) {
									tempList.add(positions.get(ceil));
									currentPos = ceil;
								} else {
									break;
								}
							} else {
								// Accept floor then ceiling if in range
								if (nextPos - floor <= 10) {
									tempList.add(positions.get(floor));
									currentPos = floor;
								} else if (ceil - nextPos <= 10) {
									tempList.add(positions.get(ceil));
									currentPos = ceil;
								} else {
									break;
								}
							}
						}
					}
					// Update result list if temporary list is longer
					if (tempList.size() > resultList.size()) {
						resultList = tempList;
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

		private String recognizeGrid(String filePath) {
			// Read image
			Mat img = Highgui.imread(filePath);
			System.out.println("Read " + filePath);
			if (img.empty()) {
				Log.e(TAG, "Cannot open " + filePath);
				System.exit(1);
			}

			// Convert image to grayscale
			Mat gray = new Mat();
			Imgproc.cvtColor(img, gray, Imgproc.COLOR_RGB2GRAY);
			System.out.println("Converted to grayscale");

			// Find edges using Canny detector
			Mat edges = new Mat();
			Imgproc.Canny(img, edges, 50, 200, 3, false);
			System.out.println("Computed edges");

			// Convert edge image to color for drawing lines in red
			Mat cedges = new Mat();
			Imgproc.cvtColor(gray, cedges, Imgproc.COLOR_GRAY2BGR);
			System.out.println("Converted edges to color");

			// Find lines using Hough transform
			Mat lines = new Mat();
			Imgproc.HoughLinesP(edges, lines, 1, DEGREE, 150, 0, img.cols());
			System.out.println("Computed lines");

			// Detect lines
			HashSet<Line> lineSet = new HashSet<Line>();
			for (int index = 0; index < lines.cols(); index++) {
				Line line = new Line(lines.get(0, index));
				lineSet.add(line);
			}
			System.out.println("Detected lines");

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
			}
			System.out.println("Found most frequent angle");

			// Base orientation on most frequent angle
			horizontal = angle;
			vertical = normalize(horizontal + DEGREE90);

			// Switch horizontal and vertical if necessary
			if (Math.abs(horizontal - DEGREE90) < Math.abs(vertical - DEGREE90)) {
				double temp = vertical;
				vertical = horizontal;
				horizontal = temp;
			}
			System.out.println(horizontal + " " + vertical);

			// Keep horizontal and vertical lines
			ArrayList<Line> hLines = new ArrayList<Line>();
			ArrayList<Line> vLines = new ArrayList<Line>();
			for (Line line : lineSet) {
				if (isHorizontal(line)) {
					hLines.add(line);
				} else if (isVertical(line)) {
					vLines.add(line);
				}
			}
			Collections.sort(hLines, new LineComparator(true));
			Collections.sort(vLines, new LineComparator(false));
			System.out.print("[");
			for (Line line : vLines) {
				System.out.print("(" + line.x1 + "," + line.y1 + ")->("
						+ line.x2 + "," + line.y2 + "), ");
			}
			System.out.println("]");

			// Compute largest set of equally spaced horizontal lines
			ArrayList<Line> hList = findEquallySpacedLines(hLines, true);
			ArrayList<Line> vList = findEquallySpacedLines(vLines, false);

			// Print lists
			System.out.print("[");
			for (Line line : hList) {
				System.out.print(getPosition(line, true) + ", ");
			}
			System.out.println("]");
			System.out.print("[");
			for (Line line : vList) {
				System.out.print(getPosition(line, false) + ", ");
			}
			System.out.println("]");

			// Draw relevant lines
			for (Line line : hList) {
				Core.line(cedges, new Point(line.x1, line.y1), new Point(
						line.x2, line.y2), new Scalar(0, 0, 255), 2,
						Core.LINE_AA, 0);
			}
			for (Line line : vList) {
				Core.line(cedges, new Point(line.x1, line.y1), new Point(
						line.x2, line.y2), new Scalar(0, 0, 255), 2,
						Core.LINE_AA, 0);
			}

			// Print puzzle size
			System.out.println(hList.size() + "x" + vList.size());

			// Print differences
			System.out.print("[");
			for (int i = 1; i < hList.size(); i++) {
				System.out.print(getPosition(hList.get(i), true)
						- getPosition(hList.get(i - 1), true) + ", ");
			}
			System.out.println("]");
			System.out.print("[");
			for (int i = 1; i < vList.size(); i++) {
				System.out.print(getPosition(vList.get(i), false)
						- getPosition(vList.get(i - 1), false) + ", ");
			}
			System.out.println("]");

			// Construct grid representation
			StringBuilder data = new StringBuilder();
			data.append("version: 1\n");
			data.append(hList.size() + "|");
			double[][] cells = new double[hList.size() - 1][vList.size() - 1];
			for (int row = 0; row < hList.size() - 1; row++) {
				for (int col = 0; col < vList.size() - 1; col++) {
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
						}
					}
					cells[row][col] /= ((coords[1] - coords[0]) * (coords[3] - coords[2]));
					if ((row + col) % 2 == 0) {
						Core.rectangle(cedges, new Point(coords[0], coords[2]),
								new Point(coords[1], coords[3]), new Scalar(
										255, 0, 0), Core.FILLED);
					}
				}
			}
			double cutoff = findCutoff(cells);
			for (int i = 0; i < cells.length; i++) {
				for (int j = 0; j < cells[i].length; j++) {
					if (cells[i][j] >= cutoff) {
						data.append("1").append("|");
						System.out.print("O ");
					} else {
						data.append("0").append("|");
						System.out.print("X ");
					}
					data.append("0").append("|");
				}
				System.out.println();
			}
			return data.toString();
		}

		private String getRandomPuzzle() {
			int[][] cells = new int[13][13];
			for (int i = 0; i < cells.length; i++) {
				for (int j = 0; j < cells[i].length; j++) {
					// 1/4 chance of black square
					if (generator.nextInt(2) == 0 && generator.nextInt(2) == 0) {
						cells[i][j] = 0;
					} else {
						cells[i][j] = 1;
					}
				}
			}
			return new Puzzle(cells.length, cells, null).serialize();
		}

		@Override
		protected void onPostExecute(String result) {
			dialog.dismiss();
		}
	}
}