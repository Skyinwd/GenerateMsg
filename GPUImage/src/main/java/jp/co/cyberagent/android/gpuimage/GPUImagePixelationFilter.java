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
 * Applies a pixelation effect to the image.
 */
public class GPUImagePixelationFilter extends GPUImageFilter {
    public static final String PIXELATION_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +

            "varying vec2 textureCoordinate;\n" +

            "uniform float imageWidthFactor;\n" +
            "uniform float imageHeightFactor;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform float pixel;\n" +

            "void main()\n" +
            "{\n" +
            "  vec2 uv  = textureCoordinate.xy;\n" +
            "  float dx = pixel * imageWidthFactor;\n" +
            "  float dy = pixel * imageHeightFactor;\n" +
            "  vec2 coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));\n" +
            "  vec3 tc = texture2D(inputImageTexture, coord).xyz;\n" +
            "  gl_FragColor = vec4(tc, 1.0);\n" +
            "}";

    private int mImageWidthFactorLocation;
    private int mImageHeightFactorLocation;
    private float mPixel;
    private int mPixelLocation;
    private int mWidth;
    private int mHeight;
    
    public GPUImagePixelationFilter() {
        super(NO_FILTER_VERTEX_SHADER, PIXELATION_FRAGMENT_SHADER);
        mPixel = 1.0f;
    }

    @Override
    public void onInit() {
        super.onInit();
        mImageWidthFactorLocation = GLES20.glGetUniformLocation(getProgram(), "imageWidthFactor");
        mImageHeightFactorLocation = GLES20.glGetUniformLocation(getProgram(), "imageHeightFactor");
        mPixelLocation = GLES20.glGetUniformLocation(getProgram(), "pixel");
        onOutputSizeChanged(getOutputWidth(), getOutputHeight());
        setPixel(mPixel);
    }

    @Override
    public void onOutputSizeChanged(final int width, final int height) {

            super.onOutputSizeChanged(width, height);
            mWidth = width;
            mHeight = height;
            adjustAspectRatio();

    }

    public void adjustAspectRatio(){
        if (Rotation.GPUImageRotationSwapsWidthAndHeight(Rotation.fromInt(inputRotation))){
            setFloat(mImageWidthFactorLocation, 1.0f / mWidth);
            setFloat(mImageHeightFactorLocation, 1.0f / mHeight);
        }
        else{
            setFloat(mImageWidthFactorLocation, .5f / mWidth);
            setFloat(mImageHeightFactorLocation, 1.0f / mHeight);
        }
    }

    public void setPixel(final float pixel) {
    		mPixel = pixel;
    		setFloat(mPixelLocation, mPixel);
    }
}
