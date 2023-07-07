package com.example.videocalling_project.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import androidx.lifecycle.MutableLiveData;

import com.example.videocalling_project.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ReceiveAudio {
    private final Thread receiverThread;
    private Context context=null;
    private MediaPlayer mediaPlayer;
    private final int channelConfig =  AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private final int sampleRate = 44100;
    private int minBufSize = 3584;
    AudioRecord recorder;
    private AudioTrack speaker;
    private MutableLiveData<Bitmap> image;
    private Thread videoThread;
    private Bitmap bitmap;
    private Executor executor;
    private boolean isAudioStreaming = false;
    private InetAddress destination;

    public ReceiveAudio(Context context, MutableLiveData<Bitmap> image){
        this.context = context;
        this.image = image;
        executor = Executors.newFixedThreadPool(2);
        receiverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    mediaPlayer = new MediaPlayer();
                    DatagramSocket serverSocket = new DatagramSocket(50005);
                    byte[] receiveData = new byte[minBufSize];
                    byte[] sendData = new byte[minBufSize];
                    speaker = new AudioTrack(AudioManager.STREAM_VOICE_CALL,sampleRate,channelConfig,audioFormat,minBufSize,AudioTrack.MODE_STREAM);
                    speaker.play();
                    while(true)
                    {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);
                        if (destination==null){
                            destination=receivePacket.getAddress();
                        }
                        speaker.write(receivePacket.getData(),0,minBufSize);
                        if (!isAudioStreaming){
                            isAudioStreaming=true;
                            startAudioStreaming();
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
        receiverThread.start();
        ReceiveVideo();
    }

    private void startAudioStreaming() {
        executor.execute(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    byte[] buffer = new byte[minBufSize];
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 10);
                    recorder.startRecording();
                    while (isAudioStreaming){
                        minBufSize = recorder.read(buffer, 0, buffer.length);
                        DatagramPacket packet = new DatagramPacket (buffer,buffer.length,destination,50008);
                        socket.send(packet);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }

    public void setBitmap(Bitmap bitmap){
        this.bitmap = bitmap;
    }

    public void ReceiveVideo(){
        videoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket(50006);
                    DatagramSocket socket2 = new DatagramSocket();
                    byte[] data = new byte[30000];
                    while (true){
                        DatagramPacket receivePacket = new DatagramPacket(data,data.length);
                        socket.receive(receivePacket);
                        Bitmap b = BitmapFactory.decodeByteArray(receivePacket.getData(),0,receivePacket.getLength());
                        ((MainActivity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                image.postValue(b);
                            }
                        });
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                            try {
                                byte[] data2;
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                RotateBitmap(bitmap,-90).compress(Bitmap.CompressFormat.JPEG,45,baos);
                                data2 = baos.toByteArray();
                                DatagramPacket packet = new DatagramPacket(data2,data2.length,receivePacket.getAddress(),50007);
                                socket2.send(packet);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
        videoThread.start();
    }

    public static Bitmap RotateBitmap(Bitmap image, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = 200*3;
            height = (int) (width / bitmapRatio);
        } else {
            height = 200*3;
            width = (int) (height * bitmapRatio);
        }
        Bitmap bitmap = Bitmap.createScaledBitmap(image,width,height,true);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public void stop(){
        isAudioStreaming=false;
        receiverThread.interrupt();
        videoThread.interrupt();
    }


}
