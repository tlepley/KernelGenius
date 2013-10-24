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

/* Manager for errors of the compiler */

package common;

import java.io.File;

import parser.MyToken;
import parser.TNode;

public class CompilerError {
  // Global error manager
  static public final CompilerError GLOBAL = new CompilerError();
  
  // Verbose level
  private int verboseLevel=0;

  // Current processed filename
  private String fileName = null;

  // ##################################################################
  // Constructors
  // ################################################################## 

  public CompilerError() {
    fileName = null;
  }

  public CompilerError(int v) {
    verboseLevel = v;
    fileName     = null;
  }

  public CompilerError(String s) {
    fileName     = s;
  }

  public CompilerError(File f) {
    fileName     = f.getPath();
  }

  public CompilerError(int v, String s) {
    verboseLevel = v;
    fileName     = s;
  }

  public CompilerError(int v, File f) {
    verboseLevel = v;
    fileName     = f.getPath();
  }

  public void setVerboseLevel(int v) {
    verboseLevel=v;
  }

  // ##################################################################
  // Global
  // ##################################################################

  // maximum number of errors allowed
  private int nbMaxErrors = 5;
  // error counter
  private int nb_errors   = 0;


  // ******************************************************************
  // setNbMaxErrors :
  //
  // Sets the maximum number of errors allowed
  // ******************************************************************
  void setNbMaxErrors(int i) {
    nbMaxErrors=i;
  }

  // ******************************************************************
  // isAnError :
  //
  // Returns 'true' if at least one error has been raised.
  // ******************************************************************
  public boolean isAnError() {
    return(nb_errors>0);
  }

  // ******************************************************************
  // addAnError:
  //
  // Increment the counter error and exit if too much errors
  // ******************************************************************
  private void addAnError() {
    if (++nb_errors >= nbMaxErrors) {
      System.err.print("Too many errors, ");
      exitWithError();
    }
  }

  // ******************************************************************
  // exit :
  //
  // Display a message and stop the compilation process
  // ******************************************************************
  public static void exitWithError() {
    System.err.println("stopping the compilation process");
    // Exit the program execution
    throw new CompilerExit(1);
  }
  public static void exitWithError(int i) {
    System.err.println("stopping the compilation process");
    // Exit the program execution
    throw new CompilerExit(i);
  }
  public static void exitNormally() {
    // Exit the program execution
    throw new CompilerExit(0);
  }

  // ******************************************************************
  // exitIfError :
  //
  // Exits if some errors are pending
  // ******************************************************************
  public void exitIfError() {
    if (isAnError()) {
      exitWithError();
    }
  }


  // ##################################################################
  // Various error/warning/messages
  // ##################################################################


  // ******************************************************************
  // printPrefix :
  //
  // Prints "<filename>" if filename is set, nothing otherwise
  // ******************************************************************
  private void printPrefix() {
    if (fileName!=null) {
      System.err.print(fileName + ":");
    }
  }
  private void printPrefix(String s) {
    System.err.print(s + ":");
  }

  // ******************************************************************
  // printPrefix :
  //
  // Prints "<filename>:<line num>: " where the line number has been
  // extracted from the TNode if the filename is set or "<line num>: "
  // otherwise
  // ******************************************************************
  @SuppressWarnings("unused")
  private void printPrefix(TNode tn) {
    printPrefix(tn,0);
  }
  private void printPrefix(TNode tn, int lineOffset) {
    if (tn==null) {return;}

    String s;
    s=(String)tn.getSource();
    if (s==null) {
      printPrefix();
    }
    else {
      printPrefix(s);
    }
    System.err.print((tn.getLine()+lineOffset)+ ": ");
  }



  // ******************************************************************
  // raiseMessage :
  //
  // Prints a warning message for AST node 'tn' (from which the line
  // number is taken).
  // ******************************************************************
  public void raiseMessage(String message) {
    raiseMessage(0,null,0,message);
  }
  public void raiseMessage(int level, String message) {
    raiseMessage(level,null,0,message);
  }
  public void raiseMessage(TNode tn, String message) {
    raiseMessage(0,tn,0,message);
  }
  public void raiseMessage(TNode tn, int lineOffset, String message) {
    raiseMessage(0,tn,lineOffset,message);
  }
  public void raiseMessage(int level, TNode tn, String message) {
    raiseMessage(level,tn,0,message);
  }
  public void raiseMessage(int level, TNode tn, int lineOffset, String message) {
    if (verboseLevel>=level) {
      printPrefix(tn,lineOffset);
      System.out.println(message);
    }
  }

  // ******************************************************************
  // raiseWarning :
  //
  // Prints a warning message for AST node 'tn' (from which the line
  // number is taken).
  // ******************************************************************
  public void raiseWarning(String message) {
    raiseWarning(0,null,0,message);
  } 
  public void raiseWarning(int level, String message) {
    raiseWarning(level,null,0,message);
  }
  public void raiseWarning(TNode tn, String message) {
    raiseWarning(0,tn,0,message);
  }
  public void raiseWarning(TNode tn, int lineOffset, String message) {
    raiseWarning(0,tn,lineOffset,message);
  }
  public void raiseWarning(int level, TNode tn, String message) {
    raiseWarning(level,tn,0,message);
  }
  public void raiseWarning(int level, TNode tn, int lineOffset, String message) {
    if (verboseLevel>=level) {
      printPrefix(tn,lineOffset);
      System.err.println("warning: " + message);
    }
  }


  // ******************************************************************
  // raiseError :
  //
  // Prints an error message for AST node 'tn' (from which the line
  // number is taken). Exit after 5 errors.
  // ******************************************************************
  public void raiseError(String message) {
    raiseError(null,0,message);
  }
  public void raiseError(TNode tn, String message) {
    raiseError(tn,0,message);
  }
  public void raiseError(TNode tn, int lineOffset, String message) {
    printPrefix(tn,lineOffset);
    System.err.println("error: " + message);
    addAnError();
  }


  // ******************************************************************
  // raiseFatalError :
  //
  // Prints an error message and exit directly
  // ******************************************************************
  public void raiseFatalError(String message) {
    raiseFatalError(null,0,message);
  }
  public void raiseFatalError(TNode tn, String message) {
    raiseFatalError(tn,0,message);
  }
  public void raiseFatalError(TNode tn, int lineOffset, String message) {
    printPrefix(tn,lineOffset);
    System.err.println("fatal error: " + message);
    exitWithError();
  }


  // ******************************************************************
  // raiseInternalError :
  //
  // Prints an error message and exit directly
  // ******************************************************************
  public void raiseInternalError(String message) {
    raiseInternalError(null,0,message);
  }
  public void raiseInternalError(TNode tn, String message) {
    raiseInternalError(tn,0,message);
  }
  public void raiseInternalError(TNode tn, int lineOffset, String message) {
    printPrefix(tn,lineOffset);
    System.err.println("fatal error: " + message);
    exitWithError();
  }
  
  
  // ##################################################################
  // Syntax error handling
  // ##################################################################
  
  private void printPrefix(MyToken tn) {
    if (tn==null) {return;}

    String s;
    s=(String)tn.getSource();
    if (s==null) {
      printPrefix();
    }
    else {
      printPrefix(s);
    }
    System.err.print(tn.getLine()+ ": ");
  }

  
  // ******************************************************************
  // raiseSyntaxError :
  //
  // Prints an error message and exit directly, since there is no
  // error recovery
  // ******************************************************************
  public void raiseSyntaxError(MyToken tn, String message) {
    printPrefix(tn);
    System.err.println("syntax error: " + message);
    exitWithError();
  }

}
