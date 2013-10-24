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
  
  Authors: Thierry Lepley
*/

package utility.thread;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

public class BufferedOutStreams {
  String target=null;
  String replacement=null;
  LinkedList<byte[]> outByteList = new LinkedList<byte[]>();
  LinkedList<byte[]> errByteList = new LinkedList<byte[]>();
  
  public BufferedOutStreams(String t, String r) {
	  target=t;
	  replacement=r;
  }
  
  // Add a string to the output buffer
  public void addLineToOut(String s) {
	  outByteList.add((s.replace(target,replacement)+'\n').getBytes());
  }
  
  // Add a string to the err buffer
  public void addLineToErr(String s) {
	  errByteList.add((s.replace(target,replacement)+'\n').getBytes());
  }
  
  // Returns the size in bytes if the buffered output stream
  public int getOutSize() {
	  int size=0;
	  for(byte[] byteArray:outByteList) {
		  size+=byteArray.length;
	  }
	  return size;
  }

  // Returns the size in bytes if the buffered output stream
  public int getErrSize() {
	  int size=0;
	  for(byte[] byteArray:errByteList) {
		  size+=byteArray.length;
	  }
	  return size;
  }

  // Send the buffered output stream to 'out'
  public void sendOutToStream(OutputStream out) throws IOException {
    for(byte[] byteArray:outByteList) {
      out.write(byteArray);
    }
  }

  // Send the buffered error stream to 'err'
  public void sendErrToStream(OutputStream out) throws IOException {
    for(byte[] byteArray:errByteList) {
      out.write(byteArray);
    }
  }
  
}

