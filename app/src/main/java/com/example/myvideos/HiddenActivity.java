package com.example.myvideos;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HiddenActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private List<VideoModel> videos = new ArrayList<>();
    private SharedPreferences prefs;
    private static final String PREF = "my_videos_pref";
    private static final String KEY_HIDDEN = "hidden_set";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }


        prefs = getSharedPreferences(PREF, Context.MODE_PRIVATE);
        recyclerView = findViewById(R.id.recyclerHidden);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new VideoAdapter(this, videos, new VideoAdapter.Callback() {
            @Override
            public void onClick(VideoModel model) {
                Intent i = new Intent(HiddenActivity.this, VideoPlayerActivity.class);
                i.putExtra("video_id", model.id);
                startActivity(i);
            }

            @Override
            public void onLongClick(VideoModel model) {
                // remove from hidden
                Set<String> hidden = new HashSet<>(prefs.getStringSet(KEY_HIDDEN, new HashSet<>()));
                String sid = String.valueOf(model.id);
                if (hidden.contains(sid)) {
                    hidden.remove(sid);
                    prefs.edit().putStringSet(KEY_HIDDEN, hidden).apply();
                    loadHidden();
                }
            }
        });
        recyclerView.setAdapter(adapter);
        loadHidden();


        MaterialToolbar top = findViewById(R.id.hiddenTop);
        setSupportActionBar(top);
        getSupportActionBar().setTitle("Hidden Videos");
        top.setOnLongClickListener(v -> {

            Intent i = new Intent(HiddenActivity.this, MainActivity.class);
                    startActivity(i);

            return true;
        });




    }

    private void loadHidden() {
        videos.clear();
        Set<String> hidden = prefs.getStringSet(KEY_HIDDEN, new HashSet<>());
        if (hidden == null || hidden.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        ContentResolver resolver = getContentResolver();
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED
        };
        Cursor cursor = resolver.query(collection, projection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC");
        if (cursor != null) {
            int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIdx);
                String name = cursor.getString(nameIdx);
                long date = cursor.getLong(dateIdx);
                if (hidden.contains(String.valueOf(id))) {
                    videos.add(new VideoModel(id, name, date));
                }
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }
}
