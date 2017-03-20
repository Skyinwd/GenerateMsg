package com.generate.generatemsg;

import android.opengl.GLES20;
import android.util.Log;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.Rotation;
/**
 * Applies a polkadot effect to the image.
 */
public class PolkaDotFilter extends GPUImageFilter {
    public static final String POLKA_DOT_FRAGMENT_SHADER = "" +
            " precision highp float;\n" +

            " varying highp vec2 textureCoordinate;\n" +

            " uniform sampler2D inputImageTexture;\n" +

            " uniform highp float fractionalWidthOfPixel;\n" +
            " uniform highp float aspectRatio;\n" +
            " uniform highp float dotScaling;\n" +

            "void main()\n" +
            "{\n" +
            " highp vec2 sampleDivisor = vec2(fractionalWidthOfPixel, fractionalWidthOfPixel / aspectRatio);\n" +

            " highp vec2 samplePos = textureCoordinate - mod(textureCoordinate, sampleDivisor) + 0.5 * sampleDivisor;\n" +
            " highp vec2 textureCoordinateToUse = vec2(textureCoordinate.x, (textureCoordinate.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
            " highp vec2 adjustedSamplePos = vec2(samplePos.x, (samplePos.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
            " highp float distanceFromSamplePoint = distance(adjustedSamplePos, textureCoordinateToUse);\n" +
            " lowp float checkForPresenceWithinDot = step(distanceFromSamplePoint, (fractionalWidthOfPixel * 0.5) * dotScaling);\n" +

            " lowp vec4 inputColor = texture2D(inputImageTexture, samplePos);\n" +

            " gl_FragColor = vec4(inputColor.rgb * checkForPresenceWithinDot, inputColor.a);\n" +
            "}";


    private int dotScalingUniform;
    private int fractionalWidthOfAPixelUniform;
    private int aspectRatioUniform;
    private float mDot;
    private float mWidth;
    private float aspectRatio;
    private float mAspectRatio;

    public PolkaDotFilter() {
        super(NO_FILTER_VERTEX_SHADER, POLKA_DOT_FRAGMENT_SHADER);

        mDot = 0.75f; // Dot's size range 0.7-0.8
        mWidth = (float)Math.pow(mDot,7)/7;
        mAspectRatio = 0.58f; // if it's 1 the result is correct but preview is wrong
        Log.d("YUE", " PolkaDotFilter ? ");

    }

    @Override
    public void onInit() {
        super.onInit();

        dotScalingUniform = GLES20.glGetUniformLocation(getProgram(), "dotScaling");
        fractionalWidthOfAPixelUniform = GLES20.glGetUniformLocation(getProgram(), "fractionalWidthOfPixel");
        aspectRatioUniform = GLES20.glGetUniformLocation(getProgram(), "aspectRatio");

        setDotScaling(mDot);
        Log.v("GENERATE_GPUIMAGE", "Width is  " + mOutputWidth);
        Log.v("GENERATE_GPUIMAGE", "Height is  " + mOutputHeight);
    }

    public void setDotScaling(final float scale) {

        mDot = scale;
        setFractionalWidthOfAPixel(mDot);
        setFloat(dotScalingUniform, mDot);
        adjustAspectRatio(mAspectRatio);
    }

    public void setAspectRatio(final float ratio) {
        setFloat(aspectRatioUniform, ratio);
    }

    public void adjustAspectRatio(float value){
        aspectRatio = value;
        setAspectRatio(aspectRatio);
    }

    public void setFractionalWidthOfAPixel(final float dotScale) {
        adjustAspectRatio(mAspectRatio);
        mWidth = (float)Math.pow(dotScale,7)/7;
        setFloat(fractionalWidthOfAPixelUniform, mWidth);
    }

}
