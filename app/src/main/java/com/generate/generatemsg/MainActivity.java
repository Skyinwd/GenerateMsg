package com.generate.generatemsg;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.facebook.messenger.MessengerThreadParams;
import com.facebook.messenger.MessengerUtils;
import com.facebook.messenger.ShareToMessengerParams;
import com.facebook.FacebookSdk;

import android.provider.MediaStore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.io.File;
import android.os.Environment;
import java.util.Date;
import android.util.Log;
import android.widget.ImageView;
import android.content.pm.PackageManager;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import jp.co.cyberagent.android.gpuimage.GPUImageView.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Button;
import android.hardware.Camera.PictureCallback;
import java.io.FileOutputStream;
import android.content.ContentValues;
import android.provider.MediaStore.Images;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import android.opengl.GLSurfaceView;
import jp.co.cyberagent.android.gpuimage.GPUImageGrayscaleFilter;
import android.graphics.Bitmap;
import android.widget.RelativeLayout;

enum ASPECT_RATIO {
  NOT_SQUARE, SQUARE
}

public class MainActivity extends Activity {

  private static final int REQUEST_CODE_SHARE_TO_MESSENGER = 1;
  public static final int MEDIA_TYPE_IMAGE = 1;
  public static final int MEDIA_TYPE_VIDEO = 2;

  private View mMessengerButton;
  private MessengerThreadParams mThreadParams;
  private boolean mPicking;

  private String TAG = "YUE:";

  private Camera mCamera;
  private CameraPreview mPreview;
  private Uri photoURI;
  private GPUImage mGPUImage;
  private android.opengl.GLSurfaceView mGLSurfaceView;
  private ASPECT_RATIO aspectRatio;
  private float inputAspectRatio = 1f;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FacebookSdk.sdkInitialize(getApplicationContext()); // facebook sdk initialize
    setContentView(R.layout.activity_main);

    // Create an instance of Camera
    mCamera = getCameraInstance();

    // Create Preview view and set it as the content of the activity.
    //mPreview = new CameraPreview(this, mCamera);
    //FrameLayout preview = (FrameLayout) findViewById(R.id.gpu_image);
    //preview.addView(mPreview);

    /*-------------------------------------------*/
    mGLSurfaceView = (GLSurfaceView)findViewById(R.id.gpu_image);
    mGLSurfaceView.post(new Runnable(){
      @Override
      public void run(){
        mGLSurfaceView.postInvalidate();
        setAspectRatio(ASPECT_RATIO.SQUARE);
      }
    });
    mGPUImage = new GPUImage(this);
    mGPUImage.setGLSurfaceView(mGLSurfaceView); //Sets the GLSurfaceView which will display the preview.
    //mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    //mGPUImage.setBackgroundColor(0.5f, 0.0f, 0.0f);
    mGPUImage.setFilter(new GPUImageGrayscaleFilter());
    //setGPUImageToCamera(); // Sets the up camera to be connected to GPUImage to get a filtered preview.
    //mCamera.startPreview();
    //mPreview = new CameraPreview(this, mCamera);
    //mGPUImage.requestRender();
    /*-------------------------------------------*/

    // Create Messenger Button
    mMessengerButton = findViewById(R.id.messenger_send_button);

    // Create and Add a listener to the Capture button
    Button captureButton = (Button) findViewById(R.id.camera_capture);
    captureButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                // get an image from the camera
                mCamera.takePicture(null, null, mPicture);
                // Get photo Uri
                //onMessengerButtonClicked(photoURI);
              }
            }
    );

    // If we received Intent.ACTION_PICK from Messenger, we were launched from a composer shortcut
    // or the reply flow.
    Intent intent = getIntent();
    if (Intent.ACTION_PICK.equals(intent.getAction())) {
      mThreadParams = MessengerUtils.getMessengerThreadParamsForIntent(intent);
      mPicking = true;
      // Note, if mThreadParams is non-null, it means the activity was launched from Messenger.
      // It will contain the metadata associated with the original content, if there was content.
    }

    mMessengerButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        Log.d(TAG, "!!!!!!!! PHOTO URI is : " + photoURI);
        onMessengerButtonClicked(photoURI);
        Log.d(TAG, "Broken??");
      }
    });
  }


  @Override
  protected void onResume() {
    // Super First
    super.onResume();

    setGPUImageToCamera();

  }


  private void setAspectRatio(ASPECT_RATIO ratio){
//        List<Size> sizes = camera.getParameters().getSupportedPictureSizes();
//        Log.d(TAG, "Supported Picture Sizes: " + sizes.toString());

    aspectRatio = ratio;
    int height = 0;
    switch (ratio){
      case NOT_SQUARE:
        float displayWidth = (float) getWindowManager().getDefaultDisplay().getWidth();
        float displayHeight = (float) getWindowManager().getDefaultDisplay().getHeight();
        inputAspectRatio = displayHeight/displayWidth;
        height = (int) displayHeight;
        break;
      case SQUARE:
        inputAspectRatio = 1f;
        height = mGLSurfaceView.getWidth();
        break;
      default:
        break;

    }


    //((LinearLayout) findViewById(R.id.aspectRatioMenu)).setVisibility(View.GONE);
    setSurfaceViewHeight(height);

  }

  private void setSurfaceViewHeight(int height) {
    RelativeLayout.LayoutParams mParams;
    mParams = (RelativeLayout.LayoutParams) mGLSurfaceView.getLayoutParams();
    mParams.height = height;
    mGLSurfaceView.setLayoutParams(mParams);
  }

  private PictureCallback mPicture = new PictureCallback() {

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

      File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

      if (pictureFile == null){
        Log.d(TAG, "Error creating media file");
        return;
      }
      try {
        FileOutputStream fos = new FileOutputStream(pictureFile);
        fos.write(data);
        fos.close();
      } catch (FileNotFoundException e) {
        Log.d(TAG, "File not found: " + e.getMessage());
      } catch (IOException e) {
        Log.d(TAG, "Error accessing file: " + e.getMessage());
      }

      //photoURI = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
      photoURI = FileProvider.getUriForFile(getApplicationContext(), "com.generage.generatemsg.android.fileprovider", pictureFile);
      Log.d("YUE", "file URI : " + photoURI.toString());
    }
  };

  /** Create a file Uri for saving an image or video */
  private Uri getOutputMediaFileUri(int type){
    //return Uri.fromFile(getOutputMediaFile(type))
    /*
    try {
      //File image = getOutputMediaFile(type);
      Log.d("YUE", "file still exist???: " + getOutputMediaFile(type).exists());

      Uri aaa = FileProvider.getUriForFile(getApplicationContext(), "com.generage.generatemsg.android.fileprovider", getOutputMediaFile(type));
      Log.d("YUE", "file URI : " + aaa.toString());
      return aaa;
    } catch (Exception e){
      Log.d("YUE", "getOutputMediaFileUri: " + e.getMessage());
    }*/
    Uri aaa = FileProvider.getUriForFile(getApplicationContext(), "com.generage.generatemsg.android.fileprovider", getOutputMediaFile(type));
    Log.d("YUE", "file URI : " + aaa.toString());
    return aaa;//FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", getOutputMediaFile(type));
  }

  /** Create a File for saving an image or video */
  private File getOutputMediaFile(int type){
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.

    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "MyCameraApp");
    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.

    // Create the storage directory if it does not exist
    if (! mediaStorageDir.exists()){
      if (! mediaStorageDir.mkdirs()){
        Log.d("MyCameraApp", "failed to create directory");
        return null;
      }
    }

    // Create a media file name
    //String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
    String timeStamp = s.format(new Date());
    File mediaFile = null;
    if (type == MEDIA_TYPE_IMAGE){
      //File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
      //mediaFile = new File(path, "yue620621.jpg");
      //-----------
      File storageDir = getApplicationContext().getFilesDir();
      Log.e("YUE", "getFilesDir " + storageDir);
      String fileName = "img";
      Log.e("YUE", "fileName " + fileName);
      mediaFile = new File(storageDir, fileName.concat(timeStamp).concat(".jpg"));
      Log.e("YUE", "mediaFile " + mediaFile);

      mGPUImage.saveToPictures(storageDir.toString(), fileName.concat(timeStamp).concat(".jpg"), null);

    } else if(type == MEDIA_TYPE_VIDEO) {
      mediaFile = new File(mediaStorageDir.getPath() + File.separator +
              "VID_"+ timeStamp + ".mp4");
    } else {
      return null;
    }

    return mediaFile;
  }

  /** Support Functions **/
  // Check if this device has a camera
  private boolean checkCameraHardware(Context context) {
    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
      return true; // this device has a camera
    } else {
      return false; // no camera on this device
    }
  }

  /** A safe way to get an instance of the Camera object. */
  public static Camera getCameraInstance(){
    Camera c = null;
    try {
      c = Camera.open(); // attempt to get a Camera instance
    }
    catch (Exception e){
      // Camera is not available (in use or does not exist)
    }
    return c; // returns null if camera is unavailable
  }

  /** A basic Camera preview class */
  public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
      super(context);
      mCamera = camera;
      mCamera.setDisplayOrientation(90);
      // Install a SurfaceHolder.Callback so we get notified when the
      // underlying surface is created and destroyed.
      mHolder = getHolder();
      mHolder.addCallback(this);
      // deprecated setting, but required on Android versions prior to 3.0
      mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
      // The Surface has been created, now tell the camera where to draw the preview.
      try {
        mCamera.setPreviewDisplay(holder);
        mCamera.startPreview();
      } catch (IOException e) {
        Log.d(TAG, "Error setting camera preview: " + e.getMessage());
      }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
      // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
      // If your preview can change or rotate, take care of those events here.
      // Make sure to stop the preview before resizing or reformatting it.

      if (mHolder.getSurface() == null){
        // preview surface does not exist
        return;
      }

      // stop preview before making changes
      try {
        mCamera.stopPreview();
      } catch (Exception e){
        // ignore: tried to stop a non-existent preview
      }

      // set preview size and make any resize, rotate or
      // reformatting changes here

      // start preview with new settings
      try {
        mCamera.setPreviewDisplay(mHolder);
        mCamera.startPreview();

      } catch (Exception e){
        Log.d(TAG, "Error starting camera preview: " + e.getMessage());
      }
    }
  }
  public void closeCamera() {
    if (mCamera != null) {
      mCamera.stopPreview();
      mCamera.setPreviewCallback(null);
      mCamera.lock();
      mCamera.release();
      mCamera = null;
    }
  }

  private void setGPUImageToCamera() {
    Boolean shouldFlipVertically = false;
    closeCamera();
    try {
      mCamera = Camera.open();
      mCamera.setParameters(setParameters());
    } catch (Exception e) {
      e.printStackTrace();
    }

    //mGPUImage.setUpCamera(mCamera);
    int currentRotation = 90;
    mGPUImage.setUpCamera(mCamera, currentRotation, false, shouldFlipVertically);
  }

  private Parameters setParameters() {
    Parameters parameters = null;
    if (mCamera != null) {
      parameters = mCamera.getParameters();
    }


    // Set Focus
    if (parameters.getSupportedFocusModes().contains(
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
      parameters
              .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    return parameters;
  }


  private File createImageFile() throws IOException {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    Log.e("YUE","temp Dir: "+storageDir);
    File image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
    );

    // Save a file: path for use with ACTION_VIEW intents
    // String mCurrentPhotoPath = image.getAbsolutePath();
    return image;
  }



  private void onMessengerButtonClicked(Uri photoURI) {
    // The URI can reference a file://, content://, or android.resource. Here we use
    // android.resource for sample purposes.
    Uri uri =photoURI;
    Log.e("YUE", "URI in click : " + uri);
    // Create the parameters for what we want to send to Messenger.
    ShareToMessengerParams shareToMessengerParams =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .build();


    if (mPicking) {
      // If we were launched from Messenger, we call MessengerUtils.finishShareToMessenger to return
      // the content to Messenger.
      MessengerUtils.finishShareToMessenger(this, shareToMessengerParams);
    } else {
      // Otherwise, we were launched directly (for example, user clicked the launcher icon). We
      // initiate the broadcast flow in Messenger. If Messenger is not installed or Messenger needs
      // to be upgraded, this will direct the user to the play store.
      try{
        MessengerUtils.shareToMessenger(
                this,
                REQUEST_CODE_SHARE_TO_MESSENGER,
                shareToMessengerParams);
      }catch (Exception e){
        Log.e("YUE", "ERROR!!! : " + e);
      }

    }
  }
}


