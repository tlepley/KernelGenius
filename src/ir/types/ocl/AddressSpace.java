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

/* OpenCL C address space qualifier */

package ir.types.ocl;

public enum AddressSpace {
  NO("<NO>", ""),

  GLOBAL("__global", "g"),
  CONSTANT("__constant", "c"),
  LOCAL("__local", "l"),
  PRIVATE("__private", "p"),

  // Code (for internal use, not defined by OCL)
  CODE("[__code]",null);

  private final String name;
  private final String signature;

  // Constructor
  private AddressSpace(String s, String s2) {
    name = s;
    signature = s2;
  }

  // Getters
  public String getName() {return name;}
  public String getSignature() {return signature;}
}
