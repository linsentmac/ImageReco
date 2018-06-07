package com.tmac.testcloudtarget;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.post);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PostNewTarget p = new PostNewTarget();
                p.postTargetThenPollStatus();
            }
        });

    }
}
