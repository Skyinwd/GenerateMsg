package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;

import java.util.TimerTask;

/**
 * Created by john_yan on 2016-10-21.
 * Adapted from Leó Stefánsson on 2016-10-14.
 * Copyright (c) 2015 Hybridity Media. All rights reserved.
 */
public class GPUImageHalloweenFilter extends GPUImageFilter {

    public static final String HALLOWEEN_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +

            "uniform float slider;\n" +
            "uniform float time;\n" +

            "highp vec4 distortion(highp vec2 uv)\n" +
            "{\n" +
            "   highp float br = uv.x*sin(time*9.0);" +
            "   highp vec4 pattern = vec4(br*uv.x,br*uv.x,br*uv.x,1.0);\n" +
            "   return pattern;\n" +
            "}\n" +

            "float dist(vec2 p0, vec2 pf){return sqrt((pf.x-p0.x)*(pf.x-p0.x)+(pf.y-p0.y)*(pf.y-p0.y));}\n" +

            "highp vec2 bulgeDistortion(highp vec2 uv, highp vec2 center, float seed)\n" +
            "{\n" +
            "   highp float aspectRatio = 1.0;" +
            "   highp float radius = 0.03;\n" +
            "   highp float scale = 0.03;\n" +

            "   highp vec2 textureCoordinateToUse = vec2(uv.x*sin(mod(time*0.03, 3.14)), (uv.y*sin(mod(time*0.03, 3.14)) * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
            "   highp float dist = distance(center, textureCoordinateToUse);\n" +
            "   textureCoordinateToUse = uv;\n" +

            "   if (dist < radius)\n" +
            "   {\n" +
            "       textureCoordinateToUse -= center;\n" +
            "       highp float percent = 1.0 - ((radius - dist) / radius) * scale * slider * sin(time+seed);\n" +
            "       percent =  percent * percent;\n" +
            "       textureCoordinateToUse = textureCoordinateToUse * percent;\n" +
            "       textureCoordinateToUse += center;\n" +
            "   }\n" +

            "   return textureCoordinateToUse;\n" +
            "}\n" +


            "float DE( vec2 pp, out bool blood, float t )\n" +
            "{\n" +
            "   pp.y *= -1.0;" +
            "   pp.y += (\n" +
            "             .4 * sin(.5*2.3*pp.x+pp.y) +\n" +
            "             .2 * sin(.5*5.5*pp.x+pp.y) +\n" +
            "             0.1*sin(.5*13.7*pp.x)+\n" +
            "             0.06*sin(.5*23.*pp.x));" +
            "   pp += vec2(0.,0.4)*t;" +
            "   float thresh = 2.3;" +
            "   blood = pp.y > thresh;" +
            "   float d = abs(pp.y - thresh);" +
            "   return d;\n" +
            "}\n" +

            //switched x and y
//            "float DE( vec2 pp, out bool blood, float t )\n" +
//            "{\n" +
//            "   pp.x *= -1.0;" +
//            "   pp.x += (\n" +
//            "             .4 * sin(.5*2.3*pp.y+pp.x) +\n" +
//            "             .2 * sin(.5*5.5*pp.y+pp.x) +\n" +
//            "             0.1*sin(.5*13.7*pp.y)+\n" +
//            "             0.06*sin(.5*23.*pp.y));" +
//            "   pp += vec2(0.4,0.4)*t;" +
//            "   float thresh = 2.3;" +
//            "   blood = pp.x > thresh;" +
//            "   float d = abs(pp.x - thresh);" +
//            "   return d;\n" +
//            "}\n" +

            "vec3 sceneColour( in vec2 pp )" +
            "{\n" +
            "   float endTime = 16.;" +
            "   float rewind = 2.;" +
            "   float t = mod( time, endTime+rewind );" +

            "   if( t > endTime )" +
            "       t = endTime * (1.-(t-endTime)/rewind);" +

            "   bool blood;" +
            "   float d = DE( pp, blood, t );" +

            "   if( !blood) {" +
            "      vec3 floorCol = vec3(.01);" +
            "      return floorCol;" +
            "   }" +

            "   float h = clamp( smoothstep(.0,.25,d), 0., 1.);" +
            "   h = 4.*pow(h,.2);" +

//            "   vec3 N = vec3(-dFdx(h), 1., -dFdy(h) );" +
            "   vec3 N = vec3(h, 1., h);" +
//            "   vec3 N = vec3(h, h, h);" +

            "   N = normalize(N);" +
            "   vec3 L = normalize(vec3(.5,.7,-.5));" +
            "   vec3 res = pow(dot(N,L),10.)*vec3(1.);" +
            "   res += vec3(.5,-.3,-0.3);" +
            "   vec2 off = pp-vec2(5.3,2.);" +

            "   return res;\n" +
            "}\n" +

            "void main()\n" +
            "{\n" +

            "   vec4 originalColor = texture2D(inputImageTexture, textureCoordinate);\n" +

            "   vec2 uv = vec2(textureCoordinate.x, textureCoordinate.y);\n" +
            "   vec4 outputColor = texture2D(inputImageTexture, uv);\n" +

            "   highp vec2 distUV = bulgeDistortion(uv, vec2(0.5, 0.5), 0.11);\n" +
            "   distUV = bulgeDistortion(distUV, vec2(0.2, 0.2), 0.21);\n" +
            "   distUV = bulgeDistortion(distUV, vec2(0.2, 0.4), 0.33);\n" +
            "   distUV = bulgeDistortion(distUV, vec2(0.4, 0.4), 0.47);\n" +
            "   distUV = bulgeDistortion(distUV, vec2(0.6, 0.6), 0.59);\n" +
            "   distUV = bulgeDistortion(distUV, vec2(0.1, 0.1), 0.67);\n" +
            "   distUV = bulgeDistortion(distUV, vec2(0.2, 0.7), 0.25);\n" +
            "   distUV = bulgeDistortion(distUV, vec2(0.7, 0.4), 0.82);\n" +
            "   distUV = bulgeDistortion(distUV, vec2(0.75, 0.6), 0.91);\n" +

            "   highp vec4 distortedColor = texture2D(inputImageTexture, distUV);\n" +
            "   highp vec4 blood = vec4(sceneColour(uv*4.), 1.0);\n" +

            "   gl_FragColor = blood + distortedColor;\n" +
            "}";

    private int sliderUniform;
    private float slider;
    private int timeUniform;
    private float time;

    public GPUImageHalloweenFilter() {
        super(GPUImageFilter.NO_FILTER_VERTEX_SHADER, HALLOWEEN_FRAGMENT_SHADER);
        slider = 0.5f;
        time = 0.0f;

    }

    @Override
    public void onInit() {
        super.onInit();

        sliderUniform = GLES20.glGetUniformLocation(getProgram(), "slider");
        timeUniform = GLES20.glGetUniformLocation(getProgram(), "time");

        setFloat(sliderUniform, slider);
        setFloat(timeUniform, time);

        TimerTask updateWithTime = new TimerTask () {
            @Override
            public void run () {
                updateTime();
            }
        };

        // schedule the task to run starting now and then every hour...
        timer.schedule(updateWithTime, 0l, 50);
    }


    private void updateTime() {
        time += 0.05f;

        setFloat(timeUniform, time);
    }
}
