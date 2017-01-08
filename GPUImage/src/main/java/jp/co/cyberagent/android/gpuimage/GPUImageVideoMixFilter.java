package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by john_yan on 2016-10-14.
 */
public class GPUImageVideoMixFilter extends GPUImageFilter {


    public static final String VIDEOMIX_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +

            "precision highp float;\n" +

            "varying lowp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +

            "uniform vec2 inputTileSize;\n" +
            "uniform vec2 displayTileSize;\n" +
            "uniform float numTiles;\n" +
            "uniform int colorOn;\n" +

            "void main()\n" +
            "{\n" +

            "vec2 xy = textureCoordinate;\n" +
            "xy = xy - mod(xy, displayTileSize);\n" +

            "vec4 lumcoeff = vec4(0.299,0.587,0.114,0.0);\n" +
            "vec4 inputColor = texture2D(inputImageTexture, xy);\n" +
            "float lum = dot(inputColor,lumcoeff);\n" +
            "lum = 1.0 - lum;\n" +

            "float stepsize = 1.0 / numTiles;\n" +
            "float lumStep = (lum - mod(lum, stepsize)) / stepsize;\n" +

            "float rowStep = 1.0 / inputTileSize.x;\n" +
            "float x = mod(lumStep, rowStep);\n" +
            "float y = floor(lumStep / rowStep);\n" +


            "vec2 startCoord = vec2(float(x) *  inputTileSize.x, float(y) * inputTileSize.x);\n" +
            "vec2 finalCoord = startCoord + ((textureCoordinate - xy) * (inputTileSize / displayTileSize));\n" +

            "vec4 color = texture2D(inputImageTexture, finalCoord);\n" +
            "if (colorOn == 1) {\n" +
            "color = color * inputColor;\n" +
            "}\n" +

            "gl_FragColor = color;\n" +

            "}";


    private int inputTileSizeUniform;
    private float[] inputTileSize;

    private int displayTileSizeUniform;
    private float[] displayTileSize;

    private float numTiles;
    private int numTilesUniform;

    private int colorOnUniform;
    private int colorOn;

    private float aspectRatio;
    //this is to prevent a glVertexAttribPointer mGLAttribPosition: glError 0x502 bug
    private boolean inited = false;


    public GPUImageVideoMixFilter(float ar) {
        super(NO_FILTER_VERTEX_SHADER, VIDEOMIX_FRAGMENT_SHADER);
        aspectRatio = ar;
        displayTileSize = new float[] {0.0650250017f, 0.0650250017f/aspectRatio};
        inputTileSize = new float[]{0.125f, 0.125f};
        numTiles = 64f;
        colorOn = 1;
    }

    @Override
    public void onInit() {
        super.onInit();
        inited = true;

        inputTileSizeUniform = GLES20.glGetUniformLocation(getProgram(), "inputTileSize");
        displayTileSizeUniform = GLES20.glGetUniformLocation(getProgram(), "displayTileSize");
        numTilesUniform = GLES20.glGetUniformLocation(getProgram(), "numTiles");
        colorOnUniform = GLES20.glGetUniformLocation(getProgram(), "colorOn");

        setFloatVec2(inputTileSizeUniform, inputTileSize);
        setFloatVec2(displayTileSizeUniform, displayTileSize);
        setNumTiles(numTiles);
        setInteger(colorOnUniform, colorOn);
    }


    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);

        float realAspectRatio;
        realAspectRatio = (float)width/(float)height;

        if (realAspectRatio == 1.0f) {
            setNumTiles(64f);
            setInputTileSize(new float[]{0.125f, 0.125f});
        } else if (realAspectRatio >= 1.0f) {
            setNumTiles(112f);
            setInputTileSize(new float[]{0.125f, 0.07142857142857142f});
        } else {
            setNumTiles(112f);
            setInputTileSize(new float[]{0.07142857142857142f, 0.125f});
        }
    }

    private void setInputTileSize(final float[] newVal) {
        inputTileSize = newVal;
        setFloatVec2(inputTileSizeUniform, inputTileSize);
    }

    private void setDisplayTileSize(final float[] newVal) {
        displayTileSize = newVal;
        setFloatVec2(displayTileSizeUniform, displayTileSize);
    }

    private void setNumTiles(final float newVal) {
        numTiles = newVal;
        setFloat(numTilesUniform, numTiles);
    }

    public void setSliderValue(final float val) {
        float newVal = (float) (Math.pow(val * 0.5f + 0.005f,2)+Math.pow(val * 0.35f + 0.005f,2));
        if (inited)
            setDisplayTileSize(new float[] {newVal, newVal/aspectRatio});

//        setInputTileSize(new float[] {inputTileSize[0], val});
//        Log.d("videoMix", String.valueOf(val));
    }

}
