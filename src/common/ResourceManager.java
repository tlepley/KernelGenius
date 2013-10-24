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

/* Manage the cleaning streams and the cleaning of temporary files.
   This class supports multi-threading and uses local thread storage
*/

package common;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;

import utility.env.FileUtilities;

public class ResourceManager {

  private static InheritableThreadLocal<LinkedList<File>> tempDirList = new InheritableThreadLocal<LinkedList<File>>() {
    @Override
    protected LinkedList<File> initialValue() {
      return new LinkedList<File>();
    }
  };
  private static InheritableThreadLocal<HashSet<Closeable>>  openStreams= new InheritableThreadLocal<HashSet<Closeable>>() {
    @Override
    protected HashSet<Closeable> initialValue() {
      return new HashSet<Closeable>();
    }
  };

  //==================================================================
  // Getters
  //==================================================================

  public static LinkedList<File> getTempDirList() {
    return tempDirList.get();
  }
  public static HashSet<Closeable> getOpenStreams() {
    return openStreams.get();
  }

  //==================================================================
  // Registering
  //==================================================================
 
  public static void registerTempDirectory(File td) {
    getTempDirList().add(td);
  }

  public static void registerStream(Closeable c) {
    getOpenStreams().add(c);
  }
  
  //==================================================================
  // Open streams
  //==================================================================
 
  public static FileInputStream openInputStream(File f) throws FileNotFoundException {
    FileInputStream is=new FileInputStream(f);
    getOpenStreams().add(is);
    return is;
  }
  public static FileOutputStream openOutputStream(File f) throws FileNotFoundException  {
    FileOutputStream os=new FileOutputStream(f);
    getOpenStreams().add(os);
    return os;
  }
  
  //==================================================================
  // Close streams
  //==================================================================

  public static void closeStream(Closeable c) throws IOException {
    c.close();
    getOpenStreams().remove(c);
  }

  
  //==================================================================
  // Thread termination
  //==================================================================

  public static void shutdown() {
      /* System.err.println("System shutdown"); */
	  // Close all open streams
	  for(Closeable c:getOpenStreams()) {
		  try { c.close(); }
		  catch (Exception e) {
			  System.err.print("shutdown: could not close an open stream");
		  }
	  }
	  getOpenStreams().clear();
	  
	  // Delete temporary directories
	  for(File tempDir:getTempDirList()) {
		  if (!FileUtilities.recursiveDelete(tempDir)) {
			  System.err.print("shutdown: could not delete temporary directory: "+tempDir.getPath());
		  }
	  }
	  getTempDirList().clear();
  }
  
}
