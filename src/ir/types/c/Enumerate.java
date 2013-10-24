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

/* C Enumerate type */

package ir.types.c;

import ir.types.Type;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.HashMap;


public class Enumerate extends ArithmeticScalar {

  //==================================================================
  // Private data
  //==================================================================
  private LinkedHashSet<String> elementSet=null;
  private boolean complete=false;
  private String signatureName=null;


  //==================================================================
  // Constructor
  //==================================================================
  public Enumerate() {
    elementSet=new LinkedHashSet<String>();
  }


  //==================================================================
  // Type management
  //==================================================================

  public boolean isEnumerate() {return true;}
  public boolean isIntegralScalar() {return true;}


  //==================================================================
  // Setters
  //==================================================================

  //------------------------------------------------------------------
  // addElement
  //
  // Add an element to the enum.
  // Returns false if this element was already declared
  //------------------------------------------------------------------
  public boolean addElement(String e) {
    return elementSet.add(e);
  }

  //------------------------------------------------------------------
  // setComplete
  //
  // Sets the tag as complete
  //------------------------------------------------------------------
  public void setComplete() {
    complete=true;
  }

  //------------------------------------------------------------------
  // setSignature
  //
  // Sets the signature of the tag (used to create function prototype
  // signatures). In case of type tags, the signature may depend
  // on the declaration name
  //------------------------------------------------------------------
  public void setSignatureName(String s) {
    signatureName=s;
  }


  //==================================================================
  // Getters
  //==================================================================

  //------------------------------------------------------------------
  // getElementSet
  //
  // Returns set of enum elements
  //------------------------------------------------------------------
  public Set<String> getElementSet() {
    return elementSet;
  }

  //------------------------------------------------------------------
  // isComplete
  //
  // Returns true if the structure or union is complete
  //------------------------------------------------------------------
  public boolean isComplete() {
    return(complete==true);
  }

  //------------------------------------------------------------------
  // getSignature
  //
  // Returns the signature of the tag (used to create function 
  // prototype signatures)
  //------------------------------------------------------------------
  public String getSignature() {
    return ""+signatureName.length()+getElementType();
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
    return IntegerScalar.Tsint;
  }


  //==================================================================
  // Target Specific information
  //==================================================================

  // Consider that it's an int, which is not necessarily true [TBW]
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
    if (!isComplete()) {
      return("enum incomplete");
    }
    else {
      boolean first=true;
      StringBuffer buff = new StringBuffer();
      buff.append("enum {");
      for(String s:elementSet) {
	if (first==false) {
	  buff.append(", ");
	}
	first=false;
	buff.append(s);
      }
      buff.append("}");
      return(buff.toString());
    }
  }

  //------------------------------------------------------------------
  // dump :
  //
  // Returns the original C type syntax
  //------------------------------------------------------------------
  public String dump() {
    if (!isComplete()) {
      return("enum");
    }
    else {
      boolean first=true;
      StringBuffer buff = new StringBuffer();
      buff.append("enum {");
      for(String s:elementSet) {
	if (first==false) {
	  buff.append(", ");
	}
	first=false;
	buff.append(s);
      }
      buff.append("}");
      return(buff.toString());
    }
  }

}
