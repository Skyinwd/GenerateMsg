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
 * Applies sobel edge detection on the image.
 */
public class GPUImageXYDerivativeFilter extends GPUImageFilterGroup {
    public static final String XY_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +

    		" precision highp float;\n" + 
    
		" varying vec2 textureCoordinate;\n" + 
		" varying vec2 leftTextureCoordinate;\n" + 
		" varying vec2 rightTextureCoordinate;\n" + 
		    
		" varying vec2 topTextureCoordinate;\n" + 
		" varying vec2 topLeftTextureCoordinate;\n" + 
		" varying vec2 topRightTextureCoordinate;\n" + 
		    
		" varying vec2 bottomTextureCoordinate;\n" + 
		" varying vec2 bottomLeftTextureCoordinate;\n" + 
		" varying vec2 bottomRightTextureCoordinate;\n" + 
		    
		" uniform sampler2D inputImageTexture;\n" +
		    

            "void main()\n" + 
            "{\n" + 

				" float topIntensity = texture2D(inputImageTexture, topTextureCoordinate).r;\n" + 
				" float topRightIntensity = texture2D(inputImageTexture, topRightTextureCoordinate).r;\n" + 
				" float topLeftIntensity = texture2D(inputImageTexture, topLeftTextureCoordinate).r;\n" + 
				" float bottomIntensity = texture2D(inputImageTexture, bottomTextureCoordinate).r;\n" + 
				" float bottomLeftIntensity = texture2D(inputImageTexture, bottomLeftTextureCoordinate).r;\n" + 
				" float bottomRightIntensity = texture2D(inputImageTexture, bottomRightTextureCoordinate).r;\n" + 
				" float leftIntensity = texture2D(inputImageTexture, leftTextureCoordinate).r;\n" + 
				" float rightIntensity = texture2D(inputImageTexture, rightTextureCoordinate).r;\n" + 
				     
				" float verticalDerivative = -topLeftIntensity - topIntensity - topRightIntensity + bottomLeftIntensity + bottomIntensity + bottomRightIntensity;\n" + 
				" float horizontalDerivative = -bottomLeftIntensity - leftIntensity - topLeftIntensity + bottomRightIntensity + rightIntensity + topRightIntensity;\n" + 
				" verticalDerivative = verticalDerivative * 6.0;\n" +
				" horizontalDerivative = horizontalDerivative * 6.0;\n" +

				" vec4 originalTexture = texture2D(inputImageTexture, textureCoordinate);\n" +
				" vec4 derivativeTexture = vec4(horizontalDerivative * horizontalDerivative, verticalDerivative * verticalDerivative, ((verticalDerivative * horizontalDerivative) + 0.5) / 2.0, 1.0);\n" +
				" vec4 filterTex = derivativeTexture;\n" +
				" filterTex = max(derivativeTexture, originalTexture);\n" +

				" gl_FragColor = filterTex;\n" +
			"}";

    private float mLineSize;

    public GPUImageXYDerivativeFilter() {
        super();
		addFilter(new GPUImageGrayscaleFilter());
        addFilter(new GPUImage3x3TextureSamplingFilter(XY_FRAGMENT_SHADER));
        mLineSize = 1.0f;
    }
    
    @Override
    public void onInit() {
        super.onInit();
        setLineSize(mLineSize);
    }

    public void setLineSize(final float size) {
        ((GPUImage3x3TextureSamplingFilter) getFilters().get(1)).setLineSize(size);
    }

}
