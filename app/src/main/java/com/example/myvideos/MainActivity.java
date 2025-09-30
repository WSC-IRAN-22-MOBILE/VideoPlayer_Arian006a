package com.example.myvideos;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private List<VideoModel> videos = new ArrayList<>();
    private SharedPreferences prefs;
    private static final String PREF = "my_videos_pref";
    private static final String KEY_PASSWORD = "password_key";
    private static final String KEY_HIDDEN = "hidden_set";

    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }

        // SharedPreferences
        prefs = getSharedPreferences(PREF, Context.MODE_PRIVATE);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new VideoAdapter(this, videos, new VideoAdapter.Callback() {
            @Override
            public void onClick(VideoModel model) {
                Intent i = new Intent(MainActivity.this, VideoPlayerActivity.class);
                i.putExtra("video_id", model.id);
                startActivity(i);
            }

            @Override
            public void onLongClick(VideoModel model) {
                String pass = prefs.getString(KEY_PASSWORD, null);
                if (pass == null) {
                    showSetPasswordDialog(confirmed -> {
                        if (confirmed) addToHidden(model.id);
                    });
                } else {
                    addToHidden(model.id);
                }
            }
        });
        recyclerView.setAdapter(adapter);

        MaterialToolbar top = findViewById(R.id.topAppBar);
        setSupportActionBar(top);
        getSupportActionBar().setTitle("Home");
        top.setOnLongClickListener(v -> {
            String pass = prefs.getString(KEY_PASSWORD, null);
            if (pass == null) {
                Toast.makeText(MainActivity.this, "Set the password first", Toast.LENGTH_SHORT).show();
                return true;
            }
            showEnterPasswordDialog(entered -> {
                if (entered) {
                    Intent i = new Intent(MainActivity.this, HiddenActivity.class);
                    startActivity(i);
                } else {
                    Toast.makeText(MainActivity.this, "Password is wrong", Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        });

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = true;
                    for (Boolean b : result.values()) if (!b) granted = false;
                    if (granted) loadVideos();
                    else Toast.makeText(this, "Access is required to display videos.", Toast.LENGTH_SHORT).show();
                }
        );

        checkPermissionsAndLoad();
    }

    private void checkPermissionsAndLoad() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!perms.isEmpty()) {
            requestPermissionLauncher.launch(perms.toArray(new String[0]));
        } else {
            loadVideos();
        }
    }

    private void loadVideos() {
        videos.clear();
        Set<String> hidden = prefs.getStringSet(KEY_HIDDEN, new HashSet<>());

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
                if (hidden.contains(String.valueOf(id))) continue;
                videos.add(new VideoModel(id, name, date));
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private void addToHidden(long id) {
        Set<String> hidden = new HashSet<>(prefs.getStringSet(KEY_HIDDEN, new HashSet<>()));
        String sid = String.valueOf(id);
        if (hidden.contains(sid)) {
            Toast.makeText(this, "This video has already been hidden.", Toast.LENGTH_SHORT).show();
            return;
        }
        hidden.add(sid);
        prefs.edit().putStringSet(KEY_HIDDEN, hidden).apply();
        Toast.makeText(this, "Video added to hidden.", Toast.LENGTH_SHORT).show();
        loadVideos();
    }

    private void showSetPasswordDialog(OnPasswordResult cb) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_pass, null);

        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        Button btnOk = dialogView.findViewById(R.id.buttonOk);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        dialog.show();

        btnOk.setOnClickListener(v -> {
            String entered = etPassword.getText().toString().trim();

            if (entered.length() < 1) {
                Toast.makeText(this, "The password is not valid", Toast.LENGTH_SHORT).show();
                cb.onResult(false);
                return;
            }

            prefs.edit().putString(KEY_PASSWORD, entered).apply();
            Toast.makeText(this, "Password saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            cb.onResult(true);
        });
    }

    private void showEnterPasswordDialog(OnPasswordResult cb) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_pass, null);

        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        Button btnOk = dialogView.findViewById(R.id.buttonOk);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        dialog.show();

        btnOk.setOnClickListener(v -> {
            String entered = etPassword.getText().toString().trim();
            String savedPass = prefs.getString(KEY_PASSWORD, "");

            if (entered.equals(savedPass)) {
                Toast.makeText(this, "The password is correct", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                cb.onResult(true);
            } else {
                Toast.makeText(this, "Password is wrong", Toast.LENGTH_SHORT).show();
                cb.onResult(false);
            }
        });
    }


    interface OnPasswordResult {
        void onResult(boolean success);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsAndLoad();
    }
}
