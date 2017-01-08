package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;

/**
 * Created by john_yan on 2016-10-13.
 */
public class GPUImageWarholFilter extends GPUImageFilter {

    //seems the same as NO_FILTER_VERTEX_SHADER, probably does not need this
//    public static final String TWITCH_VERTEX_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
//
//            "attribute vec4 position;\n" +
//            "attribute vec4 inputTextureCoordinate;\n" +
//            "varying vec2 textureCoordinate;\n" +
//
//            "uniform lowp float controlVariable;\n" +
//            "uniform highp float timeVariable;\n" +
//
//
//            "void main()\n" +
//            "{\n" +
//            "vec4 np = position;\n" +
//
//            "textureCoordinate = inputTextureCoordinate.xy;\n" +
//            "gl_Position = np;\n" +
//
//            "}";

    public static final String WARHOL_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +

            "varying lowp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +

            "uniform highp float threshold;\n" +
            "uniform lowp float controlVariable;\n" +
            "uniform highp float timeVariable;\n" +

            "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
            "const highp vec4 shadowCol = vec4(0.55, 0.24, 0.25, 1.0);\n" +
            "const highp vec4 highlightCol = vec4(0.04, 0.38, 1.0, 1.0);\n" +

            "void main()\n" +
            "{\n" +

            "highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "highp float luminance = dot(textureColor.rgb, W);\n" +
            "highp float thresholdResult = step(controlVariable, luminance);\n" +
            "highp vec4 bw = vec4(vec3(thresholdResult), textureColor.w)*highlightCol;\n" +
            "highp vec4 bwInvert = vec4(vec3(1.0 - thresholdResult), textureColor.w)*shadowCol;\n" +
            "highp vec4 duoTone = max(bw, bwInvert);\n" +

            "highp float thresholdResultOffset = step((controlVariable*0.5), luminance);\n" +
            "highp vec4 features = vec4(vec3(1.0-thresholdResultOffset), 1.0);\n" +
            "highp vec4 results = max(duoTone, floor(features));\n" +

            " gl_FragColor = results;\n" +
            "}";


    private int controlVariableUniform;
    private float controlVariable;
    private int timeVariableUniform;
    private float timeVariable;



    public GPUImageWarholFilter() {
        super(NO_FILTER_VERTEX_SHADER, WARHOL_FRAGMENT_SHADER);
        setControlVariable(0.5f);
    }

    @Override
    public void onInit() {
        super.onInit();
        controlVariableUniform = GLES20.glGetUniformLocation(getProgram(), "controlVariable");
        timeVariableUniform = GLES20.glGetUniformLocation(getProgram(), "timeVariable");

        setControlVariable(0.5f);
    }


    public void setControlVariable(final float newVal) {
        controlVariable = newVal;
        setFloat(controlVariableUniform, controlVariable);
    }


}
