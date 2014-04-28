package edu.dcc.crosscan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;

public class PinchImageView extends View {
	private final int NORMAL = 0;
	private final int ZOOM = 1;
	private final int DRAG = 2;

	private Bitmap image;
	private Matrix drawMatrix;
	private float lastFocusX;
	private float lastFocusY;
	private int screenHeight;
	private int screenWidth;
	private Paint paint;
	private GestureDetector gestures;
	private ScaleGestureDetector scaleGesture;
	private float scale = 1.0f;
	private float horizontalOffset, verticalOffset;
	private float touchX, touchY;
	private int mode = NORMAL;

	public PinchImageView(Context context) {
		this(context, null);
	}

	public PinchImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// Initializing variables
		drawMatrix = new Matrix();
		image = BitmapFactory.decodeResource(getResources(),
				R.drawable.no_photo);
		
		// This is a full screen view
		screenWidth = getResources().getDisplayMetrics().widthPixels;
		screenHeight = getResources().getDisplayMetrics().heightPixels;
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);
		paint.setColor(Color.BLACK);

		scaleGesture = new ScaleGestureDetector(getContext(),
				new ScaleListener());
		gestures = new GestureDetector(getContext(), new GestureListener());
		mode = NORMAL;
		initialize();
	}

	// Best fit image display on canvas
	private void initialize() {
		float imgPartRatio = image.getWidth() / (float) image.getHeight();
		float screenRatio = screenWidth / (float) screenHeight;

		if (screenRatio > imgPartRatio) {
			// fit height
			scale = screenHeight / (float) image.getHeight();
			horizontalOffset = (screenWidth - scale * image.getWidth()) / 2.0f;
			verticalOffset = 0;
		} else {
			// fit width
			scale = screenWidth / (float) image.getWidth();
			horizontalOffset = 0;
			verticalOffset = (screenHeight - scale * image.getHeight()) / 2.0f;
		}
		invalidate();
	}

	@SuppressLint("NewApi")
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.save();
		canvas.drawColor(0, Mode.CLEAR);
		canvas.drawColor(Color.BLACK);
		if (mode == DRAG || mode == NORMAL) {
			canvas.translate(horizontalOffset, verticalOffset);
			canvas.scale(scale, scale);
			canvas.drawBitmap(image, drawMatrix, paint);
		} else if (mode == ZOOM) {
			canvas.scale(scale, scale, touchX, touchY);
			canvas.drawBitmap(image, drawMatrix, paint);
		}
		canvas.restore();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		scaleGesture.onTouchEvent(event);
		gestures.onTouchEvent(event);
		return true;
	}

	public void setImageBitmap(Bitmap photo) {
		// Initializing variables
		image = photo;
		initialize();
	}

	public class ScaleListener implements OnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			Matrix transformationMatrix = new Matrix();
			float focusX = detector.getFocusX();
			float focusY = detector.getFocusY();

			// Zoom focus is where the fingers are centered
			transformationMatrix.postTranslate(-focusX, -focusY);

			transformationMatrix.postScale(detector.getScaleFactor(),
					detector.getScaleFactor());

			// Focus shift to allow for scrolling with two pointers down
			float focusShiftX = focusX - lastFocusX;
			float focusShiftY = focusY - lastFocusY;
			transformationMatrix.postTranslate(focusX + focusShiftX, focusY
					+ focusShiftY);
			drawMatrix.postConcat(transformationMatrix);
			lastFocusX = focusX;
			lastFocusY = focusY;
			invalidate();
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			mode = ZOOM;
			lastFocusX = detector.getFocusX();
			lastFocusY = detector.getFocusY();
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			mode = NORMAL;
		}

	}

	public class GestureListener implements GestureDetector.OnGestureListener,
			GestureDetector.OnDoubleTapListener {

		@Override
		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent downEvent,
				MotionEvent currentEvent, float distanceX, float distanceY) {
			drawMatrix.postTranslate(-2 * distanceX, -2 * distanceY);
			invalidate();
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
				float arg3) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onLongPress(MotionEvent arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}
	}
}