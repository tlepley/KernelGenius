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

/* OpenCL C integer vector types */

package ir.types.ocl;

import ir.types.Type;
import ir.types.c.IntegerScalar;

import java.util.HashMap;
import java.util.HashSet;


public class IntegerVector extends Vector {
  // 'char' vector types
  public static final IntegerVector Tschar2   = new IntegerVector(IntegerScalar.Tschar,2);
  public static final IntegerVector Tschar3   = new IntegerVector(IntegerScalar.Tschar,3);
  public static final IntegerVector Tschar4   = new IntegerVector(IntegerScalar.Tschar,4);
  public static final IntegerVector Tschar8   = new IntegerVector(IntegerScalar.Tschar,8);
  public static final IntegerVector Tschar16  = new IntegerVector(IntegerScalar.Tschar,16);

  public static final IntegerVector Tuchar2   = new IntegerVector(IntegerScalar.Tuchar,2);
  public static final IntegerVector Tuchar3   = new IntegerVector(IntegerScalar.Tuchar,3);
  public static final IntegerVector Tuchar4   = new IntegerVector(IntegerScalar.Tuchar,4);
  public static final IntegerVector Tuchar8   = new IntegerVector(IntegerScalar.Tuchar,8);
  public static final IntegerVector Tuchar16  = new IntegerVector(IntegerScalar.Tuchar,16);

  // 'short' vector types
  public static final IntegerVector Tsshort2  = new IntegerVector(IntegerScalar.Tsshort,2);
  public static final IntegerVector Tsshort3  = new IntegerVector(IntegerScalar.Tsshort,3);
  public static final IntegerVector Tsshort4  = new IntegerVector(IntegerScalar.Tsshort,4);
  public static final IntegerVector Tsshort8  = new IntegerVector(IntegerScalar.Tsshort,8);
  public static final IntegerVector Tsshort16 = new IntegerVector(IntegerScalar.Tsshort,16);

  public static final IntegerVector Tushort2  = new IntegerVector(IntegerScalar.Tushort,2);
  public static final IntegerVector Tushort3  = new IntegerVector(IntegerScalar.Tushort,3);
  public static final IntegerVector Tushort4  = new IntegerVector(IntegerScalar.Tushort,4);
  public static final IntegerVector Tushort8  = new IntegerVector(IntegerScalar.Tushort,8);
  public static final IntegerVector Tushort16 = new IntegerVector(IntegerScalar.Tushort,16);

  // 'int' vector types
  public static final IntegerVector Tsint2    = new IntegerVector(IntegerScalar.Tsint,2);
  public static final IntegerVector Tsint3    = new IntegerVector(IntegerScalar.Tsint,3);
  public static final IntegerVector Tsint4    = new IntegerVector(IntegerScalar.Tsint,4);
  public static final IntegerVector Tsint8    = new IntegerVector(IntegerScalar.Tsint,8);
  public static final IntegerVector Tsint16   = new IntegerVector(IntegerScalar.Tsint,16);

  public static final IntegerVector Tuint2    = new IntegerVector(IntegerScalar.Tuint,2);
  public static final IntegerVector Tuint3    = new IntegerVector(IntegerScalar.Tuint,3);
  public static final IntegerVector Tuint4    = new IntegerVector(IntegerScalar.Tuint,4);
  public static final IntegerVector Tuint8    = new IntegerVector(IntegerScalar.Tuint,8);
  public static final IntegerVector Tuint16   = new IntegerVector(IntegerScalar.Tuint,16);

  // 'long' vector types
  public static final IntegerVector Tslong2   = new IntegerVector(IntegerScalar.Tslong,2);
  public static final IntegerVector Tslong3   = new IntegerVector(IntegerScalar.Tslong,3);
  public static final IntegerVector Tslong4   = new IntegerVector(IntegerScalar.Tslong,4);
  public static final IntegerVector Tslong8   = new IntegerVector(IntegerScalar.Tslong,8);
  public static final IntegerVector Tslong16  = new IntegerVector(IntegerScalar.Tslong,16);

  public static final IntegerVector Tulong2   = new IntegerVector(IntegerScalar.Tulong,2);
  public static final IntegerVector Tulong3   = new IntegerVector(IntegerScalar.Tulong,3);
  public static final IntegerVector Tulong4   = new IntegerVector(IntegerScalar.Tulong,4);
  public static final IntegerVector Tulong8   = new IntegerVector(IntegerScalar.Tulong,8);
  public static final IntegerVector Tulong16  = new IntegerVector(IntegerScalar.Tulong,16);

  // No automatic promotion for vectors in OCL
  private static final IntegerVector charArray[]  ={null,null,Tschar2 ,Tschar3 ,Tschar4 ,null,null,null ,
					      Tschar8, null,null,null,null,null,null,null ,Tschar16};
  private static final IntegerVector ucharArray[] ={null,null,Tuchar2 ,Tuchar3 ,Tuchar4 ,null,null,null ,
					      Tuchar8, null,null,null,null,null,null,null ,Tuchar16};
  private static final IntegerVector shortArray[] ={null,null,Tsshort2,Tsshort3,Tsshort4,null,null,null ,
					      Tsshort8,null,null,null,null,null,null,null ,Tsshort16};
  private static final IntegerVector ushortArray[]={null,null,Tushort2,Tushort3,Tushort4,null,null,null ,
					      Tushort8,null,null,null,null,null,null,null ,Tushort16};
  private static final IntegerVector intArray[]   ={null,null,Tsint2  ,Tsint3  ,Tsint4  ,null,null,null ,
					      Tsint8,  null,null,null,null,null,null,null ,Tsint16};
  private static final IntegerVector uintArray[]  ={null,null,Tuint2  ,Tuint3  ,Tuint4  ,null,null,null ,
					      Tuint8,  null,null,null,null,null,null,null ,Tuint16};
  private static final IntegerVector longArray[]  ={null,null,Tslong2 ,Tslong3 ,Tslong4 ,null,null,null ,
					      Tslong8, null,null,null,null,null,null,null ,Tslong16};
  private static final IntegerVector ulongArray[] ={null,null,Tulong2 ,Tulong3 ,Tulong4 ,null,null,null ,
					      Tulong8, null,null,null,null,null,null,null ,Tulong16};


  public IntegerVector getUnsignedVersion() {
    switch(baseType.getBaseType()) {
    case CHAR:
      return getUcharVector(getNbElements());
    case SHORT_INT:
      return getUshortVector(getNbElements());
    case INT:
      return getUintVector(getNbElements());
    case LONG_INT:
      return getUlongVector(getNbElements());
    default:
      // Should never happen
      return null;
    }
  }

  static public IntegerVector getScharVector(int n) {
    return charArray[n];
  }
  static public IntegerVector getUcharVector(int n) {
    return ucharArray[n];
  }
  static public IntegerVector getSshortVector(int n) {
    return shortArray[n];
  }
  static public IntegerVector getUshortVector(int n) {
    return ushortArray[n];
  }
  static public IntegerVector getSintVector(int n) {
    return intArray[n];
  }
  static public IntegerVector getUintVector(int n) {
    return uintArray[n];
  }
  static public IntegerVector getSlongVector(int n) {
    return longArray[n];
  }
  static public IntegerVector getUlongVector(int n) {
    return ulongArray[n];
  }

  static public IntegerVector getVectorType(IntegerScalar t, int n) {
    // Check for correct range
    if ((n<0)||(n>16)) {
      return null;
    }
    
    if (t==IntegerScalar.Tschar) {
      return getScharVector(n);
    }
    else if (t==IntegerScalar.Tuchar) {
      return getUcharVector(n);
    }
    if (t==IntegerScalar.Tsshort) {
      return getSshortVector(n);
    }
    else if (t==IntegerScalar.Tushort) {
      return getUshortVector(n);
    }
    if (t==IntegerScalar.Tsint) {
      return getSintVector(n);
    }
    else if (t==IntegerScalar.Tuint) {
      return getUintVector(n);
    }
    if (t==IntegerScalar.Tslong) {
      return getSlongVector(n);
    }
    else if (t==IntegerScalar.Tulong) {
      return getUlongVector(n);
    }
    else {
      // Internal error
      return null;
    }
  }



  //==================================================================
  // Private data
  //==================================================================
  IntegerScalar baseType;

  //==================================================================
  // Private Constructor
  //==================================================================
  private IntegerVector(IntegerScalar base_type, int n) {
    super(n);
    baseType=base_type;
  }

  //==================================================================
  // Type class generic methods
  //==================================================================
  public boolean isIntegralVector() {return true;}

  public boolean isUnsignedIntegerVector() {return baseType.isUnsigned();}
  public boolean isSignedIntegerVector() {return baseType.isSigned();}

  public boolean isCharVector() {return hasCharElements();}
  public boolean isShortVector() {return hasShortElements();}
  public boolean isIntVector() {return hasIntElements();}
  public boolean isLongVector() {return hasLongElements();}


  //==================================================================
  // Getters
  //==================================================================
 
  //------------------------------------------------------------------
  // getBaseType
  //
  // Returns base type of the integer vector type
  //------------------------------------------------------------------
  public IntegerScalar getBaseType() {
    return baseType;
  }
  public boolean hasCharElements() {return baseType.isChar();}
  public boolean hasShortElements() {return baseType.isShort();}
  public boolean hasIntElements() {return baseType.isInt();}
  public boolean hasLongElements() {return baseType.isLong();}


  //------------------------------------------------------------------
  // getSignProperty
  //
  // Returns the sign property of the integer vector type
  //------------------------------------------------------------------
  public boolean isSigned() {
    return(baseType.isSigned());
  }
  public boolean isUnsigned() {
    return(baseType.isUnsigned());
  }


  //------------------------------------------------------------------
  // getEquivalentType
  //
  // Returns the equivalent vector of size n.
  // Returns a scalar in case n==1
  // Returns null in case of non allowed n
  //------------------------------------------------------------------
  public Type getEquivalentType(int n) {
    // Check for correct range
    if ((n<0)||(n>16)) {
      return null;
    }

    if (n==1) {
      return baseType;
    }
    else if (baseType==IntegerScalar.Tschar) {
      return getScharVector(n);
    }
    else if (baseType==IntegerScalar.Tuchar) {
      return getUcharVector(n);
    }
    if (baseType==IntegerScalar.Tsshort) {
      return getSshortVector(n);
    }
    else if (baseType==IntegerScalar.Tushort) {
      return getUshortVector(n);
    }
    if (baseType==IntegerScalar.Tsint) {
      return getSintVector(n);
    }
    else if (baseType==IntegerScalar.Tuint) {
      return getUintVector(n);
    }
    if (baseType==IntegerScalar.Tslong) {
      return getSlongVector(n);
    }
    else if (baseType==IntegerScalar.Tulong) {
      return getUlongVector(n);
    }
    else {
      // Internal error
      return null;
    }
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
    return "V" + getNbElements() + getBaseType().getSignature();
  }


  //==================================================================
  // Conversion Management
  //==================================================================
  
  //------------------------------------------------------------------
  // promote:
  //
  // No automatic promotion in OCL.
  //------------------------------------------------------------------
  public Type promote() {
    return this;
  }


  //==================================================================
  // Target Specific information
  //==================================================================

  public int sizeof() {
    int n=getNbElements();
    if (n==3) {
      // vec3 aligned on 4-elem boundary
      n=4;
    }
    return n*baseType.sizeof();
  }

  // In OCL, vectors aligned on their size
  public int alignof() {
    return sizeof();
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
    return super.toStringInternal(ts,cs)+
          " of "+baseType.toStringInternal(ts,cs);
  }

  //------------------------------------------------------------------
  // dump :
  //
  // Returns the original type syntax
  //------------------------------------------------------------------
  public String dump() {
    return baseType.dump()+getNbElements();
  }

  //------------------------------------------------------------------
  // dumpBaseType :
  //
  // Returns the original type syntax of the vector base type
  //------------------------------------------------------------------
  public String dumpBaseType() {
    return baseType.dump();
  }

}
