package jp.co.cyberagent.android.gpuimage;

/**
 * Created by john_yan on 2016-10-17.
 */
public class GPUImageNoirFilter extends GPUImageFilterGroup {

    private GPUImageLevelsFilter levelsFilter;

    public GPUImageNoirFilter(GPUImageTwoInputFilter lookupFilter) {
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
