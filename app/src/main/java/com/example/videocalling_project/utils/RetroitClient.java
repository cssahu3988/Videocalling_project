package com.example.videocalling_project.utils;

import android.content.Context;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetroitClient {
    private RetrofitAPI retrofitAPI;
    private static RetroitClient instance;
    private RetroitClient(){
        Retrofit retrofit = new Retrofit.Builder().baseUrl(Constants.baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitAPI = retrofit.create(RetrofitAPI.class);
    }
    public static synchronized RetroitClient getInstance(){
        if (instance==null){
            instance = new RetroitClient();
        }
        return instance;
    }

    public RetrofitAPI getRetrofitAPI() {
        return retrofitAPI;
    }
}
