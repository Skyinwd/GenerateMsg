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

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jp.co.cyberagent.android.gpuimage.grafika.gles.GlUtil;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GPUImageFilter {
    private static final String TAG = "GPUImageFilter";

    public static final String NO_FILTER_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    public static final String NO_FILTER_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    private final BlockingQueue<Runnable> mRunOnDraw;
    private final String mVertexShader;
    private String mFragmentShader;
    private String tempFragmentShader;
    protected int mGLProgId;
    protected int mGLAttribPosition;
    protected int mGLUniformTexture;
    protected int mGLAttribTextureCoordinate;
    protected int mOutputWidth;
    protected int mOutputHeight;
    protected int inputRotation;
    private boolean mInputIsVideo = false;
    private boolean mIsInitialized;

    //TODO: below method updates the filter by time. It is needed for some filters.
    protected Timer timer = new Timer();

    public GPUImageFilter() {
        this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
    }

    public GPUImageFilter(final String vertexShader, final String fragmentShader) {
        mRunOnDraw = new LinkedBlockingQueue<Runnable>();
        mVertexShader = vertexShader;
        tempFragmentShader = fragmentShader;
    }

    public final void init() {
        if(mInputIsVideo){
            mFragmentShader = tempFragmentShader.replace("sampler2D inputImageTexture", "samplerExternalOES inputImageTexture");
        }
        else{
            mFragmentShader = tempFragmentShader;
        }
        onInit();
        mIsInitialized = true;
        onInitialized();
    }

    public void onInit() {
        mGLProgId = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId, "inputTextureCoordinate");
        mIsInitialized = true;
    }

    public void onInitialized() {
    }

    public final void destroy() {
        mIsInitialized = false;
        GLES20.glDeleteProgram(mGLProgId);
        onDestroy();
    }

    public void onDestroy() {
    }

    public void onOutputSizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }

    public Point rotatedSize(Point sizeToRotate, int textureIndex) {
    		Point rotatedSize = sizeToRotate;
    		 if (Rotation.GPUImageRotationSwapsWidthAndHeight(Rotation.fromInt(inputRotation)))
    	        {
    	            rotatedSize.x = sizeToRotate.y;
    	            rotatedSize.y = sizeToRotate.x;
    	        }
    	        return rotatedSize;
    }

    public void onDraw(final int targetTexture, final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {

        GLES20.glUseProgram(mGLProgId);
        GlUtil.checkGlError("glUseProgram");
        runPendingOnDrawTasks(); // LinkedList Error
        if (!mIsInitialized) {
            return;
        }
        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GlUtil.checkGlError("glVertexAttribPointer mGLAttribPosition");
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GlUtil.checkGlError("glEnableVertexAttribArray mGLAttribPosition");
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GlUtil.checkGlError("mGLAttribTextureCoordinate maTextureHandle");
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GlUtil.checkGlError("glEnableVertexAttribArray maTextureHandle");
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(targetTexture, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GlUtil.checkGlError("glDrawArrays");
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(targetTexture, 0);
    }

    protected void onDrawArraysPre() {}

    //here 's a solution I dont understand http://stackoverflow.com/questions/17705658/nosuchelementexception-even-after-check
    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            try {
				mRunOnDraw.take().run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public int getOutputWidth() {
        return mOutputWidth;
    }

    public int getOutputHeight() {
        return mOutputHeight;
    }

    public int getProgram() {
        return mGLProgId;
    }

    public int getAttribPosition() {
        return mGLAttribPosition;
    }

    public int getAttribTextureCoordinate() {
        return mGLAttribTextureCoordinate;
    }

    public int getUniformTexture() {
        return mGLUniformTexture;
    }

    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES20.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }


    public void setInputRotation(int inputRotation) {
        this.inputRotation = inputRotation;
    }


    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            try {
				mRunOnDraw.put(runnable);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
    }

    public static String loadShader(String file, Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream ims = assetManager.open(file);

            String re = convertStreamToString(ims);
            ims.close();
            return re;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public boolean ismInputIsVideo() {
        return mInputIsVideo;
    }

    public void setmInputIsVideo(boolean mInputIsVideo) {
        this.mInputIsVideo = mInputIsVideo;
    }
}
