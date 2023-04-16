package ruyi.reflection.demo;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import ruyi.reflection.Reflection;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Hello World");
        setContentView(tv);

        Reflection.bypassAll(Build.VERSION.SDK_INT);
        try {
            // test a hidden api
            Class displayListCanvas = Class.forName("android.graphics.RecordingCanvas");
            displayListCanvas.getMethod("drawGLFunctor2", long.class, Runnable.class);
            tv.setText("ByPass Result TRUE");
        } catch (Throwable t) {
            t.printStackTrace();
            tv.setText("ByPass Result FALSE");
        }

        Log.e("test", "mTestInt " + Reflection.get(this, "mTestInt"));
        Reflection.setStatic(MainActivity.class,"sTestInt", 3);
        Log.e("test", "test() " + Reflection.invokeStatic(MainActivity.class, "test"));
        Reflection.set(MainActivity.this, "mTestInt", 7);
        Log.e("test", "test1() " + Reflection.invoke(this, "test1"));

    }

    private int mTestInt = 6;
    private static int sTestInt = 5;
    private static int test() {
        return sTestInt;
    }

    private int test1() {
        return mTestInt;
    }
}
