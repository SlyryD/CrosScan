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

	public PinchImageView(final Context context) {
		this(context, null);
	}

	public PinchImageView(final Context context, final AttributeSet attrs) {
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
	protected final void onDraw(final Canvas canvas) {
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
	public final boolean onTouchEvent(final MotionEvent event) {
		scaleGesture.onTouchEvent(event);
		gestures.onTouchEvent(event);
		return true;
	}

	public final void setImageBitmap(final Bitmap photo) {
		// Initializing variables
		image = photo;
		initialize();
	}

	public class ScaleListener implements OnScaleGestureListener {
		@Override
		public final boolean onScale(final ScaleGestureDetector detector) {
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
		public final boolean onScaleBegin(final ScaleGestureDetector detector) {
			mode = ZOOM;
			lastFocusX = detector.getFocusX();
			lastFocusY = detector.getFocusY();
			return true;
		}

		@Override
		public final void onScaleEnd(final ScaleGestureDetector detector) {
			mode = NORMAL;
		}

	}

	public class GestureListener implements GestureDetector.OnGestureListener,
			GestureDetector.OnDoubleTapListener {

		@Override
		public final boolean onDown(final MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public final boolean onScroll(final MotionEvent downEvent,
				final MotionEvent currentEvent, final float distanceX, final float distanceY) {
			drawMatrix.postTranslate(-4 * distanceX, -4 * distanceY);
			invalidate();
			return true;
		}

		@Override
		public final boolean onDoubleTap(final MotionEvent arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public final boolean onDoubleTapEvent(final MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public final boolean onSingleTapConfirmed(final MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public final boolean onFling(final MotionEvent arg0, final MotionEvent arg1, final float arg2,
				final float arg3) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onLongPress(final MotionEvent arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onShowPress(final MotionEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public final boolean onSingleTapUp(final MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}
	}
}