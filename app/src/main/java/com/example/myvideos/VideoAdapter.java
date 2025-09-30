package com.example.myvideos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VH> {

    public interface Callback {
        void onClick(VideoModel model);
        void onLongClick(VideoModel model);
    }

    private Context ctx;
    private List<VideoModel> list;
    private Callback callback;

    public VideoAdapter(Context ctx, List<VideoModel> list, Callback callback) {
        this.ctx = ctx;
        this.list = list;
        this.callback = callback;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_video, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        VideoModel m = list.get(position);
        holder.title.setText(m.title);

        String date = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                .format(new Date(m.dateAdded * 1000));
        holder.date.setText(date);

        Glide.with(ctx)
                .load(m.getContentUri())
                .centerCrop()
                .into(holder.thumb);

        holder.itemView.setOnClickListener(v -> callback.onClick(m));
        holder.itemView.setOnLongClickListener(v -> {
            callback.onLongClick(m);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView title, date;
        VH(View v) {
            super(v);
            thumb = v.findViewById(R.id.thumb);
            title = v.findViewById(R.id.title);
            date = v.findViewById(R.id.date);
        }
    }
}
