package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;

/**
 * Created by john_yan on 2016-10-17.
 */
public class GPUImageInvertFilter extends GPUImageFilter {
    public static final String INVERT_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +

            "precision highp float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +

            "uniform float slider;\n" +

            "void main()\n" +
            "{\n" +
                    "vec4 originalColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "highp vec4 invertedColor = vec4(((slider*0.5+0.5) - originalColor.rgb), originalColor.w);\n" +
                    "gl_FragColor = invertedColor;\n" +
            "}\n";

    private float slider;
    private int sliderUniform;

    public GPUImageInvertFilter() {
        super(NO_FILTER_VERTEX_SHADER, INVERT_FRAGMENT_SHADER);
        slider = 0.5f;
    }

    @Override
    public void onInit() {
        super.onInit();
        sliderUniform = GLES20.glGetUniformLocation(getProgram(), "slider");
    }

    public void setSlider(final float newVal) {
        slider = newVal;
        setFloat(sliderUniform, slider);
    }
}
