package com.example.myvideos;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.MediaStore;

public class VideoModel {
    public long id;
    public String title;
    public long dateAdded; // seconds since epoch

    public VideoModel(long id, String title, long dateAdded) {
        this.id = id;
        this.title = title;
        this.dateAdded = dateAdded;
    }

    public Uri getContentUri() {
        return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
    }
}
