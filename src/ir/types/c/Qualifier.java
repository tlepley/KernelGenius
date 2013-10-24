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

/* Qualifier of a type */

package ir.types.c;


import common.CompilerError;

import ir.types.Type;
import ir.types.ocl.AddressSpace;

import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;

import parser.TNode;


public class Qualifier extends Type {

  //==================================================================
  // Private data
  //==================================================================

  private Type qualifiedType=null;

  boolean isConst=false;
  boolean isVolatile=false;
  boolean isRestrict=false;  // C99 specific


  // OCL address spaces
  AddressSpace address_space = AddressSpace.NO;


  //==================================================================
  // Constructors
  //==================================================================

  public Qualifier() {
    ;
  }

  public Qualifier(Type of) {
    qualifiedType=of;
  }

  public Qualifier(AddressSpace as, Type of) {
    address_space=as;
    qualifiedType=of;
  }

  public Qualifier(boolean c, boolean v, boolean r, Type of) {
    isConst    = c;
    isVolatile = v;
    isRestrict = r;
    qualifiedType=of;
  }

  // Copy constructor
  public Qualifier(Qualifier q) {
    isConst       = q.isConst;
    isVolatile    = q.isVolatile;
    isRestrict    = q.isRestrict;
    qualifiedType = q.qualifiedType;
    address_space = q.address_space;
  }

  // Special copy constructor
  public Qualifier(Qualifier q, Type of) {
    isConst       = q.isConst;
    isVolatile    = q.isVolatile;
    isRestrict    = q.isRestrict;
    qualifiedType = of;
    address_space = q.address_space;
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
    return "Q" + address_space.getSignature() +
                qualifiedType.getSignature();
  }


  //==================================================================
  // Type management
  //==================================================================

  public boolean isVoid() {return qualifiedType.isVoid();}

  // Scalar
  public boolean isPointer() {return qualifiedType.isPointer();}
  public Type getPointedType() {return qualifiedType.getPointedType();}

  public boolean isArithmeticScalar() {return qualifiedType.isArithmeticScalar();}

  public boolean isFloatingPointScalar() {return qualifiedType.isFloatingPointScalar();}
  public boolean isFloatScalar() {return qualifiedType.isFloatScalar();}
  public boolean isDoubleScalar() {return qualifiedType.isDoubleScalar();}
  public boolean isLongDoubleScalar() {return qualifiedType.isLongDoubleScalar();}

  public boolean isIntegralScalar() {return qualifiedType.isIntegralScalar();}

  public boolean isIntegerScalar() {return qualifiedType.isIntegerScalar();}
  public boolean isUnsignedIntegerScalar() {return qualifiedType.isUnsignedIntegerScalar();}
  public boolean isSignedIntegerScalar() {return qualifiedType.isSignedIntegerScalar();}
  public boolean isCharScalar() {return qualifiedType.isCharScalar();}
  public boolean isShortScalar() {return qualifiedType.isShortScalar();}
  public boolean isIntScalar() {return qualifiedType.isIntScalar();}
  public boolean isLongScalar() {return qualifiedType.isIntScalar();}
  public boolean isLongLongScalar() {return qualifiedType.isLongLongScalar();}
  public boolean isSchar() {return qualifiedType.isSchar();}
  public boolean isUchar() {return qualifiedType.isUchar();}
  public boolean isSshort() {return qualifiedType.isSshort();}
  public boolean isUshort() {return qualifiedType.isUshort();}
  public boolean isSint() {return qualifiedType.isSint();}
  public boolean isUint() {return qualifiedType.isUint();}
  public boolean isSlong() {return qualifiedType.isSlong();}
  public boolean isUlong() {return qualifiedType.isUlong();}
  public boolean isSlonglong() {return qualifiedType.isSlonglong();}
  public boolean isUlonglong() {return qualifiedType.isUlonglong();}

  // Type tags
  public boolean isEnumerate() {return qualifiedType.isEnumerate();}
  public boolean isStructOrUnion() {return qualifiedType.isStructOrUnion();}
  public boolean isStruct() {return qualifiedType.isStruct();}
  public boolean isUnion() {return qualifiedType.isUnion();}
  public Type getFieldType(String s) {return qualifiedType.getFieldType(s);}

  // Array
  public boolean isArray() {return qualifiedType.isArray();}
  public Type getElementType() {return qualifiedType.getElementType();}

  // Function
  public boolean isFunction() { return qualifiedType.isFunction();}
  public Type getReturnType() { return qualifiedType.getReturnType();}
  public LinkedList<Type> getParameterTypeList() { return qualifiedType.getParameterTypeList();}

  // Qualifier
  public boolean isQualified() {return true;}
  public Qualifier getQualifier() {return this;}
  public boolean isConstQualified() {return isConst;}
  public boolean isVolatileQualified() {return isVolatile;}
  public boolean isRestrictQualified() {return isRestrict;}
  public boolean isAddressSpaceQualified() {return address_space!=AddressSpace.NO;}
  public boolean isConstantAddressSpaceQualified() {return address_space==AddressSpace.CONSTANT;}
  public boolean isGlobalAddressSpaceQualified() {return address_space==AddressSpace.GLOBAL;}
  public boolean isLocalAddressSpaceQualified() {return address_space==AddressSpace.LOCAL;}
  public boolean isPrivateAddressSpaceQualified() {return address_space==AddressSpace.PRIVATE;}
  public Type unqualify() {return qualifiedType;}

  // Struct/union/array
  public boolean isAggregate() {return qualifiedType.isAggregate();}
  public boolean isComplete() {return qualifiedType.isComplete();}

  // Vectors
  public boolean isVector() {return qualifiedType.isVector();}
  public Type getVectorBaseType() {return qualifiedType.getVectorBaseType();}
  public int getNbVectorElements() {return qualifiedType.getNbVectorElements();}

  public boolean isIntegralVector() {return qualifiedType.isIntegralVector();}
  public boolean isUnsignedIntegerVector() {return qualifiedType.isUnsignedIntegerVector();}
  public boolean isSignedIntegerVector() {return qualifiedType.isSignedIntegerVector();}

  public boolean isCharVector() {return qualifiedType.isCharVector();}
  public boolean isShortVector() {return qualifiedType.isShortVector();}
  public boolean isIntVector() {return qualifiedType.isIntVector();}
  public boolean isLongVector() {return qualifiedType.isLongVector();}

  public boolean isFloatingPointVector() {return qualifiedType.isFloatingPointVector();}
  public boolean isFloatVector() {return qualifiedType.isFloatVector();}


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
    if (t.isQualified()) {
      Qualifier q=(Qualifier)t;
      
      // Check same qualifier
      if ( (isConst!=q.isConst)       ||
	   (isVolatile!=q.isVolatile) ||
	   (isRestrict!=q.isRestrict) ||
	   (address_space!=q.address_space)
	   ) {
	// Not same qualifier
	return false;
      }

      // Compare qualified types
      return(qualifiedType.isEquivalentForVariableAndArrayDeclaration(q.qualifiedType));
    }
    return false;
  }

  //------------------------------------------------------------------
  // isEquivalentForFunctionDeclaration :
  //
  // This function checks the compatibility of types in the context
  // of two function prototypes declaration
  //------------------------------------------------------------------
  public boolean isEquivalentForFunctionDeclaration(Type t) {
    if (isAddressSpaceQualified()) {
      if (t.isAddressSpaceQualified()) {
	// Check for same addressing space
	if (address_space==((Qualifier)t).address_space) {
	  return qualifiedType.isEquivalentForFunctionDeclaration(t.unqualify());
	}
	else {
	  // Different address space
	  return false;
	}
      }
      else {
	return false;
      }
    }
    if (t.isAddressSpaceQualified()) {
      return false;
    }
    return qualifiedType.isEquivalentForFunctionDeclaration(t.unqualify());
  }


  //==================================================================
  // Setters
  //==================================================================

  public void setQualifiedType(Type of) {
   qualifiedType=of;
  }

  public void setConst(TNode tn, CompilerError cp) {
    if (isConst) {
      cp.raiseWarning(tn,"duplicate `const'");
    }
    isConst=true;
  }

  public void setVolatile(TNode tn, CompilerError cp) {
    if (isVolatile) {
      cp.raiseWarning(tn,"duplicate `volatile'");
    }
    isVolatile=true;
  }

  // To be studied (restrict is not a standard qualifier)
  public void setRestrict(TNode tn, CompilerError cp) {
    if (isRestrict) {
      cp.raiseWarning(tn,"duplicate `restrict'");
    }
    isRestrict=true;
  }

  public void setAddressSpace(AddressSpace as) {
    address_space=as;
  }

  public void setAddressSpace(TNode tn, CompilerError cp, AddressSpace as) {
    if (as==address_space) {
      StringBuffer sb=new StringBuffer();
      sb.append("duplicate `").append(as.getName()).append("'");
      cp.raiseWarning(tn,sb.toString());
    }
    else {
      if (address_space!=AddressSpace.NO) {
	cp.raiseError(tn,"More than one address space qualifier specified");
      }
      else {
	address_space=as;
      }
    }
  }


  //==================================================================
  // Conversion management
  //==================================================================

  //------------------------------------------------------------------
  // promote:
  //
  // Returns the type to which it must be converted in case of
  // promotion. The qualifier is kept unchanged.
  //------------------------------------------------------------------
  public Type promote() {
    Type newQualifiedtype=qualifiedType.promote();

    // Same qualified type, nothing to do
    if (newQualifiedtype==qualifiedType) {
      return this;
    }

    return new Qualifier(this,newQualifiedtype);
  }




  //==================================================================
  // Getters
  //==================================================================

  //------------------------------------------------------------------
  // getBaseType
  //
  // Returns the type which is qualified
  //------------------------------------------------------------------
  public Type getQualifiedType() {
    return qualifiedType;
  }

  //------------------------------------------------------------------
  // Qualifiers query
  //------------------------------------------------------------------
  public boolean isConst() {
    return isConst;
  }
  public boolean isVolatile() {
    return isVolatile;
  }
  public boolean isRestrict() {
    return isRestrict;
  }
  public AddressSpace getAddressSpace() {
    return address_space;
  }


  //==================================================================
  // Target Specific information
  //==================================================================

  public int sizeof() {
    return qualifiedType.sizeof();
  }

  public int alignof() {
    return qualifiedType.alignof();
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
    if (isConst) {
      buff.append("const ");
    }
    if (isVolatile) {
      buff.append("volatile ");
    }
    if (isRestrict) {
      buff.append("restrict ");
    }
    if (address_space!=AddressSpace.NO) {
      buff.append(address_space.getName()).append(" ");
    }
    buff.append(qualifiedType.toStringInternal(ts,cs));
    return(buff.toString());
  }

}
