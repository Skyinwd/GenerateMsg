package com.generate.generatemsg;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.View;

import com.facebook.messenger.MessengerThreadParams;
import com.facebook.messenger.MessengerUtils;
import com.facebook.messenger.ShareToMessengerParams;
import com.facebook.FacebookSdk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.io.File;
import java.util.Date;
import android.util.Log;
import android.widget.ImageView;
import android.content.pm.PackageManager;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import jp.co.cyberagent.android.gpuimage.GPUImageBoxBlurFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageEmbossFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageHueFilter;
import jp.co.cyberagent.android.gpuimage.GPUImagePixelationFilter;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import java.io.FileOutputStream;
import java.util.Random;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import android.opengl.GLSurfaceView;
import android.graphics.Bitmap;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Animatable;
import java.util.List;

enum ASPECT_RATIO {
  NOT_SQUARE, SQUARE
}

public class MainActivity extends Activity {

  private static final int REQUEST_CODE_SHARE_TO_MESSENGER = 1;
  public static final int MEDIA_TYPE_IMAGE = 1;
  public static final int MEDIA_TYPE_VIDEO = 2;

  // buttons
  private Button mMessengerButton;
  private Button captureButton;
  private Button reGenerateBtn;
  private Button reTakePhotoBtn;
  private ImageView loadingSignal;
  private AnimationDrawable loadingAnimation;

  private float yueTestFactorValue;

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

  private Bitmap bitmapToSave;
  private Context context;
  private int currentRotation = 90;
  private String currentFilter;
  private String lastFilter = "NONE";
  private String[] filterNameList = {"HUE","PIXEL", "RPG", "BOX", "DOT"};//{"BOX","HUE"};//
  private float currentFactor;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FacebookSdk.sdkInitialize(getApplicationContext()); // facebook sdk initialize
    context = this;
    setContentView(R.layout.activity_main);


    // Create an instance of Camera
    mCamera = getCameraInstance();
    mCamera.setParameters(setParameters());
    Log.d("YUE", "Final Size Height = "+mCamera.getParameters().getPictureSize().height);
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
    currentFactor = randomFilterFactor();
    currentFilter = randomFilter(filterNameList);
    mGPUImage.setFilter(getGPUImageFilter(currentFilter, currentFactor));

    // Create Buttons
    captureButton = (Button) findViewById(R.id.camera_capture); // take photo button
    reGenerateBtn = (Button) findViewById(R.id.regenerate);
    mMessengerButton = (Button)findViewById(R.id.send_msg); // send to Messenger button
    reTakePhotoBtn = (Button) findViewById(R.id.retakephoto);
    loadingSignal = (ImageView) findViewById(R.id.loading);

    loadingAnimation = (AnimationDrawable) loadingSignal.getBackground();
    //loadingAnimation = (AnimationDrawable)((ImageView) loadingSignal).getDrawable();


    //loadingSignal.setBackgroundResource(R.drawable.loadingAnim);


    // Create and Add a listener to the Capture button
    captureButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {

                captureImage();
                Log.d("YUE", "Saved??");

                // change UI to sendMsg btn and Retake btn
                captureButton.setVisibility(View.INVISIBLE);
                reGenerateBtn.setVisibility(View.INVISIBLE);
                //mMessengerButton.setBackgroundResource(R.drawable.btn_sendmsg_gray);
                mMessengerButton.setVisibility(View.VISIBLE);
                loadingSignal.setVisibility(View.VISIBLE);
                reTakePhotoBtn.setVisibility(View.VISIBLE);
              }
            });

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

    reGenerateBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        //random a new filter factor
        currentFactor = randomFilterFactor();
        do{
          currentFilter = randomFilter(filterNameList);

        }while (currentFilter == lastFilter);

        lastFilter = currentFilter;
        mGPUImage.setFilter(getGPUImageFilter(currentFilter, currentFactor));


      }
    });

    reTakePhotoBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //TODO: clean up current photo

        //restart camera preview
        mCamera.startPreview();

        // change UI to sendMsg btn and Retake btn
        captureButton.setVisibility(View.VISIBLE);
        reGenerateBtn.setVisibility(View.VISIBLE);
        mMessengerButton.setVisibility(View.INVISIBLE);
        mMessengerButton.setBackgroundResource(R.drawable.btn_sendmsg_gray);
        reTakePhotoBtn.setVisibility(View.INVISIBLE);
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


  private void captureImage(){
    mCamera.takePicture(null, null, new Camera.PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, final Camera camera) {
        Log.d("YUE", " onPictureTaken start ");
        // Test Data
        if(data == null) {
          Log.d("YUE", "Save Photo Fail!!!!!!!!!!!!!!!!!!!!!!!");
          return;
        }

        Log.d("YUE", " Trying to write image file ");

        // TODO: set MessengerButton to gray
        // TODO: add loading rotation icon on top
        // Use this like for loading the image in another thread
        new WriteImageFileTask().execute(data);

        Log.d("YUE", " Done!!!!! ");
      }
    });
  }

  //ASYNC IMAGE WRITING
  private class WriteImageFileTask extends AsyncTask<byte[], Object, Bitmap> {
    protected Bitmap doInBackground(byte[]... params) {

      Bitmap bitmap = BitmapFactory.decodeByteArray(params[0] , 0, params[0] .length);
      GPUImage yGPUImage = new GPUImage(context);
      yGPUImage.setFilter(getGPUImageFilter(currentFilter, currentFactor));
      Bitmap filteredBitmap = yGPUImage.getBitmapWithFilterApplied(bitmap);
      filteredBitmap = rotateImage(filteredBitmap, currentRotation);

      return filteredBitmap;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
      //bitmapToSave = result;

      SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
      String timeStamp = s.format(new Date());

      File storageDir = getApplicationContext().getFilesDir();
      String fileName = "img_filtered";
      File mediaFile = new File(storageDir, fileName.concat(timeStamp).concat(".jpg"));
      Log.e("YUE", "mediaFile " + mediaFile);


      try {
        FileOutputStream fos = new FileOutputStream(mediaFile);
        result.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        fos.close();
      } catch (FileNotFoundException e) {
        Log.d(TAG, "File not found: " + e.getMessage());
      } catch (IOException e) {
        Log.d(TAG, "Error accessing file: " + e.getMessage());
      }

      photoURI = FileProvider.getUriForFile(getApplicationContext(), "com.generage.generatemsg.android.fileprovider", mediaFile);
      loadingSignal.setVisibility(View.INVISIBLE);
      mMessengerButton.setBackgroundResource(R.drawable.btn_sendmsg);

      Log.d("YUE", "file URI : " + photoURI.toString());
    }
  }

  private Bitmap rotateImage(Bitmap source, float angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    if(source == null) {
      Toast.makeText(context, "Null Image - please try again", Toast.LENGTH_SHORT).show();
    }
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
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
    try {
      mCamera = Camera.open();
      mCamera.setParameters(setParameters());
      Log.d("YUE", "Final Size Height = "+mCamera.getParameters().getPictureSize().height);
    } catch (Exception e) {
      e.printStackTrace();
    }
    mGPUImage.setUpCamera(mCamera, currentRotation, false, shouldFlipVertically);
  }

  // setup parameters for Camera
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

    // Set camera to square
    int width = parameters.getPreviewSize().width;
    parameters.setPictureSize(width,width);
    List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();

    for (Camera.Size size : previewSizeList) {
      Log.e("YUE", "Size : " + size.width + " : " + size.height);

    }
    parameters.setPreviewSize(previewSizeList.get(0).width,previewSizeList.get(0).height);

    return parameters;
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

  /**  **/

  private GPUImageFilter getGPUImageFilter(String filterName, float filterFactor){
    //TODO: Each type of filter have different factor numbers. Maybe send random seed and random all factors at here.
    Log.d("YUE", "filter : "+filterName);
    switch (filterName){
      case "HUE":
        GPUImageHueFilter filterHUE = new GPUImageHueFilter();
        filterHUE.setHue(filterFactor*360);
        return filterHUE;
      case "PIXEL":
        GPUImagePixelationFilter filterPIXEL = new GPUImagePixelationFilter();
        filterPIXEL.setPixel(filterFactor*30);
        return filterPIXEL;
      case "RPG":
        GPUImageEmbossFilter filterRPG = new GPUImageEmbossFilter();
        filterRPG.setLineSize(filterFactor*360);
        filterRPG.setTexelHeight(filterFactor*3);
        filterRPG.setTexelWidth(filterFactor*7);
        return filterRPG;
      case "DOT":
        PolkaDotFilter filterDOT = new PolkaDotFilter();
        filterDOT.setDotScaling(filterFactor*0.1f+0.7f);
        return filterDOT;
      case "BOX":
        GPUImageBoxBlurFilter filterBox = new GPUImageBoxBlurFilter();
        filterBox.setBlurSize(filterFactor*12+10);
        return filterBox;
    }
    return null;
  }

  private String randomFilter(String[] filterNameList){
    int seed = new Random().nextInt(filterNameList.length);
    Log.d("YUE", "Random filter ::: " + seed);

    return filterNameList[seed];
  }

  private float randomFilterFactor(){

    float randomSeed = new Random().nextFloat();
    //float factor = randomBase*360;
    Log.d("YUE", "Random number ::: " + randomSeed);
    return randomSeed;
  }

}

