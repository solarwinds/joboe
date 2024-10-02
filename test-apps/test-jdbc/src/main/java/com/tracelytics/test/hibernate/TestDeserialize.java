package com.tracelytics.test.hibernate;

import org.hibernate.lob.SerializableBlob;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class TestDeserialize {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream("blob");
        ObjectInputStream in = new ObjectInputStream(fileIn);
        SerializableBlob blob = (SerializableBlob) in.readObject();
        in.close();
        fileIn.close();

        System.out.println(blob);
    }
}
