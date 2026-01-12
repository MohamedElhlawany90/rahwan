package com.blueWhale.Rahwan.util;

import java.io.ByteArrayOutputStream;
import java.util.zip.Inflater;

public class ImageUtility {

    public static byte[] compressImage(byte[] data) {

//        Deflater deflater = new Deflater();
//        deflater.setLevel(Deflater.NO_COMPRESSION);
//        deflater.setInput(data);
//        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        // byte[] tmp = new byte[4*1024];
//        while (!deflater.finished()) {
//            int size = deflater.deflate(tmp);
        //   outputStream.write(tmp, 0, size);
        outputStream.write(data, 0, data.length);
        //  }
        try {
            outputStream.close();
        } catch (Exception e) {
        }
        return outputStream.toByteArray();
    }

    public static byte[] decompressImage(byte[] data) {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] tmp = new byte[4 * 1024];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(tmp);
                outputStream.write(tmp, 0, count);
            }
            outputStream.close();
        } catch (Exception exception) {
        }
        return outputStream.toByteArray();
    }
}