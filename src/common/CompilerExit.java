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

package common;

public class CompilerExit extends Error {
  private static final long serialVersionUID = 1L;
  
  private int returnStatus=1;

  public int getReturnStatus() {
     return returnStatus;
  }
  
  public CompilerExit(int status) {
    super();
    returnStatus=status;
  }

  public CompilerExit(int status,String message,Throwable cause) {
    super(message, cause);
    returnStatus=status;
  }

  public CompilerExit(int status,String message) {
    super(message);
    returnStatus=status;
  }

  public CompilerExit(int status,Throwable cause) {
    super(cause);
    returnStatus=status;
  }
  
  //-----------------------
  // Standard constructors
  //-----------------------
  public CompilerExit() {
    super();
  }

  public CompilerExit(String message, Throwable cause) {
    super(message, cause);
  }

  public CompilerExit(String message) {
    super(message);
  }

  public CompilerExit(Throwable cause) {
    super(cause);
  }

}
