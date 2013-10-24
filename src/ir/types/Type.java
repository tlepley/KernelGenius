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

/* Generic C type (mother class of all types) */

package ir.types;

import ir.types.abi.ABI;
import ir.types.abi.C_ABI_ilp32;
import ir.types.c.Qualifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

abstract public class Type implements Cloneable {
  
  //--------------------------------------------------------------------
  // ABI as a local thread storage (one compiler instance)
  //--------------------------------------------------------------------
  // By default, both source and target are 32 bits C ABI
  private static InheritableThreadLocal<ABI> source_abi = new InheritableThreadLocal<ABI>() {
    @Override
    protected ABI initialValue() {
      return C_ABI_ilp32.abi;
    }
  };
  private static InheritableThreadLocal<ABI> target_abi = new InheritableThreadLocal<ABI>() {
    @Override
    protected ABI initialValue() {
      return C_ABI_ilp32.abi;
    }
  };  
  public static ABI getSourceABI() {
    return source_abi.get();
  }
  public static ABI getTargetABI() {
    return target_abi.get();
  }
  public static void setSourceABI(ABI a) {
    source_abi.set(a);
  }
  public static void setTargetABI(ABI a) {
    target_abi.set(a);
  }


  //------------------------------------------------------------------
  // clone :
  //
  // Standard cloning function
  //------------------------------------------------------------------
  public Type clone() {
    try {
      return (Type)super.clone();
    } catch(CloneNotSupportedException e) {
      return(null);
    }
  }


  //==================================================================
  // Conversion management
  //==================================================================

  //------------------------------------------------------------------
  // promote:
  //
  // Returns the type to which it must be converted in case of
  // promotion.
  //------------------------------------------------------------------
  public Type promote() {
    // By default, returns itself
    return this;
  }

  //==================================================================
  // Signature management (for arguments of function prototypes)
  //==================================================================
  abstract public String getSignature();


  
  //==================================================================
  // C Type extension management
  //==================================================================
  
  // Array range
  public boolean isArrayRange() {return false;}

  // Vectors
  public boolean isVector() {return false;}
  public Type getVectorBaseType() {return null;}
  public int getNbVectorElements() {return 0;}

  public boolean isIntegralVector() {return false;}
  public boolean isUnsignedIntegerVector() {return false;}
  public boolean isSignedIntegerVector() {return false;}

  public boolean isCharVector() {return false;}
  public boolean isShortVector() {return false;}
  public boolean isIntVector() {return false;}
  public boolean isLongVector() {return false;}

  public boolean isFloatingPointVector() {return false;}
  public boolean isFloatVector() {return false;}

  
  
  //==================================================================
  // C Type management
  //==================================================================

  public boolean isVoid() {return false;}

  // Scalar
  public boolean isPointer() {return false;}
  public Type getPointedType() {return null;}

  public boolean isArithmeticScalar() {return false;}

  public boolean isFloatingPointScalar() {return false;}
  public boolean isFloatScalar() {return false;}
  public boolean isDoubleScalar() {return false;}
  public boolean isLongDoubleScalar() {return false;}

  public boolean isIntegralScalar() {return false;}

  public boolean isBitfield() {return false;}

  public boolean isIntegerScalar() {return false;}
  public boolean isUnsignedIntegerScalar() {return false;}
  public boolean isSignedIntegerScalar() {return false;}
  public boolean isCharScalar() {return false;}
  public boolean isShortScalar() {return false;}
  public boolean isIntScalar() {return false;}
  public boolean isLongScalar() {return false;}
  public boolean isLongLongScalar() {return false;}
  public boolean isSchar() {return false;}
  public boolean isUchar() {return false;}
  public boolean isSshort() {return false;}
  public boolean isUshort() {return false;}
  public boolean isSint() {return false;}
  public boolean isUint() {return false;}
  public boolean isSlong() {return false;}
  public boolean isUlong() {return false;}
  public boolean isSlonglong() {return false;}
  public boolean isUlonglong() {return false;}

  // Type tags
  public boolean isEnumerate() {return false;}
  public boolean isStructOrUnion() {return false;}
  public boolean isStruct() {return false;}
  public boolean isUnion() {return false;}
  public Type getFieldType(String s) {return null;}

  // Array
  public boolean isArray() {return false;}
  public Type getElementType() {return null;}

  // Function
  public boolean isFunction() {return false;}
  public Type getReturnType() {return null;}
  public LinkedList<Type> getParameterTypeList() {return null;}

  // Qualifier
  public boolean isQualified() {return false;}
  public Qualifier getQualifier() {return null;}
  public boolean isConstQualified() {return false;}
  public boolean isVolatileQualified() {return false;}
  public boolean isRestrictQualified() {return false;}
  public boolean isAddressSpaceQualified() {return false;}
  public boolean isConstantAddressSpaceQualified() {return false;}
  public boolean isGlobalAddressSpaceQualified() {return false;}
  public boolean isLocalAddressSpaceQualified() {return false;}
  public boolean isPrivateAddressSpaceQualified() {return false;}
  public Type unqualify() {return this;}

  // Struct/union/array
  public boolean isAggregate() {return false;}
  public boolean isComplete() {return true;}

  // Combinations
  public boolean isPointerOrArray() {return isPointer()||isArray();}
  public boolean isPointerOrLabel() {return isPointer()||isArray()||isFunction();}
  public boolean isScalar() {return isArithmeticScalar()||isPointer();}
  public boolean isScalarOrLabel() {return isArithmeticScalar()||isPointerOrLabel();}
  public boolean isDerivedType() {return isPointer()||isArray()||isFunction();}
  public boolean isIncompleteOrVoid() {return isVoid()||(!isComplete());}
  public boolean isIncomplete() {return !isComplete();}


  //==================================================================
  // Target Specific information
  //==================================================================

  abstract public int sizeof();
  abstract public int alignof();

  //==================================================================
  // Compatibility checks
  //==================================================================
  
  //------------------------------------------------------------------
  // isEquivalentForVariableAndArrayDeclaration :
  //
  // This function checks the compatibility of types in the context
  // of two variable/array declarations
  //------------------------------------------------------------------
  public boolean isEquivalentForVariableAndArrayDeclaration(Type t) {
    return this==t;
  }
  
  //------------------------------------------------------------------
  // isEquivalentForFunctionDeclaration :
  //
  // This function checks the compatibility of types in the context
  // of two function prototypes declaration
  //------------------------------------------------------------------
  public boolean isEquivalentForFunctionDeclaration(Type t) {
    if (t.isAddressSpaceQualified()) {
      return false;
    }
    Type tu=t.unqualify();

    // Should be an array
    return this==tu;
  }



  //==================================================================
  // Verbose functions
  //==================================================================

  // Counter in local thread storage
  protected static final InheritableThreadLocal<Integer> treeSetCounter= new InheritableThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };
  protected int incrementIdCounter() {
    int i=treeSetCounter.get();
    treeSetCounter.set(i+1);
    return i;
  }


  //------------------------------------------------------------------
  // getTreeSet:
  //
  // Function allows detecting multiple type reference in the type
  // tree. It is in particular useful to manage the possible
  // struct/union cycles. 'ts' holds type node encountered and 'cs'
  // holds multiply encountered type nodes.
  //------------------------------------------------------------------
  public void getTreeSet(HashSet<Type> ts, HashMap<Type,Integer> cs) {
    // Nothing to do here
  }

  
  //------------------------------------------------------------------
  // toStringInternal:
  //
  // Returns string a textual representation of the type. Use 'ts' and
  // 'cs' to avoid displaying multiple times the same type
  // (and avoid cycles)
  //------------------------------------------------------------------
  abstract public String toStringInternal(HashSet<Type> ts, HashMap<Type,Integer> cs);

  
  //------------------------------------------------------------------
  // toString :
  //
  // Returns string a textual representation of the type
  //------------------------------------------------------------------
  public String toString() {
    HashSet<Type>         ts = new HashSet<Type>();
    HashMap<Type,Integer> cs = new HashMap<Type,Integer>();

    treeSetCounter.set(0);
    getTreeSet(ts,cs);

    HashSet<Type> ts2=new HashSet<Type>();
    return toStringInternal(ts2,cs);
  }

}
