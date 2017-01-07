package com.generate.generatemsg;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.facebook.messenger.MessengerThreadParams;
import com.facebook.messenger.MessengerUtils;
import com.facebook.messenger.ShareToMessengerParams;
import com.facebook.FacebookSdk;

import android.provider.MediaStore;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.io.File;
import android.os.Environment;
import java.util.Date;
import android.util.Log;
import android.widget.ImageView;

public class MainActivity extends Activity {

  // This is the request code that the SDK uses for startActivityForResult. See the code below
  // that references it. Messenger currently doesn't return any data back to the calling
  // application.
  private static final int REQUEST_CODE_SHARE_TO_MESSENGER = 1;

  private Toolbar mToolbar;
  private View mMessengerButton;
  private MessengerThreadParams mThreadParams;
  private boolean mPicking;
  private ImageView photoContainer;
  Uri photoURI;
  static final int REQUEST_TAKE_PHOTO = 1;
  static final int REQUEST_IMAGE_CAPTURE = 1;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FacebookSdk.sdkInitialize(getApplicationContext());

    setContentView(R.layout.activity_main);
    mToolbar = (Toolbar) findViewById(R.id.toolbar);
    photoContainer = (ImageView) findViewById(R.id.photoContainer);
    mMessengerButton = findViewById(R.id.messenger_send_button);

    //mToolbar.setTitle(R.string.app_name);

    // If we received Intent.ACTION_PICK from Messenger, we were launched from a composer shortcut
    // or the reply flow.
    Intent intent = getIntent();
    if (Intent.ACTION_PICK.equals(intent.getAction())) {
      mThreadParams = MessengerUtils.getMessengerThreadParamsForIntent(intent);
      mPicking = true;

      // Note, if mThreadParams is non-null, it means the activity was launched from Messenger.
      // It will contain the metadata associated with the original content, if there was content.
    }

    // open camera take photo
    dispatchTakePictureIntent();

    mMessengerButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onMessengerButtonClicked();
      }
    });
  }



  private void dispatchTakePictureIntent() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      // Create the File where the photo should go
      File photoFile = null;
      try {
        photoFile = createImageFile();
      } catch (IOException ex) {
        // Error occurred while creating the File
      }
      // Continue only if the File was successfully created
      if (photoFile != null) {
        photoURI = Uri.fromFile(photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
      }
    }
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
    //String mCurrentPhotoPath = image.getAbsolutePath();
    return image;
  }





  private void onMessengerButtonClicked() {
    // The URI can reference a file://, content://, or android.resource. Here we use
    // android.resource for sample purposes.
    Uri uri =photoURI;

    // Create the parameters for what we want to send to Messenger.
    ShareToMessengerParams shareToMessengerParams =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .setMetaData("{ \"image\" : \"tree\" }")
            .build();

    if (mPicking) {
      // If we were launched from Messenger, we call MessengerUtils.finishShareToMessenger to return
      // the content to Messenger.
      MessengerUtils.finishShareToMessenger(this, shareToMessengerParams);
    } else {
      // Otherwise, we were launched directly (for example, user clicked the launcher icon). We
      // initiate the broadcast flow in Messenger. If Messenger is not installed or Messenger needs
      // to be upgraded, this will direct the user to the play store.
      MessengerUtils.shareToMessenger(
          this,
          REQUEST_CODE_SHARE_TO_MESSENGER,
          shareToMessengerParams);
    }
  }
}
