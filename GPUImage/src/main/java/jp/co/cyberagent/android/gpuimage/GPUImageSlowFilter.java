package jp.co.cyberagent.android.gpuimage;

/**
 * Created by john_yan on 2016-09-23.
 * does not work. Need full implementation of buffer filter
 */
public class GPUImageSlowFilter extends GPUImageFilterGroup {

    private float filterStrength;
    private GPUImageMixBlendFilter dissolveBlendFilter;
    private GPUImageBufferFilter bufferFilter;

    public GPUImageSlowFilter() {
        super();
        dissolveBlendFilter = new GPUImageDissolveBlendFilter();
        bufferFilter = new GPUImageBufferFilter();
        addFilter(dissolveBlendFilter);
        addFilter(bufferFilter);

        //set the mix to 1 maybe
        dissolveBlendFilter.setMix(.5f);

        filterStrength = .5f;
    }

    @Override
    public void onInit() {
        super.onInit();
    }
}
