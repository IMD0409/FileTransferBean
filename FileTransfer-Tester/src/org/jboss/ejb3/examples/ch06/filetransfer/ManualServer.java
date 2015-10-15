package org.jboss.ejb3.examples.ch06.filetransfer;

public class ManualServer {

	public ManualServer() {
	}

	public static void main (String[] args) {
		
		try {
			
			FileTransferUnitTestCase testCase = new FileTransferUnitTestCase();
			
			//Create the test directories
			System.out.println("FTPHome: " + FileTransferUnitTestCase.getFtpHome());
			
			//Create the server
			FileTransferUnitTestCase.createFtpService();
		
			Thread.sleep(1000000);
		
			
			
			
		} catch (Exception e) {
			System.out.println("Error testing");
		}
	}

}
