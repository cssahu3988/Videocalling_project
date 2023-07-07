package com.example.videocalling_project.utils;

import com.example.videocalling_project.models.InitiateCallModel;
import com.example.videocalling_project.models.InitiateCallResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitAPI {
    @POST("/setUserNamePorts")
    Call<InitiateCallResponse> initiateCall(@Body InitiateCallModel model);
}
