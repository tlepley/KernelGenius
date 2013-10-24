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

/* C integer floating point types */

package ir.types.c;

import ir.types.Type;

import java.util.HashMap;
import java.util.HashSet;

public class FloatingPointScalar extends ArithmeticScalar {

  // Integral floating point types
  public static final FloatingPointScalar Tfloat      = new FloatingPointScalar(FloatingType.FLOAT);
  public static final FloatingPointScalar Tdouble     = new FloatingPointScalar(FloatingType.DOUBLE);
  public static final FloatingPointScalar Tlongdouble = new FloatingPointScalar(FloatingType.LONG_DOUBLE);

  // Specifiers
  public enum FloatingType {
    FLOAT, DOUBLE, LONG_DOUBLE
  };


  //==================================================================
  // Private data
  //==================================================================
  private FloatingType baseType=null;


  //==================================================================
  // Private constructor
  //==================================================================
  private FloatingPointScalar(FloatingType t) {
    baseType=t;
  }


  //==================================================================
  // Type class generic methods
  //==================================================================

  public boolean isFloatingPointScalar() {return true;}
  public boolean isFloatScalar() {return isFloat();}
  public boolean isDoubleScalar() {return isDouble();}
  public boolean isLongDoubleScalar() {return isLongDouble();}


  //==================================================================
  // Getters
  //==================================================================

  //------------------------------------------------------------------
  // getBaseType
  //
  // Returns base type of the floating point scalar type
  //------------------------------------------------------------------
  public FloatingType getBaseType() {
    return baseType;
  }
  public boolean isFloat() {return baseType==FloatingType.FLOAT;}
  public boolean isDouble() {return baseType==FloatingType.DOUBLE;}
  public boolean isLongDouble() {return baseType==FloatingType.LONG_DOUBLE;}


  //==================================================================
  // Signature management (for arguments of function prototypes)
  //==================================================================

  public String getSignature() {
    switch(baseType) {
    case FLOAT:
      return "f";
    case DOUBLE:
      return "d";
    case LONG_DOUBLE:
      return "e";
    }
    // Should never occur
    return null;
  }


  //==================================================================
  // Target Specific information
  //==================================================================

  public int sizeof() {
    switch (baseType) {
    case FLOAT:
      return Type.getSourceABI().getFloatSize();
    case DOUBLE:
      return Type.getSourceABI().getDoubleSize();
    case LONG_DOUBLE:
      return Type.getSourceABI().getLongdoubleSize();
    default:
      // Error
      return 0;
    }
  }

  public int alignof() {
    switch (baseType) {
    case FLOAT:
      return Type.getSourceABI().getFloatAlignment();
    case DOUBLE:
      return Type.getSourceABI().getDoubleAlignment();
    case LONG_DOUBLE:
      return Type.getSourceABI().getLongdoubleAlignment();
    default:
      // Error
      return 0;
    }
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

    switch (baseType) {
    case FLOAT: buff.append("float"); break;
    case DOUBLE: buff.append("double"); break;
    case LONG_DOUBLE: buff.append("long double"); break;
    default:
      // Internal error
    }

    return(buff.toString());
  }

  //------------------------------------------------------------------
  // dump :
  //
  // Returns the original type syntax
  //------------------------------------------------------------------
  public String dump() {
    switch (baseType) {
    case FLOAT: return "float";
    case DOUBLE: return "double";
    case LONG_DOUBLE: return "long double";
    default:
      // Internal error
      return null;
    }
  }
  
}
