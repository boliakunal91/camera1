package com.example.android.camera1;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.media.FaceDetector;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.javacodegeeks.androidcameraexample.R;

public class AndroidCameraExample extends Activity {
	private Camera mCamera;
	private CameraPreview mPreview;
	private PictureCallback mPicture;
	private Button capture, switchCamera;
	private Context myContext;
	private LinearLayout cameraPreview;
	private boolean cameraFront = false;
	private File newpicturefile;
	private ImageView newimageview;
	private int face_count;
	private FaceDetector.Face [] faces;
	private int MAX_FACES = 10;
	//private Button mask;
	private Paint tmp_paint = new Paint();
	private Bitmap m1Bitmap;
	private String address;
	private Bitmap mBitmap=null;

	private PointF tmp_point= new PointF();
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		myContext = this;
		initialize();
	}

	private int findFrontFacingCamera() {
		int cameraId = -1;
		// Search for the front facing camera
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				cameraId = i;
				cameraFront = true;
				break;
			}
		}
		return cameraId;
	}

	private int findBackFacingCamera() {
		int cameraId = -1;
		//Search for the back facing camera
		//get the number of cameras
		int numberOfCameras = Camera.getNumberOfCameras();
		//for every camera check
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				cameraId = i;
				cameraFront = false;
				break;
			}
		}
		return cameraId;
	}

	public void onResume() {
		super.onResume();
		if (!hasCamera(myContext)) {
			Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
			toast.show();
			finish();
		}
		if (mCamera == null) {
			//if the front facing camera does not exist
			if (findFrontFacingCamera() < 0) {
				Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
				switchCamera.setVisibility(View.GONE);
			}
			mCamera = Camera.open(findBackFacingCamera());
			mPicture = getPictureCallback();
			mPreview.refreshCamera(mCamera);
		}
	}

	public void initialize() {
		cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
		mPreview = new CameraPreview(myContext, mCamera);
		cameraPreview.addView(mPreview);

		capture = (Button) findViewById(R.id.button_capture);
		capture.setOnClickListener(captrureListener);

		switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
		switchCamera.setOnClickListener(switchCameraListener);
	}

	OnClickListener switchCameraListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			//get the number of cameras
			int camerasNumber = Camera.getNumberOfCameras();
			if (camerasNumber > 1) {
				//release the old camera instance
				//switch camera, from the front and the back and vice versa

				releaseCamera();
				chooseCamera();
			} else {
				Toast toast = Toast.makeText(myContext, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
				toast.show();
			}
		}
	};

	public void chooseCamera() {
		//if the camera preview is the front
		if (cameraFront) {
			int cameraId = findBackFacingCamera();
			if (cameraId >= 0) {
				//open the backFacingCamera
				//set a picture callback
				//refresh the preview

				mCamera = Camera.open(cameraId);
				mPicture = getPictureCallback();

			}
		} else {
			int cameraId = findFrontFacingCamera();
			if (cameraId >= 0) {


				mCamera = Camera.open(cameraId);
				mPicture = getPictureCallback();
				mPreview.refreshCamera(mCamera);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		//when on Pause, release camera in order to be used from other applications
		releaseCamera();
	}

	private boolean hasCamera(Context context) {
		//check if the device has camera
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return true;
		} else {
			return false;
		}
	}

	private PictureCallback getPictureCallback() {
		PictureCallback picture = new PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				//make a new picture file
				newpicturefile = getOutputMediaFile();
				Bitmap picBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				if (newpicturefile == null) {
					return;
				}
				setContentView(R.layout.mask);
				ImageView picView =(ImageView) findViewById(R.id.imageView_mask);
				picView.setImageBitmap(picBitmap);
				mBitmap = picBitmap.copy(Bitmap.Config.RGB_565, true);
				Button mask;
				mask =(Button) findViewById(R.id.button_mask);
				mask.setOnClickListener(new OnClickListener() {
											public void onClick(View v) {

												detectFace(mBitmap);
												//ImageView picView1 =(ImageView) findViewById(R.id.imageView_mask);
												//picView1.setImageBitmap(m1Bitmap);
												//repreview();
												//drawPen(mBitmap);
												//put mask method call here
											}
										}
				);




				mPreview.refreshCamera(mCamera);
			}
		};
		return picture;
	}

	OnClickListener captrureListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			mCamera.takePicture(null, null, mPicture);
            //maskactivity();

		}
	};


	//make picture and save to a folder
	private static File getOutputMediaFile() {
		//make a new file directory inside the "sdcard" folder
		File mediaStorageDir = new File("/sdcard/", "JCG_Camera");

		//if this "JCGCamera folder does not exist
		if (!mediaStorageDir.exists()) {
			//if you cannot make this folder return
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}

		//take the current timeStamp
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		//and make a media file:
		mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

		return mediaFile;
	}

	private void releaseCamera() {
		// stop and release camera
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}

	private void detectFace(Bitmap bitmap){
		FaceDetector face_detector = new FaceDetector(
				bitmap.getWidth(), bitmap.getHeight(),MAX_FACES);
		faces = new FaceDetector.Face[MAX_FACES];
		face_count = face_detector.findFaces(bitmap, faces);
		Toast.makeText(this, String.valueOf(face_count)+ " Face(s) found!" ,Toast.LENGTH_LONG).show();

		//test code below

		drawPen(bitmap);


		//finish();

	}

	private void drawPen(Bitmap bitmap) {
		Canvas acanvas = new Canvas(bitmap);

		if (acanvas == null) {
			//Log.e(TAG, "Cannot draw onto the canvas as it's null");
		} else {
			acanvas.drawBitmap(bitmap,0,0,null);

			for (int i = 0; i < face_count; i++) {
				FaceDetector.Face face = faces[i];

				tmp_paint.setColor(Color.BLUE);
				tmp_paint.setAlpha(100);
				face.getMidPoint(tmp_point);
				acanvas.drawCircle(tmp_point.x, tmp_point.y, face.eyesDistance(),
						tmp_paint);
			}
			this.m1Bitmap = bitmap;
			setContentView(R.layout.mask);
			ImageView picView1 =(ImageView) findViewById(R.id.imageView_mask);

			picView1.setImageBitmap(bitmap);
			//repreview();
//latest change here
			Button mask;
			mask =(Button) findViewById(R.id.button_mask);
			mask.setText("Preview");
			mask.setOnClickListener(new OnClickListener() {
										public void onClick(View v) {
											setContentView(R.layout.activity_main);
											initialize();

											//drawPen(mBitmap);
											//put mask method call here
										}
									}
			);

			//latest change ends
		}

	}

	private void displaymask(Bitmap bitmap){
		ImageView picView1 =(ImageView) findViewById(R.id.imageView_mask);
		picView1.setImageBitmap(bitmap);





		//repreview();

	}
	private void repreview() {
		setContentView(R.layout.mask);
		Button mask = (Button)findViewById(R.id.button_mask);
		mask.setText("Preview");
		mask.setOnClickListener(new OnClickListener() {
									public void onClick(View v) {
										setContentView(R.layout.activity_main);
										initialize();

										//drawPen(mBitmap);
										//put mask method call here
									}
								}
		);
	}

	private void maskactivity(){
		/*
		setContentView(R.layout.mask);

		Toast.makeText(getApplicationContext(), address,
				Toast.LENGTH_LONG).show();
		newimageview =(ImageView)findViewById(R.id.imageView_mask);
		//ImageView myImage = new ImageView(myContext);

		newimageview.setImageResource(R.drawable.bengal_tiger);



		//newimageview.setImageBitmap(BitmapFactory.decodeFile());
		//newimageview.setImageBitmap(BitmapFactory.decodeFile(newpicturefile.getPath()));

		//Bitmap bmImg = BitmapFactory.decodeFile(newpicturefile.getPath());
		//myImage.setImageBitmap(bmImg);
		//cameraPreview.addView(myImage);
		*/
	}
}