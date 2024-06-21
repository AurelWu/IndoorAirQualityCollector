package com.aurelwu.indoorairqualitycollector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;



public class DataArrayBuilder
{
    public static byte[] packDataRequestCO2History(short startIndex) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        try {
            byte header = 0x61;
            byte co2ID = 0x04;
            dataOutputStream.writeByte(header);     // Write 1 byte
            dataOutputStream.writeByte(co2ID);      // Write 1 byte
            dataOutputStream.writeShort(Short.reverseBytes(startIndex)); // Write 2 bytes (little-endian)

            byte[] data = byteArrayOutputStream.toByteArray();
            System.out.println("Sent data: " + bytesToHex(data));
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                dataOutputStream.close();
                byteArrayOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
