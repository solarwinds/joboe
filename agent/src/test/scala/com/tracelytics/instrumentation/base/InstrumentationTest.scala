package com.tracelytics.instrumentation.base

import com.tracelytics.joboe.EventImpl
import com.tracelytics.joboe.ReporterFactory
import org.scalatest.BeforeAndAfterEach
import com.tracelytics.joboe.Context
import com.tracelytics.joboe.TestReporter
import com.tracelytics.ExpectedEvent
import com.tracelytics.ValueValidator
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import scala.collection.JavaConversions._
import InstrumentationTest.IGNORED_ENTRY_KEYS
import InstrumentationTest.MAX_STRING_DISPLAY_LENGTH
import com.tracelytics.joboe.TestReporter.DeserializedEvent

class InstrumentationTest extends FlatSpec with Matchers with BeforeAndAfterEach {
  import InstrumentationTest._
  
  InstrumentationTest //trigger the companion object
  
  val field = classOf[EventImpl].getDeclaredField("DEFAULT_REPORTER")
  field.setAccessible(true);
  
  val reporter = ReporterFactory.getInstance().buildTestReporter(false);
  val originalReporter = field.get(null)
  
  override def beforeEach() {
    Context.startTrace(); //initialize the metadata
    field.set(null, reporter)
    super.beforeEach() // To be stackable, must call super.beforeEach
  }

  override def afterEach() {
    try super.afterEach() // To be stackable, must call super.afterEach
    finally {
      Context.clearMetadata()
      reporter.reset(); //clear the events
      
      field.set(null, originalReporter)
    } 
  }
    
  def assertEvents(expectedEvents : List[ExpectedEvent]) {
    val sentEvents = reporter.getSentEvents();

    assert(expectedEvents.size == sentEvents.size)
    for (i <- 0 until expectedEvents.size) {
      val expectedEvent = expectedEvents.get(i)
      val sentEvent = sentEvents.get(i)
      val sentEntries = sentEvent.getSentEntries()
            
            
      //remove the ignored values
      sentEntries.keySet().removeAll(IGNORED_ENTRY_KEYS)
      val expectedEntries = expectedEvent.getExpectedEntries()
            
      assert(expectedEntries.size == sentEntries.size, "Size of event with index [" + i + "]")
      for (requiredKey <- expectedEntries.keySet()) {
         assert(sentEntries.containsKey(requiredKey), "Cannot find the key [" + requiredKey + "] in the event with index [" + i + "]")
      }
            
      //make sure all the required entries have same values
      for (expectedEntry <- expectedEntries.entrySet()) {
        val key = expectedEntry.getKey();
        val expectedValue = expectedEntry.getValue();
        val sentValue = sentEntries.get(key);
              
        var expectedValueString =
          if (expectedValue == null) {
            null;
          } else if (expectedValue.isInstanceOf[ValueValidator[Any]]) {
            expectedValue.asInstanceOf[ValueValidator[Any]].getValueString()
          } else{
            expectedValue.toString();
          }
                
        var sentValueString = 
          if (sentValue != null) { sentValue.toString() } else { null }
        
        if (expectedValueString != null && expectedValueString.length() > MAX_STRING_DISPLAY_LENGTH) {
          expectedValueString = truncateString(expectedValueString);
        }
                
        if (sentValueString != null && sentValueString.length() > MAX_STRING_DISPLAY_LENGTH) {
          sentValueString = truncateString(sentValueString);
        }
  
        val isValid =
          if (expectedValue == null) {
            sentValue == null
          } else if (expectedValue.isInstanceOf[ValueValidator[Any]]) {
            expectedValue.asInstanceOf[ValueValidator[Any]].isValid(sentValue);
          } else {
            expectedValue.equals(sentValue);
          }
                
        assert(isValid, "Sent event value does not match the expected value of key [" + key + "] in the event with index [" + i + "], expected [" + expectedValueString + "] found [" + sentValueString + "]")
      }
    }
  }
  
  def getSentEvents(filterJmx : Boolean = true) : List[DeserializedEvent] = {
    if (filterJmx) {
      reporter.getSentEvents().filter { event =>
        val sentEntries = event.getSentEntries
        !sentEntries.containsKey("__Init") && (sentEntries.get("Layer") == null || sentEntries.get("Layer") != "JMX")
      }.toList
    } else {
      reporter.getSentEvents().toList
    }
  }
    
    
   def truncateString(expectedValueString : String) : String = {
        val truncatedString = new StringBuffer(expectedValueString.substring(0, MAX_STRING_DISPLAY_LENGTH));
        truncatedString.append("(truncated " + (expectedValueString.length() - MAX_STRING_DISPLAY_LENGTH) + " character(s)");
        
        truncatedString.toString();
    }
    
    def resetReporter() {
        reporter.reset()
    }
}

object InstrumentationTest {
  val IGNORED_ENTRY_KEYS = Set("X-Trace-Header", "X-Trace", "X-TV-Meta", "PID", "Edge", "Hostname", "Timestamp", "Timestamp_u", "TID") //Event keys that can be ignored in the validation
  val IGNORED_VALUE_KEYS = Set("Backtrace") //Event value that can be ignored in the validation. Though the existence of the key should still be verified
  val MAX_STRING_DISPLAY_LENGTH = 1000;
}