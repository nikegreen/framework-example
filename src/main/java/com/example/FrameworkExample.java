package com.example;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class FrameworkExample 
{
	private        Framework framework = null;
	private final  String    cache     =  "bundles-cache";
	private final  String[]  files     = {"bundles/bundle-locale-1.0.0.jar",
			"bundles/bundle-resource-1.0.0.jar"};

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public FrameworkExample() {
        System.out.println ("Create framework ..." );
		createFramework();
        if (framework == null) {
            System.out.println ("framework is not created");
			return;
        }
        System.out.println ("framework is created");
		printBundleParams(framework, "Framework");
        try {
			try {
				updateFramework();
				//------------------------
				System.out.println ("");
				System.out.println ("Framework init ..." );
				initFramework  ();
				printBundleState(framework, "Framework");
				printBundleContext();
				//------------------------
//		        System.out.println ("");
//		        System.out.println ("Framework update ..." );
//	            updateFramework(); 
//				//------------------------
//		        System.out.println ("");
//		        System.out.println ("Framework stop ..." );
//				System.out.println ("Framework state before stop");
//				printBundleState(framework, "Framework");
//	            stopFramework  ();
//	            printBundleContext();
//				printBundleState(framework, "Framework");
//				//------------------------
				System.out.println ("");
				System.out.println ("Framework start ..." );
				startFramework();
				printBundleContext();
				printBundleState(framework, "Framework");
				//------------------------
				System.out.println ("");
				System.out.println ("Load bundles ..." );
				loadBundles();
				//------------------------
				System.out.println ("");
				System.out.println ("");
				System.out.print ("Enter exit : " );
				// создание объекта чтения из стандартного потока ввода
				Scanner in = new Scanner(System.in);
				// чтение строки из консоли
				String cmd = in.nextLine();
				while (!cmd.equalsIgnoreCase("exit")) {
					cmd = in.nextLine();
				}
				in.close();

				//------------------------
				System.out.println ("");
				System.out.println ("Framework stop ..." );
				System.out.println ("Framework state before stop");
				printBundleState(framework, "Framework");
				stopFramework  ();
				printBundleContext();
				printBundleState(framework, "Framework");
				//------------------------
				System.out.println ("");
				System.out.println ("Framework update ..." );
				updateFramework();

			} catch (Exception e) {
				System.err.println("Exception : " + e.getMessage());
				System.err.println(".......................");
				e.printStackTrace();
			}
        } finally {
			if (framework != null) {
				try {
					framework.stop();
					framework.waitForStop(2000);
				} catch (BundleException e) {
					System.err.println ("BundleException : " + e.getMessage());
				} catch (InterruptedException e) {
					System.err.println ("InterruptedException : " + e.getMessage());
				}
			}
        }
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void printBundleContext() {
		if (framework != null) {
			if (framework.getBundleContext() != null)
				System.out.println ("framework.getBundleContext() not NULL");
			else
				System.out.println ("framework.getBundleContext() is NULL");
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void createFramework() {
		ServiceLoader<FrameworkFactory> serviceLoader = ServiceLoader.load(FrameworkFactory.class);

		FrameworkFactory factory = serviceLoader.iterator().next();

		try {
			if (factory != null) {
				Map<String, String> props = new HashMap<String, String>();
				props.put("org.osgi.framework.storage", cache);
				framework = factory.newFramework(props);
				if (framework == null) {
					System.out.println ("framework is created");
					printBundleParams(framework, "Framework");
				}
			}
		} catch(Exception e){
			System.err.println ("Create Framework Exception : " + e.getMessage());
		}
	}
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Update
	 * @throws Exception
	 */
	public void updateFramework() throws Exception {
		int previousState = framework.getState();
		printBundleState(framework, "Framework");

		FrameworkEvent[] result  = new FrameworkEvent[1];
		Exception     [] failure = new Exception[1];

		Thread waitForStop = waitForStopThread(framework, 10000, result, failure);
		waitForStop.start();
		try {
			Thread.sleep(500);
			framework.update();
		} catch (BundleException e) {
			e.printStackTrace();
		}
		waitForStop.join();
		if (failure[0] != null)
			throw new Exception("Error occurred while waiting : " + failure[0]);

		if (result[0] != null)
			System.out.println ("Wait for stop event");

		/*
	     * Если фремворк не в состояниях STARTING или ACTIVE, то полагаем,
	     *  что метод waitForStop немедленно вернет FrameworkEvent.STOPPED
	     */
		int expectedFrameworkEvent = (previousState & (Bundle.STARTING | Bundle.ACTIVE)) != 0 ? FrameworkEvent.STOPPED_UPDATE
				: FrameworkEvent.STOPPED;
		if (expectedFrameworkEvent == FrameworkEvent.STOPPED_UPDATE)
			System.out.println ("expected frameworkEvent = STOPPED_UPDATE");
		else if (expectedFrameworkEvent == FrameworkEvent.STOPPED)
			System.out.println ("expected frameworkEvent = STOPPED");

		// Hack, not sure how to listen for when a framework is done starting back up.
		for (int i = 0; i < 20; i++) {
			if (framework.getState() != previousState) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			} else {
				break;
			}
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private Thread waitForStopThread(final Framework framework, final long timeout,
                                     final FrameworkEvent[] success, final Exception[] failure) {
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
	private void initFramework() throws BundleException {
		framework.init();
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void startFramework() throws BundleException {
		framework.start();
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void stopFramework() throws BundleException, InterruptedException {
		framework.stop();
		FrameworkEvent event = framework.waitForStop(10000);
		if (event != null)
			printFrameworkEvent(event);
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void loadBundles() {
		BundleContext bctx = framework.getBundleContext();
		Bundle bundle;
		String MSG_TEMPL = "Bundle &lt;%s&gt; installed";
		try {
			for (int i = 0; i < files.length; i++) {
				File file = new File (files[i]);
				if (file.exists()) {
					String location = file.toURI().toURL().toString();
					System.out.println("load bundle from file:" + location);
					bundle = bctx.installBundle(location);
					if (bundle != null) {
						String msg = String.format(MSG_TEMPL, file.getName());
						System.out.println (msg);
						printBundleParams(bundle, "Bundle");
					}
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (BundleException e) {
			e.printStackTrace();
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void printBundleState(Bundle bundle, final String prefix) {
		if (bundle == null)
			return;
		switch (bundle.getState()) {
			case Bundle.UNINSTALLED :  // 0x00000001
				System.out.println (prefix + " state : UNINSTALLED");
				break;
			case Bundle.INSTALLED :    // 0x00000002
				System.out.println (prefix + " state : INSTALLED");
				break;
			case Bundle.RESOLVED :     // 0x00000004
				System.out.println (prefix + " state : RESOLVED");
				break;
			case Bundle.STARTING :     // 0x00000008
				System.out.println (prefix + " state : STARTING");
				break;
			case Bundle.STOPPING :     // 0x00000010
				System.out.println (prefix + " state : STOPPING");
				break;
			case Bundle.ACTIVE :       // 0x00000020
				System.out.println (prefix + " state : ACTIVE");
				break;
			default :
				System.out.println (prefix + " state.code = " + bundle.getState());
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void printBundleParams(Bundle bundle, final String prefix) {
		printBundleState(bundle, "   " + prefix);
		System.out.println ("   Version = " + bundle.getVersion());
		System.out.println ("   SymbolicName = " + bundle.getSymbolicName());
		System.out.println ("   Location = " + bundle.getLocation());
		System.out.println ("   BundleId = " + bundle.getBundleId());
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void printFrameworkEvent(FrameworkEvent event) {
		if (event == null)
			return;
		switch (event.getType()) {
			case FrameworkEvent.STARTED :  // 0x00000001
				System.out.println ("FrameworkEvent STARTED");
				break;
			case FrameworkEvent.ERROR :  // 0x00000002
				System.out.println ("FrameworkEvent ERROR");
				break;
			case FrameworkEvent.PACKAGES_REFRESHED :  // 0x00000004
				System.out.println ("FrameworkEvent PACKAGES_REFRESHED");
				break;
			case FrameworkEvent.STARTLEVEL_CHANGED :  // 0x00000008
				System.out.println ("FrameworkEvent STARTLEVEL_CHANGED");
				break;
			case FrameworkEvent.WARNING :  // 0x00000010
				System.out.println ("FrameworkEvent WARNING");
				break;
			case FrameworkEvent.INFO :  // 0x00000020
				System.out.println ("FrameworkEvent INFO");
				break;
			case FrameworkEvent.STOPPED :  // 0x00000040;
				System.out.println ("FrameworkEvent STOPPED");
				break;
			case FrameworkEvent.STOPPED_UPDATE :  // 0x00000080
				System.out.println ("FrameworkEvent STOPPED_UPDATE");
				break;
			case FrameworkEvent.WAIT_TIMEDOUT :   // 0x00000200
				System.out.println ("FrameworkEvent WAIT_TIMEDOUT");
				break;
			default :
				System.out.println ("FrameworkEvent code = " + event.getType());
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public static void main( String[] args )
    {
        new FrameworkExample();
        System.exit(0);
    }
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
}
