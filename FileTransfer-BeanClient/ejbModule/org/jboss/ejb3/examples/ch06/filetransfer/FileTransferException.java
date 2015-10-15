package org.jboss.ejb3.examples.ch06.filetransfer;

public class FileTransferException extends RuntimeException {

	// -------------------------------------------------------------------------------------||
	// Class Members
	// ----------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	private static final long serialVersionUID = 1L;

	// -------------------------------------------------------------------------------------||
	// Constructor
	// ------------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	public FileTransferException() {
		super();
	}

	public FileTransferException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public FileTransferException(final String message) {
		super(message);
	}

	public FileTransferException(final Throwable cause) {
		super(cause);
	}

}
