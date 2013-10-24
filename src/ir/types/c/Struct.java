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

/* C Struct type */

package ir.types.c;

import ir.types.Type;

import java.util.HashSet;
import java.util.HashMap;

public class Struct extends StructOrUnion {

  //==================================================================
  // Constructor
  //==================================================================
  public Struct() {
    ;
  }



  //==================================================================
  // Type management
  //==================================================================

  public boolean isStruct() {return true;}



  //==================================================================
  // Target Specific information
  //==================================================================

  public int sizeof() {
    return sizeofAllFields();
  }



  //==================================================================
  // Verbose functions
  //==================================================================

  //------------------------------------------------------------------
  // toStringInternal:
  //
  // Returns string a textual representation of the type. Use 'ts' and
  // 'cs' to 'ts' to avoid displaying multiple times the same type
  // (and avoid cycles)
  //------------------------------------------------------------------
  public String toStringInternal(HashSet<Type> ts, HashMap<Type,Integer> cs) {
    StringBuffer buff = new StringBuffer();
    buff.append("struct ").append(super.toStringInternal(ts,cs));
    return(buff.toString());
  }
}
