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
  long deviceLevelCacheSize=0;
  long deviceLevelMemorySize=0;
  int nbComputeUnits = 0;

  // Assumes an homogeneous device
  ComputeUnit computeUnit=null;
  ComputeElement computeElement=null;

  
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
        deviceLevelMemorySize=i;
      } 
    }
    else if (prop.equals("cacheSize")) {
        if (i<=0) {
          ce.raiseError("cacheSize must be strictly positive");
        }
        else {
          deviceLevelCacheSize=i;
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
 
	public boolean hasDeviceLevelCache() {
		return deviceLevelCacheSize>=0;
	}
	public long getDeviceLevelCacheSize() {
		return deviceLevelCacheSize;
	}
  public boolean hasDeviceLevelMemory() {
    return deviceLevelMemorySize>=0;
  }
  public long getDeviceLevelMemorySize() {
    return deviceLevelMemorySize;
  }
  public boolean hasUnitLevelMemory() {
    return computeUnit.hasLocalMemory();
  }
  public long getUnitLevelMemorySize() {
    return computeUnit.getLocalMemorySize();
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
    // Cache/memory
    if (hasDeviceLevelCache()) {
      sb.append(", device level cache = ").append(Long.toString(deviceLevelCacheSize)).append(" bytes");
    }
    else {
      sb.append(", no device level cache");
    }
    if (hasDeviceLevelMemory()) {
      sb.append(", device level memory = ").append(Long.toString(deviceLevelMemorySize)).append(" bytes");
    }
    else {
      sb.append(", no device level memory");
    }
    sb.append("nbComputeUnits=").append(Integer.toString(nbComputeUnits));
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
