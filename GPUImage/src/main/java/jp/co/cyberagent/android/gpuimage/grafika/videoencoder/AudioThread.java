package jp.co.cyberagent.android.gpuimage.grafika.videoencoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by martin on 15-04-06.
 */
public class AudioThread extends Thread {
    private static final String TAG = "AudioThread";
    private static final boolean DEBUG = true;
    private static final int SAMPLE_RATE = 44100;
    private AudioRecord mAudioRecord;

    public void setmListener(AudioThreadListener mListener) {
        this.mListener = mListener;
    }

    private AudioThreadListener mListener;

    public double getmVolume() {
        return mVolume;
    }

    private double mVolume = 0;

    public boolean ismIsRecording() {
        return mIsRecording;
    }

    public void setmIsRecording(boolean mIsRecording) {
        this.mIsRecording = mIsRecording;
    }

    private boolean mIsRecording = false;


    public AudioThread() {
        super();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            final int buf_sz = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 4;
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buf_sz);

            mAudioRecord.startRecording();

            try {
                if (mIsRecording) {
                    if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
                    final byte[] buf = new byte[buf_sz];
                    int readBytes;
                    if (mAudioRecord.getState() != AudioRecord.RECORDSTATE_RECORDING) {
                        mAudioRecord.startRecording();
                    }
                    try {
                        while (mIsRecording) {
                            // read audio data from internal mic
                            readBytes = mAudioRecord.read(buf, 0, buf_sz);
                            if (readBytes > 0) {
                                // set audio data to encoder
                                //encode(buf, readBytes, getPTSUs());

                                long ptsu = getPTSUs();
                                mListener.onAudioFrameAvailable(buf, readBytes, ptsu);
                                mListener.onAudioFrameAvailableSoon();
                                Log.d(TAG, currentThread().toString());
                                prevOutputPTSUs = ptsu;

                            // Get Amplitude
                            //int vol = mic.getPercentangeAmplitude();
                            double sum = 0;
                            for (int i = 0; i < buf_sz; i++) {
                                sum += buf[i] * buf[i];
                            }
                            if (buf_sz > 0) {
                                mVolume = sum / buf_sz;
                            }
                            }
                        }
                        mListener.onAudioFrameAvailableSoon();

                    } finally {
                        //We don't want to stop it here, needed for audioreactivity
                        mAudioRecord.stop();
                    }
                }
            } finally {
                mAudioRecord.release();
                mListener = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "AudioThread#run", e);
        }
        if (DEBUG) Log.v(TAG, "AudioThread:finished");
    }

    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;


    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }

    public interface AudioThreadListener {
        void onAudioFrameAvailable(byte[] buf, int readBytes, long presentationTime);
        void onAudioFrameAvailableSoon();

    }

}