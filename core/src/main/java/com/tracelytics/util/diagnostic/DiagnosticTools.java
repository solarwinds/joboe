package com.tracelytics.util.diagnostic;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tracelytics.joboe.config.*;
import com.tracelytics.joboe.rpc.Client;
import com.tracelytics.joboe.rpc.ClientException;
import com.tracelytics.joboe.rpc.ResultCode;
import com.tracelytics.joboe.rpc.RpcClientManager;
import com.tracelytics.joboe.rpc.RpcClientManager.OperationType;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.ServiceKeyUtils;

/**
 * Diagnostic tools that verify service key and connectivity by connecting to the collector server
 * 
 * This is packaged into the java agent jar and can be invoked as:
 * 
 * java -cp solarwinds-apm-agent.jar com.tracelytics.util.diagnostic.DiagnosticTools [optional parameters]
 * 
 * For example: java -cp solarwinds-apm-agent.jar com.tracelytics.util.diagnostic.DiagnosticTools service_key=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef:my-service timeout=10000 log_file=solarwinds-apm-diagnostics.log
 * 
 * @author Patson
 *
 */
public class DiagnosticTools {
    private static final int DEFAULT_TIMEOUT = 8000; //8 seconds
    private static final Logger logger = LoggerFactory.getLogger();
    private static final int APPOPTICS_API_TOKEN_LENGTH = 64;
    private static final int SWOKEN_API_TOKEN_LENGTH = 71;
    
    private static int timeout = DEFAULT_TIMEOUT;
    
    public static void main(String[] args) {
        Result result = null;
        try {
            String serviceKey = null;
            if (args.length == 0) {
                logger.info("No parameters string is provided, using defaults");
                printUsage();
            } else {
                Map<ParameterKey, String> parameters = parseParameters(args);
                String logFileString = parameters.get(ParameterKey.LOG_FILE);
                
                if (logFileString != null) {
                    PrintStream logFileStream = getLogFile(logFileString);
                    if (logFileStream != null) {
                        System.setOut(logFileStream);
                        System.setErr(logFileStream);
                    }
                }

                serviceKey = parameters.get(ParameterKey.SERVICE_KEY);
                String timeoutFromParameter = parameters.get(ParameterKey.TIMEOUT); 
                if (timeoutFromParameter != null) {
                    try {
                        timeout = Integer.parseInt(timeoutFromParameter);
                    } catch (NumberFormatException e) {
                        logger.warn("Cannot parse timeout from argument [" + timeoutFromParameter + "]. Using default " + DEFAULT_TIMEOUT + " ms instead.");
                    }
                }
            } 

            logger.info("Using service key " + ServiceKeyUtils.maskServiceKey(serviceKey));
            ConfigManager.initialize(new ConfigContainer());
            result = testServiceKey(serviceKey);
        } catch (InvalidArgumentsException e) {
            logger.warn(e.getMessage());
            printUsage();
            result = Result.invalidArguments(args);
        } finally {
            if (result != null) {
                logger.info(result.toString());
                System.exit(result.resultType.exitCode);
            }
        }
    }
    
    private static void printUsage() {
        logger.info("Usage example : java -cp solarwinds-apm-agent.jar com.tracelytics.util.diagnostic.DiagnosticTools service_key=YourSolarWindsApiToken:YourServiceName");
        logger.info("All program parameters are optional and in format of [key]=[value], available parameters are:");
        logger.info("service_key : Service key to be used for the diagnostics");
        logger.info("timeout     : Max time to wait for the diagnostics to finish");
        logger.info("log_file    : File location to print the logs to, could either be relative or absolute path");
    }

    private enum ParameterKey {
        SERVICE_KEY("service_key"),
        TIMEOUT("timeout"),
        LOG_FILE("log_file"),
        AGENT_CONFIG("config");
        
        private final String key;
        private final static Map<String, ParameterKey> map = new HashMap<String, DiagnosticTools.ParameterKey>();
        static {
            for (ParameterKey parameter : ParameterKey.values()) {
                map.put(parameter.key, parameter);
            }
        }
        
        private ParameterKey(String key) {
            this.key = key;
        }
        
        private static ParameterKey fromKey(String key) {
            return map.get(key);
        }
    }
    
    
    
    private static PrintStream getLogFile(String logFileString) {
        try {
            return new PrintStream(logFileString);
        } catch (FileNotFoundException e) {
            logger.warn("Cannot write to log file [" + logFileString + "]");
            return null;
        }
    }



    private static Map<ParameterKey, String> parseParameters(String[] argments) throws InvalidArgumentsException {
        Map<ParameterKey, String> parameters = new HashMap<ParameterKey, String>();
        for (String keyValue : argments) {
            int separator = keyValue.indexOf('=');
            if (separator <= 0) {
                throw new InvalidArgumentsException("parameter entry [" + keyValue + "] is unexpected, should be in the form of [key]=[value]");
            } else {
                String keyString = keyValue.substring(0, separator);
                ParameterKey key = ParameterKey.fromKey(keyString);
                if (key == null) {
                    throw new InvalidArgumentsException("parameter key [" + keyString + "] is not a valid parameter key");
                }
                parameters.put(key, keyValue.substring(separator + 1));
            }
        }
         
        return parameters;
    }



    private static class Result {
        private final ResultType resultType;
        private final String message;
        
        private static final Result OK = new Result(ResultType.OK, "Diagnostics successful");
        private static final Result TIMEOUT_FAILED = new Result(ResultType.CONNECTION_FAILURE, "Failed to get response from SolarWinds server after waiting for " + timeout + " milliseconds");
        private static final Result TRY_LATER = new Result(ResultType.TRY_LATER, "SolarWinds server returned non-ok status code TRY_LATER");
        private static final Result LIMIT_EXCEEDED = new Result(ResultType.LIMIT_EXCEEDED, "SolarWinds server returned non-ok status code LIMIT_EXCEEDED");
        
        private Result(ResultType resultType) {
            this(resultType, null);
        }
        
        private Result(ResultType resultType, String message) {
            this.resultType = resultType;
            this.message = message;
        }
        
        private static Result ok() {
            return OK;
        }
        
        private static Result invalidServiceKey(String serviceKey, String warning) {
            return new Result(ResultType.INVALID_SERVICE_KEY, "Service key [" + ServiceKeyUtils.maskServiceKey(serviceKey) + "] is invalid : " + warning);
        }
        
        private static Result connectionFailure(ClientException e) {
            StringBuilder message = new StringBuilder("Failed to connect to AppOptics collector due to connection problem.");
            
            if (e.getMessage() != null) {
                message.append(" Reason: " + e.getMessage());
            }
            
            return new Result(ResultType.CONNECTION_FAILURE_FATAL, message.toString());
        }
        
        private static Result timeout() {
            return TIMEOUT_FAILED;
        }
        
        
        private static Result unexpectedException(Exception e) {
            StringBuilder message = new StringBuilder("Failed to connect to SolarWinds collector due to unexpected exception.");
            
            if (e != null && e.getMessage() != null) {
                message.append(" Reason: " + e.getMessage());
            }
            
            return new Result(ResultType.UNKNOWN_ERROR, message.toString());
        }
        
        private static Result invalidFormatArgument(String argumentKey, String argumentValue) {
            return new Result(ResultType.INVALID_FORMAT_ARGUMENT, "Invalid format for argument [" + argumentKey + "] with value [" + argumentValue + "]");
        }
        
        private static Result invalidArguments(String[] arguments) {
            String argumentString = "";
            for (String argument : arguments) {
                argumentString += argument + " ";
            }
            argumentString = argumentString.trim();
            return new Result(ResultType.INVALID_ARGUMENTS, "[" + argumentString + "] is not a valid argument");
        }
        
        private static Result tryLater() {
            return TRY_LATER;
        }
        
        private static Result limitExceeded() {
            return LIMIT_EXCEEDED;
        }
        
        @Override
        public String toString() {
            return "[" + resultType + "]" + (message != null ? " message: " + message : "");
        }
        
    }
    
    private enum ResultType {
        OK(0), UNKNOWN_ERROR(101), INVALID_FORMAT_ARGUMENT(102), INVALID_ARGUMENTS(103), INVALID_SERVICE_KEY(104), CONNECTION_FAILURE(105), TRY_LATER(106), LIMIT_EXCEEDED(107), CONNECTION_FAILURE_FATAL(108);
        
        private final int exitCode;
        
        private ResultType(int exitCode) {
            this.exitCode = exitCode;
        }
    }
    
    private static Result testServiceKey(String serviceKey) {
        if (!isValidServiceKeyFormat(serviceKey)) {
            return Result.invalidFormatArgument(ParameterKey.SERVICE_KEY.key, serviceKey);
        }
        
        Client client = null;
        try {
            client = RpcClientManager.getClient(OperationType.STATUS, serviceKey);
            com.tracelytics.joboe.rpc.Result rpcCallResult = client.postStatus(generateDiagnosticMessage(), null).get(timeout, TimeUnit.MILLISECONDS);

            if (!"".equals(rpcCallResult.getWarning())) { //warning for postStatus call is likely for service key errors, see https://swicloud.atlassian.net/browse/AO-16547?focusedCommentId=197795
                return Result.invalidServiceKey(serviceKey, rpcCallResult.getWarning());
            }  else if (rpcCallResult.getResultCode() == ResultCode.TRY_LATER) {
                return Result.tryLater();
            } else if (rpcCallResult.getResultCode() == ResultCode.LIMIT_EXCEEDED) { 
                return Result.limitExceeded();
            } else {//all other result code are considered as successful
                return Result.ok();
            }
            
        } catch (ClientException e) {
            return Result.connectionFailure(e);  
        } catch (ExecutionException e) {
            if (e.getCause() instanceof com.tracelytics.joboe.rpc.ClientException) {
                return Result.connectionFailure((ClientException) e.getCause());
            } else {
                return Result.unexpectedException(e);
            }
        } catch (TimeoutException e) {
            return Result.timeout();
        }  catch (Exception e) {
            return Result.unexpectedException(e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
    
    private static boolean isValidServiceKeyFormat(String serviceKey) {
        String[] tokens = serviceKey.split(":", 2);
        if (tokens.length !=  2) {
            logger.warn("Service Key [" + ServiceKeyUtils.maskServiceKey(serviceKey) + "] format is incorrect - not in <API Token>:<Service Name>");
            return false;
        }
        
        String apiToken = tokens[0];
        String serviceName = tokens[1];
        
        if (apiToken.length() != APPOPTICS_API_TOKEN_LENGTH && apiToken.length() != SWOKEN_API_TOKEN_LENGTH) {
            logger.warn("Service Key [" + ServiceKeyUtils.maskServiceKey(serviceKey) + "] format is incorrect - API token is not in " + APPOPTICS_API_TOKEN_LENGTH + " or " + SWOKEN_API_TOKEN_LENGTH + " characters. Found " + apiToken.length() + " character(s)");
            return false;
        }
        
        if ("".equals(serviceName.trim())) {
            logger.warn("Service Name should not be empty in Service Key [" + ServiceKeyUtils.maskServiceKey(serviceKey) + "]");
            return false;
        }
        
        return true;
    }

    private static List<Map<String, Object>> generateDiagnosticMessage() {
        String version = DiagnosticTools.class.getPackage().getImplementationVersion();
        logger.info("Fetched version " + version + " in the diagnostic tool.");
        Map<String, Object> initMessage = new HashMap<String, Object>();
        
        initMessage.put("__Diagnostic", true);
        
        if (version != null) {
            initMessage.put("Java.SolarWindsAPM.Version", version);
        }
        
        initMessage.put("DiagnosticTimestamp", System.currentTimeMillis() / 1000); //seconds since epoch
        
        logger.info("Connectivity check message: " + initMessage);
        
        return Collections.singletonList(initMessage);
        
    }
}
