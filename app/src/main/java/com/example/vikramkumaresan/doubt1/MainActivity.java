package com.example.vikramkumaresan.doubt1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static com.example.vikramkumaresan.doubt1.R.id.img;

public class MainActivity extends AppCompatActivity  {
    static ArrayAdapter adapt;
    public static ArrayList<Uri> pathcollector;  //Use this to get the Path generated in Cust_Adapter
    public static int target;   //To get the position from Adapter
    public  static  boolean flag =false;    //To distinguish btw gallery and camera launch (wrt notifyDataSetChanged() )
    static ListView list;

    Button new_row;
    public static ArrayList<Uri> stuff;   //The parent data structure to be passed to the adapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pathcollector = new ArrayList<Uri>();
        target=-1;
        new_row=(Button)findViewById(R.id.button);
        list = (ListView)findViewById(R.id.list);
        stuff = new ArrayList<Uri>();

        //Retrieving Saved Info   (Saved onPause() )
        SharedPreferences pref = getSharedPreferences("stuff_stored",MODE_PRIVATE);
        int size=pref.getInt("size",0); //To get check if anything was stored. If size =0 (Default) nothing was stored

        if(size!=0){
            for (int i=0;i<size;i++){
                stuff.add(Uri.parse(pref.getString(""+i,null)));    //Recreate the stored stuff list
            }
        }

        adapt = new Custom_Adapter(this,stuff); //Pass the stuff array to the cust_adapter
        list.setAdapter(adapt);
        //.......................

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==1 && resultCode==RESULT_OK){    //Camera capture intent calls this

            stuff.set(target,pathcollector.get(0)); //Get the Uri path (Put into pathcollector by cust_adapt) (target also set by cust_adapt)
            adapt.notifyDataSetChanged();   //I have overrided this method in cust_adapt
            list.invalidateViews();         //notifyDataSetChanged() sometimes doesn't refresh list view
                                            //invalidateViews() forcefully refreshes the list. So the camera pick taken is added
        }
        else if(requestCode==2 && resultCode==RESULT_OK){   //Gallery launch intent calls this

            Uri imgpath = data.getData();   //You can get the Google Photo Uri from data.getData()
            pathcollector.add(imgpath);
            flag =true; //Just fo Custom_Adapter to differentiate

            adapt.notifyDataSetChanged();
            list.invalidateViews();

        }
    }

    public void Create_Row(View view){
        stuff.add(Uri.parse(""));        //Adds blank Uri into parent data type (For placeholding)
        pathcollector.add(Uri.parse(""));   //For Cust_Adpater to retrieve
        adapt.notifyDataSetChanged();
        list.invalidateViews();
    }

    public static void Delete_Row (int position){
        stuff.remove(position); //Deletes from parent array
        list.invalidateViews();
    }

    @Override
    protected void onPause() {  //Gets called every time app looses focus. No doubt.
        // Stores the parent data type 'stuff' array
        SharedPreferences pref = getSharedPreferences("stuff_stored",MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.putInt("size",stuff.size());   //Helps in loop during reconstruction

        for(int i=0;i<stuff.size();i++){
            edit.putString(""+i,stuff.get(i).toString());   //Store all the elements with key as their index value
        }
        edit.apply();
        Custom_Adapter.store_comments();    //Call to store comments
        Custom_Adapter.store_preloaded();   //Call to store preloaded img
        super.onPause();    //You have to call the main onPause method.....
        //...........................................
    }

}
