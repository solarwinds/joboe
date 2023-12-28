package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.*;

public class ExecUtils {
    private static final Logger logger = LoggerFactory.getLogger();
    private static final int EXEC_TIMEOUT = 5000; //5 seconds for exec
    /**
     * Executes the command and returns the result as string
     * @param command
     * @throws Exception  if the exec command failed to execute
     * @return
     */
    public static String exec(String command, String newLine) throws Exception {
        ExecutorService executorService = Executors.newCachedThreadPool(DaemonThreadFactory.newInstance("exec"));

        try {
            Process process = Runtime.getRuntime().exec(command);
            process.getOutputStream().close(); //to indicate that we do not want to write anything to the process input, for powershell this is necessary otherwise it will hang

            Future<String> errorStreamFuture = executorService.submit(new ReadStreamCallable(process.getErrorStream(), newLine));
            Future<String> inputStreamFuture = executorService.submit(new ReadStreamCallable(process.getInputStream(), newLine));

            String errorResult = errorStreamFuture.get(EXEC_TIMEOUT, TimeUnit.SECONDS);
            String standardResult = inputStreamFuture.get(EXEC_TIMEOUT, TimeUnit.SECONDS);

            if (!errorResult.isEmpty()) {
                logger.debug("exec " + command + " output to error stream : " + errorResult);
            }

            return standardResult;
        } finally {
            executorService.shutdown();
        }
    }

    public static String exec(String command) throws Exception {
        return exec(command, null);
    }

    private static class ReadStreamCallable implements Callable<String> {
        private final InputStream inputStream;
        private String newLine;
        private ReadStreamCallable(InputStream inputStream) {
            this.inputStream = inputStream;
        }
        private ReadStreamCallable(InputStream inputStream, String newLine) {
            this.inputStream = inputStream;
            this.newLine = newLine;
        }

        @Override
        public String call() {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = bufferedReader.readLine()) != null) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    stringBuilder.append(line);
                    if (newLine != null) {
                        stringBuilder.append(newLine);
                    }
                }

                return stringBuilder.toString();
            } catch (IOException ex) {
                logger.warn("exec failed with: " + ex.getMessage());
                return null;
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        logger.warn("Failed to close reader for exec");
                    }
                }
            }
        }
    }
}
