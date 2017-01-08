package jp.co.cyberagent.android.gpuimage.grafika.videoencoder;

import android.graphics.SurfaceTexture;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.grafika.gles.EglCore;
import jp.co.cyberagent.android.gpuimage.grafika.gles.FullFrameRect;
import jp.co.cyberagent.android.gpuimage.grafika.gles.WindowSurface;

/**
 * Created by martin on 15-01-16.
 */
public class GPUImageMovieEncoder  implements Runnable, EncoderInterface {
    private static final String TAG = "GPUImageMovieEncoder";
    private static final boolean VERBOSE = true;

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;
    private static final int MSG_SET_TEXTURE_BUFFER = 6;
    private static final int MSG_SET_CUBE_BUFFER = 7;
    private static final int MSG_SET_FILTER = 8;
    private static final int MSG_SET_TARGET_TEXTURE = 9;
    private static final int MSG_AUDIO_FRAME_AVAILABLE = 10;
    private static final int MSG_AUDIO_FRAME_AVAILABLE_SOON = 11;


    static final float CUBE[] = { -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
            1.0f, };

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private FullFrameRect mFullScreen;
    private GPUImageFilter mFilter;
    private int mTextureId;
    private int mTargetTexture;
    private int mFrameNum;

    private int mOutputWidth;
    private int mOutputHeight;

    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;
    private VideoEncoderCore mVideoEncoder;

    private AudioEncoderCore mAudioEncoder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;

    private MediaMuxer mMediaMuxer; // API >= 18

    private int encoderStarted = 0;

    private Thread mEncoderThread;

    @Override
    public void startMediaMuxer() {
        if (VERBOSE) Log.v(TAG,  "start:");
        encoderStarted++;
        if(encoderStarted > 1){
            mMediaMuxer.start();
            mAudioEncoder.setMuxerStarted(true);
            mVideoEncoder.setMuxerStarted(true);
        }
    }

    @Override
    public void stopMediaMuxer() {
        encoderStarted--;
        if(encoderStarted < 1){
            mAudioEncoder.setMuxerStarted(false);
            mVideoEncoder.setMuxerStarted(false);
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
            if(mEncoderThread != null){
                mEncoderThread.interrupt();
                mEncoderThread = null;
                mRunning = false;
            }else{
                mRunning = false;
            }
        }
    }

    @Override
    public void prepared() {
        if (VERBOSE) Log.v(TAG, "onPrepared:encoder");
    }

    /**
     * Encoder configuration.
     * <p>
     * Object is immutable, which means we can safely pass it between threads without
     * explicit synchronization (and don't need to worry about it getting tweaked out from
     * under us).
     * <p>
     * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
     *       with reasonable defaults for those and bit rate.
     */
    public static class EncoderConfig {
        final File mOutputFile;
        final int mWidth;
        final int mHeight;
        final int mBitRate;
        final EGLContext mEglContext;

        public EncoderConfig(File outputFile, int width, int height, int bitRate,
                             EGLContext sharedEglContext) {
            mOutputFile = outputFile;
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mEglContext = sharedEglContext;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " to '" + mOutputFile.toString() + "' ctxt=" + mEglContext;
        }
    }

    /**
     * Tells the video recorder to start recording.  (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will create an encoder using the provided configuration.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages.  The
     * encoder may not yet be fully configured.
     */
    public void startRecording(EncoderConfig config) {
        Log.d(TAG, "Encoder: startRecording()");
        synchronized (mReadyFence) {
//            if (mRunning) {
//                Log.w(TAG, "Encoder thread already running");
//                return;
//            }

            Log.d(TAG, "GPUImageMovieEncoder" + Thread.currentThread().toString());
            mRunning = true;
            mEncoderThread = new Thread(this, "GPUImageMovieEncoder");
            mEncoderThread.start();
            while (!mReady) {
                try {
                    Log.d(TAG, "waiting for Encoder Thread to start ...");
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     * TODO: have the encoder thread invoke a callback on the UI thread just before it shuts down
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stopRecording() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    /**
     * Returns true if recording has been started.
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    public void frameAvailable(SurfaceTexture st) {
        synchronized (mReadyFence) {
            if (!mReady) {
                Log.d(TAG,"NOT READY, Returning");
                return;
            }
        }

        float[] transform = new float[16];      // TODO - avoid alloc every frame
        st.getTransformMatrix(transform);
        long timestamp = st.getTimestamp();
        if (timestamp == 0) {
            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            //
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            Log.w(TAG, "HEY: got SurfaceTexture with timestamp of zero");
            return;
        }

        //Log.d(TAG,"BEFORE: " + Long.toString(timestamp));
        synchronized (mReadyFence) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE,
                    (int) (timestamp >> 32), (int) timestamp, transform));
        }
    }

    class AudioFrame{
        protected byte[] buf;
        protected int readBytes;
        protected long ptsu;
        public AudioFrame(byte[] buf, int readBytes,long ptsu){
            this.buf = buf;
            this.readBytes = readBytes;
            this.ptsu = ptsu;
        }
    }

    public void audioFrameAvailableSoon(){
        synchronized (mReadyFence) {
            if (!mReady) {
                Log.d(TAG, "NOT READY, Returning");
                return;
            }
            mReadyFence.notifyAll();
            mHandler.sendMessage(mHandler.obtainMessage(MSG_AUDIO_FRAME_AVAILABLE_SOON, 0, 0, null));
        }
    }

    public void audioFrameAvailable(byte[] buf, int readBytes,long ptsu){
        synchronized (mReadyFence) {
            if (!mReady) {
                Log.d(TAG, "NOT READY, Returning");
                return;
            }
            mReadyFence.notifyAll();
            AudioFrame frame = new AudioFrame(buf, readBytes, ptsu);
            mHandler.sendMessage(mHandler.obtainMessage(MSG_AUDIO_FRAME_AVAILABLE, 0, 0, frame));
        }
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     * <p>
     * TODO: do something less clumsy
     */
    public void setTextureId(int id) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
        }

    }

    public void setTextureBuffer(FloatBuffer fb) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_BUFFER, 0, 0, fb));
        }
    }

    public void setCubeBuffer(FloatBuffer fb) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_CUBE_BUFFER, 0, 0, fb));
        }

    }

    public void setFilter(GPUImageFilter filter) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_FILTER, 0, 0, filter));
        }

    }

    public void setTargetTexture(int targetTexture) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TARGET_TEXTURE, targetTexture, 0, null));
        }

    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     * <p>
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            if (VERBOSE) Log.v(TAG,  "Encoder thread exiting starting");
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<GPUImageMovieEncoder> mWeakEncoder;

        public EncoderHandler(GPUImageMovieEncoder encoder) {
            mWeakEncoder = new WeakReference<GPUImageMovieEncoder>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;
            GPUImageMovieEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);
                    //Log.d(TAG,"AFTER: " +  Long.toString(timestamp));
                    encoder.handleFrameAvailable((float[]) obj, timestamp);
                    break;
                case MSG_AUDIO_FRAME_AVAILABLE:
                    encoder.handleAudioFrameAvailable((AudioFrame) inputMessage.obj);
                    break;
                case MSG_AUDIO_FRAME_AVAILABLE_SOON:
                    encoder.handleAudioFrameAvailableSoon();
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                case MSG_SET_TARGET_TEXTURE:
                    encoder.handleSetTargetTexture(inputMessage.arg1);
                    break;
                case MSG_SET_TEXTURE_BUFFER:
                    encoder.handleSetTextureBuffer((FloatBuffer) inputMessage.obj);
                    break;
                case MSG_SET_CUBE_BUFFER:
                    encoder.handleSetCubeBuffer((FloatBuffer) inputMessage.obj);
                    break;
                case MSG_SET_FILTER:
                    encoder.handleSetFilter((GPUImageFilter) inputMessage.obj);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    /**
     * Starts recording.
     */
    private void handleStartRecording(EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
        mFrameNum = 0;
        mOutputHeight = config.mHeight;
        mOutputWidth = config.mWidth;

        prepareEncoder(config.mEglContext, config.mWidth, config.mHeight, config.mBitRate,
                config.mOutputFile);
    }

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     * @param transform The texture transform, from SurfaceTexture.
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    private void handleFrameAvailable(float[] transform, long timestampNanos) {
        if (VERBOSE) Log.d(TAG, "handleFrameAvailable tr=" + transform);
        mVideoEncoder.drainEncoder(false);
        mFilter.onDraw(mTargetTexture, mTextureId, mGLCubeBuffer, mGLTextureBuffer);
        mInputWindowSurface.setPresentationTime(timestampNanos);
        mInputWindowSurface.swapBuffers();
    }

    private void handleAudioFrameAvailableSoon(){
        if (VERBOSE) Log.d(TAG, "handleAudioFrameAvailable");
        if(mAudioEncoder == null) return;
        mAudioEncoder.setMsgAudioFrameAvailableSoon();
    }

    private void handleAudioFrameAvailable(AudioFrame frame){
        if(mAudioEncoder == null) return;
        mAudioEncoder.setMsgAudioFrameAvailable(frame.buf, frame.readBytes, frame.ptsu);
    }

    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
        mAudioEncoder.stopRecording();
        mVideoEncoder.drainEncoder(true);
        releaseEncoder();
    }

    /**
     * Sets the texture name that SurfaceTexture will use when frames are received.
     */
    private void handleSetTexture(int id) {
        //Log.d(TAG, "handleSetTexture " + id);
        mTextureId = id;
    }

    private void handleSetCubeBuffer(FloatBuffer gLCubeBuffer) {
        mGLCubeBuffer = gLCubeBuffer;
    }

    public void handleSetTargetTexture(final int targetTexture){
        mTargetTexture = targetTexture;
    }

    public void handleSetFilter(final GPUImageFilter filter) {
        final GPUImageFilter oldFilter = mFilter;
        mFilter = filter;
        if (oldFilter != null) {
            oldFilter.destroy();
        }
        mFilter.init();
        GLES20.glUseProgram(mFilter.getProgram());
        mFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
    }

    private void handleSetTextureBuffer(FloatBuffer gLTextureBuffer) {
        mGLTextureBuffer = gLTextureBuffer;
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

    }

    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate,
                                File outputFile) {

        // Here should the muxer live. Maybe?
        try {
            mMediaMuxer = new MediaMuxer(outputFile.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if(mMediaMuxer == null){
                Toast.makeText(null, "Videorecording no working", Toast.LENGTH_SHORT);
            }
            mAudioEncoder = new AudioEncoderCore(this, mMediaMuxer);
            mVideoEncoder = new VideoEncoderCore(this, width, height, bitRate, mMediaMuxer);


        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();
    }

    private void releaseEncoder() {
        mVideoEncoder.release();
        //mAudioEncoder.release();
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }
}