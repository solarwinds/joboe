package com.tracelytics.test.hibernate;

import org.hibernate.lob.SerializableBlob;

import javax.sql.rowset.serial.SerialBlob;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestSerialize {
    public static void main(String[] args) throws Exception {
        Files.deleteIfExists(Paths.get("blob"));

        SerializableBlob blob = new SerializableBlob(new SerialBlob("abc".getBytes()));
        FileOutputStream fileOut =
                new FileOutputStream("blob");
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(blob);
        out.close();
        fileOut.close();
        System.out.printf("Serialized data is blob");
    }
}
