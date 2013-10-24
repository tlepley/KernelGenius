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

public class ComputeDevice extends GenericDevice {  
  long  memorySize=0;
  ComputeUnit computeUnit=null;
  ComputeElement computeElement=null;
  int nbComputeUnits = 0;

  //==================================================================
  // Building
  //==================================================================
 
  public ComputeDevice(String s) {
    super(s);
  }

  public void setComputeUnit(ComputeUnit cu, CompilerError ce) {
    if (computeUnit!=null) {
      ce.raiseError("Redefining a compute unit for device '"+name+"'");
    }
    computeUnit=cu;
  }

  public void setComputeElement(ComputeElement cte, CompilerError ce) {
    if (computeElement!=null) {
      ce.raiseError("Redefining a compute element for device '"+name+"'");
    }
    computeElement=cte;
  }

  @Override
  public void setProperty(String prop, CompilerError ce) {
    raiseUnknownPropertyError(prop,ce);
  }

  @Override
  public void SetPropertyWithIdentifier(String prop, String ident,
      CompilerError ce) {
    raiseUnknownIdentifierPropertyError(prop,ce);
  }

  @Override
  public void SetPropertyWithInteger(String prop, long i, CompilerError ce) {
    if (prop.equals("nbComputeUnits")) {
      if (nbComputeUnits>0) {
        ce.raiseError("Redefining nbComputeUnits for device '"+name+"'");
      }
      else if (i<0) {
        ce.raiseError("nbComputeUnits must be strictly positive");
      }
      else {
        nbComputeUnits=(int) i;
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
    if (nbComputeUnits==0) {
      ce.raiseWarning("setting default nbComputeUnits value (1)");
      nbComputeUnits=1;
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
  public ComputeUnit getComputeUnit() {
    return computeUnit;
  }
  public int getNbComputeUnits() {
    return nbComputeUnits;
  }
  public int getNbComputeElements() {
    return nbComputeUnits*computeUnit.getNbComputeElements();
  }
  public ComputeElement getComputeElement() {
    return computeElement;
  }

  
  //==================================================================
  // Verbose
  //==================================================================
 
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Device '").append(getName()).append("': ");
    sb.append("nbComputeUnits=").append(Integer.toString(nbComputeUnits));
    // memory
    if (isMemory()) {
      sb.append(", local memory = ").append(Long.toString(memorySize)).append(" bytes");
    }
    else {
      sb.append(", no local memory");
    }
    sb.append(", computeUnit '").append(computeUnit.getName()).append("'");
    sb.append(", computeElement '").append(computeElement.getName()).append("'");
    sb.append("\n");
    
    sb.append(computeUnit.toString());
    sb.append("\n");
    
    sb.append(computeElement.toString());
    sb.append("\n");
    
    return sb.toString();
  }

}
