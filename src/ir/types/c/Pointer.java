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

/* C pointer type */

package ir.types.c;

import ir.types.Type;

import java.util.HashSet;
import java.util.HashMap;

public class Pointer extends ChildType {

  //==================================================================
  // Constructor
  //==================================================================
  public Pointer() {
    ;
  }

  public Pointer(Type of) {
    super(of);
  }


  //==================================================================
  // Type management
  //==================================================================
  public boolean isPointer() {return true;}



  //==================================================================
  // Getter
  //==================================================================

  //------------------------------------------------------------------
  // getPointedType
  //
  // Returns type pointed
  //------------------------------------------------------------------
  public Type getPointedType() {
    return getChild();
  }


  //==================================================================
  // Signature management (for arguments of function prototypes)
  //==================================================================

  public String getSignature() {
    return "P" + getPointedType().getSignature();
  }


  //==================================================================
  // Target Specific information
  //==================================================================

  public int sizeof() {
    return Type.getSourceABI().getPointerSize();
  }

  public int alignof() {
    return Type.getSourceABI().getPointerAlignment();
  }


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
    if (t.isPointer()) {
      return getPointedType().isEquivalentForVariableAndArrayDeclaration(t.getPointedType());
    } 
    else {
      // Qualified or other type
      return false;
    }
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
    if (!tu.isPointer()) {
      return false;
    }

    // Check element type compatibility
    return getPointedType().isEquivalentForFunctionDeclaration(tu.getPointedType());
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
    if (getPointedType()==null) {
      buff.append("* { NULL }");
    }
    else {
      buff.append("* {").append(getPointedType().toStringInternal(ts,cs)).append("}");
    }
    return(buff.toString());
  }

}
