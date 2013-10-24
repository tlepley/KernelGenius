/*
  This file is part of KernelGenius.

  Copyright (C) 2013 STMicroelectronics

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.
 
  You should have received a copy of the GNU Lesser General Public
  License along with this program; if not, write to the Free
  Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
  Boston, MA 02110-1301 USA.
  
  Authors: Matthieu Leclerc, Thierry Lepley
*/

/* Helper to execute a Unix command from java */

package utility.thread;

import static java.lang.Character.isWhitespace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class ExecHelper {
	private ExecHelper() {
	}

	/*----------------------------------------------------------------------
	  exec :

	  Executes a command and returns the status after the command completed
	  (with the command status)
	-----------------------------------------------------------------------*/
	public static int exec(final String command) throws InterruptedException,
	IOException {
		return exec(splitOptionString(command));
	}
	public static int exec(final String command,
			BufferedOutStreams streamsBuffer)
	throws InterruptedException, IOException {
		return exec(splitOptionString(command),streamsBuffer);
	}

	public static int exec(final String[] cmdArray)
	throws InterruptedException, IOException {
		return exec(Arrays.asList(cmdArray));
	}
	public static int exec(final List<String> cmdList)
	throws InterruptedException, IOException {
		final Process process = new ProcessBuilder(cmdList).start();
		return forwardProcessStreamsAndSynchronize(process, true);
	}
	public static int exec(final List<String> cmdList,
			BufferedOutStreams streamsBuffer)
	throws InterruptedException, IOException {
		final Process process = new ProcessBuilder(cmdList).start();
		return catchProcessStreamsAndSynchronize(process, streamsBuffer, true);
	}


	/*----------------------------------------------------------------------
	 asyncExec :

	 Creates a process which executes the command and returns immediately
	 before the command completes. The process is returned by the function
	 -----------------------------------------------------------------------*/
	public static Process asyncExec(final List<String> cmdList)
	throws IOException, InterruptedException {
		final Process process = new ProcessBuilder(cmdList).start();
		forwardProcessStreamsAndSynchronize(process, false);
		return process;
	}
	public static Process asyncExec(final List<String> cmdList,
			BufferedOutStreams streamsBuffer)
	throws IOException, InterruptedException {
		final Process process = new ProcessBuilder(cmdList).start();
		catchProcessStreamsAndSynchronize(process, streamsBuffer, false);
		return process;
	}

	/*----------------------------------------------------------------------
	  catchProcessStreamsAndSynchronize :

	  Captures into 'streamsBuffer' 'out' and 'err' of the process
	  passed into parameter.
	  If join is set to 'true', wait for the process to complete before
	  returning
	  -----------------------------------------------------------------------*/
	private static int catchProcessStreamsAndSynchronize(final Process process, 
			final BufferedOutStreams streamsBuffer, boolean join)
	throws IOException, InterruptedException {
		// Read standard output produced by process in a parallel thread in
		// order
		// to avoid the process to block
		final Thread readerThread1 = new Thread() {
			@Override
			public void run() {
				// output output and error stream on the logger.
				final BufferedReader reader = new BufferedReader(
						new InputStreamReader(process.getInputStream()));
				try {
					String line = reader.readLine();
					while (line != null) {
						streamsBuffer.addLineToOut(line);
						line = reader.readLine();
					}
					reader.close();
				} catch (final IOException e) {
					throw new Error(
							"Can't read standard output stream of process", e);
				}
			}
		};
		// Read error outputs produced by process in a parallel thread in order
		// to avoid the process to block
		final Thread readerThread2 = new Thread() {
			@Override
			public void run() {
				// output output and error stream on the logger.
				final BufferedReader reader = new BufferedReader(
						new InputStreamReader(process.getErrorStream()));
				try {
					String line = reader.readLine();
					while (line != null) {
						streamsBuffer.addLineToErr(line);
						line = reader.readLine();
					}
					reader.close();
				} catch (final IOException e) {
					throw new Error("Can't read error stream of process", e);
				}
			}
		};

		readerThread1.start();
		readerThread2.start();

		if (join) {
			final int rValue = process.waitFor();
			readerThread1.join();
			readerThread2.join();

			return rValue;
		} else {
			return 0;
		}
	}


	/*----------------------------------------------------------------------
	  forwardProcessStreamsAndSynchronize :

	  Forward to 'out' and 'err' of the current process 'err' and 'out' of
	  the process passed into parameter
	  If join is set to 'true', wait for the process to complete before
	  returning
	  -----------------------------------------------------------------------*/
	private static int forwardProcessStreamsAndSynchronize(final Process process,
			boolean join)
	throws IOException, InterruptedException {
		// Read standard output produced by process in a parallel thread in
		// order
		// to avoid the process to block
		final Thread readerThread1 = new Thread() {
			@Override
			public void run() {
				// output output and error stream on the logger.
				final BufferedReader reader = new BufferedReader(
						new InputStreamReader(process.getInputStream()));
				try {
					String line = reader.readLine();
					while (line != null) {
						System.out.println(line);
						line = reader.readLine();
					}
					reader.close();
				} catch (final IOException e) {
					throw new Error(
							"Can't read standard output stream of process", e);
				}
			}
		};
		// Read error outputs produced by process in a parallel thread in order
		// to avoid the process to block
		final Thread readerThread2 = new Thread() {
			@Override
			public void run() {
				// output output and error stream on the logger.
				final BufferedReader reader = new BufferedReader(
						new InputStreamReader(process.getErrorStream()));
				try {
					String line = reader.readLine();
					while (line != null) {
						System.err.println(line);
						line = reader.readLine();
					}
					reader.close();
				} catch (final IOException e) {
					throw new Error("Can't read error stream of process", e);
				}
			}
		};

		readerThread1.start();
		readerThread2.start();

		if (join) {
			final int rValue = process.waitFor();
			readerThread1.join();
			readerThread2.join();

			return rValue;
		} else {
			return 0;
		}
	}




	
	
	
	
	/**
	 * Executes the given command line and returns the exit value.
	 * 
	 * @param cmdArray
	 *            the command to execute.
	 * @return the exit value
	 * @throws IOException
	 *             If an error occurs while running the command.
	 * @throws InterruptedException
	 *             if the calling thread has been interrupted while waiting for
	 *             the process to finish.
	 * @see #exec(List)
	 * @see Runtime#exec(String[])
	 */

	public static List<String> splitOptionString(final String s) {
		if (s == null) {
			return Collections.emptyList();
		}

		final List<String> result = new ArrayList<String>();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			final char c = s.charAt(i);
			if (isWhitespace(c)) {
				if (sb.length() != 0) {
					result.add(sb.toString());
					sb = new StringBuilder();
				}
			} else if (c == '\\') {
				if (i + 1 < s.length() && isWhitespace(s.charAt(i + 1))) {
					sb.append(s.charAt(i + 1));
					i++;
				} else {
					sb.append('\\');
				}
			} else {
				sb.append(c);
			}
		}
		if (sb.length() != 0)
			result.add(sb.toString());

		return result;
	}

}
