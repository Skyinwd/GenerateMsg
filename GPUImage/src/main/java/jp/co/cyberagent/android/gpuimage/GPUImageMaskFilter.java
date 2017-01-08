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

/*
 * Taken From https://github.com/joplaete/android-gpuimage/blob/36c7510bf07ebfaeae75abd607efe29699fe8bb7/library/src/jp/co/cyberagent/android/gpuimage/GPUImageMaskFilter.java
 */
package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;

public class GPUImageMaskFilter extends GPUImageTwoInputFilter {
	public static final String MASK_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +

		 " varying highp vec2 textureCoordinate; \n" +
		 " varying highp vec2 textureCoordinate2; \n" +
		 " varying highp vec2 textureCoordinate3; \n" +
		 
		 " uniform sampler2D inputImageTexture; \n" +
		 " uniform sampler2D inputImageTexture2; \n" +
		 " uniform sampler2D inputImageTexture3; \n" +
		 
		" uniform highp float time; \n" +
		" uniform highp float threshold; \n" +
            
            
            " void main()\n" +
            " {\n" +
			     " highp vec4 colour2 = texture2D(inputImageTexture, textureCoordinate); \n" +
			     " highp vec4 colour1 = texture2D(inputImageTexture2, textureCoordinate); \n" +
			     " highp vec4 colour0 = texture2D(inputImageTexture3, textureCoordinate); \n" +
			     
			     " highp vec3 q = vec3(textureCoordinate, time); \n" +
			
			     " if((colour0.r+colour0.g+colour0.b)/3.0 < threshold) { \n" +
			         " gl_FragColor = colour1; \n" +
			     " } \n" +
			     " else { \n" +
			         " gl_FragColor = colour2; \n" +
			     " } \n" +            
            " }";

	private int timeUniform;
	private int thresholdUniform;
	private float mThreshold;
	
    public GPUImageMaskFilter() {
        super(MASK_FRAGMENT_SHADER);
    }
    
    @Override
    public void onInit() {
        super.onInit();

		System.out.println("onIniting");
        timeUniform = GLES20.glGetUniformLocation(getProgram(), "time");
        thresholdUniform = GLES20.glGetUniformLocation(getProgram(), "threshold");
        
        setThreshold(mThreshold);

    }
    
    public void setTime(float newTime) {
    		setFloat(timeUniform, newTime);
    }
    
    public void setThreshold(float newThreshold) {
    	mThreshold = newThreshold;
		setFloat(thresholdUniform, mThreshold);
    }
    
}