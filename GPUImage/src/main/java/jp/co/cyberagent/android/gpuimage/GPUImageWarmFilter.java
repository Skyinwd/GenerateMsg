package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;

/**
 * Created by john_yan on 2016-09-27.
 */
public class GPUImageWarmFilter extends GPUImageFilterGroup {


//    private float mThreshold;
//    private int thresholdUniform;
    private GPUImageLevelsFilter levelsFilter;

    public GPUImageWarmFilter(GPUImageTwoInputFilter lookupFilter) {
        addFilter(lookupFilter);
        levelsFilter = new GPUImageLevelsFilter();
        addFilter(levelsFilter);
    }

    @Override
    public void onInit() {
        super.onInit();
    }

    public void setThreshold(float newThreshold) {
        levelsFilter.setMin(newThreshold, 1f, 1f);
    }
}
