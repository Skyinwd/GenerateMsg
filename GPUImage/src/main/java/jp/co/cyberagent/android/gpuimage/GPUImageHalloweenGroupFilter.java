package jp.co.cyberagent.android.gpuimage;

import android.graphics.PointF;


/**
 * Created by john_yan on 2016-10-25.
 */
public class GPUImageHalloweenGroupFilter extends GPUImageFilterGroup {
    GPUImageNoirFilter noirFilter;
    GPUImageHalloweenFilter halloweenFilter;


    public GPUImageHalloweenGroupFilter(GPUImageNoirFilter nFilter) {
        noirFilter = nFilter;
        addFilter(noirFilter);

        halloweenFilter = new GPUImageHalloweenFilter();
        addFilter(halloweenFilter);
    }

    @Override
    public void onInit() {
        super.onInit();
    }

}
