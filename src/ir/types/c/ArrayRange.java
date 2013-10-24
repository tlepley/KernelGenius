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

package ir.types.c;

import ir.base.KernelData;
import ir.literals.Literal;
import ir.literals.c.RangeLiteral;
import ir.types.Type;

import java.util.HashMap;
import java.util.HashSet;

import common.CompilerError;

public class ArrayRange extends Array {

  boolean isMaxIndexRange = false;
  int firstIndex = 0;
  int lastIndex = 0;
  KernelData sizeSpecifierData=null;

  public ArrayRange(Type of) {
    super(of);
  }
  
  //==================================================================
  // Setters
  //==================================================================

  public void setMaxNbElements(int i) {
    isMaxIndexRange=true;
    firstIndex=0;
    lastIndex=i-1;
    
    // Array data
    setVariableSize();
    nbElements=i;
  }

  public void setNbElements(int i) {
    firstIndex=0;
    lastIndex=i-1;
    
    // Array data
    super.setNbElements(i);
  }
  
  public void setNbElements(KernelData kd, CompilerError ce) {
    sizeSpecifierData=kd;

    Literal init=kd.getInitializer();
    if (init==null) {
      // No initializer, simply set the array as 'variable'
      setVariableSize();
    }
    else {
      // Must be a range literal
      if (init instanceof RangeLiteral) {
        RangeLiteral rl=(RangeLiteral)init;
        if ((rl.getFirstValue()<0) || (rl.getLastValue()<0) ){
          ce.raiseError("array size specifier variable must have a range value striclty included in the positive space");
        }
        setMaxNbElements((int)rl.getLastValue());
      }
      else {
        ce.raiseError("variable array size specifier variable must have range literal");
      }
    }

  }


  public void setIndexRange(int min, int max) {
    firstIndex=min;
    lastIndex=max;
    
    // Array data
    hasSizeSpecifier=true;
    hasConstantSize=true;
    nbElements=max-min+1;
  }
  
  public void setMaxIndexRange(int min, int max) {
    isMaxIndexRange=true;
    firstIndex=min;
    lastIndex=max;

    // Array data
    setVariableSize();
    nbElements=max-min+1;
  }
  
  
  //==================================================================
  // Getters
  //==================================================================
  
  public boolean isArrayRange() {return true;}


  // Dynamic array with a maximum size specified
  public boolean hasMaxIndexRange() {
    return isMaxIndexRange;
  }

  public int getFirstIndex() {
    return firstIndex;
  }

  public int getLastIndex() {
    return lastIndex;
  }

  public boolean hasSizeSpecifierData() {
    return sizeSpecifierData!=null;
  }

  public KernelData getSizeSpecifierData() {
    return sizeSpecifierData;
  }
  
  
  //==================================================================
  // Verbose functions
  //==================================================================

  //------------------------------------------------------------------
  // toStringInternal:
  //
  // Returns string a textual representation of the type. Use 'ts' and
  // 'cs' to avoid displaying multiple times the same type
  // (and avoid cycles)
  //------------------------------------------------------------------
  public String toStringInternal(HashSet<Type> ts, HashMap<Type,Integer> cs) {
    StringBuffer buff = new StringBuffer();
    buff.append("array[");
    if (hasSizeSpecifier()) {
      if (hasConstantSize()) {
        if (firstIndex!=0) {
          buff.append(firstIndex).append("..").append(lastIndex);
        }
        else {
          buff.append(nbElements);
        }
      }
      else {
        if (isMaxIndexRange) {
          buff.append("<=");
          if (firstIndex!=0) {
            buff.append(firstIndex).append("..").append(lastIndex);
          }
          else {
            buff.append(nbElements);
          }
        }
      }
    }

    buff.append("] of {").append(getElementType().toStringInternal(ts,cs)).append("}");
    return(buff.toString());
  }

}
