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

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.opengl.GLES20;

/**
 * Applies a Buffer (to be used with Low/Hi Pass)
 */
public class GPUImageBufferFilter extends GPUImageFilter {
//    public static final String BUFFER_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
//
//            "precision mediump float;\n" +
//
//            "varying vec2 textureCoordinate;\n" +
//            "varying vec2 leftTextureCoordinate;\n" +
//            "varying vec2 rightTextureCoordinate;\n" +
//
//            "varying vec2 topTextureCoordinate;\n" +
//            "varying vec2 topLeftTextureCoordinate;\n" +
//            "varying vec2 topRightTextureCoordinate;\n" +
//
//            "varying vec2 bottomTextureCoordinate;\n" +
//            "varying vec2 bottomLeftTextureCoordinate;\n" +
//            "varying vec2 bottomRightTextureCoordinate;\n" +
//
//            "uniform sampler2D inputImageTexture;\n" +
//
//            "void main()\n" +
//            "{\n" +
//            "    float bottomLeftIntensity = texture2D(inputImageTexture, bottomLeftTextureCoordinate).r;\n" +
//            "    float topRightIntensity = texture2D(inputImageTexture, topRightTextureCoordinate).r;\n" +
//            "    float topLeftIntensity = texture2D(inputImageTexture, topLeftTextureCoordinate).r;\n" +
//            "    float bottomRightIntensity = texture2D(inputImageTexture, bottomRightTextureCoordinate).r;\n" +
//            "    float leftIntensity = texture2D(inputImageTexture, leftTextureCoordinate).r;\n" +
//            "    float rightIntensity = texture2D(inputImageTexture, rightTextureCoordinate).r;\n" +
//            "    float bottomIntensity = texture2D(inputImageTexture, bottomTextureCoordinate).r;\n" +
//            "    float topIntensity = texture2D(inputImageTexture, topTextureCoordinate).r;\n" +
//            "    float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
//            "    float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +
//
//            "    float mag = length(vec2(h, v));\n" +
//
//            "    gl_FragColor = vec4(vec3(mag), 1.0);\n" +
//            "}";

    private int bufferSize;
    private int generateTexture;
    private ArrayList<Integer> bufferedTextures = new ArrayList();
    
    public GPUImageBufferFilter() {
        super();
        
        // self initializeOutputTextureIfNeeded ?? (cant find in hierarchy)
        bufferedTextures.add(1); // add result of above
        bufferSize = 1;

    }
    
    public void newFrameReadyAtTime(double frameTime, int textureIndex) {
    	
    }
    
    public void renderToTextureWithVertices( final float vertices, final float textureCoordinates, final int sourceTexture ) {
    	
    }
    
    public int textureForOutput() {
    		return bufferedTextures.get(0);
    }
    
    public int generateTexture() {
    		int newTextureName = 0;
    	
    		// 
    		
    		return newTextureName;
    }
    
    public void removeTexture(int textureToRemove) {
    		//glDeleteTextures(1, &textureToRemove);
    		int[] texs = convertIntegers(bufferedTextures);
    		IntBuffer buffer = null; 
    		buffer.allocate(texs.length);
    		buffer.put(texs);
    		GLES20.glDeleteTextures(1, buffer);
    }

    public void removeTexture() {
        int[] texs = convertIntegers(bufferedTextures);
        IntBuffer buffer = null;
        buffer.allocate(texs.length);
        buffer.put(texs);
        GLES20.glDeleteTextures(1, buffer);
    }
    
    public static int[] convertIntegers(List<Integer> integers) {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = iterator.next().intValue();
        }
        return ret;
    }
    
    public void setBufferSize(int newValue) {
    	
    		if ( (newValue == bufferSize) || (newValue < 1) ) {
    	        return;
    	    }
    		
    	    if (newValue > bufferSize)  {
    	        int texturesToAdd = newValue - bufferSize;
    	        for (int currentTextureIndex = 0; currentTextureIndex < texturesToAdd; currentTextureIndex++) {
    	            bufferedTextures.add(generateTexture()); 
    	        }
    	    }
    	    else {
    	        int texturesToRemove = bufferSize - newValue;
    	        for (int currentTextureIndex = 0; currentTextureIndex < texturesToRemove; currentTextureIndex++)  {
    	            int lastTextureName = (Integer) bufferedTextures.get(bufferedTextures.size() - 1);
    	            bufferedTextures.remove(bufferedTextures.size() - 1);
    	            removeTexture(lastTextureName);
    	        }
    	    }

    	  bufferSize = newValue;
    	
    }


}