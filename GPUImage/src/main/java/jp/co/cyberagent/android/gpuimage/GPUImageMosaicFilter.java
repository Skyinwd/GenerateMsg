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

/*
 *  Mosaic Filter... hope i got the ranges right!
 */
public class GPUImageMosaicFilter extends GPUImageTwoInputFilter {
    private static final String MOSAIC_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"  +

			" precision highp float;\n" +
			    
			" varying vec2 textureCoordinate;\n" +
			 
			" uniform sampler2D inputImageTexture;\n" +
			" uniform sampler2D inputImageTexture2;\n" +
			 
			" uniform vec2 inputTileSize;\n" +
			" uniform vec2 displayTileSize;\n" +
			" uniform float numTiles;\n" +
			" uniform int colorOn;\n" +

            "void main()\n" +
            "{\n" +
	            " vec2 xy = textureCoordinate;\n" +
	            " xy = xy - mod(xy, displayTileSize);\n" +
	            
	            " vec4 lumcoeff = vec4(0.299,0.587,0.114,0.0);\n" +
	            
	            " vec4 inputColor = texture2D(inputImageTexture2, xy);\n" +
	            " float lum = dot(inputColor,lumcoeff);\n" +
	            " lum = 1.0 - lum;\n" +
	            
	            " float stepsize = 1.0 / numTiles;\n" +
	            " float lumStep = (lum - mod(lum, stepsize)) / stepsize; \n" +
	         
	            " float rowStep = 1.0 / inputTileSize.x;\n" +
	            " float x = mod(lumStep, rowStep);\n" +
	            " float y = floor(lumStep / rowStep);\n" +
	            
	            " vec2 startCoord = vec2(float(x) *  inputTileSize.x, float(y) * inputTileSize.y);\n" +
	            " vec2 finalCoord = startCoord + ((textureCoordinate - xy) * (inputTileSize / displayTileSize));\n" +
	            
	            " vec4 color = texture2D(inputImageTexture, finalCoord);\n" +
	            " if (colorOn == 1) {\n" +
	                " color = color * inputColor;\n" +
	            " }\n" +
	            " gl_FragColor = color;\n" + 
            "}";
    
    public int inputTileSizeUniform;
    public int displayTileSizeUniform;
    public int numTilesUniform;
    public int colorOnUniform;

    public GPUImageMosaicFilter() {
    		super(NO_FILTER_VERTEX_SHADER, MOSAIC_FRAGMENT_SHADER);
    }

    @Override
    public void onInit() {
        super.onInit();
        
        inputTileSizeUniform = GLES20.glGetAttribLocation(getProgram(), "inputTileSize");
        displayTileSizeUniform = GLES20.glGetAttribLocation(getProgram(), "displayTileSize");
        numTilesUniform = GLES20.glGetAttribLocation(getProgram(), "numTiles");
        colorOnUniform = GLES20.glGetAttribLocation(getProgram(), "colorOn");

        setInputTileSize(0.125f);
        setDisplayTileSize(0.025f);
        setNumTiles(64.0f);
        setTileSet();
        setColorOn();
    }
    
    public void setInputTileSize(final float inputTileSize) {
		float newinputTileSize = inputTileSize;
    	if (inputTileSize > 1.0) {
    		newinputTileSize = 1.0f;
			 }
			 if (inputTileSize < 0.0) {
				 newinputTileSize = 0.0f;
			 }
    	
    	float[] sizeArray = {newinputTileSize, newinputTileSize};
		setFloatVec2(inputTileSizeUniform, sizeArray);
    }
    
    public void setDisplayTileSize(final float displayTileSize) {
		float newinputTileSize = displayTileSize;
    	if (displayTileSize > 1.0) {
    		newinputTileSize = 1.0f;
			 }
			 if (displayTileSize < 0.0) {
				 newinputTileSize = 0.0f;
			 }
    		float[] sizeArray = {newinputTileSize, newinputTileSize};
    		setFloatVec2(displayTileSizeUniform, sizeArray);
    }
    
    public void setNumTiles(final float numTiles) {
		setFloat(numTilesUniform, numTiles);
    }
    
    public void setTileSet() {
    		// pushed into GPUImageFilterTools as the Bitmap
    }
    
    public void setColorOn() {
    		GLES20.glUniform1i(colorOnUniform, 1);
    }
 
}
