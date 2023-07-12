package com.example.videocalling_project.viewModels;

import android.app.Application;
import android.graphics.Bitmap;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.videocalling_project.models.InitiateCallResponse;
import com.example.videocalling_project.repositories.MainActivityRepo;

public class MainActivityViewModel extends ViewModel {
    public MutableLiveData<String> ipAddress = new MutableLiveData<>();
    public MutableLiveData<String> calleeIpAddress = new MutableLiveData<>();
    public MutableLiveData<Boolean> call = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> refresh = new MutableLiveData<>(true);
    public MutableLiveData<Boolean> cancelCall = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> receive = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> previewView = new MutableLiveData<>(true);
    public MutableLiveData<Bitmap> image = new MutableLiveData<>(null);
    public MutableLiveData<String> user_name = new MutableLiveData<>(null);
    public MutableLiveData<String> audio_port = new MutableLiveData<>(null);
    public LiveData<InitiateCallResponse> initiateCallResponseLiveData = mainActivityRepo().getInitiateCallResponseMutableLiveData();


    private MainActivityRepo mainActivityRepo;



    public void call(){
        call.postValue(true);
    }
    public void refresh(){
        refresh.postValue(true);
    }
    public void cancelCall(){
        cancelCall.postValue(true);
    }
    public void receive(){
        receive.postValue(Boolean.FALSE.equals(receive.getValue()));
    }



    private MainActivityRepo mainActivityRepo(){
        if (mainActivityRepo==null){
            mainActivityRepo = new MainActivityRepo(null);
        }
        return mainActivityRepo;
    }



}
