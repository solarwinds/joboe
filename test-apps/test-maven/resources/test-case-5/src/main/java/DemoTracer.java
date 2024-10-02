import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;
public class DemoTracer {
    public static void main(String[] args) {
        TraceEvent event = Trace.startTrace("IBakalov-PC");
        event.report();
        for (int i=0; i<5; i++) {
            TraceEvent infoEvent = Trace.createInfoEvent("IBakalov-PC");
            infoEvent.addInfo("Component ID", "Maven Demo");
            infoEvent.addInfo("Event ID", "Event number: "+i);
            infoEvent.report();
        }
        String s = Trace.endTrace("IBakalov-PC");
        System.out.println("Trace events sent.");
    }
}