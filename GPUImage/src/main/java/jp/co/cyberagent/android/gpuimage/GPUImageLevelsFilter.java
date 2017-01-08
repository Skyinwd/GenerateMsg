package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;

/**
 * Created by john_yan on 2016-09-27.
 * this filter controls LUT filters giving effects to the slide bar
 */
public class GPUImageLevelsFilter extends  GPUImageFilter{
    public static final String LEVEL_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

                    "vec3 GammaCorrection(vec3 color, vec3 gamma) {\n" +
                    "return pow(color, 1.0 / gamma); }\n" +

                    "vec3 LevelsControlInputRange(vec3 color, vec3 minInput, vec3 maxInput) {\n" +
                    "return min(max(color - minInput, vec3(0.0)) / (maxInput - minInput), vec3(1.0)); }\n" +

                    "vec3 LevelsControlInput(vec3 color, vec3 minInput, vec3 gamma, vec3 maxInput) {\n" +
                    "return GammaCorrection(LevelsControlInputRange(color, minInput, maxInput), gamma); }\n" +

                    "vec3 LevelsControlOutputRange(vec3 color, vec3 minOutput, vec3 maxOutput) {\n" +
                    "return mix(minOutput, maxOutput, color); }\n" +

                    "vec3 LevelsControl(vec3 color, vec3 minInput, vec3 gamma, vec3 maxInput, vec3 minOutput, vec3 maxOutput) {\n" +
                    "return LevelsControlOutputRange(LevelsControlInput(color, minInput, gamma, maxInput), minOutput, maxOutput); }\n" +

            "varying lowp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +
//            "uniform lowp float threshold;\n" +

            "uniform lowp vec3 levelMinimum;\n" +
            "uniform lowp vec3 levelMiddle;\n" +
            "uniform lowp vec3 levelMaximum;\n" +
            "uniform lowp vec3 minOutput;\n" +
            "uniform lowp vec3 maxOutput;\n" +



    "void main()\n" +
            "{\n" +
            "mediump vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +

//            "gl_FragColor = vec4(textureColor.xyz * threshold, textureColor.a);\n" +
                    "gl_FragColor = vec4(LevelsControl(textureColor.rgb, levelMinimum, levelMiddle, levelMaximum, minOutput, maxOutput), textureColor.a);\n" +

                    "}";


    private float[] minVector;
    private int minUniform;
    private float[] midVector;
    private int midUniform;
    private float[] maxVector;
    private int maxUniform;

    private float[] minOutput;
    private int minOutputUniform;
    private float[] maxOutput;
    private int maxOutputUniform;

    public GPUImageLevelsFilter () {
        super(NO_FILTER_VERTEX_SHADER, LEVEL_FRAGMENT_SHADER);
        minVector = new float[]{0f, 0f, 0f};
        midVector = new float[]{0f, 0f, 0f};
        maxVector = new float[]{0f, 0f, 0f};
        minOutput = new float[]{0f, 0f, 0f};
        maxOutput = new float[]{0f, 0f, 0f};
    }

    @Override
    public void onInit() {
        super.onInit();

        minUniform = GLES20.glGetUniformLocation(getProgram(), "levelMinimum");
        midUniform = GLES20.glGetUniformLocation(getProgram(), "levelMiddle");
        maxUniform = GLES20.glGetUniformLocation(getProgram(), "levelMaximum");

        minOutputUniform = GLES20.glGetUniformLocation(getProgram(), "minOutput");
        maxOutputUniform = GLES20.glGetUniformLocation(getProgram(), "maxOutput");

        setMin(.3f, 1f, 1f);
    }


    private void setRedMin (float min, float mid, float max, float minOut, float maxOut) {
        minVector[0] = min;
        midVector[0] = mid;
        maxVector[0] = max;
        minOutput[0] = minOut;
        maxOutput[0] = maxOut;

        updateUniform();
    }

    private void setRedMin(float min, float mid, float max) {
        setRedMin(min, mid, max, 0f, 1f);
    }

    private void setGreenMin (float min, float mid, float max, float minOut, float maxOut) {
        minVector[1] = min;
        midVector[1] = mid;
        maxVector[1] = max;
        minOutput[1] = minOut;
        maxOutput[1] = maxOut;

        updateUniform();
    }

    private void SetGreenMin(float min, float mid, float max) {
        setGreenMin(min, mid, max, 0f, 1f);
    }

    private void setBlueMin (float min, float mid, float max, float minOut, float maxOut) {
        minVector[2] = min;
        midVector[2] = mid;
        maxVector[2] = max;
        minOutput[2] = minOut;
        maxOutput[2] = maxOut;

        updateUniform();
    }

    private void setBlueMin (float min, float mid, float max) {
        setGreenMin(min, mid, max, 0f, 1f);
    }


    private void setMin (float min, float mid, float max, float minOut, float maxOut) {
        setBlueMin(min, mid, max, minOut, maxOut);
        setRedMin(min, mid, max, minOut, maxOut);
        setGreenMin(min, mid, max, minOut, maxOut);
    }

    public void setMin (float min, float mid, float max) {
        setMin(min, mid, max, 0f, 1f);
    }

    private void updateUniform() {
        setFloatVec3(minUniform, minVector);
        setFloatVec3(midUniform, midVector);
        setFloatVec3(maxUniform, maxVector);
        setFloatVec3(minOutputUniform, minOutput);
        setFloatVec3(maxOutputUniform, maxOutput);
    }



//    public void setThreshold(float t) {
//        mThreshold = t;
//        setFloat(thresholdUniform, mThreshold);
//    }

}
