package com.example.videocalling_project.models;

import com.google.gson.annotations.SerializedName;

public class InitiateCallResponse {
    @SerializedName("msj")
    private String msg="";

    public InitiateCallResponse(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
