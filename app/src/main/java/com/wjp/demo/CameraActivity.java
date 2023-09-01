package com.wjp.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;

import com.wjp.demo.task.TaskService;
import com.wjp.demo.task.impl.CameraTask;

public class CameraActivity extends Activity {

    TextureView mTexture;
    EditText mEdit;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.camera);
        mTexture = findViewById(R.id.camera_texture);
        mEdit = findViewById(R.id.camera_edit);

        findViewById(R.id.camera_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskService.getInstance().getTask(CameraTask.class).execute("0", mTexture);
            }
        });

        findViewById(R.id.camera_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskService.getInstance().getTask(CameraTask.class).stopRecord();
            }
        });
    }
}
