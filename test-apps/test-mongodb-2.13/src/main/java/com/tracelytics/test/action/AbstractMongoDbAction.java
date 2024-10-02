package com.tracelytics.test.action;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;


public abstract class AbstractMongoDbAction extends ActionSupport implements Preparable {
    protected static final String TEST_COLLECTION = "testCollection";
    protected static final String TEST_DB = "testDb";
	protected MongoClient mongoClient;
    protected static Logger logger = Logger.getLogger(AbstractMongoDbAction.class);
    
    private static final List<ServerAddress> HOSTS = new ArrayList<ServerAddress>(); 
    
    static {
    	try {
//			HOSTS.add(new ServerAddress("localhost"));
			HOSTS.add(new ServerAddress("127.0.0.1"));
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage(), e);
		}
    	initializeDb();
    	
    }
    
    private List<String> extendedOutput = null;
    
    protected AbstractMongoDbAction() {
    	mongoClient = new MongoClient(HOSTS);
		
    }
    
    protected DBCollection getCollection() {
    	return mongoClient.getDB(TEST_DB).getCollection(TEST_COLLECTION);
    }
    
    protected DB getDb() {
    	return mongoClient.getDB(TEST_DB);
    }

	private static void initializeDb() {
		//nothing is needed...		
	}
	
	public List<String> getExtendedOutput() {
		return extendedOutput;
	}
	
	public void setExtendedOutput(List<String> extendedOutput) {
		this.extendedOutput = extendedOutput;
	}
	
	public void appendExtendedOutput(String text) {
		if (extendedOutput == null) {
			extendedOutput = new LinkedList<String>();
		}
		extendedOutput.add(text);
	}
	
	
	@Override
	public void prepare() throws Exception {
		extendedOutput = null; //clear the output		
	}
	
	
	 /**
     * Take note this could take up a fair amount of time for the output hence might screw the trace result
     * @param cursor
     */
    protected static void printCursorToLogger(DBCursor cursor) {
    	if (cursor.hasNext()) {
    		logger.info("Found documents:");
    		
    		List<String> result = getStringsFromCursor(cursor);
    		for (String entry : result) {
    			logger.info(entry);
    		}
    	} else {
    		logger.info("Nothing found");
    	}
    }
    
    /**
     * Print the cursor content to the extendedOutput section, which will be in the "console" div of the page
     * @param cursor
     */
    protected void printCursorToExtendedOutput(DBCursor cursor) {
    	List<String> result = getStringsFromCursor(cursor);
    	
    	setExtendedOutput(new LinkedList<String>(result));
    }
    
    
    protected static void iterateCursor(DBCursor cursor) {
    	while (cursor.hasNext()) {
    		cursor.next();
    	}
    }
    
    private static List<String> getStringsFromCursor(DBCursor cursor) {
    	List<String> result = new LinkedList<String>();
    	for (Object entry : cursor) {
    		result.add(entry.toString());
		}
    	
    	return result;
    }
    
}
