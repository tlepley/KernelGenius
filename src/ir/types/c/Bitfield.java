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

/* C bitfield */

package ir.types.c;

import ir.types.Type;

import java.util.HashSet;
import java.util.HashMap;


public class Bitfield extends ArithmeticScalar {

  //==================================================================
  // Private data
  //==================================================================
  Type baseType=null; // _Bool, int or unsigned int

  // Size of the bitfield
  int size_in_bits=1;


  //==================================================================
  // Constructor
  //==================================================================
  public Bitfield(Type t, int s) {
    baseType=t;
    size_in_bits=s;
  }


  //==================================================================
  // Type management
  //==================================================================

  public boolean isIntegralScalar() {return true;}

  public boolean isBitfield() {return true;}

  //==================================================================
  // Conversion Management
  //==================================================================
  
  //------------------------------------------------------------------
  // promote:
  //
  // Returns the type to which it must be converted in case of
  // promotion.
  //------------------------------------------------------------------
  public Type promote() {
    return baseType.promote();
  }


  //==================================================================
  // Getters
  //==================================================================

  //------------------------------------------------------------------
  // getBaseType
  //
  // Returns the base type of the bitfield
  //------------------------------------------------------------------
  public Type getBaseType() {
    return baseType;
  }
 
  //------------------------------------------------------------------
  // getSize
  //
  // Returns the size of the bitfield
  //------------------------------------------------------------------
  public int getSizeInBits() {
    return size_in_bits;
  }
 
  public int getSizeInBytes() {
    return ((size_in_bits-1)>>3)+1;
  }
 

  // Signature: should never be called
  public String getSignature() {
    return null;
  }

  //==================================================================
  // Target Specific information
  //==================================================================
  
  public int sizeof() {
    return baseType.sizeof();
  }

  public int alignof() {
    return baseType.alignof();
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
  public String toStringInternal(HashSet<Type> ts,
				    HashMap<Type,Integer> cs) {
    return dump();
  }

  //------------------------------------------------------------------
  // dump :
  //
  // Returns the original C type syntax
  //------------------------------------------------------------------
  public String dump() {
    StringBuffer buff = new StringBuffer();
    buff.append(((ArithmeticScalar)baseType).dump()+":"+size_in_bits);
    return(buff.toString());
  }

}
