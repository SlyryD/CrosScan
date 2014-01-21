package edu.dcc.crosswordscan;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.opencv.android.JavaCameraView;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

public class ScanView extends JavaCameraView {

	private static final String TAG = "Sample::Tutorial3View";
	private static final int MEDIA_TYPE_IMAGE = 44;

	public ScanView(Context context, AttributeSet attrs) {
		super(context, attrs);
//		// get Camera parameters
//		Camera.Parameters params = mCamera.getParameters();
//		// set the focus mode
//		params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//		// set Camera parameters
//		mCamera.setParameters(params);
	}

	public void focus() {
		mCamera.autoFocus(null);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		super.surfaceChanged(arg0, arg1, arg2, arg3);
		
	}

	public File takePicture(final ContentResolver cr) {
		// Retrieve picture file
		final File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

		// Abort if picture file not found
		if (pictureFile == null) {
			return null;
		}

		// Print path of photo
		System.out.println(pictureFile.getAbsolutePath());

		PictureCallback callback = new PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				savePhoto(pictureFile, cr, data);
			}
		};

		mCamera.takePicture(null, null, callback);

		return pictureFile;
	}

	private void savePhoto(File pictureFile, ContentResolver cr, byte[]... jpeg) {
		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			fos.write(jpeg[0]);
			fos.close();
			MediaStore.Images.Media.insertImage(cr,
					pictureFile.getAbsolutePath(), pictureFile.getName(),
					pictureFile.getName());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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

		// Use unique identifier for file
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",
				Locale.US);
		String currentDateandTime = sdf.format(new Date());
		String filename = Environment.getExternalStorageDirectory().getPath()
				+ File.separator + "IMG_CrosswordScan_" + currentDateandTime
				+ ".jpg";

		// Create a media file name IMG_CrosswordScan_DATETIME.jpg
		File mediaFile = null;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(filename);
		} else {
			return null;
		}

		return mediaFile;
	}

}
