package com.example.videocalling_project.repositories;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.videocalling_project.models.InitiateCallModel;
import com.example.videocalling_project.models.InitiateCallResponse;
import com.example.videocalling_project.utils.Constants;
import com.example.videocalling_project.utils.RetrofitAPI;
import com.example.videocalling_project.utils.RetroitClient;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivityRepo {
    private final Context context;
    private RetrofitAPI retrofitAPI;
    private final MutableLiveData<InitiateCallResponse> initiateCallResponseMutableLiveData = new MutableLiveData<>();

    public MainActivityRepo(Context context){
        this.context = context;
        init();
    }

    private void init() {
        retrofitAPI = RetroitClient.getInstance().getRetrofitAPI();
        initiateCall(new InitiateCallModel("sonu","192.168.0.100","192.168.0.101","5000","5000"));
    }

    public void initiateCall(InitiateCallModel model){
        retrofitAPI.initiateCall(model).enqueue(new Callback<InitiateCallResponse>() {
            @Override
            public void onResponse(@NonNull Call<InitiateCallResponse> call, @NonNull Response<InitiateCallResponse> response) {
                assert response.body() != null;
                Log.d(Constants.TAG, "onResponse: "+response.body().getMsg());
                initiateCallResponseMutableLiveData.postValue(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<InitiateCallResponse> call, @NonNull Throwable t) {
                Log.d(Constants.TAG, "onFailure: "+ t);
            }
        });
    }

    public LiveData<InitiateCallResponse> getInitiateCallResponseMutableLiveData() {
        return initiateCallResponseMutableLiveData;
    }
}
