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

package target;

import ir.literals.Literal;

import common.CompilerError;

public class ComputeUnit extends GenericDevice {
  boolean dataCache=false;
  long    memorySize=0;
  boolean dataConnectivityToExtDMA=false;
  boolean dataConnectivityToExtLDST=false;
  boolean dataConnectivityToLateralDMA=false;
  boolean dataConnectivityToLateralLDST=false;
  int     nbComputeElements=-1;
  
  //==================================================================
  // Building
  //==================================================================
 
  public ComputeUnit(String s) {
    super(s);
  }

  @Override
  public void setProperty(String prop, CompilerError ce) {
	  if (prop.equals("dataCache")) {
		  if (dataCache) {
			  raiseRedefinePropertyError(prop,ce);
		  }
		  dataCache=true;
	  }
	  else {
		  raiseUnknownPropertyError(prop,ce);
	  }
  }

  @Override
  public void SetPropertyWithIdentifier(String prop, String ident,
      CompilerError ce) {
    if (prop.equals("dataConnectivityToExt")) {
      if (ident.equals("dma")) {
        dataConnectivityToExtDMA=true;  
      }
      else if (ident.equals("loadstore")) {
        dataConnectivityToExtLDST=true;
      }
      else {
        raiseUnknownPropertyValueError(prop,ident,ce);
      }
    }
    else if (prop.equals("dataConnectivityToLateral")) {
      if (ident.equals("dma")) {
        dataConnectivityToLateralDMA=true;  
      }
      else if (ident.equals("loadstore")) {
        dataConnectivityToLateralLDST=true;
      }
      else {
        raiseUnknownPropertyValueError(prop,ident,ce);
      }
    }     
    else {
      raiseUnknownIdentifierPropertyError(prop,ce);
    }
  }

  @Override
  public void SetPropertyWithInteger(String prop, long i, CompilerError ce) {
    if (prop.equals("nbComputeElements")) {
      if (nbComputeElements>0) {
        ce.raiseError("Redefining nbComputeElements for compute unit '"+name+"'");
      }
       if (i<=0) {
        ce.raiseError("nbComputeElements must be strictly positive");
      }
      else {
        nbComputeElements=(int) i;
      }
    }
    else if (prop.equals("memorySize")) {
      if (i<=0) {
        ce.raiseError("memorySize must be strictly positive");
      }
      else {
        memorySize=i;
      } 
    }
    else {
      raiseUnknownIntegerPropertyError(prop,ce);
    }
  }

  @Override
  public void SetPropertyWithLiteral(String prop, Literal array,
      CompilerError ce) {
    raiseUnknownLiteralPropertyError(prop,ce);
  }

  @Override
  public void SetPropertyWithString(String prop, String s, CompilerError ce) {
    raiseUnknownStringPropertyError(prop,ce);
  }

  @Override
  public void finalCheck(CompilerError ce) {
    // Default values
    if (nbComputeElements==0) {
      ce.raiseWarning("setting default nbComputeElements value (1)");
      nbComputeElements=1;
    }
    // Connectivity
    if (!dataConnectivityToExtDMA && !dataConnectivityToExtLDST) {
      ce.raiseError("no Connectivity to the external memory defined for compute unit '"+getName()+"'");
    }
  }

  //==================================================================
  // Getters
  //================================================================== 
  
  public boolean isMemory() {
    return memorySize>=0;
  }
  public long getMemorySize() {
    return memorySize;
  }
  public boolean isExtDMAConnectivity() {
    return dataConnectivityToExtDMA;
  }
  public boolean isExtLoadStoreConnectivity() {
    return dataConnectivityToExtLDST;
  }
  public boolean isLateralConnectivity() {
    return dataConnectivityToLateralDMA || dataConnectivityToLateralLDST;
  }
  public boolean isLateralDMAConnectivity() {
    return dataConnectivityToLateralDMA;
  }
  public boolean isLateralLoadStoreConnectivity() {
    return dataConnectivityToLateralLDST;
  }
  
 public int getNbComputeElements() {
    return nbComputeElements;
  }
  
  //==================================================================
  // Verbose
  //==================================================================

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Compute unit '").append(getName()).append("': ");
    // Compute elements
    sb.append("nbComputeElements=").append(Integer.toString(nbComputeElements));
    // memory
    if (isMemory()) {
      sb.append(", local memory = ").append(Long.toString(memorySize)).append(" bytes");
    }
    else {
      sb.append(", no local memory");
    }
    // Connectivity
    sb.append(", EXTmem connectivity=");
    if (isExtDMAConnectivity())       sb.append(" dma");
    if (isExtLoadStoreConnectivity()) sb.append(" loadstore");
    if (isLateralDMAConnectivity()) {
      sb.append(", Lateral connectivity=");
      if (isLateralDMAConnectivity())       sb.append(" dma");
      if (isLateralLoadStoreConnectivity()) sb.append(" loadstore");
    }
    else {
      sb.append(", no Lateral connectivity");
    }
    return sb.toString();
  }

 
}
