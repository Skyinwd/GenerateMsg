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

public class GPUImageLuminanceThresholdFilter extends GPUImageFilter {
    public static final String LUMINOSITY_THRESHOLD_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +

    		   " varying highp vec2 textureCoordinate;\n" +
            "  \n" +
            "  uniform sampler2D inputImageTexture;\n" +
            "  \n" +
            " uniform highp float threshold;\n" +
            " \n" +
            " const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "   highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "   highp float luminance = dot(textureColor.rgb, W);\n" +
            "   highp float thresholdResult = step(threshold, luminance);\n" +
            "   \n" +
            "   gl_FragColor = vec4(vec3(thresholdResult), textureColor.w);\n" +
            " }";

    private int mBrightnessLocation;
    
    public GPUImageLuminanceThresholdFilter() {
        super(NO_FILTER_VERTEX_SHADER, LUMINOSITY_THRESHOLD_FRAGMENT_SHADER);
    }
    
    @Override
    public void onInit() {
        super.onInit();
        mBrightnessLocation = GLES20.glGetUniformLocation(getProgram(), "threshold");  
        setThreshold(0.5f);
    }
    
    public void setThreshold(final float threshold) {
        setFloat(mBrightnessLocation, threshold);
    }
}
