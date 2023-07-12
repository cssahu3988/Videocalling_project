package com.example.videocalling_project;

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
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.videocalling_project.databinding.ActivityMainBinding;
import com.example.videocalling_project.models.InitiateCallModel;
import com.example.videocalling_project.models.InitiateCallResponse;
import com.example.videocalling_project.service.ReceiveAudio;
import com.example.videocalling_project.utils.RetroitClient;
import com.example.videocalling_project.viewModels.MainActivityViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    MainActivityViewModel model;
    public byte[] buffer;
    public static DatagramSocket audioSocket;
    public static DatagramSocket videoSocket;
    private int port = 50005;

    AudioRecord recorder;

    private int sampleRate = 44100; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean status = true;
    private ReceiveAudio receiveAudio;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Executor executor;
    private Bitmap bitmap;
    private InetAddress inetAddress;
    private Thread receiversPackets;
    private Handler handler;
    private MediaPlayer mediaPlayer ;
    private AudioTrack speaker;
    private DatagramSocket serverSocket;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            init();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        observe();
    }

    private void init() throws SocketException {
        handler = new Handler();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        model = new ViewModelProvider(this).get(MainActivityViewModel.class);
        binding.setModel(model);
        binding.setLifecycleOwner(this);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        executor = Executors.newFixedThreadPool(2);
        cameraProviderFuture.addListener(()->{
            try {
                ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();
                bindPreview(processCameraProvider);
            }catch (Exception e){
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
        serverSocket = new DatagramSocket();
    }

    private void bindPreview(ProcessCameraProvider processCameraProvider) {
        binding.previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        Preview preview = new Preview.Builder().setTargetRotation(Surface.ROTATION_90).setTargetResolution(new Size(200*3,130*3)).build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // enable the following line if RGBA output is needed.
                        //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setTargetResolution(new Size(200*3, 130*3))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                //Log.d("TAG", "analyze: rotation:"+rotationDegrees);
                bitmap = imageProxy.toBitmap();
                if (receiveAudio !=null){
                    receiveAudio.setBitmap(bitmap);
                }
                imageProxy.close();
            }
        });

        Camera camera = processCameraProvider.bindToLifecycle(this,cameraSelector,imageAnalysis,preview);
    }

    private void observe() {
        model.refresh.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    getIpAddress();
                }
            }
        });
        model.call.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    startAudioStreaming();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startVideoStreaming();
                            receiverSideDataPackets();
                        }
                    },1000);
                }
            }
        });
        model.cancelCall.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean){
                    status = false;
                    if (recorder!=null){
                        recorder.release();
                    }
                    if (handler!=null){
                        handler.removeCallbacksAndMessages(null);
                    }
                    if (receiversPackets!=null){
                        receiversPackets.interrupt();
                    }
                }
            }
        });
        model.receive.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean==null){
                    return;
                }
                if (aBoolean){
                    try {
                        socket();
                    } catch (SocketException e) {
                        throw new RuntimeException(e);
                    }
                    receiveAudio = new ReceiveAudio(MainActivity.this,model.image);
                }
                else{
                    if (receiveAudio !=null){
                         receiveAudio.stop();
                    }
                }
            }
        });
        model.audio_port.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (!s.isEmpty()){
                    InitiateCallModel m = new InitiateCallModel(model.user_name.getValue(), model.ipAddress.getValue(), s, model.calleeIpAddress.getValue());
                    RetroitClient.getInstance().getRetrofitAPI().initiateCall(m).enqueue(new Callback<InitiateCallResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<InitiateCallResponse> call, @NonNull Response<InitiateCallResponse> response) {
                            Toast.makeText(MainActivity.this, ""+response.body(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<InitiateCallResponse> call, Throwable t) {

                        }
                    });
                }
            }
        });
    }


    /***methods***/
    private void getIpAddress() {
        Context context = getApplicationContext();
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        model.ipAddress.postValue(ip);
    }

    public void startAudioStreaming() {
        Thread streamThread = new Thread(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                try {

                    byte[] buffer = new byte[minBufSize];

                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName(model.calleeIpAddress.getValue());

                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 10);

                    recorder.startRecording();

                    startListeningAudioFromReceiver();


                    while(status) {
                        //reading data from MIC into buffer
                        minBufSize = recorder.read(buffer, 0, buffer.length);

                        //putting buffer in the packet
                        packet = new DatagramPacket (buffer,buffer.length,destination,port);

                        socket().send(packet);
                        //System.out.println("MinBufferSize: " +minBufSize);
                    }


                } catch(UnknownHostException e) {
                    Log.e("VS", "UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("VS", "IOException");
                }
            }

        });
        streamThread.start();
    }

    private void startListeningAudioFromReceiver() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    mediaPlayer = new MediaPlayer();
                    byte[] receiveData = new byte[minBufSize];
                    byte[] sendData = new byte[minBufSize];
                    speaker = new AudioTrack(AudioManager.STREAM_VOICE_CALL,sampleRate,channelConfig,audioFormat,minBufSize,AudioTrack.MODE_STREAM);
                    speaker.play();
                    while(true)
                    {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);
                        speaker.write(receivePacket.getData(),0,minBufSize);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    public void startVideoStreaming(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executor.execute(() -> {
                    if (bitmap==null){
                        return;
                    }
                    MainActivity.this.runOnUiThread(MainActivity.this::startVideoStreaming);
                    MainActivity.this.runOnUiThread(()->{
                        byte[] data;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Log.d("TAG", "run: "+"width:"+bitmap.getWidth()+" height:"+bitmap.getHeight()+"byte count:"+bitmap.getByteCount());
                        RotateBitmap(bitmap,-90).compress(Bitmap.CompressFormat.JPEG,45,baos);
                        data = baos.toByteArray();
                        Log.d("TAG", "run: compressed:"+data.length);
                        sendVideoStream(data);
                    });
                });
            }
        },50);
    }

    private void sendVideoStream(byte[] data) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramPacket packet = new DatagramPacket(data,data.length,inetAddress(),50006);
                    socket().send(packet);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void receiverSideDataPackets() {
        receiversPackets = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket receiversPackets = new DatagramSocket(50007);
                    byte[] data = new byte[30000];
                    while (true){
                        DatagramPacket packet = new DatagramPacket(data,data.length);
                        receiversPackets.receive(packet);
                        Bitmap b = BitmapFactory.decodeByteArray(packet.getData(),0,packet.getLength());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.imageView.setImageBitmap(b);
                            }
                        });
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        receiversPackets.start();
    }

    private DatagramSocket socket() throws SocketException {
        if (audioSocket ==null){
            audioSocket = new DatagramSocket();
            model.audio_port.postValue(String.valueOf(audioSocket.getPort()));
        }
        return audioSocket;
    }

    private InetAddress inetAddress() throws UnknownHostException {
        if (inetAddress==null){
            inetAddress = InetAddress.getByName(model.calleeIpAddress.getValue());
        }
        return inetAddress;
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

    @Override
    protected void onDestroy() {
        receiveAudio.stop();
        if (receiversPackets!=null){
            receiversPackets.interrupt();
        }
        super.onDestroy();
    }
}