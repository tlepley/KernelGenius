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

/* Mother class for drivers */

package driver;

import common.CompilerError;
import utility.env.FileUtilities;
import utility.env.OSUtil;
import utility.thread.ExecHelper;

import java.util.LinkedList;
import java.io.File;

import driver.options.GeneralOptions;


public abstract class DriverHelper {
  public enum STAGE {NO, PREPROC, C2C};
  
  // ##################################################################
  //  File/directory utility functions
  // ##################################################################

  // ******************************************************************
  // createOutputDirectory :
  //
  // Check if the directory given in parameter exists, is valid and
  // create the directory if it does not exist
  // ******************************************************************
  static public void createOutputDirectory(String dirName) {
    File outputDirectory=null;
    if (dirName!=null) {
      outputDirectory = new File(dirName);
      if (!outputDirectory.exists()) {
        // Raise a warning
        CompilerError.GLOBAL.raiseWarning("output directory '"
            + outputDirectory.toString()
            + "' does not exist, create it"
            );
        // create the directory
        outputDirectory.mkdirs();
      }
      else {
        if (!outputDirectory.isDirectory()) {
          // fatal error
          CompilerError.GLOBAL.raiseFatalError("output directory '"
              + outputDirectory.toString()
              + "' is not a directory"
              );
        }
      }
    }
  }


  // ******************************************************************
  // checkInputFile :
  //
  // Check if the file given in parameter exists
  // ******************************************************************
  static public boolean checkInputFile(File file) {
    if (!file.exists()) {
      // File does not exist
      CompilerError.GLOBAL.raiseError("input file '"
          + file.getPath() + "' does not exist");
      return false;
    }
    else if (!file.canRead()) {
      // The application can not read the file
      CompilerError.GLOBAL.raiseError("can not read input file '"
          + file.getPath() + "'");
      return false;
    }
    return true;
  }



  // ******************************************************************
  // copyBackTempFiles :
  //
  // Copy back temporary files that must be kept
  // ******************************************************************
  static public void copyBackTempFiles(STAGE currentStage, STAGE endStage,
      boolean keep,
      LinkedList<File> tempFileList,
      LinkedList<File> additionalTempFileList,
      String outputFileName,
      String outputDirectoryName) 
          throws Exception {
    if (endStage==currentStage) {
      // Copy back temporary files
      for(File tmpFile:tempFileList) {
        FileUtilities.copy(tmpFile,outputFileName,outputDirectoryName,GeneralOptions.getDebugLevel());
      }
      if (additionalTempFileList!=null) {
        for (File tmpFile:additionalTempFileList) {
          FileUtilities.copy(tmpFile,null,outputDirectoryName,GeneralOptions.getDebugLevel());
        }
      }
    }
    else if (keep) {
      for(File tmpFile:tempFileList) {
        FileUtilities.copy(tmpFile,null,outputDirectoryName,GeneralOptions.getDebugLevel());
      }
      if (additionalTempFileList!=null) {
        for (File tmpFile:additionalTempFileList) {
          FileUtilities.copy(tmpFile,null,outputDirectoryName,GeneralOptions.getDebugLevel());
        }
      }
    }
  }


  // ******************************************************************
  // * makeOutputFile :	                                              *
  // *                                                                *
  // * Generate file version corresponding to the inputFile name      *
  // * located in directory 'outDir'		              		      *
  // *                                                                *
  // ******************************************************************
  static public File makeOutputFile(File file, File outDir) {
    return new File(outDir.getPath() + "/"+file.getName());
  }
  static public File makeOutputFile(String name, File outDir) {
    return new File(outDir.getPath() + "/"+name);
  }


  // ******************************************************************
  // * makeOutputFileList :                                           *
  // *                                                                *
  // * Generate a list of output files from a list of input files by  *
  // * Substituting 'inputExt' by 'outputExt' and by putting	      *
  // * files in the directory 'outDir'				                  *
  // *                                                                *
  // ******************************************************************
  static public LinkedList<File> makeOutputFileList(LinkedList<File> inputFileList, String inputExt, String outputExt,
      File outDir) {
    LinkedList<File> outputFileList=new LinkedList<File>();
    for (File fileToProcess:inputFileList) {
      // Create the output file
      String input_name=fileToProcess.getName();
      File outputTempFile=new File(outDir.getPath()+"/"+input_name.substring(0,input_name.length()-inputExt.length()) + outputExt);
      outputFileList.add(outputTempFile);
    }
    // Returns the list of generated files
    return outputFileList;
  }




  // ##################################################################
  //  Execution functions
  // ##################################################################


  // ******************************************************************
  // * runPreprocessor :                                              *
  // *                                                                *
  // * Executes synchronously the preprocessor command.               *
  // *                                                                *
  // ******************************************************************
  static public LinkedList<File> runPreprocessor(LinkedList<File> inputFileList, String inputExt, 
      String outputExt, File outDir,
      String compiler, LinkedList<String> backendCompilerOptionList,
      LinkedList<String> additionalOptionList
      ) {

    // Generated files
    LinkedList<File> outputFileList=new LinkedList<File>();

    if (GeneralOptions.getDebugLevel() > 0) {
      StringBuffer sb=new StringBuffer();
      boolean flag=false;
      sb.append("  ... Preprocessing [");
      for(File file: inputFileList) {
        if (flag) sb.append(" ");
        sb.append(file.getName());
        flag=true;
      }
      sb.append("]");
      CompilerError.GLOBAL.raiseMessage(sb.toString());
    }

    // Create the command line
    for (File fileToProcess:inputFileList) {
      // Create the output file
      String input_name=fileToProcess.getName();
      File outputTempFile=new File(outDir.getPath()+"/"+input_name.substring(0,input_name.length()-inputExt.length()) + outputExt);
      outputFileList.add(outputTempFile);

      // Create the command
      LinkedList<String> commandLine=new LinkedList<String>();
      commandLine.add(compiler);
      commandLine.add("-E");
      commandLine.add("-xc"); // Sets the language as C
      commandLine.addAll(backendCompilerOptionList);
      if (additionalOptionList!=null) {
        commandLine.addAll(additionalOptionList);
      }
      commandLine.add("-o");commandLine.add(outputTempFile.getPath());
      commandLine.add(fileToProcess.getPath());

      // Executes the preprocessing command
      try {
    	  if (OSUtil.isWindows()) {
    		  // System.err.println("Hello, we are in windows");

    		  // Verbosing
    		  if (GeneralOptions.getDebugLevel() > 0) {
    			  StringBuffer sb=new StringBuffer();
    			  sb.append("   -> copy "+fileToProcess.getPath()+" to "+outputTempFile.getPath());
    			  CompilerError.GLOBAL.raiseMessage(sb.toString());
    		  }
    		  FileUtilities.copy(fileToProcess, outputTempFile);
    	  }
    	  else {
    		  // System.err.println("Hello, we are NOT in windows");

    		  // Verbosing
    		  if (GeneralOptions.getDebugLevel() > 0) {
    			  StringBuffer sb=new StringBuffer();
    			  boolean flag=false;
    			  sb.append("   -> ");
    			  for(String s: commandLine) {
    				  if (flag) sb.append(" ");
    				  sb.append(s);
    				  flag=true;
    			  }
    			  CompilerError.GLOBAL.raiseMessage(sb.toString());
    		  }

    		  int returnValue;
    		  if ((returnValue=ExecHelper.exec(commandLine))!=0) {
    			  CompilerError.exitWithError(returnValue);
    		  }

    	  }
      }
      catch (Exception e) {
        CompilerError.GLOBAL.raiseFatalError("Error preprocessing file:" + e);
      }
    }

    // Verbosing
    if (GeneralOptions.getDebugLevel() > 0) {
      StringBuffer sb=new StringBuffer();
      boolean flag=false;
      sb.append("  ... generating [");
      for(File file: outputFileList) {
        if (flag) sb.append(" ");
        sb.append(file.getName());
        flag=true;
      }
      sb.append("]");
      CompilerError.GLOBAL.raiseMessage(sb.toString());
    }

    // Returns the list of generated files
    return outputFileList;
  }



  // ******************************************************************
  // * runBackendCompilerAsProcess                                    *
  // *                                                                *
  // * Executes the backend compiler asynchronousy as a process.      *
  // * This function returns the handler of the created process.      *
  // *                                                                *
  // ******************************************************************
  static public Process runBackendCompilerAsProcess(LinkedList<File> inputFileList,
      String compiler,
      LinkedList<String> backendCompilerOptionList,
      LinkedList<String> backendCompilerLibraryOptionList
      ) {
    // Get the command line
    LinkedList<String> commandLine=createBackendCompilerCommandLine(inputFileList,compiler,
        backendCompilerOptionList,
        backendCompilerLibraryOptionList);

    // Executes the backend compiler
    Process process=null;
    try {
      process=ExecHelper.asyncExec(commandLine);
    }
    catch (Exception e) {
      CompilerError.GLOBAL.raiseFatalError("Error processing files in the backend compiler");
    }

    return process;
  }



  // ******************************************************************
  // * runBackendCompiler                                             *
  // *                                                                *
  // * Executes the backend compiler synchronousy                     *
  // *                                                                *
  // ******************************************************************
  static public void runBackendCompiler(LinkedList<File> inputFileList,
      String compiler,
      LinkedList<String> backendCompilerOptionList,
      LinkedList<String> backendCompilerLibraryOptionList) {
    // Get the command line
    LinkedList<String> commandLine=createBackendCompilerCommandLine(inputFileList,compiler,
        backendCompilerOptionList,
        backendCompilerLibraryOptionList);

    // Executes the backend compiler command
    try {
      int returnValue;
      if ((returnValue=ExecHelper.exec(commandLine))!=0) {
        CompilerError.exitWithError(returnValue);
      }
    }
    catch (Exception e) {
      CompilerError.GLOBAL.raiseFatalError("Error preprocessing files");
    }
  }


  // ******************************************************************
  // * createBackendCompilerCommandLine                               *
  // *                                                                *
  // * Creates the command line for the C backend compilation stage   *
  // *                                                                *
  // ******************************************************************
  static public LinkedList<String> createBackendCompilerCommandLine(LinkedList<File> inputFileList,
      String compiler,
      LinkedList<String> backendCompilerOptionList,
      LinkedList<String> backendCompilerLibraryOptionList
      ) {
    LinkedList<String> commandLine=new LinkedList<String>();

    // Verbosing
    if (GeneralOptions.getDebugLevel() > 0) {
      StringBuffer sb=new StringBuffer();
      boolean flag=false;
      sb.append("  ... Compiling (Backend)\n      [");
      for(File file: inputFileList) {
        if (flag) sb.append(" ");
        sb.append(file.getName());
        flag=true;
      }
      sb.append("]");
      CompilerError.GLOBAL.raiseMessage(sb.toString());
    }

    // Create the command line
    commandLine.add(compiler);
    commandLine.addAll(backendCompilerOptionList);
    for (File fileToProcess:inputFileList) {
      commandLine.add(fileToProcess.getPath());
    }
    commandLine.addAll(backendCompilerLibraryOptionList);

    // Verbosing
    if (GeneralOptions.getDebugLevel() > 0) {
      StringBuffer sb=new StringBuffer();
      boolean flag=false;
      sb.append("   -> ");
      for(String s: commandLine) {
        if (flag) sb.append(" ");
        sb.append(s);
        flag=true;
      }
      CompilerError.GLOBAL.raiseMessage(sb.toString());
    }

    return commandLine;
  }
  

  // ##################################################################
  //  Dump functions
  // ##################################################################

  //------------------------------------------------------------------
  // print
  //
  // Dumps to stdout the object given in parameters
  //------------------------------------------------------------------
  
  public static void print(Object o, File file, String message) {
    System.out.println("FILE '" + file.getName() + "'" + message);
    System.out.print("-------");
    for (int j = 0; j < (file.getName().length() + message.length()); j++) {
      System.out.print("-");
    }
    System.out.println();
    System.out.println(o.toString());
    System.out.flush();
  }


}


