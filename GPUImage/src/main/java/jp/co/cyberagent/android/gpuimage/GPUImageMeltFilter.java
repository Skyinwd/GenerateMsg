package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;
import jp.co.cyberagent.android.gpuimage.GPUImageTwoInputFilter;

/**
 * Created by john_yan on 2016-10-04.
 */
public class GPUImageMeltFilter extends GPUImageTwoInputFilter  {
    public static final String MELT_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +

            " precision highp float; \n" +

            " varying highp vec2 textureCoordinate; \n" +
            " varying highp vec2 textureCoordinate2; \n" +

            " uniform sampler2D inputImageTexture; \n" + //camera texture
            " uniform sampler2D inputImageTexture2; \n" + //chosen photo texture

            " uniform highp float sliderValue; \n" +
            " uniform highp float inputAmplitude; \n" +


//            " void main()\n" +
//            " {\n" +
//            " vec4 libraryColor = texture2D(inputImageTexture, textureCoordinate); \n" +
//            " vec4 originalColor = texture2D(inputImageTexture2, textureCoordinate2); \n" +
//            " highp vec4 remap1; \n" +
//            " highp vec4 remap2; \n" +
//
//            " highp float offset; \n" +
//            " highp float b = (originalColor.r + originalColor.g + originalColor.b)/3.0; \n" +
//
//            " if(b < 0.9) { \n" +
//
//            " offset = sliderValue + pow(inputAmplitude*0.7, 2.0); \n" +
//            " remap1 = texture2D(inputImageTexture, vec2(textureCoordinate.x-originalColor.r*sliderValue, textureCoordinate2.y-originalColor.g*sliderValue)); \n" +
//            " remap2 = texture2D(inputImageTexture, vec2(textureCoordinate2.x+originalColor.r*offset, textureCoordinate2.y+originalColor.g*offset)); \n" +
//
//            " } \n" +
//            " else { \n" +
//
//            " offset = sliderValue; \n" +
//            " remap1 = texture2D(inputImageTexture, vec2(textureCoordinate.x+originalColor.r*offset, textureCoordinate2.y+originalColor.g*offset)); \n" +
//            " remap2 = texture2D(inputImageTexture, vec2(textureCoordinate2.x-originalColor.r*offset, textureCoordinate2.y-originalColor.g*offset)); \n" +
//
//            " } \n" +
//
//            " highp vec4 outputColor = mix(remap1, remap2, 0.5); \n" +
//
//            " gl_FragColor = originalColor; \n" +
//
//            " }";

            " void main()\n" +
            " {\n" +
            " vec4 originalColor = texture2D(inputImageTexture, textureCoordinate); \n" +
            " vec4 libraryColor = texture2D(inputImageTexture2, textureCoordinate2); \n" +
            " highp vec4 remap1; \n" +
            " highp vec4 remap2; \n" +

            " highp float offset; \n" +
            " highp float b = (originalColor.r + originalColor.g + originalColor.b)/3.0; \n" +

            " if(b < 0.9) { \n" +

            " offset = sliderValue + pow(inputAmplitude*0.7, 2.0); \n" +
            " remap1 = texture2D(inputImageTexture2, vec2(textureCoordinate.x-originalColor.r*sliderValue, textureCoordinate.y-originalColor.g*sliderValue)); \n" +
            " remap2 = texture2D(inputImageTexture2, vec2(textureCoordinate.x+originalColor.r*offset, textureCoordinate.y+originalColor.g*offset)); \n" +

            " } \n" +
            " else { \n" +

            " offset = sliderValue; \n" +
            " remap1 = texture2D(inputImageTexture2, vec2(textureCoordinate.x+originalColor.r*offset, textureCoordinate.y+originalColor.g*offset)); \n" +
            " remap2 = texture2D(inputImageTexture2, vec2(textureCoordinate.x-originalColor.r*offset, textureCoordinate.y-originalColor.g*offset)); \n" +

            " } \n" +

            " highp vec4 outputColor = mix(remap1, remap2, 0.5); \n" +


            " gl_FragColor = outputColor; \n" +

            " }";

    private int sliderValueUniform;
    private float sliderValue;
    private int inputAmplitudeUniform;
    private float amplitude;


    public GPUImageMeltFilter() {
        super(MELT_FRAGMENT_SHADER);
    }

    @Override
    public void onInit() {
        super.onInit();

        sliderValueUniform = GLES20.glGetUniformLocation(getProgram(), "sliderValue");
        inputAmplitudeUniform = GLES20.glGetUniformLocation(getProgram(), "inputAmplitude");

        amplitude = 0.4f;
        setAmplitude(amplitude);

        sliderValue = amplitude*.35f;
        setFloat(sliderValueUniform, sliderValue);
    }

    public void setAmplitude(float newThreshold) {
        sliderValue = newThreshold*.35f;
        setFloat(sliderValueUniform, sliderValue);

        amplitude = newThreshold;
        setFloat(inputAmplitudeUniform, amplitude);
    }
}
