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
 * Applies a pointillism effect to the image.
 */
public class GPUImageHalftoneFilter extends GPUImageFilter {
    public static final String HALFTONE_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +

	    		"varying highp vec2 textureCoordinate;\n" +
			"uniform sampler2D inputImageTexture;\n" +
		
			"uniform highp float fractionalWidthOfPixel;\n" +
			"uniform highp float aspectRatio;\n" +
			"uniform highp float dotScaling;\n" +
			
			"const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +

            "void main()\n" +
            "{\n" +
			" highp vec2 sampleDivisor = vec2(fractionalWidthOfPixel, fractionalWidthOfPixel / aspectRatio);\n" +
            
			" highp vec2 samplePos = textureCoordinate - mod(textureCoordinate, sampleDivisor) + 0.5 * sampleDivisor;\n" +
			" highp vec2 textureCoordinateToUse = vec2(textureCoordinate.x, (textureCoordinate.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
			" highp vec2 adjustedSamplePos = vec2(samplePos.x, (samplePos.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
			" highp float distanceFromSamplePoint = distance(adjustedSamplePos, textureCoordinateToUse);\n" +
			
			" lowp vec3 sampledColor = texture2D(inputImageTexture, samplePos).rgb;\n" +
			" highp float dotScaling = 1.0 - dot(sampledColor, W);\n" +
			
			" lowp float checkForPresenceWithinDot = 1.0 - step(distanceFromSamplePoint, (fractionalWidthOfPixel * 0.5) * dotScaling);\n" +
			
			" gl_FragColor = vec4(vec3(checkForPresenceWithinDot), 1.0);\n" +
            "}";


    private int dotScalingUniform;
    private int fractionalWidthOfAPixelUniform;
    private int aspectRatioUniform;
    private float aspectRatio;
    private float mDot;
    private float mWidth;
    
    public GPUImageHalftoneFilter() {
        super(NO_FILTER_VERTEX_SHADER, HALFTONE_FRAGMENT_SHADER);
        mDot = 0.5f;
        mWidth = 0.05f;
        aspectRatio = 0.8f;
    }

    @Override
    public void onInit() {
        super.onInit();

        fractionalWidthOfAPixelUniform = GLES20.glGetUniformLocation(getProgram(), "fractionalWidthOfPixel");
        aspectRatioUniform = GLES20.glGetUniformLocation(getProgram(), "aspectRatio");
        dotScalingUniform = GLES20.glGetUniformLocation(getProgram(), "dotScaling");
        
        setFractionalWidthOfAPixel(mWidth);              
        setDotScaling(mDot);  
        adjustAspectRatio();
    }

    public void setDotScaling(final float dot) {
		mDot = dot;
		setFloat(dotScalingUniform, mDot);    
	}
    
    public void setAspectRatio(final float ratio) {
		setFloat(aspectRatioUniform, ratio);
    }


    public void adjustAspectRatio(){
        if (Rotation.GPUImageRotationSwapsWidthAndHeight(Rotation.fromInt(inputRotation))){
            aspectRatio = 1.4f;
            setAspectRatio(aspectRatio);
        }
        else{
            aspectRatio = 0.8f;
            setAspectRatio(aspectRatio);
        }
    }

	public void setFractionalWidthOfAPixel(final float fraction) {
        adjustAspectRatio();
		mWidth = fraction;	
		setFloat(fractionalWidthOfAPixelUniform, mWidth); 	
	}
    
}
