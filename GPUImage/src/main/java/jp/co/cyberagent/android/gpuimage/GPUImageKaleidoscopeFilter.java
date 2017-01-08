/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;

/**
 * Kaleidoscope Yo.
 */
public class GPUImageKaleidoscopeFilter extends GPUImageFilter {
    public static final String KALEIDOSCOPE_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +

            "varying lowp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform lowp vec2 uScreenResolution;\n" +
            "uniform lowp float fractionalWidthOfPixel;\n" +
            "uniform lowp float aspectRatio;\n" +


            "void main()\n" +
            "{\n" +
                "highp vec2 uv = -1.0 + 2.0 * gl_FragCoord.xy / vec2(1.0, aspectRatio);\n" +
                "highp float a = (fractionalWidthOfPixel * 0.01);\n" +
                "highp vec4 color = vec4(0.0);\n" +

                 "for(lowp float i = 1.0; i < 3.0; i += 1.0) {\n" +
                    "uv = vec2(sin(a)*uv.y - cos(a)*uv.x, sin(a)*uv.x + cos(a)*uv.y);\n" +
                    "uv = vec2(abs(fract(uv/2.0) - 0.5)*2.0);\n" +
                    "a += i;\n" +
                    "a /= i;\n" +
                 "}" +
                " gl_FragColor = texture2D(inputImageTexture, vec2(abs(fract(uv) - 0.5)*2.0));\n" +
            "}";


    private int fractionalWidthOfAPixelUniform;
    private int aspectRatioUniform;
    private float aspectRatio;
    private float mWidth;

//    private int mImageWidthFactorLocation;
//    private int mImageHeightFactorLocation;
//    private float mPixelLocation;



    public GPUImageKaleidoscopeFilter() {
        super(NO_FILTER_VERTEX_SHADER, KALEIDOSCOPE_FRAGMENT_SHADER);
        mWidth = 0.05f;
        aspectRatio = 0.8f;
//        mImageWidthFactorLocation = GLES20.glGetUniformLocation(getProgram(), "imageWidthFactor");
//        mImageHeightFactorLocation = GLES20.glGetUniformLocation(getProgram(), "imageHeightFactor");
//        mPixelLocation = GLES20.glGetUniformLocation(getProgram(), "pixel");
//        onOutputSizeChanged(getOutputWidth(), getOutputHeight());
    }

    @Override
    public void onInit() {
        super.onInit();
        fractionalWidthOfAPixelUniform = GLES20.glGetUniformLocation(getProgram(), "fractionalWidthOfPixel");
        aspectRatioUniform = GLES20.glGetUniformLocation(getProgram(), "aspectRatio");
//        screenWidth = GLES20.glGetUniformLocation(getProgram(), "screenWidth");
//        screenHeight = GLES20.glGetUniformLocation(getProgram(), "screenHeight");

//        setFloat(screenWidth, 1f);
//        setFloat(screenHeight, 1f);

        setFractionalWidthOfAPixel(mWidth);
    }

    //Added here:
    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);

//        setFloat(screenWidth, width);
//        setFloat(screenHeight, height);
//        adjustAspectRatio();
//        aspectRatio =  ((float)width/height);
        aspectRatio = 1f;

        setFloat(aspectRatioUniform, aspectRatio);

        System.out.println("width: " + width);
        System.out.println("height: " + height);
        System.out.println("setAspectRatio: " + aspectRatio);
    }

    public void setFractionalWidthOfAPixel(final float fraction) {
        mWidth = fraction;
        setFloat(fractionalWidthOfAPixelUniform, mWidth);
//        System.out.println("fractionalWidthOfAPixelUniform: " + mWidth);

    }


}
