package com.tracelytics.test.struts;

import com.opensymphony.xwork2.Action;

import junit.framework.TestCase;

/** Simple unit test to run the TestAction itself. */
public class TestActionTest extends TestCase {
	public void testExecute() {
		TestAction action = new TestAction();
		assertEquals(Action.SUCCESS, action.execute());
		assertNotNull(action.getTime());
	}
}
