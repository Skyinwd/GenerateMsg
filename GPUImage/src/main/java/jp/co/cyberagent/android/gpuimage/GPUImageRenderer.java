/*
* Copyright (C) 2012 CyberAgent
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package jp.co.cyberagent.android.gpuimage;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Presentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import jp.co.cyberagent.android.gpuimage.grafika.gles.GlUtil;
import jp.co.cyberagent.android.gpuimage.grafika.videoencoder.AudioThread;
import jp.co.cyberagent.android.gpuimage.grafika.videoencoder.GPUImageMovieEncoder;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.Queue;
import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;
@SuppressLint("WrongCall")
@TargetApi(11)
public class GPUImageRenderer implements Renderer, PreviewCallback, SurfaceTexture.OnFrameAvailableListener, AudioThread.AudioThreadListener{

    public static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    public static final int NO_IMAGE = -1;
    static final float CUBE[] = { -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
            1.0f, };


    private boolean updateSurface = false;
    private boolean mediaPlaying = false;

    private static final String TAG = "GPUImageRenderer";

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    private boolean mRecordingEnabled;
    private int mRecordingStatus;

    private static final int PLAYER_IDLE = 0;
    private static final int PLAYER_INITIALIZED = 1;
    private static final int PLAYER_PREPARED = 2;
    private static final int PLAYER_STARTED = 3;
    private static final int PLAYER_PAUSED = 4;
    private static final int PLAYER_STOPPED = 5;

    private static final boolean VERBOSE = true;

    private int mPlayerState;

    private int currentContext = -1;

    private File mOutputFile;

    private PlayerThread mPlayer = null;
    private Uri mMediaUri = null;
    private Context mContext = null;

    private final static GPUImageMovieEncoder mVideoEncoder = new GPUImageMovieEncoder();
    public final Object mSurfaceChangedWaiter = new Object();
    private int mGLTextureId = NO_IMAGE;
    private SurfaceTexture mSurfaceTexture = null;
    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private IntBuffer mGLRgbBuffer;
    private int mOutputWidth;
    private int mOutputHeight;
    private int mRecordingWidth;
    private int mRecordingHeight;
    private int mRecordingBitrate;
    private int mImageWidth;
    private int mImageHeight;
    private int mAddedPadding;
    private final Queue<Runnable> mRunOnDraw;
    private final Queue<Runnable> mRunOnDrawEnd;
    private Rotation mRotation;
    private boolean mFlipHorizontal;
    private boolean mFlipVertical;
    private GPUImage.ScaleType mScaleType = GPUImage.ScaleType.CENTER_CROP;

    private GPUImageFilter mFilter;
    private GPUImageFilter mPresentationFilter;

    private int mPresentationOutputWidth;
    private int mPresentationOutputHeight;

    private GL10 gl1;
    private GL10 glPresentation;

    android.opengl.EGLDisplay mPresentationEglDisplay;
    android.opengl.EGLSurface mPresentationEglDrawSurface;
    android.opengl.EGLSurface mPresentationEglReadSurface;
    android.opengl.EGLContext mPresentationEglContext;

    android.opengl.EGLDisplay mEGLDisplay;
    android.opengl.EGLSurface mEGLSurface;
    android.opengl.EGLContext mEGLContext;

    public GPUImageRenderer(final GPUImageFilter filter) {

        mFilter = filter;
        mRunOnDraw = new LinkedList<Runnable>();
        mRunOnDrawEnd = new LinkedList<Runnable>();
        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);
        mGLTextureBuffer = ByteBuffer
                .allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        setRotation(Rotation.NORMAL, false, false);
    }

    private int getGLId(GL10 gl) {
        if (gl.equals(gl1)) {
            return 1;
        } else if (gl.equals(glPresentation)) {
            return 2;
        } else {
            return -1;
        }
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        if(gl1 == null){
            gl1 = unused;
            GLES20.glClearColor(0, 1, 0, 1);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            mFilter.init();

        }
        else if (glPresentation == null){
            glPresentation = unused;
            GLES20.glClearColor(0, 1, 0, 1);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            mPresentationFilter.init();
        }


        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mSurfaceTexture = new SurfaceTexture(textures[0]);
    }
    @Override
    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        if (getGLId(gl) == 1){
            mOutputWidth = width;
            mOutputHeight = height;
            GLES20.glViewport(0, 0, width, height);
            GLES20.glUseProgram(mFilter.getProgram());
            mFilter.onOutputSizeChanged(width, height);
            saveRenderState(false);
        }
        else if (getGLId(gl) == 2){
            mPresentationOutputWidth = width;
            mPresentationOutputHeight = height;
            GLES20.glViewport(0, 0, width, height);
            GLES20.glUseProgram(mPresentationFilter.getProgram());
            mPresentationFilter.onOutputSizeChanged(width, height);
            saveRenderState(true);
        }
        adjustImageScaling();
        synchronized (mSurfaceChangedWaiter) {
            mSurfaceChangedWaiter.notifyAll();
        }
    }
    @Override
    public void onDrawFrame(final GL10 gl) {
        //if (getGLId(gl) == 2) return;

        // If the recording state is changing, take care of it here.  Ideally we wouldn't
        // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
        // makes it hard to do elsewhere.
        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    Log.d(TAG, "START recording");
                    // start recording
                    setOutputFilePath();
                    mVideoEncoder.startRecording(new GPUImageMovieEncoder.EncoderConfig(
                            mOutputFile, mRecordingWidth, mRecordingHeight, mRecordingBitrate, EGL14.eglGetCurrentContext()));
                    mVideoEncoder.setTargetTexture((mediaPlaying ? GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D));
                    mRecordingStatus = RECORDING_ON;
                    setFilter(mFilter);
                    break;
                case RECORDING_RESUMED:
                    Log.d(TAG, "RESUME recording");
                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        } else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    Log.d(TAG, "STOP recording");
                    mVideoEncoder.stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }

        boolean swapBuffer = false;
        boolean toScreen = false;
        toScreen  = getGLId(gl) == 2 ? true : false;
        if (currentContext != getGLId(gl)){
            //Log.d(TAG, String.valueOf(getGLId(gl)));

            makeCurrent(toScreen, null);
            currentContext = getGLId(gl);
            updateSurface = false;
            if (mSurfaceTexture != null) {
//              mSurfaceTexture.detachFromGLContext();
//              mSurfaceTexture.attachToGLContext(EGL14.eglGetCurrentContext().hashCode());
            }
            //if (!toScreen) EGL14.eglSwapBuffers(mPresentationEglDisplay, mPresentationEglReadSurface);
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        runAll(mRunOnDraw); // METHOD CALLED AFTER RELEASE BUG

        if (toScreen){
            mPresentationFilter.onDraw((mediaPlaying ? GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D), mGLTextureId, mGLCubeBuffer, mGLTextureBuffer);
        }
        else {
            mFilter.onDraw((mediaPlaying ? GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D), mGLTextureId, mGLCubeBuffer, mGLTextureBuffer);
        }
        if (mSurfaceTexture != null) {

            // Set the video encoder's texture name.  We only need to do this once, but in the
            // current implementation it has to happen after the video encoder is started, so
            // we just do it here.
            //
            // TODO: be less lame.
            if (mRecordingStatus == RECORDING_ON){
                mVideoEncoder.setTextureId(mGLTextureId);
                mVideoEncoder.setCubeBuffer(mGLCubeBuffer);
                mVideoEncoder.setTextureBuffer(mGLTextureBuffer);

                // Tell the video encoder thread that a new frame is available.
                // This will be ignored if we're not actually recording.
                mVideoEncoder.frameAvailable(mSurfaceTexture);
            }
            if (mediaPlaying){
                if (updateSurface) {
                    mSurfaceTexture.updateTexImage();
                    updateSurface = false;
                }
            }
            else {
                //if (updateSurface) {
                    mSurfaceTexture.updateTexImage();
                    //updateSurface = false;
               // }
                //else {
                    //updateSurface = true;
                    if(toScreen){
                        //EGL14.eglSwapBuffers((toScreen ? mPresentationEglDisplay : mEGLDisplay), (toScreen ? mPresentationEglReadSurface : mEGLSurface));
                        //EGL14.eglSwapBuffers(mPresentationEglDisplay, mPresentationEglReadSurface);
               //     }
                }
            }
        }

        runAll(mRunOnDrawEnd);
    }

    private void saveRenderState(boolean toScreen) {
        //System.arraycopy(mProjectionMatrix, 0, mSavedMatrix, 0, mProjectionMatrix.length);
        if (toScreen) {
            mPresentationEglDisplay = EGL14.eglGetCurrentDisplay();
            mPresentationEglDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
            mPresentationEglReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
            mPresentationEglContext = EGL14.eglGetCurrentContext();
        }
        else {
            mEGLDisplay = EGL14.eglGetCurrentDisplay();
            mEGLSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
            mEGLContext = EGL14.eglGetCurrentContext();
        }
    }

    public void makeCurrent(boolean toScreen, Surface surface) {
        if (toScreen) { //as opposed to toEncoder
            makeScreenSurfaceCurrent();
            return;
        }
        //EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);
        //GlUtil.checkGlError("eglMakeCurrent");
    }

    private void makeScreenSurfaceCurrent() {
        EGL14.eglMakeCurrent(mPresentationEglDisplay, mPresentationEglDrawSurface,
                mPresentationEglReadSurface, mPresentationEglContext);
        GlUtil.checkGlError("eglMakeCurrent");
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run(); // METHOD CALLED AFTER RELEASE BUG - Pt2
            }
        }
    }
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        final Size previewSize = camera.getParameters().getPreviewSize();
        if (mGLRgbBuffer == null) {
            mGLRgbBuffer = IntBuffer.allocate(previewSize.width
                    * previewSize.height);
        }
        if (mRunOnDraw.isEmpty()) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    GPUImageNativeLibrary.YUVtoRBGA(data, previewSize.width,
                            previewSize.height, mGLRgbBuffer.array());
                    mGLTextureId = OpenGlUtils.loadTexture(mGLRgbBuffer,
                            previewSize, mGLTextureId);
                    camera.addCallbackBuffer(data);
                    if (mImageWidth != previewSize.width) {
                        mImageWidth = previewSize.width;
                        mImageHeight = previewSize.height;
                        adjustImageScaling();
                    }
                }
            });
        }
    }

    public void startRecording() {
        mRecordingEnabled = true;
    }
    public void stopRecording() {
        mRecordingEnabled = false;
    }
    public void setUpSurfaceTexture(final Camera camera) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                try {
                    camera.setPreviewTexture(mSurfaceTexture); // METHOD CALLED
                    // AFTER RELEASE
                    // BUG - Pt1
                    camera.setPreviewCallback(GPUImageRenderer.this);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private AudioThread mAudioThread = null;

    public void setUpAudioRecord(AudioThread audioThread){
        mAudioThread = audioThread;
        mAudioThread.setmListener(this);
    }

    public void setFilter(final GPUImageFilter filter) {
        if(mRecordingEnabled){
            mVideoEncoder.setFilter(filter);
        }
        filter.setmInputIsVideo(mediaPlaying);
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                final GPUImageFilter oldFilter = mFilter;
                mFilter = filter;
                mPresentationFilter = filter;
                if (oldFilter != null) {
                    oldFilter.destroy();
                }
                mFilter.setmInputIsVideo(mediaPlaying);
                mPresentationFilter.setmInputIsVideo(mediaPlaying);

                mFilter.init();
                //mPresentationFilter.init();

                GLES20.glUseProgram(mFilter.getProgram());
                mFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);

                //mPresentationFilter.onOutputSizeChanged(mPresentationOutputWidth, mPresentationOutputHeight);
            }
        });
    }

    public void setVideo(final Context context, final Uri uri){
        //mediaPlaying = true;
        //creating and off screen surface
        mGLRgbBuffer = null;
        mSurfaceTexture = new SurfaceTexture(mGLTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mMediaUri = uri;
        mContext = context;
        if (mPlayer == null) {
                mPlayer = new PlayerThread(null, mContext, mMediaUri);
                mPlayer.setLoopMode(true);
        }
    }

    public void startVideo(){
        mPlayer.start();
    }

    public void stopVideo(){
        mPlayer.requestStop();
        //mPlayer.stop();
    }

    public void deleteImage() {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glDeleteTextures(1, new int[]{mGLTextureId}, 0);
                mGLTextureId = NO_IMAGE;
            }
        });
    }
    public void setImageBitmap(final Bitmap bitmap) {
        setImageBitmap(bitmap, true);
    }
    public void setImageBitmap(final Bitmap bitmap, final boolean recycle) {
        if (bitmap == null) {
            return;
        }
        mediaPlaying = false;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                Bitmap resizedBitmap = null;
                if (bitmap.getWidth() % 2 == 1) {
                    resizedBitmap = Bitmap.createBitmap(bitmap.getWidth() + 1,
                            bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas can = new Canvas(resizedBitmap);
                    can.drawARGB(0x00, 0x00, 0x00, 0x00);
                    can.drawBitmap(bitmap, 0, 0, null);
                    mAddedPadding = 1;
                } else {
                    mAddedPadding = 0;
                }
                mGLTextureId = OpenGlUtils.loadTexture(
                        resizedBitmap != null ? resizedBitmap : bitmap,
                        mGLTextureId, recycle);
                if (resizedBitmap != null) {
                    resizedBitmap.recycle();
                }
                mImageWidth = bitmap.getWidth();
                mImageHeight = bitmap.getHeight();
                adjustImageScaling();
            }
        });
    }
    public void setScaleType(GPUImage.ScaleType scaleType) {
        mScaleType = scaleType;
    }
    protected int getFrameWidth() {
        return mOutputWidth;
    }
    protected int getFrameHeight() {
        return mOutputHeight;
    }
    private void adjustImageScaling() {
        float outputWidth = mOutputWidth;
        float outputHeight = mOutputHeight;
        if (mRotation == Rotation.ROTATION_270
                || mRotation == Rotation.ROTATION_90) {
            outputWidth = mOutputHeight;
            outputHeight = mOutputWidth;
        }
        float ratio1 = outputWidth / mImageWidth;
        float ratio2 = outputHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);
        float ratioWidth = imageWidthNew / outputWidth;
        float ratioHeight = imageHeightNew / outputHeight;
        float[] cube = CUBE;
        float[] textureCords = TextureRotationUtil.getRotation(mRotation,
                mFlipHorizontal, mFlipVertical);
        if (mScaleType == GPUImage.ScaleType.CENTER_CROP) {
            float distHorizontal = (1 - 1 / ratioWidth) / 2;
            float distVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[] {
                    addDistance(textureCords[0], distHorizontal),
                    addDistance(textureCords[1], distVertical),
                    addDistance(textureCords[2], distHorizontal),
                    addDistance(textureCords[3], distVertical),
                    addDistance(textureCords[4], distHorizontal),
                    addDistance(textureCords[5], distVertical),
                    addDistance(textureCords[6], distHorizontal),
                    addDistance(textureCords[7], distVertical), };
        } else {
            cube = new float[] { CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                    CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                    CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                    CUBE[6] / ratioHeight, CUBE[7] / ratioWidth, };
        }
        mGLCubeBuffer.clear();
        mGLCubeBuffer.put(cube).position(0);
        mGLTextureBuffer.clear();
        mGLTextureBuffer.put(textureCords).position(0);
    }
    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }
    public void setRotationCamera(final Rotation rotation,
                                  final boolean flipHorizontal, final boolean flipVertical) {
        setRotation(rotation, flipVertical, flipHorizontal);
    }
    public void setRotation(final Rotation rotation) {
        mRotation = rotation;
        adjustImageScaling();
    }
    public void setRotation(final Rotation rotation,
                            final boolean flipHorizontal, final boolean flipVertical) {
        mFlipHorizontal = flipHorizontal;
        mFlipVertical = flipVertical;
        setRotation(rotation);
    }
    public Rotation getRotation() {
        return mRotation;
    }
    public boolean isFlippedHorizontally() {
        return mFlipHorizontal;
    }
    public boolean isFlippedVertically() {
        return mFlipVertical;
    }
    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }
    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (mRunOnDrawEnd) {
            mRunOnDrawEnd.add(runnable);
        }
    }

    public void setOutputFilePath (){
        String basePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        String customPath = "/Generate";
        String savePath = basePath.concat(customPath);
        File folder = new File(savePath);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        if (success) {
            mOutputFile = new File(savePath, System.currentTimeMillis() + ".mp4");
        } else {
            Toast.makeText(null, "Failed to create Generate Filder", Toast.LENGTH_SHORT);
        }
    }

    public File getVideoOutputFile(){
        return mOutputFile;
    }

    public int getmRecordingWidth() {
        return mRecordingWidth;
    }

    public void setmRecordingWidth(int mRecordingWidth) {
        this.mRecordingWidth = mRecordingWidth;
    }

    public int getmRecordingHeight() {
        return mRecordingHeight;
    }

    public void setmRecordingHeight(int mRecordingHeight) {
        this.mRecordingHeight = mRecordingHeight;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                //updateSurface = true;
            }
        });
    }

    @Override
    public void onAudioFrameAvailable(byte[] buf, int readBytes, long presentationTime) {
        if(mRecordingEnabled){
            mVideoEncoder.audioFrameAvailable(buf, readBytes, presentationTime);
        }

    }

    public Double getVolume(){
        if(mAudioThread == null) return 0.0;
        return mAudioThread.getmVolume();
    }

    @Override
    public void onAudioFrameAvailableSoon() {
        if(mRecordingEnabled){
            mVideoEncoder.audioFrameAvailableSoon();
        }

    }

    public void setmRecordingBitrate(int mRecordingBitrate) {
        this.mRecordingBitrate = mRecordingBitrate;
    }

    private class PlayerThread extends Thread {
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private Surface mOutputSurface;
        private Uri uri;
        private Context context;

        private FrameCallback mFrameCallback;

        private int mVideoWidth;
        private int mVideoHeight;

        private boolean mIsStopRequested;

        private boolean mLoop;

        // Declare this here to reduce allocations.
        private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

        public PlayerThread(Surface surface, Context context, final Uri uri) {
            this.mOutputSurface = surface;
            this.uri = uri;
            this.context = context;
        }
        @Override
        public void run() {

            try {
                extractor = new MediaExtractor();
                try {
                    extractor.setDataSource(context, uri, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int trackIndex = selectTrack(extractor);
/*                if (trackIndex < 0) {
                    throw new RuntimeException("No video track found in " + mSourceFile);
                }*/
                extractor.selectTrack(trackIndex);

                MediaFormat format = extractor.getTrackFormat(trackIndex);
                mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                // Create a MediaCodec decoder, and configure it with the MediaFormat from the
                // extractor.  It's very important to use the format from the extractor because
                // it contains a copy of the CSD-0/CSD-1 codec-specific data chunks.
                String mime = format.getString(MediaFormat.KEY_MIME);
                try {
                    decoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                decoder.configure(format, null, null, 0);
                //decoder.configure(format, mOutputSurface, null, 0);
                decoder.start();

                //mOutputSurface.release();

                doExtract(extractor, trackIndex, decoder, mFrameCallback);
            } finally {
                // release everything we grabbed
                if (decoder != null) {
                    decoder.stop();
                    decoder.release();
                    decoder = null;
                }
                if (extractor != null) {
                    extractor.release();
                    extractor = null;
                }
            }
        }

        /**
         * Returns the width, in pixels, of the video.
         */
        public int getVideoWidth() {
            return mVideoWidth;
        }

        /**
         * Returns the height, in pixels, of the video.
         */
        public int getVideoHeight() {
            return mVideoHeight;
        }

        /**
         * Sets the loop mode.  If true, playback will loop forever.
         */
        public void setLoopMode(boolean loopMode) {
            mLoop = loopMode;
        }

        /**
         * Asks the player to stop.  Returns without waiting for playback to halt.
         * <p>
         * Called from arbitrary thread.
         */
        public void requestStop() {
            mIsStopRequested = true;
        }


        /**
         * Work loop.  We execute here until we run out of video or are told to stop.
         */
        private void doExtract(MediaExtractor extractor, int trackIndex, MediaCodec decoder,
                               FrameCallback frameCallback) {
            // We need to strike a balance between providing input and reading output that
            // operates efficiently without delays on the output side.
            //
            // To avoid delays on the output side, we need to keep the codec's input buffers
            // fed.  There can be significant latency between submitting frame N to the decoder
            // and receiving frame N on the output, so we need to stay ahead of the game.
            //
            // Many video decoders seem to want several frames of video before they start
            // producing output -- one implementation wanted four before it appeared to
            // configure itself.  We need to provide a bunch of input frames up front, and try
            // to keep the queue full as we go.
            //
            // (Note it's possible for the encoded data to be written to the stream out of order,
            // so we can't generally submit a single frame and wait for it to appear.)
            //
            // We can't just fixate on the input side though.  If we spend too much time trying
            // to stuff the input, we might miss a presentation deadline.  At 60Hz we have 16.7ms
            // between frames, so sleeping for 10ms would eat up a significant fraction of the
            // time allowed.  (Most video is at 30Hz or less, so for most content we'll have
            // significantly longer.)  Waiting for output is okay, but sleeping on availability
            // of input buffers is unwise if we need to be providing output on a regular schedule.
            //
            //
            // In some situations, startup latency may be a concern.  To minimize startup time,
            // we'd want to stuff the input full as quickly as possible.  This turns out to be
            // somewhat complicated, as the codec may still be starting up and will refuse to
            // accept input.  Removing the timeout from dequeueInputBuffer() results in spinning
            // on the CPU.
            //
            // If you have tight startup latency requirements, it would probably be best to
            // "prime the pump" with a sequence of frames that aren't actually shown (e.g.
            // grab the first 10 NAL units and shove them through, then rewind to the start of
            // the first key frame).
            //
            // The actual latency seems to depend on strongly on the nature of the video (e.g.
            // resolution).
            //
            //
            // One conceptually nice approach is to loop on the input side to ensure that the codec
            // always has all the input it can handle.  After submitting a buffer, we immediately
            // check to see if it will accept another.  We can use a short timeout so we don't
            // miss a presentation deadline.  On the output side we only check once, with a longer
            // timeout, then return to the outer loop to see if the codec is hungry for more input.
            //
            // In practice, every call to check for available buffers involves a lot of message-
            // passing between threads and processes.  Setting a very brief timeout doesn't
            // exactly work because the overhead required to determine that no buffer is available
            // is substantial.  On one device, the "clever" approach caused significantly greater
            // and more highly variable startup latency.
            //
            // The code below takes a very simple-minded approach that works, but carries a risk
            // of occasionally running out of output.  A more sophisticated approach might
            // detect an output timeout and use that as a signal to try to enqueue several input
            // buffers on the next iteration.
            //
            // If you want to experiment, set the VERBOSE flag to true and watch the behavior
            // in logcat.  Use "logcat -v threadtime" to see sub-second timing.

            final int TIMEOUT_USEC = 10000;
            ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
            int inputChunk = 0;
            long firstInputTimeNsec = -1;

            boolean outputDone = false;
            boolean inputDone = false;

            while (!outputDone) {
                if (VERBOSE) Log.d(TAG, "loop");
                if (mIsStopRequested) {
                    Log.d(TAG, "Stop requested");
                    return;
                }

                // Feed more data to the decoder.
                if (!inputDone) {
                    int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputBufIndex >= 0) {
                        if (firstInputTimeNsec == -1) {
                            firstInputTimeNsec = System.nanoTime();
                        }
                        ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];

                        // Read the sample data into the ByteBuffer.  This neither respects nor
                        // updates inputBuf's position, limit, etc.
                        int chunkSize = extractor.readSampleData(inputBuf, 0);
                        if (chunkSize < 0) {
                            // End of stream -- send empty frame with EOS flag set.
                            decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                            if (VERBOSE) Log.d(TAG, "sent input EOS");
                        } else {
                            if (extractor.getSampleTrackIndex() != trackIndex) {
                                Log.w(TAG, "WEIRD: got sample from track " +
                                        extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                            }
                            long presentationTimeUs = extractor.getSampleTime();
                            decoder.queueInputBuffer(inputBufIndex, 0, chunkSize,
                                    presentationTimeUs, 0 /*flags*/);
                            if (VERBOSE) {
                                Log.d(TAG, "submitted frame " + inputChunk + " to dec, size=" +
                                        chunkSize);
                            }
                            inputChunk++;
                            extractor.advance();
                        }
                    } else {
                        if (VERBOSE) Log.d(TAG, "input buffer not available");
                    }
                }

                if (!outputDone) {
                    int decoderStatus = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                    if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        if (VERBOSE) Log.d(TAG, "no output from decoder available");
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // not important for us, since we're using Surface
                        if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = decoder.getOutputFormat();
                        if (VERBOSE) Log.d(TAG, "decoder output format changed: " + newFormat);
                    } else if (decoderStatus < 0) {
                        throw new RuntimeException(
                                "unexpected result from decoder.dequeueOutputBuffer: " +
                                        decoderStatus);
                    } else {

                     // decoderStatus >= 0
                        if (firstInputTimeNsec != 0) {
                            // Log the delay from the first buffer of input to the first buffer
                            // of output.
                            long nowNsec = System.nanoTime();
                            Log.d(TAG, "startup lag " + ((nowNsec-firstInputTimeNsec) / 1000000.0) + " ms");
                            firstInputTimeNsec = 0;
                        }
                        boolean doLoop = false;
                        if (VERBOSE) Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                                " (size=" + mBufferInfo.size + ")");
                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE) Log.d(TAG, "output EOS");
                            if (mLoop) {
                                doLoop = true;
                            } else {
                                outputDone = true;
                            }
                        }

                        boolean doRender = (mBufferInfo.size != 0);

                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture.  We can't control when it
                        // appears on-screen, but we can manage the pace at which we release
                        // the buffers.
                        if (doRender && frameCallback != null) {
                            frameCallback.preRender(mBufferInfo.presentationTimeUs);

                        }

                        //MY STUFF

                        //ByteBuffer buffer = decoderOutputBuffers[decoderStatus];
                        ByteBuffer buffer = decoder.getOutputBuffer(decoderStatus);

                        if(buffer != null){
                            buffer.position(mBufferInfo.offset);
                            buffer.limit(mBufferInfo.offset + mBufferInfo.size);

                            Log.d(TAG, "offset: " + mBufferInfo.offset + " size: " + mBufferInfo.size);

                            final byte[] ba = new byte[buffer.remaining()];
                            buffer.get(ba);

                            if (mGLRgbBuffer == null) {
                                mGLRgbBuffer = IntBuffer.allocate(mVideoHeight
                                        * mVideoWidth);
                            }

                            if (mRunOnDraw.isEmpty()) {
                                runOnDraw(new Runnable() {
                                    @Override
                                    public void run() {
                                        GPUImageNativeLibrary.YUVtoRBGA(ba, mVideoWidth,
                                                mVideoHeight, mGLRgbBuffer.array());
                                        mGLTextureId = OpenGlUtils.loadTexture(mGLRgbBuffer,
                                                mVideoWidth, mVideoHeight, mGLTextureId);
                                        if (mImageWidth != mVideoWidth) {
                                            mImageWidth = mVideoWidth;
                                            mImageHeight = mVideoHeight;
                                            adjustImageScaling();
                                        }

                                    }
                                });
                            }

                        }

                        //END OF MY STUFF

                        decoder.releaseOutputBuffer(decoderStatus, doRender);

                        if(doRender){
                            if (mRunOnDraw.isEmpty()) {
                                runOnDraw(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "Will update surface");
                                        updateSurface = true;
                                    }
                                });
                            }
                            else {
                                Log.d(TAG, "Run on draw not empty");
                            }
                        }

                        if (doRender && frameCallback != null) {
                            frameCallback.postRender();
                        }

                        if (doLoop) {
                            Log.d(TAG, "Reached EOS, looping");
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            inputDone = false;
                            decoder.flush();    // reset decoder state
                            //frameCallback.loopReset();
                        }
                    }
                }
            }
        }
    }

    /**
     * Selects the video track, if any.
     *
     * @return the track index, or -1 if no video track is found.
     */
    private static int selectTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }

        return -1;
    }

    /**
     * Callback invoked when rendering video frames. The MoviePlayer client must
     * provide one of these.
     */
    public interface FrameCallback {
        /**
         * Called immediately before the frame is rendered.
         * @param presentationTimeUsec The desired presentation time, in microseconds.
         */
        void preRender(long presentationTimeUsec);

        /**
         * Called immediately after the frame render call returns.  The frame may not have
         * actually been rendered yet.
         * TODO: is this actually useful?
         */
        void postRender();

        /**
         * Called after the last frame of a looped movie has been rendered.  This allows the
         * callback to adjust its expectations of the next presentation time stamp.
         */
        void loopReset();
    }


    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * Returns a color format that is supported by the codec and by this test code. If no
     * match is found, this throws a test failure -- the set of formats known to the test
     * should be expanded for new platforms.
     */
    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        //fail("couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return 0; // not reached
    }

    /**
     * Returns true if this is a color format that this test code understands (i.e. we know how
     * to read and generate frames in this format).
     */
    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }
}