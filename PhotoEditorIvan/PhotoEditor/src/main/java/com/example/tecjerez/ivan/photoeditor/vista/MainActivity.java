package com.example.tecjerez.ivan.photoeditor.vista;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.tecjerez.ivan.photoeditor.R;
import com.example.tecjerez.ivan.photoeditor.controlador.ViewAdapter;
import com.github.clans.fab.FloatingActionButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA};;
    public RecyclerView rv;
    public FloatingActionButton btnCamera, btnGallery;
    public ArrayList<Uri> imagenes = new ArrayList<Uri>();
    public SharedPreferences sp;
    public File storageDir,image;
    public Uri u;
    public static final  String TAG = "IGU";
    private final int RESULT_GALLERY_IMAGE = 4;
    private final int RESULT_CAMERA_IMAGE = 3;
    private final int REQUEST_PERMISSIONS = 7;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //OpenCVLoader.initDebug();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(permissions,REQUEST_PERMISSIONS);
        storageDir = new File(Environment.getExternalStorageDirectory()+"/photo_editor_opencv");
        if (!storageDir.exists())storageDir.mkdir();
        String[] files = null;
        files = storageDir.list();
        File im;
        if (files!=null)
            for (String fileName:files) {
                im = new File(storageDir,fileName);
                Uri u = Uri.fromFile(im);
                Log.i(TAG,"Uri Cargado de Archivo: "+u.getPath());
                if (!imagenes.contains(u))
                    imagenes.add(u);
            }
        sp = getPreferences(MODE_PRIVATE);
        btnCamera = findViewById(R.id.floating_menu_btn_camera);
        btnGallery = findViewById(R.id.floating_menu_btn_gallery);
        rv = findViewById(R.id.imagesview);
        rv.setAdapter(new ViewAdapter(imagenes,getContentResolver()));
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(100);
        rv.setDrawingCacheEnabled(true);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(permissions,REQUEST_PERMISSIONS);
                }
                else{
                    try {
                        crearImagen();
                        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        i.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getBaseContext(),"com.example.tecjerez.ivan.photoeditor.fileprovider",image));
                        startActivityForResult(i,RESULT_CAMERA_IMAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(permissions,REQUEST_PERMISSIONS);
                }
                else{
                    Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i,RESULT_GALLERY_IMAGE);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && resultCode == RESULT_OK){
            if (requestCode == RESULT_GALLERY_IMAGE){
                try {
                    crearImagen();
                    copyInputStreamToFile((getContentResolver().openInputStream(data.getData())),image);
                    u = Uri.fromFile(image);
                    Log.i(TAG,"Uri Cargado de Galeria: "+u.getPath());
                    imagenes.add(u);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (requestCode==RESULT_CAMERA_IMAGE){
                imagenes.add(Uri.fromFile(image));
            }
            if (u!=null)
                rv.getAdapter().notifyItemInserted(imagenes.indexOf(u));

        }
        else if (resultCode == RESULT_CANCELED && requestCode == RESULT_CAMERA_IMAGE){
            imagenes.remove(imagenes.size()-1);
            if (image!=null)
                if(image.exists())
                    image.delete();
        }
    }
    public void crearImagen() throws IOException {
        image = new File(storageDir,getImageName());
        Log.i("IGU",image.getPath());
        FileOutputStream out = new FileOutputStream(image);
        out.flush();
        out.close();
    }
    private void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if ( out != null ) {
                    out.close();
                }
                in.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }
    public String getImageName(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return "photo_" + timeStamp + "_.jpg";
    }
}