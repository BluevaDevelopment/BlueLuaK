/******************************************************************************
 *  ____  _            _                    _
 * | __ )| |_   _  ___| |   _   _  __ _    | |
 * |  _ \| | | | |/ _ \ |  | | | |/ _` |_  | |
 * | |_) | | |_| |  __/ |__| |_| | (_| | |_| |
 * |____/|_|\__,_|\___|_____\__,_|\__,_|\___/
 *
 *  BlueLuaK
 *  https://github.com/BluevaDevelopment/BlueLuaK/t
 *
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak.lib.jse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Analog of Process that pipes input and output to client-specified streams.
 */
public class JseProcess {

	final Process process;
	final Thread input,output,error;

	/** Construct a process around a command, with specified streams to redirect input and output to.
	 * 
	 * @param cmd The command to execute, including arguments, if any
	 * @param stdin Optional InputStream to read from as process input, or null if input is not needed.
	 * @param stdout Optional OutputStream to copy process output to, or null if output is ignored.
	 * @param stderr Optinoal OutputStream to copy process stderr output to, or null if output is ignored.
	 * @throws IOException If the system process could not be created.
	 * @see Process
	 */
	public JseProcess(String[] cmd, InputStream stdin, OutputStream stdout, OutputStream stderr) throws IOException {
		this(Runtime.getRuntime().exec(cmd), stdin, stdout, stderr);	
	}

	/** Construct a process around a command, with specified streams to redirect input and output to.
	 * 
	 * @param cmd The command to execute, including arguments, if any
	 * @param stdin Optional InputStream to read from as process input, or null if input is not needed.
	 * @param stdout Optional OutputStream to copy process output to, or null if output is ignored.
	 * @param stderr Optinoal OutputStream to copy process stderr output to, or null if output is ignored.
	 * @throws IOException If the system process could not be created.
	 * @see Process
	 */
	public JseProcess(String cmd, InputStream stdin, OutputStream stdout, OutputStream stderr) throws IOException {
		this(Runtime.getRuntime().exec(cmd), stdin, stdout, stderr);	
	}

	private JseProcess(Process process, InputStream stdin, OutputStream stdout, OutputStream stderr) {
		this.process = process;
		input = stdin == null? null: copyBytes(stdin, process.getOutputStream(), null, process.getOutputStream());
		output = stdout == null? null: copyBytes(process.getInputStream(), stdout, process.getInputStream(), null);
		error = stderr == null? null: copyBytes(process.getErrorStream(), stderr, process.getErrorStream(), null);
	}

	/** Get the exit value of the process. */
	public int exitValue() {
		return process.exitValue();
	}

	/** Wait for the process to complete, and all pending output to finish.
	 * @return The exit status.
	 * @throws InterruptedException
	 */
	public int waitFor() throws InterruptedException {
		int r = process.waitFor();
		if (input != null)
			input.join();
		if (output != null)
			output.join();
		if (error != null)
			error.join();
		process.destroy();
		return r;
	}

	/** Create a thread to copy bytes from input to output. */
	private Thread copyBytes(final InputStream input,
			final OutputStream output, final InputStream ownedInput,
			final OutputStream ownedOutput) {
		Thread t = (new CopyThread(output, ownedOutput, ownedInput, input));
		t.start();
		return t;
	}

	private static final class CopyThread extends Thread {
		private final OutputStream output;
		private final OutputStream ownedOutput;
		private final InputStream ownedInput;
		private final InputStream input;

		private CopyThread(OutputStream output, OutputStream ownedOutput,
				InputStream ownedInput, InputStream input) {
			this.output = output;
			this.ownedOutput = ownedOutput;
			this.ownedInput = ownedInput;
			this.input = input;
		}

		public void run() {
			try {
				byte[] buf = new byte[1024];
				int r;
				try {
					while ((r = input.read(buf)) >= 0) {
						output.write(buf, 0, r);
					}
				} finally {
					if (ownedInput != null)
						ownedInput.close();
					if (ownedOutput != null)
						ownedOutput.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
