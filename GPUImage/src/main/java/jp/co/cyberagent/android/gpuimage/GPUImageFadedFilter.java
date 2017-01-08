package jp.co.cyberagent.android.gpuimage;

import android.graphics.PointF;
import android.opengl.GLES20;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;

/**
 * Created by john_yan on 2016-09-26.
 */
public class GPUImageFadedFilter extends GPUImageFilterGroup {

    GPUImageTwoInputFilter warmFilter;
    GPUImageBulgeDistortionFilter bulgeFilter;
    GPUImageBrightnessFilter brightnessFilter;
    GPUImageVignetteFilter vignetteFilter;

    public GPUImageFadedFilter(GPUImageTwoInputFilter wFilter) {
        warmFilter = wFilter;
        addFilter(warmFilter);
        PointF center = new PointF(.5f, .5f);

        vignetteFilter = new GPUImageVignetteFilter();
        addFilter(vignetteFilter);

        bulgeFilter = new GPUImageBulgeDistortionFilter(1f, .2f, center);
        addFilter(bulgeFilter);

        brightnessFilter = new GPUImageBrightnessFilter(.2f);
        addFilter(brightnessFilter);

        TimerTask updateWithTIme = new TimerTask () {
            @Override
            public void run () {
                randomBrightness();
            }
        };

        // schedule the task to run starting now and then every hour...
        timer.schedule(updateWithTIme, 0l, 100);
    }

    @Override
    public void onInit() {
        super.onInit();
        PointF center = new PointF(.5f, .5f);

        vignetteFilter.setVignetteEnd(.8f);
        vignetteFilter.setVignetteCenter(center);
    }

    private void randomBrightness() {
        Random ran = new Random();
        float r = ran.nextFloat()*.1f-.1f;
        brightnessFilter.setBrightness(r);

    }



    public void setThreshold(float newThreshold) {
        bulgeFilter.setRadius(newThreshold);
    }

}
