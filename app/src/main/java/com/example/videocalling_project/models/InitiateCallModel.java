package com.example.videocalling_project.models;

public class InitiateCallModel {
    String user_name,own_ip,own_port,receiver_user_name;

    public InitiateCallModel(String user_name, String own_ip, String own_port,String receiver_user_name) {
        this.user_name = user_name;
        this.own_ip = own_ip;
        this.own_port = own_port;
        this.receiver_user_name = receiver_user_name;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getOwn_ip() {
        return own_ip;
    }

    public void setOwn_ip(String own_ip) {
        this.own_ip = own_ip;
    }

    public String getOwn_port() {
        return own_port;
    }

    public void setOwn_port(String own_port) {
        this.own_port = own_port;
    }

    public String getReceiver_user_name() {
        return receiver_user_name;
    }

    public void setReceiver_user_name(String receiver_user_name) {
        this.receiver_user_name = receiver_user_name;
    }
}
