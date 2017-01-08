package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;
import android.util.Log;
/**
 * Created by john_yan on 2016-10-19.
 * Adapted from Leó Stefánsson
 * Copyright (c) 2015 Hybridity Media. All rights reserved.
 */
public class GPUImagePinkFilter extends GPUImageFilter {

    public static final String PINK_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +

            "uniform float controlVariable;\n" +
            "uniform float inputAmplitude;\n" +

            "void main()\n" +
            "{\n" +
            "   highp vec4 originalColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "   vec4 blueColor = vec4(0.33, 0.89, 0.97, 1.0);\n" +
            "   vec4 pinkColor = vec4(0.93, 0.0, 0.89, 1.0);\n" +
            "   vec4 outputColor;\n" +

            "   float offset = controlVariable*0.5-0.1;\n" +
            //x and y are inverted, not sure why??? TODO: check why this is happening
            "   vec2 uv = vec2(textureCoordinate.x, textureCoordinate.y-offset);\n" +
            "   vec4 offsetColorR = texture2D(inputImageTexture, uv);\n" +
            "   vec4 offsetColorL = texture2D(inputImageTexture, vec2(textureCoordinate.x, textureCoordinate.y + offset));\n" +

            "   vec4 clR = clamp(offsetColorR, vec4(0.5*controlVariable), vec4(0.5+controlVariable));\n" +
            "   vec4 clL = clamp(offsetColorL, vec4(0.5*controlVariable), vec4(0.5+controlVariable));\n" +

            //audio
            "   clR -= vec4(vec3(inputAmplitude*0.3), 1.0);\n" +
            "   clL -= vec4(vec3(inputAmplitude*0.3), 1.0);\n" +

            "   vec4 maskR = step(clR, vec4(controlVariable*0.3));\n" +
            "   vec4 maskL = step(clL, vec4(controlVariable*0.3));\n" +

            "   maskR = step(clR, vec4(controlVariable));\n" +
            "   maskL = step(clL, vec4(controlVariable));\n" +

            "   maskR = vec4(vec3(maskR.r*maskR.g*maskR.b), 1.0);\n" +
            "   maskL = vec4(vec3(maskL.r*maskL.g*maskL.b), 1.0);\n" +

            "   if (maskR.r > 0.5) {\n" +
            "       maskR = pow(pinkColor, vec4(vec3(2.0), 1.0));\n" +
            "   }\n" +

            "   if (maskL.r > 0.5) {\n" +
            "       maskL = pow(blueColor, vec4(vec3(2.0), 1.0));\n" +
            "   }\n" +

            "   vec4 blend = maskL+maskR;\n" +

            "   float b = blend.r+blend.g+blend.b;\n" +
            "   b = b/3.0;\n" +

            "   if (b > 0.95) {\n" +
            "       outputColor = originalColor;\n" +
            "   }\n" +
            "   else {\n" +
            "      outputColor = blend+originalColor;\n" +
            "   }\n" +

            "   gl_FragColor = outputColor;\n" +
            "}";

    private int controlVariableUniform;
    private float controlVariable;

    private int inputAmplitudeUniform;
    private float inputAmplitude;

    public GPUImagePinkFilter() {
        super(GPUImageFilter.NO_FILTER_VERTEX_SHADER, PINK_FRAGMENT_SHADER);
        controlVariable = 0.1f;
        inputAmplitude = 0.0f;
    }

    @Override
    public void onInit() {
        super.onInit();

        controlVariable = 0.4f;
        controlVariableUniform = GLES20.glGetUniformLocation(getProgram(), "controlVariable");
        inputAmplitudeUniform = GLES20.glGetUniformLocation(getProgram(), "inputAmplitude");

        setFloat(controlVariableUniform, controlVariable);
        setFloat(inputAmplitudeUniform, inputAmplitude);
    }

    public void setLevels(final float colorLevels) {
        controlVariable = colorLevels;
        setFloat(controlVariableUniform, controlVariable);
    }

    //still need to make the sound reactivity work
    public void setAmplitude(final float newAmp) {
        inputAmplitude = newAmp;
        setFloat(inputAmplitudeUniform, inputAmplitude);
    }
}