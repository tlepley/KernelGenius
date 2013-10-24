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

/* C Array type */

package ir.types.c;

import ir.types.Type;

import java.util.HashSet;
import java.util.HashMap;

public class Array extends ChildType {
  //==================================================================
  // Private data
  //==================================================================
  protected boolean hasSizeSpecifier=false;
  protected boolean hasConstantSize=false;
  protected int nbElements = 0;

  //==================================================================
  // Constructor
  //==================================================================
  public Array(Type of) {
    super(of);
  }


  //==================================================================
  // Type management
  //==================================================================
  public boolean isArray() {return true;}
  public boolean isAggregate() {return true;}

  // Specific to Qualifier:
  // An array is never qualified by himself, only its elements can be
  // qualified
  public boolean isQualified() {return getChild().isQualified();}
  public Qualifier getQualifier() {return getChild().getQualifier();}
  public boolean isConstQualified() {return getChild().isConstQualified();}
  public boolean isVolatileQualified() {return getChild().isVolatileQualified();}
  public boolean isRestrictQualified() {return getChild().isRestrictQualified();}
  public boolean isAddressSpaceQualified() {return getChild().isAddressSpaceQualified();}
  public boolean isConstantAddressSpaceQualified() {return getChild().isConstantAddressSpaceQualified();}
  public boolean isGlobalAddressSpaceQualified() {return getChild().isGlobalAddressSpaceQualified();}
  public boolean isLocalAddressSpaceQualified() {return getChild().isLocalAddressSpaceQualified();}
  public boolean isPrivateAddressSpaceQualified() {return getChild().isPrivateAddressSpaceQualified();}


  //==================================================================
  // Setters
  //==================================================================

  public void setVariableSize() {
    hasSizeSpecifier=true;
    hasConstantSize=false;
  }
  
  public void setNbElements(int i) {
    hasSizeSpecifier=true;
    hasConstantSize=true;
    nbElements=i;
  }


  //==================================================================
  // Getters
  //==================================================================

  public boolean hasSizeSpecifier() {
    return hasSizeSpecifier;
  }

  // A dynamic array has size is defined in the program but
  // this size specifier is a runtime variable
  public boolean isDynamic() {
    return hasSizeSpecifier && (!hasConstantSize);
  }

  // Such array has a compile time known size. The size specifier
  // is a compile time constant
  public boolean hasConstantSize() {
    return hasSizeSpecifier && hasConstantSize;
  }
 
  public int getNbElements() {
    return nbElements;
  }
  
  public boolean isComplete() {
    // We should check also that the size is constant [TBW]
    // leave it like this until sizeof is not managed as a constant
    if ((!hasSizeSpecifier) || (!getElementType().isComplete())) {
      return false;
    }
    return true;
  }

  public boolean hasCompleteElement() {
    return getElementType().isComplete();
  }


  //------------------------------------------------------------------
  // getElementType
  //
  // Returns type of array elements
  //------------------------------------------------------------------
  public Type getElementType() {
    return getChild();
  }


  //==================================================================
  // Signature management (for arguments of function prototypes)
  // Should never occur since arrays as function arguments are
  // transformed into pointer
  //==================================================================

  public String getSignature() {
    // Note: as function parameter, an array is equivalent to a pointer
    return "P" + getElementType().getSignature();

    // return "A" + getNbElements()+ "_" + getElementType().getSignature();
  }


  //==================================================================
  // Target Specific information
  //==================================================================

  // Relevant only if complete
  public int sizeof() {
    return nbElements*getElementType().sizeof();
  }

  // Relevant only if complete
  public int alignof() {
    return getElementType().alignof();
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
    if (t.isArray()) {
      return getElementType().isEquivalentForVariableAndArrayDeclaration(t.getElementType());
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
    if (!tu.isArray()) {
      return false;
    }

    // Check array size
    if (getNbElements()!=((Array)tu).getNbElements()) {
      System.err.println("size "+getNbElements() + "!=" + ((Array)tu).getNbElements());
      return false;
    }

    // Check element type compatibility
    return getElementType().isEquivalentForFunctionDeclaration(tu.getElementType());
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
    buff.append("array[");
    if (hasSizeSpecifier()) {
      if (hasConstantSize()) {
        buff.append(nbElements);
      }
      else {
        buff.append("<n>");
      }
    }

    buff.append("] of {").append(getElementType().toStringInternal(ts,cs)).append("}");
    return(buff.toString());
  }
}
