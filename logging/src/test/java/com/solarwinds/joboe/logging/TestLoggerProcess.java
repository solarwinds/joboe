package com.solarwinds.joboe.logging;


import java.io.IOException;
import java.nio.file.Paths;

public class TestLoggerProcess {
    //args: logFileName, maxSize, maxBackup, iterationCount, printString
    public static void main(String[] args) throws IOException, InterruptedException {

        String logFileName = args[0];
        int maxSize = Integer.parseInt(args[1]); //in bytes
        int maxBackup = Integer.parseInt(args[2]);
        int iterationCount = Integer.parseInt(args[3]);
        String printString = args[4];

        long start = System.currentTimeMillis();
        FileLoggerStream loggerStream = new FileLoggerStream(Paths.get(logFileName), maxSize, maxBackup);
        try {
            for (int i = 0; i < iterationCount; i++) {
                loggerStream.println(printString);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            loggerStream.close(10);
        }
        long end = System.currentTimeMillis();
        System.out.println("Per operation : " + (end - start) * 1.0 / iterationCount);
    }
}
