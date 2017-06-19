package com.example.vikramkumaresan.doubt1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.VectorEnabledTintResources;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class Camera_vs_Gallery extends AppCompatActivity {
    TextView camera_button;
    TextView gallery_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_vs__gallery);

        camera_button = (TextView)findViewById(R.id.camera_button);
        gallery_button=(TextView)findViewById(R.id.gallery_button);
    }

    public void launch_camera(View view){
        Custom_Adapter.CameraLauncher(getIntent().getIntExtra("position",-1));  //Launches camera and passed the 'position' (Given as extra while launching)
        finish();   //Collapses the current activity and forces it to go back to the previous activity
    }

    public void launch_gallery(View view){
        Custom_Adapter.GalleryLauncher(getIntent().getIntExtra("position",-1)); //Launches Gallery
        finish();
    }
}
