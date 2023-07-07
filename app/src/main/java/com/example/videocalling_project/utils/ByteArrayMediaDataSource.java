package com.example.videocalling_project.utils;

import android.annotation.SuppressLint;
import android.media.MediaDataSource;

import com.google.common.primitives.Bytes;

import java.io.IOException;

public class ByteArrayMediaDataSource extends MediaDataSource {

    private byte[] data;

    public ByteArrayMediaDataSource(byte []data) {
        assert data != null;
        this.data = data;
    }

    @SuppressLint("CheckResult")
    public void addData(byte[] packet){
        data = Bytes.concat(data,packet);
    }

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        if (position >= data.length) return -1;

        int endPosition = (int)position + size;
        int size2 = size;
        if (endPosition > data.length)
            size2 -= endPosition - data.length;
        System.arraycopy(data, (int)position, buffer, offset, size2);
        return size;
    }

    @Override
    public long getSize() throws IOException {
        return data.length;
    }

    @Override
    public void close() throws IOException {
        // Nothing to do here
    }
}
