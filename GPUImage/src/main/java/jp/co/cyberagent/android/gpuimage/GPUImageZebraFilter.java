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
 * SILO
 */
public class GPUImageZebraFilter extends GPUImageFilter {
    public static final String ZEBRA_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +

            " uniform sampler2D inputImageTexture;\n" +
            " varying highp vec2 textureCoordinate;\n" +

            " uniform highp float fractionalWidthOfPixel;\n" +
            " uniform highp float aspectRatio;\n" +

            "void main()\n" +
            "{\n" +
       
				" highp float phase= fractionalWidthOfPixel*2.5;//sin(iGlobalTime);\n" +
				" highp float levels= 8.;\n" +
				" highp vec2 xy = textureCoordinate.xy / vec2(aspectRatio, 1);\n" +
				" highp vec4 tx = texture2D(inputImageTexture, xy);\n" +
		
		        	" highp vec4 x = tx;\n" +
		
				" x = mod(x + phase, 1.);\n" +
				" x = floor(x*levels);\n" +
				" x = mod(x,2.);\n" +
						
				" gl_FragColor= vec4(vec3(x), tx.a);\n" +
            
            "}";

    private int fractionalWidthOfAPixelUniform;
    private int aspectRatioUniform;

    public GPUImageZebraFilter() {
    		super(NO_FILTER_VERTEX_SHADER, ZEBRA_FRAGMENT_SHADER);
    }

    @Override
    public void onInit() {
        super.onInit();
        fractionalWidthOfAPixelUniform = GLES20.glGetUniformLocation(getProgram(), "fractionalWidthOfPixel");
        aspectRatioUniform = GLES20.glGetUniformLocation(getProgram(), "aspectRatio");

        //setFractionalWidthOfAPixel(0.01f);
        setAspectRatio(1.0f);
    }


    public void setFractionalWidthOfAPixel(final float pixel) {
        setFloat(fractionalWidthOfAPixelUniform, pixel);
    }
    
    public void setAspectRatio(final float aspectRatio) {
        setFloat(aspectRatioUniform, aspectRatio);
    }
}
