package com.example.tecjerez.ivan.photoeditor.controlador;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.tecjerez.ivan.photoeditor.R;
import com.example.tecjerez.ivan.photoeditor.vista.MainActivity;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.MyViewHolder>{
    private List<Uri> listImages;
    private ViewAdapter adapter;
    private int height = 400;
    public ContentResolver cr;
    public int currentPos;
    public ViewAdapter(List<Uri> listImages,ContentResolver cr){
        this.listImages = listImages;
        adapter = this;
        this.cr = cr;
    }
    static class MyViewHolder extends RecyclerView.ViewHolder{
        private ImageView iv;
        private MyViewHolder(@NonNull final View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.img_my_gallery);
        }
    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View tv = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_card,parent,false);
        return new MyViewHolder(tv);
    }
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.iv.setOnClickListener(new MyViewListener(listImages.get(position)));
        try {
            Bitmap b = MediaStore.Images.Media.getBitmap(holder.itemView.getContext().getContentResolver(),listImages.get(position));
            if (b!=null){
                holder.iv.setImageBitmap(Bitmap.createScaledBitmap(b,b.getWidth()/(b.getHeight()/height),height,true));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public int getItemCount() {
        return listImages.size();
    }
    class MyViewListener implements View.OnClickListener{
        private Uri u;
        public MyViewListener(Uri u){
            this.u = u;
        }

        @Override
        public void onClick(View v) {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.setOnMenuItemClickListener(new MyPopupMenuListener(u));
            popup.inflate(R.menu.popup_menu_opencv);
            popup.show();
        }
    }
    class MyPopupMenuListener implements  PopupMenu.OnMenuItemClickListener{
        private Uri u;
        public MyPopupMenuListener(Uri u){
            this.u = u;
        }
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Log.i(MainActivity.TAG,"Soy el item: "+listImages.indexOf(u));
            int filtro = 0;
            switch (item.getItemId()) {
                case R.id.popup_menu_delete_item:
                    if (new File(u.getPath()).delete())
                        Log.i(MainActivity.TAG,"Eliminado");
                    int p = listImages.indexOf(u);
                    listImages.remove(p);
                    adapter.notifyItemRemoved(p);
                    return true;
                case R.id.popup_menu_gray_item:
                    filtro = Imgproc.COLOR_RGB2GRAY;
                    break;
                case R.id.popup_menu_hsv_item:
                    filtro = Imgproc.COLOR_RGB2HSV;
                    break;
                case R.id.popup_menu_lab_item:
                    filtro = Imgproc.COLOR_RGB2Lab;
                    break;
                case R.id.popup_menu_hls_item:
                    filtro = Imgproc.COLOR_RGB2HLS;
                    break;
                default:
                    return false;
            }
            try {
                Bitmap b = MediaStore.Images.Media.getBitmap(cr,u);
                Mat tmp = new Mat(b.getWidth(),b.getHeight(), CvType.CV_8UC1);
                Utils.bitmapToMat(b,tmp);
                Imgproc.cvtColor(tmp,tmp,filtro);
                Utils.matToBitmap(tmp, b);
                FileOutputStream out = new FileOutputStream(u.getPath());
                b.compress(Bitmap.CompressFormat.JPEG, 100, out);
                adapter.notifyItemChanged(listImages.indexOf(u));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
    }
}
