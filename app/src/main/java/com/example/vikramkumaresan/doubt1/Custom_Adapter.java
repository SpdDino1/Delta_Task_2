package com.example.vikramkumaresan.doubt1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import static android.R.attr.path;

public class Custom_Adapter extends ArrayAdapter {

    EditText comment;
    static Context cont;

    ArrayList<Bitmap> loadedpics;  //Holds all the bitmaps. Preloading them helps save time and unnecessary repeated decoding of the Uri. They are all resized.
    static ArrayList<String> preloadedpics; //This is used to store preloaded pics of gallery launched pictures. I did this as while adding they get resized. So after
                                            //app restarts they shouldn't get resized again (Double resize = pixelated image) So they sit in their own array. If in loaded pics
                                            // they will be forced to be resized upon restart
    static ArrayList<String> comments; //Stores comments
    ImageView remove_row;  //The 'Cross' to delete a row

    public Custom_Adapter(Context context, ArrayList<Uri> stuff) {
        super(context,R.layout.custom, stuff);
        cont = context;
        loadedpics = new ArrayList<Bitmap>();
        comments = new ArrayList<String>();
        preloadedpics = new ArrayList<String>();

        //Generating Loaded pics, preloaded pics and comments array after restart

        SharedPreferences pref2 = cont.getSharedPreferences("preloaded stored",Context.MODE_PRIVATE);
        int size=pref2.getInt("size",-1);   //To get list size. So now i can set loop limit

        if(size!=-1){
            for (int i=0;i<size;i++){
                preloadedpics.add(pref2.getString(""+i,null));  //Generates (preloaded array) which holds all the indices of stuff that are from gallery
            }
        }

        SharedPreferences pref= cont.getSharedPreferences("comments stored",Context.MODE_PRIVATE);
        for(int i=0;i<stuff.size();i++){
            if(!preloadedpics.contains(""+i)){
                try {
                    PicLoader(stuff.get(i),i,1);    //If preloaded doesn't contain, then it was a camera image. So go ahead, resize, and put it into loaded pics.
                }catch (FileNotFoundException e){
                    loadedpics.add(null);
                }
            }
            else{
                try {
                    Bitmap pic = BitmapFactory.decodeStream(cont.getContentResolver().openInputStream(stuff.get(i)));
                    loadedpics.add(pic);    //If it was in preloaded --->It was gallery image ---->It was already resized. So simply put it into loadedpics. SKIP RESIZING!
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            comments.add(i,pref.getString(""+i,""));    //Generates comment list
        }
        //..........................................................................

    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflate = LayoutInflater.from(getContext());
        final View custom_view= inflate.inflate(R.layout.custom,parent,false);

        comment = (EditText) custom_view.findViewById(R.id.comment);
        ImageView img = (ImageView) custom_view.findViewById(R.id.img);
        ImageView remove = (ImageView)custom_view.findViewById(R.id.delete_cross);

        //Comment Updation and Storage Mechanism
        comment.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        comments.set(position,s.toString());    // When the text in any edittext in the list changes, this part gets activated
                                                                // Literally updates the comments array (particular index) to match what is beging typed
                    }
                }
        );
        comment.setText(comments.get(position));    //Finally set the edit text to what you have put in the array. This is done as when the view gets refreshed, the edittext
                                                    // becomes blank again as it forgets what was stored. So you need to keep manually adding the text back after every refresh.
                                                    //THE getView() IS CALLED DURING EACH REFRESH
        //.......................................

        remove.setOnClickListener(  //When cross clicked
                new ImageView.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadedpics.remove(position);
                        comments.remove(position);
                        if(preloadedpics.contains(""+position)){
                            preloadedpics.remove(""+position);
                        }
                        MainActivity.Delete_Row(position);
                    }
                }
        );

        img.setOnClickListener( //When the add pic imageview icon clicked
                new ImageView.OnClickListener() {
                    @Override
                    public void onClick(View v){
                        Intent intent= new Intent((Activity)cont,Camera_vs_Gallery.class);  //Calls the camera or gallery chooser activity
                        intent.putExtra("position",position);
                        ((Activity)cont).startActivity(intent);
                    }
                }
        );

        try {
            img.setImageBitmap(loadedpics.get(position));   //Manually adds the resized, already loaded bitmaps to the list. Has to be done as the imageviews forget the image
                                                            // upon refreshing

        }catch (Exception e){
            e.printStackTrace();
        }
        return  custom_view;        //Finally throw out view
    }

    private static File FileMaker(){    //Creates an empty 0B file at a location
        try {
            File pic = File.createTempFile("TestImage",".jpg",cont.getExternalFilesDir("Test"));    //Location = Test Folder in getExternalFilesDir(). File Name = TestImage.jpg
                                                                                                    //getExternalFilesDir() is the folder your app is installed in
            return pic;
        } catch (IOException e) {
            Toast.makeText(cont,"Error Making File",Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public void PicLoader(Uri path,int position,int calltype) throws FileNotFoundException {
        //Call type = 1 means adding a new element
        //Call type = 2 means changing an existing element
        BitmapFactory.Options options = new BitmapFactory.Options();    //You need Options to resize and rescaling
        options.inJustDecodeBounds=false;   //True = Create a Virtual Bitmap that you can query (What is height, width? etc: with actually making anything --->memory saved)
                                            //False = Actually Create an image. That's what I want....an image
        options.inSampleSize=8;             //Compresses img to 1/8th the size. So every 8th pixel is only counte
                                    //You need decode stream to create a Bitmap from a Uri                                  Options incorporated
        Bitmap pic = BitmapFactory.decodeStream(cont.getContentResolver().openInputStream((Uri) path),new Rect(-1,-1,-1,-1),options); //THIS STEP TAKES TIME
        Bitmap resized_pic = Bitmap.createScaledBitmap(pic,167,167,true);   //Resizes the generated bitmap to 167 x 167 px

        if(calltype == 1){                      //Throws into loadedpics array
            loadedpics.add(resized_pic);
        }
        else if(calltype ==2){
            loadedpics.set(position,resized_pic);
        }
    } //Hence by decoding only once, we save a lot of time. Much better than simply decoding everytime the view refreshes.

    @Override
    public void notifyDataSetChanged() {
        if(MainActivity.target!=-1){    //Called when an existing row's img is clicked
            if (!MainActivity.flag){    //Captured from camera
                try {
                    PicLoader(MainActivity.pathcollector.get(0),MainActivity.target,2); //Simply take saved pic, resize and throw into loadedpics

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else {                      //Captured from gallery
                MainActivity.flag=false;

                try {
                    File out = File.createTempFile("TestImage",".png",cont.getExternalFilesDir("Test"));    //Create a 0B file

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds=false;
                    options.inSampleSize=8;

                    Bitmap pic = BitmapFactory.decodeStream(cont.getContentResolver().openInputStream(MainActivity.pathcollector.get(0)),new Rect(-1,-1,-1,-1),options);
                    Bitmap resized_pic = Bitmap.createScaledBitmap(pic,167,167,true);   //The usual resizing, scaling
                    /* When selecting from the gallery, we use Google Photos to select the pic. Google photos gives us a Uri to the photo the user selected
                    However this Uri is active only for one time use; we can't store the Uri in teh stuff array and expect the image to be back when the app restarts
                    So we have to take that one chance, generate the image and copy it somewhere else (getExternalFilesDir() , as this is our app's safe haven. We can access
                    whenever we want)
                   */

                    //Copying Pic Mechanism

                    //To copy anything in Java, you need a output File, Input File, Output Stream and Input Stream
                    //Output File = out         Input File = resized_pic
                    //Output Stream = FileOutputStream fos    Input Stream =  ByteArrayOutputStream stream (Misnomer)

                    ByteArrayOutputStream stream = new ByteArrayOutputStream(); //Converts the 'Input' Bitmap to 'Output' Byte Stream. Actually this stream is the input stream

                    resized_pic.compress(Bitmap.CompressFormat.PNG,0,stream);   // To convert a bitmap to a stream you need to compress it to one of the img formats
                                                                                // .png   =  Lossless, Takes time (Depending on pic size)
                                                                                // .jpeg  =  Lossy, Less time (Depends on pic size, and the quality factor set)
                                                                                            //Low factor = less time, messy img High = More time, more quality (Not perfect, like .png)

                    FileOutputStream fos = new FileOutputStream(out);
                    fos.write(stream.toByteArray());
                    fos.flush();    //Flush out buffer
                    fos.close();    //Closes the stream
                    //...................

                    preloadedpics.add(""+MainActivity.target);      //Adds index to preloadedpics. This pic is already resized, so this arra helps the constructor know that this pic
                                                                    // needs no resizing
                    MainActivity.stuff.set(MainActivity.target,Uri.fromFile(out));  //Store the Uri of the newly created output file to stuff. NOT THE ONE GOOGLE PHOTOS GAVE
                    loadedpics.set(MainActivity.target,resized_pic);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    Toast.makeText(cont,"Pls Select Only Images",Toast.LENGTH_SHORT).show();    //In case videos are selected from Google Photos
                }
            }
        }
        else if(MainActivity.target==-1){   //Called when a new row is created
            loadedpics.add(null);
            comments.add("");
        }
        MainActivity.pathcollector.clear(); //Reset flag values
        MainActivity.target=-1;
    }

    public static void CameraLauncher(int position){    //Called from Camera_vs_Gallery activity
        if(position==-1){
            Toast.makeText(cont,"Internal Error While Launching Camera",Toast.LENGTH_LONG).show();
        }
        else {
            //Special paramater to initialize a camera capture intent
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File pic = FileMaker(); //Make blank file

                                            //You require this to get the Uri of the created file
            Uri imagepath = FileProvider.getUriForFile(cont, BuildConfig.APPLICATION_ID + ".provider", pic);
            MainActivity.pathcollector.add(imagepath);  //Throw Uri path to pathcollector in Main Activity

            //This extra data is for the android system. Tells it to put the result (Selected pic) at the given path
            i.putExtra(MediaStore.EXTRA_OUTPUT, imagepath);
            MainActivity.target = position;

            ((Activity) cont).startActivityForResult(i, 1); //Start Intent with resultcode=1
        }
    }

    public static void GalleryLauncher(int position){   //Called from Camera_vs_Gallery activity
        if (position==-1){
            Toast.makeText(cont,"Internal Error While Opening Gallery",Toast.LENGTH_LONG).show();
        }
        else {
            //  Special Intent parameter to start gallery intent
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            MainActivity.target=position;
            ((Activity)cont).startActivityForResult(i,2);   //Result code = 2
        }
    }

    public static void store_comments(){
        SharedPreferences pref = cont.getSharedPreferences("comments stored",Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();

        for(int i =0;i<comments.size();i++){
            edit.putString(""+i,comments.get(i));
        }
        edit.apply();
    }

    public static void store_preloaded(){
        SharedPreferences pref = cont.getSharedPreferences("preloaded stored",Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();

        edit.putInt("size",preloadedpics.size());

        for(int i=0;i<preloadedpics.size();i++){
            edit.putString(""+i,preloadedpics.get(i));
        }
        edit.apply();
    }
}
