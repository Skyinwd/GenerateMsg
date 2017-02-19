package com.generate.generatemsg;

import android.util.Log;

import java.util.Random;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageHueFilter;

/**
 * Created by mingxin_yue on 2017-02-19.
 */
public class RandomFilter {
    private boolean haveFactor = false; // not sure if I need it right now
    private GPUImageFilter filter;
    private String[] filterNameList = {"HUE","GRAY"};

    public void RandomFilter(String name){
        switch (randomFilterName()){
            case "HUE":
                filter = new GPUImageHueFilter();
                //filter.setHue(filterFactor);
            case "GRAY":
                filter = new GPUImageGrayscaleFilter();
        }
    }

    private String randomFilterName(){
        float seed = new Random().nextFloat();
        int seedInt = (int)seed*(filterNameList.length-1);
        Log.d("YUE", "filter name is ::: " + filterNameList[seedInt]);

        return filterNameList[seedInt];
    }
}
