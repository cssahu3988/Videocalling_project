package com.example.videocalling_project.models;

public class InitiateCallModel {
    String user_name,own_ip,receiver_ip,own_port,receiver_port;

    public InitiateCallModel(String user_name, String own_ip, String receiver_ip, String own_port, String receiver_port) {
        this.user_name = user_name;
        this.own_ip = own_ip;
        this.receiver_ip = receiver_ip;
        this.own_port = own_port;
        this.receiver_port = receiver_port;
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

    public String getReceiver_ip() {
        return receiver_ip;
    }

    public void setReceiver_ip(String receiver_ip) {
        this.receiver_ip = receiver_ip;
    }

    public String getOwn_port() {
        return own_port;
    }

    public void setOwn_port(String own_port) {
        this.own_port = own_port;
    }

    public String getReceiver_port() {
        return receiver_port;
    }

    public void setReceiver_port(String receiver_port) {
        this.receiver_port = receiver_port;
    }
}
