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

/* OpenCL C floating point vector types */

package ir.types.ocl;

import ir.types.Type;
import ir.types.c.FloatingPointScalar;

import java.util.HashMap;
import java.util.HashSet;


public class FloatingPointVector extends Vector {
  // Vector types
  public static final FloatingPointVector Tfloat2   = new FloatingPointVector(FloatingPointScalar.Tfloat,2);
  public static final FloatingPointVector Tfloat3   = new FloatingPointVector(FloatingPointScalar.Tfloat,3);
  public static final FloatingPointVector Tfloat4   = new FloatingPointVector(FloatingPointScalar.Tfloat,4);
  public static final FloatingPointVector Tfloat8   = new FloatingPointVector(FloatingPointScalar.Tfloat,8);
  public static final FloatingPointVector Tfloat16  = new FloatingPointVector(FloatingPointScalar.Tfloat,16);

  private static final FloatingPointVector floatArray[]  ={null,null,Tfloat2,Tfloat3,Tfloat4,null,null,null,
						     Tfloat8,null,null,null,null,null,null,null,Tfloat16};

  static public FloatingPointVector getFloatType(int n) {
    return floatArray[n];
  }


  //==================================================================
  // Private data
  //==================================================================
  FloatingPointScalar baseType;

  //==================================================================
  // Private Constructor
  //==================================================================
  private FloatingPointVector(FloatingPointScalar base_type, int n) {
    super(n);
    baseType=base_type;
  }

  //==================================================================
  // Type class generic methods
  //==================================================================
  public boolean isFloatingPointVector() {return true;}

  public boolean isFloatVector() {return hasFloatElements();}


  //==================================================================
  // Getters
  //==================================================================
 
  //------------------------------------------------------------------
  // getBaseType
  //
  // Returns base type of the integer vector type
  //------------------------------------------------------------------
  public FloatingPointScalar getBaseType() {
    return baseType;
  }
  public boolean hasFloatElements() {return baseType.isFloat();}


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
    else if (baseType==FloatingPointScalar.Tfloat) {
      return getFloatType(n);
    }
    else {
      // Internal error
      return null;
    }
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
