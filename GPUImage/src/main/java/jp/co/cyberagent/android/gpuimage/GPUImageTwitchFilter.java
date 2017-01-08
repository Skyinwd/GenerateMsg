package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;
import java.util.TimerTask;
import android.util.Log;

/**
 * Created by john_yan on 2016-10-13.
 */
public class GPUImageTwitchFilter extends GPUImageFilter {


    public static final String TWITCH_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +

            "varying lowp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +

            "uniform highp float slider;\n" +
            "uniform highp float time;\n" +

            "void main()\n" +
            "{\n" +

            "highp vec2 uv = vec2(textureCoordinate.x, textureCoordinate.y);\n" +
            "highp float time = (mod(time, 0.11)+0.8)*slider;\n" +
            "uv.x += mod(time,0.01+mod(time, sin(uv.y*10.0)/10.0*slider+0.01));\n" +
            "uv.y += 0.02+mod(time, sin(uv.x*7.0)/10.0*slider+0.01);\n" +

            "highp vec4 color = texture2D(inputImageTexture, uv);\n" +
            " gl_FragColor = color;\n" +
            "}";


    private int sliderUniform;
    private float slider;
    private int timeVariableUniform;
    private float timeVariable;



    public GPUImageTwitchFilter() {
        super(NO_FILTER_VERTEX_SHADER, TWITCH_FRAGMENT_SHADER);
        setSlider(0.5f);
        timeVariable = 0f;

        TimerTask updateWithTime = new TimerTask () {
            @Override
            public void run () {
                updateTime();
            }
        };

        // schedule the task to run starting now and then every hour...
        timer.schedule(updateWithTime, 0l, 50);
    }

    @Override
    public void onInit() {
        super.onInit();
        sliderUniform = GLES20.glGetUniformLocation(getProgram(), "slider");
        timeVariableUniform = GLES20.glGetUniformLocation(getProgram(), "time");

        setSlider(0.5f);
    }

    private void updateTime() {
        timeVariable += 0.0008f;

        setFloat(timeVariableUniform, timeVariable);
    }

    public void setSlider(final float newVal) {
        slider = newVal;
        setFloat(sliderUniform, slider);

        Log.d("testPrinting", "newVal: " + Float.toString(slider));
    }
}
