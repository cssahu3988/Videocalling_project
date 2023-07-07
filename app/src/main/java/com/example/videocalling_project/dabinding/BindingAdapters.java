package com.example.videocalling_project.dabinding;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

public class BindingAdapters {
    @BindingAdapter("src")
    public static void src(View view, Bitmap bitmap){
        ((ImageView)view).setImageBitmap(bitmap);
    }
}
