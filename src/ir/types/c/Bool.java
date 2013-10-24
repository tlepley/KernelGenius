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

/* C boolean type */

package ir.types.c;

import ir.types.Type;

import java.util.HashSet;
import java.util.HashMap;

public class Bool extends ArithmeticScalar {

  static public final Bool Tbool=new Bool();


  //==================================================================
  // Private constructor
  //==================================================================

  private Bool() {
    ;
  }


  //==================================================================
  // Type management
  //==================================================================

  public boolean isIntegralScalar() {return true;}


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
    return IntegerScalar.Tsint;
  }


  //==================================================================
  // Signature management (for arguments of function prototypes)
  //==================================================================

  public String getSignature() {
    return "b";
  }

  //==================================================================
  // Target Specific information
  //==================================================================

  public int sizeof() {
    return Type.getSourceABI().getIntSize();
  }

  public int alignof() {
    return Type.getSourceABI().getIntAlignment();
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
    return("_Bool");
  }

  //------------------------------------------------------------------
  // dump :
  //
  // Returns the original C type syntax
  //------------------------------------------------------------------
  public String dump() {
    return("_Bool");
  }
}
