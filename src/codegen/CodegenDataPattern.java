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

/* Class containing relevant code generation information */

package codegen;


public class CodegenDataPattern {
  // Tiling patterns
  public enum ComputeGranularity {
    NO,
    
    // The standard pattern today
    D2_BY_RAW,
    
    // 1D data
    D1_INTRINSIC, D1_BY_1D_CHUNKS,
    D2_INTRINSIC, D2_BY_1D_CHUNKS,
    
    // 2D data
    D2_BY_COLUMN, 
    D2_BY_COLUMN_2D_BLOCK
    
    // Do we need those ?
    ,D2_BY_RAW_CIRCULAR_BUFFER,D2_BY_COLUMN_CIRCULAR_BUFFER,
  };
  
  ComputeGranularity accessType=ComputeGranularity.D2_BY_RAW;
  
  // Scheduling and buffer allocation information
  int schedulingTime=-1;
  int nbBufferSlot=0;

  
  //========================================================
  // Building
  //========================================================

  public CodegenDataPattern() {}
     
  public void setAccessType(ComputeGranularity cg) {
    accessType=cg;
  }
  
  
  //========================================================
  // Getters
  //========================================================

  public ComputeGranularity getDataAccessType() {
    return accessType;
  }
   
  public boolean hasAccessType() {
    return accessType!=ComputeGranularity.NO;
  }
  
  //========================================================
  // Scheduling and buffer allocation
  //========================================================
  public void setSchedulingCycle(int s) {
    schedulingTime=s;
  } 
  public int getSchedulingCycle() {
    return schedulingTime;
  }
  public boolean isScheduled() {
    return schedulingTime >= 0;
  }
  
  public void setNbBufferSlot(int s) {
    nbBufferSlot=s;
  }
   public int getNbBufferSlot() {
    return nbBufferSlot;
  }
   
  
  //========================================================
  // Code generation
  //========================================================

  // TODO: seems to be not used anymore : to remove
 
  // D1_INTRINSIC    : pointer to element/array of elements
  // D1_BY_1D_CHUNKS : pointer to array of elements
  // D2_INTRINSIC    : pointer to array of elements
  // D2_BY_1D_CHUNKS : pointer to array of elements
  // D2_BY_RAW       : Pointer to array of elements
  // D2_BY_RAW_CIRCULAR_BUFFER : Pointer to array of elements
  // D2_BY_COLUMN    : pointer to array of elements
  // D2_BY_COLUMN_CIRCULAR_BUFFER : Pointer to array of elements
  // D2_BY_2D_BLOCK  : ???
  
 /* public void generateDeclaration(PrintStream ps, Type baseCType, String name) {
    switch(accessType) {
    case D1_BY_1D_CHUNKS :
    case D2_BY_1D_CHUNKS :
    case D2_BY_RAW :
    case D2_BY_COLUMN :
    case D2_BY_RAW_CIRCULAR_BUFFER :
    case D2_BY_COLUMN_CIRCULAR_BUFFER:
      ps.print("local ");
      break;
    case D1_INTRINSIC :
    case D2_INTRINSIC :
      break;
    default:
      System.err.println("generateDeclaration: Not supported pattern "+accessType);
    }

    baseCType.generate(ps);

    switch(accessType) {
    case D1_BY_1D_CHUNKS :
    case D2_BY_1D_CHUNKS :
    case D2_BY_RAW :
    case D2_BY_COLUMN :
    case D1_INTRINSIC :
    case D2_INTRINSIC :
      ps.print(" *");
      break;
    case D2_BY_RAW_CIRCULAR_BUFFER :
    case D2_BY_COLUMN_CIRCULAR_BUFFER:
      ps.print(" **");
      break;
    default:
      System.err.println("generateDeclaration: Not supported pattern "+accessType);
    }
    ps.print(" ");
    ps.print(name);
  }

*/
  
  //========================================================
  // Verbose
  //========================================================
 
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(" ").append(accessType.toString());
    sb.append(" sched=").append(schedulingTime);
    sb.append(" slots=").append(nbBufferSlot);
    return sb.toString();
  }
}
