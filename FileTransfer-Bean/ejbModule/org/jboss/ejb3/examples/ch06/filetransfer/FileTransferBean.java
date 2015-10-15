package org.jboss.ejb3.examples.ch06.filetransfer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.jboss.ejb3.examples.ch06.filetransfer.FileTransferException;
import org.jboss.ejb3.examples.ch06.filetransfer.FileTransferLocalBusiness;


/**
 * Bean Implementation class of the FileTransferEJB, modeled
 * as a Stateful Session Bean
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
@Stateful(name = FileTransferBean.EJB_NAME)
@Local(FileTransferLocalBusiness.class)
@Remote(FileTransferRemoteBusiness.class)
public class FileTransferBean implements FileTransferLocalBusiness, Serializable
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Serial Version UID
    */
   private static final long serialVersionUID = 1L;

   /**
    * Name of the EJB, used in Global JNDI addresses
    */
   public static final String EJB_NAME = "FileTransferEJB";

   /**
    * The name of the host to which we'll connect.
    * In production systems would typically be externalized
    * via configurable environment entry
    */
   private static String CONNECT_HOST = "localhost";

   /**
    * The port to which we'll connect.
    * In production systems would typically be externalized
    * via configurable environment entry.  IANA standard
    * for FTP ports is 21, though this requires root access
    * on *nix for testing, so we'll use the nonstandard 12345.
    */
   private static int CONNECT_PORT = 12345;

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * The underlying FTP Client.  We don't want its state
    * getting Serialized during passivation.  We'll
    * reinitialize this client and its connections
    * upon activation.
    */
   private FTPClient client;

   /**
    * Name of the present working directory.  In cases where
    * we're passivated, if this is specified
    * we'll change into this directory upon activation.
    */
   private String presentWorkingDirectory;

   //-------------------------------------------------------------------------------------||
   // Lifecycle Callbacks ----------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Called by the container when the instance is about to be passivated or brought
    * out of service entirely.
    *
    * @see org.jboss.ejb3.examples.ch06.filetransfer.FileTransferCommonBusiness#disconnect()
    */
   @PrePassivate
   @PreDestroy
   @Override
   public void disconnect()
   {
      // Obtain FTP Client
      final FTPClient client = this.getClient();

      // If exists
      if (client != null)
      {
         // If connected
         if (client.isConnected())
         {
            // Logout
            try
            {
               client.logout();
               System.out.println("Logged out of: " + client);
            }
            catch (final IOException ioe)
            {
            	System.out.println("Exception encountered in logging out of the FTP client: " + ioe.getMessage());
            }

            // Disconnect
            try
            {
            	System.out.println("Disconnecting: " + client);
               client.disconnect();
               System.out.println("Disconnected: " + client);
            }
            catch (final IOException ioe)
            {
            	System.out.println("Exception encountered in disconnecting the FTP client: " + ioe.getMessage());
            }

            // Null out the client so it's not serialized
            this.client = null;
         }
      }
   }

   /**
    * Called by the container when the instance has been created or re-activated
    * (brought out of passivated state).  Will construct the underlying FTP Client
    * and open all appropriate connections.
    *
    * @see org.jboss.ejb3.examples.ch06.filetransfer.FileTransferCommonBusiness#connect()
    */
   @PostConstruct
   @PostActivate
   @Override
   public void connect() throws IllegalStateException, FileTransferException
   {
      /*
       * Precondition checks
       */
      final FTPClient clientBefore = this.getClient();
      if (clientBefore != null && clientBefore.isConnected())
      {
         throw new IllegalStateException("FTP Client is already initialized");
      }

      // Get the connection properties
      final String connectHost = this.getConnectHost();
      final int connectPort = this.getConnectPort();

      // Create the client
      final FTPClient client = new FTPClient();
      final String canonicalServerName = connectHost + ":" + connectPort;
      System.out.println("Connecting to FTP Server at " + canonicalServerName);
      try
      {
         client.connect(connectHost, connectPort);
      }
      catch (final IOException ioe)
      {
         throw new FileTransferException("Error in connecting to " + canonicalServerName, ioe);
      }

      // Set
      System.out.println("Connected to FTP Server at: " + canonicalServerName);
      this.setClient(client);

      // Check that the last operation succeeded
      this.checkLastOperation();

      try
      {
         // Login
         client.login("user", "password");

         // Check that the last operation succeeded
         this.checkLastOperation();
      }
      catch (final Exception e)
      {
         throw new FileTransferException("Could not log in", e);
      }

      // If there's a pwd defined, cd into it.
      final String pwd = this.getPresentWorkingDirectory();
      if (pwd != null)
      {
         this.cd(pwd);
      }

   }

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /* (non-Javadoc)
    * @see org.jboss.ejb3.examples.ch06.filetransfer.FileTransferCommonBusiness#cd(java.lang.String)
    */
   @Override
   public void cd(final String directory)
   {
      // Get the client
      final FTPClient client = this.getClient();

      // Exec cd
      try
      {
         // Exec cd
         client.changeWorkingDirectory(directory);

         // Check reply for success
         this.checkLastOperation();
      }
      catch (final Exception e)
      {
         throw new FileTransferException("Could not change working directory to \"" + directory + "\"", e);
      }

      // Set the pwd (used upon activation)
      System.out.println("cd > " + directory);
      this.setPresentWorkingDirectory(directory);
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.examples.ch06.filetransfer.FileTransferCommonBusiness#mkdir(java.lang.String)
    */
   @Override
   public void mkdir(final String directory)
   {
      // Get the client
      final FTPClient client = this.getClient();

      // Exec cd
      try
      {
         // Exec mkdir
         client.makeDirectory(directory);

         // Check reply for success
         this.checkLastOperation();
      }
      catch (final Exception e)
      {
         throw new FileTransferException("Could not make directory \"" + directory + "\"", e);
      }

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.examples.ch06.filetransfer.FileTransferCommonBusiness#pwd()
    */
   @Override
   public String pwd()
   {
      // Get the client
      final FTPClient client = this.getClient();

      // Exec pwd
      try
      {
         final FTPFile[] files = client.listFiles();
         for (final FTPFile file : files)
         {
        	 System.out.println(file.toString());
         }

          // Exec pwd
          String dir = client.printWorkingDirectory();
			String separator = File.separator;

          if ("\\".equals(separator)) {
              // reformat to use for windows
              if (dir.startsWith("/")) {
                  dir = dir.substring(1);
              }
              dir = dir.replaceAll("/", "\\" + separator);
          }

          return dir;

      }
      catch (final IOException ioe)
      {
         throw new FileTransferException("Could not print working directory", ioe);
      }
   }

   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods ------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Ensures that the last operation succeeded with a positive
    * reply code.  Otherwise a {@link FileTransferException} 
    * is raised, noting the reply code denoting the error.
    * 
    * @throws FileTransferException
    */
   protected void checkLastOperation() throws FileTransferException
   {
      // Get the client
      final FTPClient client = this.getClient();

      // Obtain and check the reply from the connection
      final int connectReply = client.getReplyCode();
      if (!FTPReply.isPositiveCompletion(connectReply))
      {
         // Indicate the problem
         throw new FileTransferException("Did not receive positive completion code from server, instead code was: "
               + connectReply);
      }

   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.examples.ch06.filetransfer.FileTransferRemoteBusiness#endSession()
    */
   @Remove
   @Override
   public void endSession()
   {
	   System.out.println("Session Ending...");
   }

   //-------------------------------------------------------------------------------------||
   // Accessors / Mutators ---------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * @return the connectHost
    */
   public String getConnectHost()
   {
      return CONNECT_HOST;
   }

   /**
    * @return the connectPort
    */
   public int getConnectPort()
   {
      return CONNECT_PORT;
   }

   /**
    * @return the client
    */
   protected final FTPClient getClient()
   {
      return client;
   }

   /**
    * @param client the client to set
    */
   private void setClient(final FTPClient client)
   {
      this.client = client;
   }

   /**
    * @return the presentWorkingDirectory
    */
   private String getPresentWorkingDirectory()
   {
      return presentWorkingDirectory;
   }

   /**
    * @param presentWorkingDirectory the presentWorkingDirectory to set
    */
   private void setPresentWorkingDirectory(String presentWorkingDirectory)
   {
      this.presentWorkingDirectory = presentWorkingDirectory;
   }

}