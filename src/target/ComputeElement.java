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

public class ComputeElement extends GenericDevice {
  enum TYPE_SUPPORT {INIT, NO, SOFT, NATIVE};
  
  // Memory & connectivity
  boolean dataCache=false;
  long    memorySize=0;
  boolean dataConnectivityToExtDMA=false;
  boolean dataConnectivityToExtLDST=false;
  boolean dataConnectivityToLateralDMA=false;
  boolean dataConnectivityToLateralLDST=false;
  
  // Parallelism
  int nbHwThreads = -1;
  boolean swThreads = false;
  
  // Type emulation
  TYPE_SUPPORT longSupport=TYPE_SUPPORT.INIT;
  TYPE_SUPPORT doubleSupport=TYPE_SUPPORT.INIT;;
  TYPE_SUPPORT floatSupport=TYPE_SUPPORT.INIT;
  
  //==================================================================
  // Building
  //==================================================================

  public ComputeElement(String s) {
    super(s);
  }
  
  @Override
  public void setProperty(String prop, CompilerError ce) {
    if (prop.equals("swThreads")) {
      if (swThreads) {
        raiseRedefinePropertyError(prop,ce);
      }
      swThreads=true;
    }
    else if (prop.equals("dataCache")) {
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
    // Connectivity
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
    
    // Type support
    else if (prop.equals("Tfloat")) {
      if (floatSupport!=TYPE_SUPPORT.INIT) {
        raiseRedefinePropertyError(prop,ce);
      }
      if (ident.equals("soft")) {
        floatSupport=TYPE_SUPPORT.SOFT;
      }
      else if (ident.equals("native")) {
        floatSupport=TYPE_SUPPORT.NATIVE;
      }
      else {
        raiseUnknownPropertyValueError(prop,ident,ce);
      }
    }
    else if (prop.equals("Tdouble")) {
      if (doubleSupport!=TYPE_SUPPORT.INIT) {
        raiseRedefinePropertyError(prop,ce);
      }
      if (ident.equals("no")) {
        doubleSupport=TYPE_SUPPORT.NO;
      }
      else if (ident.equals("soft")) {
        doubleSupport=TYPE_SUPPORT.SOFT;
      }
      else if (ident.equals("native")) {
        doubleSupport=TYPE_SUPPORT.NATIVE;
      }
      else {
        raiseUnknownPropertyValueError(prop,ident,ce);
      }
    }
    else if (prop.equals("Tlong")) {
      if (longSupport!=TYPE_SUPPORT.INIT) {
        raiseRedefinePropertyError(prop,ce);
      }
      if (ident.equals("no")) {
        longSupport=TYPE_SUPPORT.NO;
     }
      else if (ident.equals("soft")) {
        longSupport=TYPE_SUPPORT.SOFT;
      }
      else if (ident.equals("native")) {
        longSupport=TYPE_SUPPORT.NATIVE;
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
    if (prop.equals("memorySize")) {
      if (i<=0) {
        raiseMessagePropertyError(prop," must be strictly positive",ce);
      }
      else {
        memorySize=i;
      } 
    }
    else if (prop.equals("nbHwThreads")) {
      if (nbHwThreads>0) {
        raiseRedefinePropertyError(prop,ce);
      }
      if (i<=0) {
        raiseMessagePropertyError(prop," must be strictly positive",ce);
      }
      else {
        nbHwThreads=(int) i;
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
    // Type support
    if (longSupport==TYPE_SUPPORT.INIT) {
      longSupport=TYPE_SUPPORT.NATIVE;
    }
    if (floatSupport==TYPE_SUPPORT.INIT) {
      floatSupport=TYPE_SUPPORT.NATIVE;
    }
    if (doubleSupport==TYPE_SUPPORT.INIT) {
      doubleSupport=TYPE_SUPPORT.NATIVE;
    }
    // Connectivity
    if (!dataConnectivityToExtDMA && !dataConnectivityToExtLDST) {
      ce.raiseError("no Connectivity to the external memory defined for compute element '"+getName()+"'");
    }
    // Parallelism
    if (nbHwThreads<0) {
      nbHwThreads=1;
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
  public boolean isDataCache() {
    return dataCache;
  }
  
  public int getNbHwThreads() {
    return nbHwThreads;
  }
  public boolean hasSwThreads() {
    return swThreads;
  }

  TYPE_SUPPORT getLongSupport() {
    return longSupport;
  }
  TYPE_SUPPORT getFloatSupport() {
    return floatSupport;
  }
  TYPE_SUPPORT getDoubleSupport() {
    return doubleSupport;
  }

  
  //==================================================================
  // Verbose
  //==================================================================

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Compute element '").append(getName()).append("': ");
    // memory
    if (isMemory()) {
      sb.append("private memory = ").append(Long.toString(memorySize)).append(" bytes");
    }
    else {
      sb.append("no local memory");
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
    if (isDataCache()) {sb.append(", data cache");}
    else {sb.append(", no data cache");}
    sb.append(", ").append(Integer.toString(nbHwThreads)).append(" hw threads");
    if (hasSwThreads()) {sb.append(", sw threads");}
    else {sb.append(", no sw threads");}
    sb.append(", ").append(longSupport.toString()).append(" long");
    sb.append(", ").append(floatSupport.toString()).append(" float");
    sb.append(", ").append(doubleSupport.toString()).append(" double");
    
    return sb.toString();
  }

}
