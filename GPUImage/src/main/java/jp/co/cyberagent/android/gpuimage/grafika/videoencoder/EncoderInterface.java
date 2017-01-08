package jp.co.cyberagent.android.gpuimage.grafika.videoencoder;

/**
 * Created by martin on 15-03-30.
 */
public interface EncoderInterface {
    public void startMediaMuxer();
    public void stopMediaMuxer();
    public void prepared();
}