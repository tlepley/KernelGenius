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

/* Various useful functions for managing files */

package utility.env;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public abstract class FileUtilities {

  //------------------------------------------------------------------
  // copy :
  //
  // Copy file 'src' to file 'dest'
  //------------------------------------------------------------------
  public static void copy(File src, File dest) throws Exception {
    FileInputStream fi=null;
    FileOutputStream fd=null;

    try {
      fi=new FileInputStream(src);
    }
    catch (Exception e) {
      System.err.println("copy: can not open input file: " + src.getPath());
      throw e;
    }
    try {
      fd=new FileOutputStream(dest);
    }
    catch (Exception e) {
      System.err.println("copy: can not open output file: " + dest.getPath());
      throw e;
    }

    byte[] buffer=new byte[1024];
    int nb;
    try {
      while ((nb=fi.read(buffer))>=0) {
        try {
          fd.write(buffer,0,nb);
        }
        catch (Exception e) {
          System.err.println("copy: error writing file: " + dest.getPath());
          throw e;
        }
      }
    }
    catch (Exception e) {
      System.err.println("copy: error reading file: " + src.getPath());
      throw e;
    }

    try {
      fi.close();
    }
    catch (Exception e) {
      System.err.println("copy: can not close input file: " + src.getPath());
      throw e;
    }
    try {
      fd.close();
    }
    catch (Exception e) {
      System.err.println("copy: can not close output file: " + dest.getPath());
      throw e;
    }
  }

  //------------------------------------------------------------------
  // copy :
  //
  // Copy a 'inputFile' to a directory specified by its path name 
  // (or to the current directory if its path is null), and
  // renamed to file to 'outputFileName' if it is not null
  //------------------------------------------------------------------
  public static void copy(File inputFile,
      String outputFileName, String outputDirectoryPath,
      int dumpLevel) throws Exception{
    // Destination file name
    String dest_name;
    if (outputFileName==null) {
      dest_name = inputFile.getName();
    }
    else {
      dest_name=outputFileName;
    }

    // Destination directory name
    String dest_path=dest_name;
    if (dest_name.charAt(0)!='/') {
      // This is a relative path, so that we add the output
      // directory path if defined
      if (outputDirectoryPath!=null) {
        dest_path=outputDirectoryPath+"/"+dest_name;
      }
    }

    File destFile=new File(dest_path);

    // Check that the directory exists
    File parent=destFile.getParentFile();
    if (!parent.exists()) {
      System.err.println("copy: output directory does not exist: " + parent.getPath());
      throw new Exception();
    }

    // Copy the file
    copy(inputFile,destFile);

    // Verbose
    if (dumpLevel > 0) {
      System.err.println("  ... copy " + inputFile.getName()+" -> "+destFile.getPath());	  
    }
  }

  //------------------------------------------------------------------
  // recursiveDelete :
  //
  // Delete the File given in parameter. If it is a directory, it is
  // deleted recusively. This function returns false if the File
  // could not be deleted
  //------------------------------------------------------------------
  public static boolean recursiveDelete(File fileOrDir) {
    if (!fileOrDir.exists()) {
      System.err.println("delete: no such file or directory: "+fileOrDir.getPath());
      return false;
    }

    if(fileOrDir.isDirectory()) {
      // recursively delete contents
      for(File innerFile: fileOrDir.listFiles()) {
        if(!recursiveDelete(innerFile)) {
          return false;
        }
      }
    }
    //System.err.println("recursive delete = " + fileOrDir.getPath());
    return fileOrDir.delete();
  }

}
