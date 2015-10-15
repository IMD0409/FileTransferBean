package org.jboss.ejb3.examples.ch06.filetransfer;

import java.io.File;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.NoSuchEJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestCase;

public class FileTransferUnitTestCase2 extends FileTransferTestCaseBase {

	// -------------------------------------------------------------------------------------||
	// Class Members
	// ----------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	/**
	 * Logger
	 */
	private static final Logger log = Logger.getLogger(FileTransferUnitTestCase.class.getName());

	/**
	 * The FTP Service to which we'll connect
	 */
	private static FtpServerPojo ftpService;

	/**
	 * Port to which the FTP Service will bind
	 */
	private static final int FTP_SERVICE_BIND_PORT = 12345;

	/**
	 * Name of the users configuration file for the server
	 */
	private static final String FILE_NAME_USERS_CONFIG = "ftpusers.properties";

	// -------------------------------------------------------------------------------------||
	// Instance Members
	// -------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	/**
	 * Our view of the EJB, local business interface type of the Proxy
	 */
	@EJB(mappedName = "java:global/FileTransfer-EAR/FileTransfer-Bean/FileTransferEJB!org.jboss.ejb3.examples.ch06.filetransfer.FileTransferRemoteBusiness")
	private FileTransferRemoteBusiness client1;

	/**
	 * Another FTP Client Session
	 */
	@EJB(mappedName = "java:global/FileTransfer-EAR/FileTransfer-Bean/FileTransferEJB!org.jboss.ejb3.examples.ch06.filetransfer.FileTransferRemoteBusiness")
	private FileTransferRemoteBusiness client2;

	// -------------------------------------------------------------------------------------||
	// Lifecycle
	// --------------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	/**
	 * Creates, initializes, and starts the FTP Service to which our test
	 * clients will connect. Called once before any tests run.
	 */
	@BeforeClass
	public static void createFtpService() throws Exception {
		// Create the FTP Service
		final FtpServerPojo service = new FtpServerPojo();

		// Configure
		service.setBindPort(FTP_SERVICE_BIND_PORT);
		service.setUsersConfigFileName(FILE_NAME_USERS_CONFIG);

		// Initialize
		service.initializeServer();

		// Start
		service.startServer();

		// Set (on success)
		log.info("Started up test FTP Service: " + service);
		ftpService = service;
	}

	/**
	 * Stops and resets the FTP Service. Called once after all tests are done.
	 * 
	 * @throws Exception
	 */
	@AfterClass
	public static void destroyFtpService() throws Exception {
		// Only run if initialization finished
		if (ftpService == null) {
			return;
		}

		// Stop the server
		ftpService.stopServer();

		// Reset
		ftpService = null;
		log.info("Brought down test FTP Service");
	}

	/**
	 * Disconnects and resets the FTP Client. Fired after each test has
	 * completed.
	 * 
	 * @throws Exception
	 */
	@After
	public void cleanup() throws Exception {
		// End the session for client 1
		try {
			client1.endSession();
		}
		// If we've already been ended
		catch (final NoSuchEJBException nsee) {
			// Ignore
		}

		// End the session for client 2
		try {
			client2.endSession();
		}
		// If we've already been ended
		catch (final NoSuchEJBException nsee) {
			// Ignore
		}
	}

	// -------------------------------------------------------------------------------------||
	// Tests
	// ------------------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	/**
	 * Tests that two separate sessions will act in isolation from each other
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSessionIsolation() throws Exception {
		// Log
		log.info("testSessionIsolation");

		// Get the existing client as made from the test lifecycle
		final FileTransferRemoteBusiness session1 = this.getClient();

		// Use another client
		final FileTransferRemoteBusiness session2 = this.client2;

		// cd into a home directory for each
		final String ftpHome = getFtpHome().getAbsolutePath();
		session1.cd(ftpHome);
		session2.cd(ftpHome);

		// Now make a new directory for each session, and go into it
		final String newDirSession1 = "newDirSession1";
		final String newDirSession2 = "newDirSession2";
		session1.mkdir(newDirSession1);
		session1.cd(newDirSession1);
		session2.mkdir(newDirSession2);
		session2.cd(newDirSession2);

		// Get the current working directory for each session
		final String pwdSession1 = session1.pwd();
		final String pwdSession2 = session2.pwd();

		// Ensure each session is in the proper working directory
		TestCase.assertEquals("Session 1 is in unexpected pwd", ftpHome + File.separator + newDirSession1, pwdSession1);
		TestCase.assertEquals("Session 2 is in unexpected pwd", ftpHome + File.separator + newDirSession2, pwdSession2);

		// End the session manually for session2 (session1 will be ended by test
		// lifecycle)
		session2.endSession();
	}

	/**
	 * Tests that a call to {@link FileTransferLocalBusiness#endSession()}
	 * results in the SFSB's backing instance removal, and that subsequent
	 * operations result in a {@link NoSuchEJBException}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSfsbRemoval() throws Exception {
		// Log
		log.info("testSfsbRemoval");

		// Get the existing client as made from the test lifecycle
		final FileTransferRemoteBusiness sfsb = this.getClient();

		// cd into the home directory
		final String ftpHome = getFtpHome().getAbsolutePath();
		sfsb.cd(ftpHome);

		// Get and test the pwd
		final String pwdBefore = sfsb.pwd();
		TestCase.assertEquals("Session should be in the FTP Home directory", ftpHome, pwdBefore);

		// End the session, resulting in an underlying instance
		// removal due to the annotation with @Remove upon
		// the bean implementation class
		sfsb.endSession();

		// Now try some other operation, and ensure that we get a
		// NoSuchEJBException
		boolean gotExpectedException = false;
		try {
			// This should not succeed, because we've called a method marked as
			// @Remove
			sfsb.pwd();
		} catch (final NoSuchEJBException nsee) {
			gotExpectedException = true;
		}
		TestCase.assertTrue("Call to end the session did not result in underlying removal of the SFSB bean instance",
				gotExpectedException);
	}

	public FileTransferUnitTestCase2() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected FileTransferRemoteBusiness getClient() {
		// The "standard" JNDI lookup
        final Hashtable jndiProperties = new Hashtable();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        //jndiProperties.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false"); // needed for a login module that requires the password in plaintext
        jndiProperties.put(Context.PROVIDER_URL, "remote://localhost:8080");
        jndiProperties.put(Context.SECURITY_PRINCIPAL, "ejbuser");
        jndiProperties.put(Context.SECURITY_CREDENTIALS, "ejbuser123!");
        jndiProperties.put("jboss.naming.client.ejb.context", true);
        Context context;
		try {
			context = new InitialContext(jndiProperties);
	        client1 = (FileTransferRemoteBusiness)context.lookup("java:global/FileTransfer-EAR/FileTransfer-Bean/FileTransferEJB!org.jboss.ejb3.examples.ch06.filetransfer.FileTransferRemoteBusiness");
	        
	    	client2 = (FileTransferRemoteBusiness)context.lookup("java:global/FileTransfer-EAR/FileTransfer-Bean/FileTransferEJB!org.jboss.ejb3.examples.ch06.filetransfer.FileTransferRemoteBusiness");

		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
        
        
    	
		return this.client1;
	}

}
