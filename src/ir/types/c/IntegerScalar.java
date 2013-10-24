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

/* C integer scalar types */

package ir.types.c;

import ir.types.Type;

import java.util.HashSet;
import java.util.HashMap;

public class IntegerScalar extends ArithmeticScalar {

  // Integral integer types
  public static final IntegerScalar Tschar     = new IntegerScalar(IntegerBaseType.CHAR,     SignProperty.SIGNED);
  public static final IntegerScalar Tuchar     = new IntegerScalar(IntegerBaseType.CHAR,     SignProperty.UNSIGNED);
  public static final IntegerScalar Tsshort    = new IntegerScalar(IntegerBaseType.SHORT_INT,SignProperty.SIGNED);
  public static final IntegerScalar Tushort    = new IntegerScalar(IntegerBaseType.SHORT_INT,SignProperty.UNSIGNED);
  public static final IntegerScalar Tsint      = new IntegerScalar(IntegerBaseType.INT,      SignProperty.SIGNED);
  public static final IntegerScalar Tuint      = new IntegerScalar(IntegerBaseType.INT,      SignProperty.UNSIGNED);
  public static final IntegerScalar Tslong     = new IntegerScalar(IntegerBaseType.LONG_INT, SignProperty.SIGNED);
  public static final IntegerScalar Tulong     = new IntegerScalar(IntegerBaseType.LONG_INT, SignProperty.UNSIGNED);
  public static final IntegerScalar Tslonglong = new IntegerScalar(IntegerBaseType.LONG_LONG_INT, SignProperty.SIGNED);
  public static final IntegerScalar Tulonglong = new IntegerScalar(IntegerBaseType.LONG_LONG_INT, SignProperty.UNSIGNED);

  // Query functions
  public boolean isSchar() {return this==Tschar;}
  public boolean isUchar() {return this==Tuchar;}
  public boolean isSshort() {return this==Tsshort;}
  public boolean isUshort() {return this==Tushort;}
  public boolean isSint() {return this==Tsint;}
  public boolean isUint() {return this==Tuint;}
  public boolean isSlong() {return this==Tslong;}
  public boolean isUlong() {return this==Tulong;}
  public boolean isSlonglong() {return this==Tslonglong;}
  public boolean isUlonglong() {return this==Tulonglong;}


  public IntegerScalar getUnsignedVersion() {
    switch(baseType) {
    case CHAR:
      return Tuchar;
    case SHORT_INT:
      return Tushort;
    case INT:
      return Tuint;
    case LONG_INT:
      return Tulong;
    case LONG_LONG_INT:
      return Tulonglong;
    default:
      // Should never happen
      return null;
    }
  }

  // Specifiers
  public enum IntegerBaseType {
    CHAR, SHORT_INT, INT, LONG_INT, LONG_LONG_INT
  };

  //==================================================================
  // Private data
  //==================================================================
  protected enum SignProperty {
    SIGNED, UNSIGNED
  };

  IntegerBaseType baseType=null;
  SignProperty signProperty=SignProperty.SIGNED;


  //==================================================================
  // Private Constructor
  //==================================================================
  private IntegerScalar(IntegerBaseType t, SignProperty s) {
    baseType=t;
    signProperty=s;
  }


  //==================================================================
  // Type class generic methods
  //==================================================================
  public boolean isIntegralScalar() {return true;}

  public boolean isIntegerScalar() {return true;}
  public boolean isUnsignedIntegerScalar() {return isUnsigned();}
  public boolean isSignedIntegerScalar() {return isSigned();}

  public boolean isCharScalar() {return isChar();}
  public boolean isShortScalar() {return isShort();}
  public boolean isIntScalar() {return isInt();}
  public boolean isLongScalar() {return isLong();}
  public boolean isLongLongScalar() {return isLongLong();}


  //==================================================================
  // Getters
  //==================================================================
 
  //------------------------------------------------------------------
  // getBaseType
  //
  // Returns base type of the integer scalar type
  //------------------------------------------------------------------
  public IntegerBaseType getBaseType() {
    return baseType;
  }
  public boolean isChar() {return baseType==IntegerBaseType.CHAR;}
  public boolean isShort() {return baseType==IntegerBaseType.SHORT_INT;}
  public boolean isInt() {return baseType==IntegerBaseType.INT;}
  public boolean isLong() {return baseType==IntegerBaseType.LONG_INT;}
  public boolean isLongLong() {return baseType==IntegerBaseType.LONG_LONG_INT;}


  //------------------------------------------------------------------
  // getSignProperty
  //
  // Returns the sign property of the integer scalar type
  //------------------------------------------------------------------
  public boolean isSigned() {
    return(signProperty==SignProperty.SIGNED);
  }
  public boolean isUnsigned() {
    return(signProperty==SignProperty.UNSIGNED);
  }


  //==================================================================
  // Signature management (for arguments of function prototypes)
  //==================================================================

  //------------------------------------------------------------------
  // getSignature
  //
  // Returns a string corresponding to the signature of the type
  // (for function mangling)
  //------------------------------------------------------------------
  public String getSignature() {
    switch(baseType) {
    case CHAR:
      if (isSigned()) {
	return "c";
      }
      else {
	return "h";
      }
    case SHORT_INT:
      if (isSigned()) {
	return "s";
      }
      else {
	return "t";
      }
    case INT:
      if (isSigned()) {
	return "i";
      }
      else {
	return "j";
      }
    case LONG_INT:
      if (isSigned()) {
	return "l";
      }
      else {
	return "m";
      }
    case LONG_LONG_INT:
      if (isSigned()) {
	return "x";
      }
      else {
	return "y";
      }
    }
    // Should never occur
    return null;
  }


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
    switch (baseType) {
    case CHAR:
      return Tsint;
    case SHORT_INT:
      return Tsint;
    case INT:
      return this;
    case LONG_INT:
      return this;
    case LONG_LONG_INT:
      return this;
    }
    return this;
  }



  //==================================================================
  // Target Specific information
  //==================================================================

  public int sizeof() {
    switch (baseType) {
    case CHAR:
      return Type.getSourceABI().getCharSize();
    case SHORT_INT:
      return Type.getSourceABI().getShortSize();
    case INT:
      return Type.getSourceABI().getIntSize();
    case LONG_INT:
      return Type.getSourceABI().getLongSize();
    case LONG_LONG_INT:
      return Type.getSourceABI().getLonglongSize();
    default:
      // Error
      return 0;
    }
  }

  public int alignof() {
    switch (baseType) {
    case CHAR:
      return Type.getSourceABI().getCharAlignment();
    case SHORT_INT:
      return Type.getSourceABI().getShortAlignment();
    case INT:
      return Type.getSourceABI().getIntAlignment();
    case LONG_INT:
      return Type.getSourceABI().getLongAlignment();
    case LONG_LONG_INT:
      return Type.getSourceABI().getLonglongAlignment();
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

    switch (signProperty) {
    case UNSIGNED: buff.append("unsigned "); break;
    default:
    }

    switch (baseType) {
    case CHAR: buff.append("char"); break;
    case SHORT_INT: buff.append("short int"); break;
    case INT: buff.append("int"); break;
    case LONG_INT: buff.append("long int"); break;
    default:
      // Internal error
    }

    return(buff.toString());
  }

  //------------------------------------------------------------------
  // dump :
  //
  // Returns the original C type syntax
  //------------------------------------------------------------------
  public String dump() {
    StringBuffer buff = new StringBuffer();

    switch (signProperty) {
    case UNSIGNED: buff.append("u"); break;
    default:
    }

    switch (baseType) {
    case CHAR: buff.append("char"); break;
    case SHORT_INT: buff.append("short"); break;
    case INT: buff.append("int"); break;
    case LONG_INT: buff.append("long"); break;
    default:
      // Internal error
    }

    return(buff.toString());
  }
}

