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

package ir.symbolTable;

import ir.base.KernelData;
import ir.types.Type;

public class KernelParameterLabel extends Symbol {
  
  private KernelData kernelData=null;

  public KernelParameterLabel(String s, KernelData kp, Type t) {
    super(s,t);
    kernelData=kp;
  }
 
  //========================================================
  // Getters
  //========================================================
  
  public KernelData getKernelData() {
    return kernelData;
  }

}
