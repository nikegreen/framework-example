package com.example;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.HashMap;
import java.util.ServiceLoader;

import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework; 
import org.osgi.framework.launch.FrameworkFactory; 

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FrameworkTest 
{
	private static Framework framework = null;
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{}
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{}
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception{}
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception{}
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Test 
	public void test00CreateFramework() throws Exception
	{ 
    	ServiceLoader<FrameworkFactory> ffs = ServiceLoader.load(FrameworkFactory.class);
    	
    	FrameworkFactory factory = ffs.iterator().next();

    	assertNotNull("FrameworkFactory not loaded", factory);
    	
		if (factory != null) {
			Map<String, String> props = new HashMap<String, String>(); 
			props.put("org.osgi.framework.storage", "target/osgi-store"); 
			props.put("org.osgi.framework.storage.clean", "onFirstInit"); 
		 
			framework = factory.newFramework(props);
		}
	}
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Test 
	public void test01UpdateFramework() throws Exception
	{ 
		int previousState = framework.getState(); 
		FrameworkEvent[] result = new FrameworkEvent[1]; 
		Exception[] failureException = new Exception[1]; 
		Thread waitForStop = waitForStopThread(framework, 10000, result, failureException); 
		waitForStop.start(); 
		try { 
			Thread.sleep(500); 
			framework.update(); 
		} catch (BundleException e) { 
			fail("Failed to update the framework: " + e); 
		} 
		waitForStop.join(); 
		if (failureException[0] != null) 
			fail("Error occurred while waiting " + failureException[0]); 
	 
		assertNotNull("Wait for stop event not null", result[0]); 
	 
		// If the framework was not STARTING or ACTIVE then we assume the waitForStop returned immediately with a FrameworkEvent.STOPPED 
		int expectedFrameworkEvent = (previousState & (Bundle.STARTING | Bundle.ACTIVE)) != 0 ? FrameworkEvent.STOPPED_UPDATE : FrameworkEvent.STOPPED; 
		assertEquals("Wait for stop event type is wrong", expectedFrameworkEvent, result[0].getType()); 
	 
		// Hack, not sure how to listen for when a framework is done starting back up. 
		for (int i = 0; i < 20; i++) { 
			if (framework.getState() != previousState) { 
				try { 
					Thread.sleep(100); 
				} catch (InterruptedException e) { 
					// nothing 
				} 
			} else { 
				break; 
			} 
		} 
		assertEquals("Back at previous state after update", previousState, framework.getState()); 
	} 
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   @Test 
    public void test02InitFramework() throws Exception 
    { 
		framework.init();
		assertNotNull("BundleContext not null after init", framework.getBundleContext()); 
		assertEquals("Framework state after init", Bundle.STARTING, framework.getState()); 
	} 
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Test 
    public void test03StartFramework() throws Exception
	{ 
//		assertNotNull("Framework is NULL", framework);
		framework.start();
		assertNotNull("BundleContext not null after start", framework.getBundleContext()); 
		assertEquals("Framework state after start", Bundle.ACTIVE, framework.getState()); 
	} 
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Test 
    public void test04StopFramework() throws Exception
	{ 
		int previousState = framework.getState(); 
		framework.stop();
		FrameworkEvent event = framework.waitForStop(10000);
		assertNotNull("FrameworkEvent not null", event); 
		assertEquals("Stop event type", FrameworkEvent.STOPPED, event.getType()); 
		assertNull("BundleContext null after stop", framework.getBundleContext()); 
		int expectedState = (previousState & (Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING)) != 0 ? 
				                                   Bundle.RESOLVED : previousState; 
		assertEquals("Framework state after stop", expectedState, framework.getState()); 
	} 
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private Thread waitForStopThread(final Framework framework, final long timeout, 
			                         final FrameworkEvent[] success, final Exception[] failure)
	{ 
		return new Thread(new Runnable() { 
			public void run() { 
				try { 
					success[0] = framework.waitForStop(10000); 
				} catch (InterruptedException e) { 
					failure[0] = e; 
				} 
			} 
		}, "waitForStop thread"); 
	} 
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
}
