package com.example.checkevemaster.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.checkevemaster.R;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GalleryRecycler extends RecyclerView.Adapter<GalleryRecycler.ImageHolder> {

    private List<GalleryModel> list;
    private Context context;
    private clicks cli;

    public GalleryRecycler(Context context, List<GalleryModel> list, clicks cli) {
        this.cli = cli;
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.gallery_recycler, parent, false);
        return new ImageHolder(view, cli);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageHolder holder, final int position) {
        GalleryModel model=list.get(position);
        String image=model.getImageURL();
        Glide.with(context).load(image).into(holder.imageView);
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cli.onImageClick(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ImageHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView imageView;
        private clicks cli;

        ImageHolder(@NonNull View itemView, clicks cli) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageRecyclerImageView);
            this.cli = cli;
        }

        @Override
        public void onClick(View v) {
            cli.onImageClick(getAdapterPosition());
        }
    }

    public interface clicks {
        void onImageClick(int index);
    }
}
