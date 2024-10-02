package com.tracelytics.instrumentation.http.webflow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.config.FlowDefinitionResourceFactory;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.RequestControlContext;
import org.springframework.webflow.test.MockFlowBuilderContext;
import org.springframework.webflow.test.MockRequestControlContext;
import org.springframework.webflow.test.execution.AbstractXmlFlowExecutionTests;

import com.tracelytics.ExpectedEvent;
import com.tracelytics.ValueValidator;
import com.tracelytics.instrumentation.AbstractInstrumentationTest;

public class StateInstrumentationTest extends AbstractInstrumentationTest<StateInstrumentation> {
    

    public void testActionState() throws Exception {
        Flow mockedFlow = new MockedFlowBuilder("action-flow.xml").createFlow();

        RequestControlContext mockContext = new MockRequestControlContext(mockedFlow);
        
        mockedFlow.start(mockContext, new LocalAttributeMap());
        
        List<ExpectedEvent> expectedEvents = new ArrayList<ExpectedEvent>();
        
        ExpectedEvent stateEntryEvent = new ExpectedEvent();
        stateEntryEvent.addInfo("Layer", "webflow");
        stateEntryEvent.addInfo("Label", "info");
        stateEntryEvent.addInfo("Type", "Enter a State");
        stateEntryEvent.addInfo("FlowId", "action-flow");
        stateEntryEvent.addInfo("StateId", "state1");
        stateEntryEvent.addInfo("StateType", "Action State");
        
        expectedEvents.add(stateEntryEvent);
        
        ExpectedEvent expressionEntryEvent = new ExpectedEvent();
        expressionEntryEvent.addInfo("Layer", "webflow-action");
        expressionEntryEvent.addInfo("Language", "java");
        expressionEntryEvent.addInfo("Label", "entry");
        expressionEntryEvent.addInfo("FunctionName", "'finish'");
        expressionEntryEvent.addInfo("FlowId", "action-flow");
        expressionEntryEvent.addInfo("StateId", "state1");
        
        expectedEvents.add(expressionEntryEvent);
        
        ExpectedEvent expressionExitEvent = new ExpectedEvent();
        expressionExitEvent.addInfo("Label", "exit");
        
        expectedEvents.add(expressionExitEvent);
        
        ExpectedEvent stateExitEvent = new ExpectedEvent();
        stateExitEvent.addInfo("Layer", "webflow");
        stateExitEvent.addInfo("Label", "info");
        stateExitEvent.addInfo("Type", "Exit a State");
        stateExitEvent.addInfo("TransitionOn", "finish");
        stateExitEvent.addInfo("FlowId", "action-flow");
        stateExitEvent.addInfo("StateId", "state1");
        stateExitEvent.addInfo("StateType", "Action State");
        
        expectedEvents.add(stateExitEvent);
        
        
        ExpectedEvent endStateEvent = new ExpectedEvent();
        endStateEvent.addInfo("Layer", "webflow");
        endStateEvent.addInfo("Label", "info");
        endStateEvent.addInfo("Type", "Enter a State");
        endStateEvent.addInfo("TransitionOn", "finish");
        endStateEvent.addInfo("FlowId", "action-flow");
        endStateEvent.addInfo("StateId", "state2");
        endStateEvent.addInfo("StateType", "End State");
        
        expectedEvents.add(endStateEvent);
        
        assertEvents(expectedEvents);
    }
    
    
    public void testViewState() throws Exception {
        Flow mockedFlow = new MockedFlowBuilder("view-flow.xml").createFlow();

        RequestControlContext mockContext = new MockRequestControlContext(mockedFlow);
        
        mockedFlow.start(mockContext, new LocalAttributeMap());
        
        List<ExpectedEvent> expectedEvents;
        
        expectedEvents= new ArrayList<ExpectedEvent>();
        
        ExpectedEvent stateEntryEvent = new ExpectedEvent();
        stateEntryEvent.addInfo("Layer", "webflow");
        stateEntryEvent.addInfo("Label", "info");
        stateEntryEvent.addInfo("Type", "Enter a State");
        stateEntryEvent.addInfo("FlowId", "view-flow");
        stateEntryEvent.addInfo("StateId", "state1");
        stateEntryEvent.addInfo("StateType", "View State");
        
        expectedEvents.add(stateEntryEvent);
        
        ExpectedEvent renderEvent = new ExpectedEvent();
        renderEvent.addInfo("Layer", "webflow");
        renderEvent.addInfo("Label", "info");
        renderEvent.addInfo("Type", "Start Rendering a View");
        renderEvent.addInfo("View", new ViewIdValidator("state1"));
        
        expectedEvents.add(renderEvent);
        
        assertEvents(expectedEvents);
        
        //now test on the refresh
        resetReporter();
        
        mockedFlow.resume(mockContext);
        
        expectedEvents= new ArrayList<ExpectedEvent>();
        
        ExpectedEvent refreshEvent = new ExpectedEvent();
        refreshEvent.addInfo("Layer", "webflow");
        refreshEvent.addInfo("Label", "info");
        refreshEvent.addInfo("Type", "Refresh on a View State");
        refreshEvent.addInfo("FlowId", "view-flow");
        refreshEvent.addInfo("StateId", "state1");
        refreshEvent.addInfo("StateType", "View State");
        
        expectedEvents.add(refreshEvent);
      
        assertEvents(expectedEvents);
    }
    
    public void testDecisionState() throws Exception {
        Flow mockedFlow = new MockedFlowBuilder("decision-flow.xml").createFlow();

        RequestControlContext mockContext = new MockRequestControlContext(mockedFlow);
        
        mockedFlow.start(mockContext, new LocalAttributeMap());
        
        List<ExpectedEvent> expectedEvents = new ArrayList<ExpectedEvent>();
        
        //from state1 to state2
        ExpectedEvent stateEntryEvent = new ExpectedEvent();
        stateEntryEvent.addInfo("Layer", "webflow");
        stateEntryEvent.addInfo("Label", "info");
        stateEntryEvent.addInfo("Type", "Enter a State");
        stateEntryEvent.addInfo("FlowId", "decision-flow");
        stateEntryEvent.addInfo("StateId", "state1");
        stateEntryEvent.addInfo("StateType", "Decision State");
        
        expectedEvents.add(stateEntryEvent);
        
        ExpectedEvent expressionEntryEvent = new ExpectedEvent();
        expressionEntryEvent.addInfo("Layer", "webflow-matches");
        expressionEntryEvent.addInfo("Language", "java");
        expressionEntryEvent.addInfo("Label", "entry");
        expressionEntryEvent.addInfo("Expression", "true");
        expressionEntryEvent.addInfo("FlowId", "decision-flow");
        expressionEntryEvent.addInfo("StateId", "state1");
        
        expectedEvents.add(expressionEntryEvent);
        
        ExpectedEvent expressionExitEvent = new ExpectedEvent();
        expressionExitEvent.addInfo("Label", "exit");
        
        expectedEvents.add(expressionExitEvent);
        
        ExpectedEvent stateExitEvent = new ExpectedEvent();
        stateExitEvent.addInfo("Layer", "webflow");
        stateExitEvent.addInfo("Label", "info");
        stateExitEvent.addInfo("Type", "Exit a State");
        stateExitEvent.addInfo("TransitionOn", "yes");
        stateExitEvent.addInfo("FlowId", "decision-flow");
        stateExitEvent.addInfo("StateId", "state1");
        stateExitEvent.addInfo("StateType", "Decision State");
        
        expectedEvents.add(stateExitEvent);
        
        
        //from state2 to state3
        
        stateEntryEvent = new ExpectedEvent();
        stateEntryEvent.addInfo("Layer", "webflow");
        stateEntryEvent.addInfo("Label", "info");
        stateEntryEvent.addInfo("Type", "Enter a State");
        stateEntryEvent.addInfo("TransitionOn", "yes");
        stateEntryEvent.addInfo("FlowId", "decision-flow");
        stateEntryEvent.addInfo("StateId", "state2");
        stateEntryEvent.addInfo("StateType", "Decision State");
        
        expectedEvents.add(stateEntryEvent);
        
        expressionEntryEvent = new ExpectedEvent();
        expressionEntryEvent.addInfo("Layer", "webflow-matches");
        expressionEntryEvent.addInfo("Language", "java");
        expressionEntryEvent.addInfo("Label", "entry");
        expressionEntryEvent.addInfo("Expression", "false");
        expressionEntryEvent.addInfo("FlowId", "decision-flow");
        expressionEntryEvent.addInfo("StateId", "state2");
        
        expectedEvents.add(expressionEntryEvent);
        
        expressionExitEvent = new ExpectedEvent();
        expressionExitEvent.addInfo("Label", "exit");
        
        expectedEvents.add(expressionExitEvent);
        
        
        expressionEntryEvent = new ExpectedEvent();
        expressionEntryEvent.addInfo("Layer", "webflow-matches");
        expressionEntryEvent.addInfo("Language", "java");
        expressionEntryEvent.addInfo("Label", "entry");
        expressionEntryEvent.addInfo("Expression", "*");
        expressionEntryEvent.addInfo("FlowId", "decision-flow");
        expressionEntryEvent.addInfo("StateId", "state2");
        
        expectedEvents.add(expressionEntryEvent);
        
        expressionExitEvent = new ExpectedEvent();
        expressionExitEvent.addInfo("Label", "exit");
        
        expectedEvents.add(expressionExitEvent);
        
        stateExitEvent = new ExpectedEvent();
        stateExitEvent.addInfo("Layer", "webflow");
        stateExitEvent.addInfo("Label", "info");
        stateExitEvent.addInfo("Type", "Exit a State");
        stateExitEvent.addInfo("TransitionOn", "no");
        stateExitEvent.addInfo("FlowId", "decision-flow");
        stateExitEvent.addInfo("StateId", "state2");
        stateExitEvent.addInfo("StateType", "Decision State");
        
        expectedEvents.add(stateExitEvent);
        
        //enter state3
        
        ExpectedEvent endStateEvent = new ExpectedEvent();
        endStateEvent.addInfo("Layer", "webflow");
        endStateEvent.addInfo("Label", "info");
        endStateEvent.addInfo("Type", "Enter a State");
        endStateEvent.addInfo("TransitionOn", "no");
        endStateEvent.addInfo("FlowId", "decision-flow");
        endStateEvent.addInfo("StateId", "state3");
        endStateEvent.addInfo("StateType", "End State");
        
        expectedEvents.add(endStateEvent);
        
        assertEvents(expectedEvents);
    }
    
    
    public void testSubflowState() throws Exception {
        final Flow mockedSubflow = new MockedFlowBuilder("sub-flow.xml").createFlow();
        
        Flow mockedFlow = new MockedFlowBuilder("main-flow.xml"){
            protected void configureFlowBuilderContext(MockFlowBuilderContext builderContext) {
                builderContext.registerSubflow(mockedSubflow);
                super.configureFlowBuilderContext(builderContext);
                
            }
        }.createFlow();

        RequestControlContext mockContext = new MockRequestControlContext(mockedFlow);
        
        mockedFlow.start(mockContext, new LocalAttributeMap());
        
        List<ExpectedEvent> expectedEvents = new ArrayList<ExpectedEvent>();
        
        ExpectedEvent stateEntryEvent = new ExpectedEvent();
        stateEntryEvent.addInfo("Layer", "webflow");
        stateEntryEvent.addInfo("Label", "info");
        stateEntryEvent.addInfo("Type", "Enter a State");
        stateEntryEvent.addInfo("FlowId", "main-flow");
        stateEntryEvent.addInfo("StateId", "state1");
        stateEntryEvent.addInfo("StateType", "Subflow State");
        
        expectedEvents.add(stateEntryEvent);
        
        
        ExpectedEvent stateExitEvent = new ExpectedEvent();
        stateExitEvent.addInfo("Layer", "webflow");
        stateExitEvent.addInfo("Label", "info");
        stateExitEvent.addInfo("Type", "Enter a State");
        stateExitEvent.addInfo("FlowId", "sub-flow");
        stateExitEvent.addInfo("StateId", "state1");
        stateExitEvent.addInfo("StateType", "End State");
        
        expectedEvents.add(stateExitEvent);
        
        
        ExpectedEvent endStateEvent = new ExpectedEvent();
        endStateEvent.addInfo("Layer", "webflow");
        endStateEvent.addInfo("Label", "info");
        endStateEvent.addInfo("Type", "Exit a State");
        endStateEvent.addInfo("TransitionOn", "*");
        endStateEvent.addInfo("FlowId", "main-flow");
        endStateEvent.addInfo("StateId", "state1");
        endStateEvent.addInfo("StateType", "Subflow State");
        
        expectedEvents.add(endStateEvent);
        
        
        endStateEvent = new ExpectedEvent();
        endStateEvent.addInfo("Layer", "webflow");
        endStateEvent.addInfo("Label", "info");
        endStateEvent.addInfo("Type", "Enter a State");
        endStateEvent.addInfo("TransitionOn", "*");
        endStateEvent.addInfo("FlowId", "main-flow");
        endStateEvent.addInfo("StateId", "state2");
        endStateEvent.addInfo("StateType", "End State");
        expectedEvents.add(endStateEvent);
        
        assertEvents(expectedEvents);
    }
    
    
    
    private class ViewIdValidator implements ValueValidator<String> {
//        Pattern viewIdPattern = Pattern.compile("viewId\\s=\\s(\\S+)\\]");
        Pattern viewIdPattern = Pattern.compile(".*viewId\\s=\\s'([\\S]+)'\\]");
        
        private String viewId;
        
        private ViewIdValidator(String viewId) {
            super();
            this.viewId = viewId;
        }

        @Override
        public boolean isValid(String actualValue) {
            if (actualValue == null) {
                return false;
            }
            Matcher matcher = viewIdPattern.matcher(actualValue);
            
            if (!matcher.matches()) {
                return false;
            }
            
            String actualViewId = matcher.group(1);
            
            return viewId.equals(actualViewId);
        }

        @Override
        public String getValueString() {
            return viewId;
        }
        
    }
    
    
}

class MockedFlowBuilder extends AbstractXmlFlowExecutionTests {
    private String flowfileName;

    public MockedFlowBuilder(String flowFileName) {
        this.flowfileName = flowFileName;
    }
    
    @Override
    protected FlowDefinitionResource getResource(FlowDefinitionResourceFactory resourceFactory) {
        return resourceFactory.createClassPathResource(flowfileName, getClass());
    }
    
    public Flow createFlow() {
        return super.buildFlow();
    }
}