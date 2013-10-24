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

/* Generic type for final types which have a sub-type */

package ir.types.c;

import ir.types.Type;

import java.util.HashSet;
import java.util.HashMap;


public abstract class ChildType extends Type {
  //==================================================================
  // Private data
   //==================================================================
  private Type child=null;


  //==================================================================
  // Constructors
  //==================================================================

  public ChildType() {
    ;
  }

  public ChildType(Type c) {
    setChild(c);
  }


  //==================================================================
  // Setters
  //==================================================================

  public void setChild(Type c) {
    if (c instanceof Marker) {
      ((Marker)c).setParent(this);
    }
    else {
      child=c;
    }
  }


  //==================================================================
  // Getters
  //==================================================================

  public Type getChild() {
    return child;
  }



  //==================================================================
  // Verbose functions
  //==================================================================

  public void getTreeSet(HashSet<Type> ts, HashMap<Type,Integer> cs) {
    child.getTreeSet(ts, cs);
  }

  //------------------------------------------------------------------
  // toStringInternal:
  //
  // Returns string a textual representation of the type. Use 'ts' and
  // 'cs' to avoid displaying multiple times the same type
  // (and avoid cycles)
  //------------------------------------------------------------------
  public String toStringInternal(HashSet<Type> ts, HashMap<Type,Integer> cs) {
    StringBuffer buff = new StringBuffer();
    buff.append("<...> of {").append(getChild().toStringInternal(ts,cs)).append("}");
    return(buff.toString());
  }




/* ******************************************************************
   Description:
         Marker for the type construction in declarators
****************************************************************** */
  public static class Marker extends Type {
    //==================================================================
    // Private data
    //==================================================================
    private Type parent=null;

    //==================================================================
    // Constructor / building
    //==================================================================
    public Marker() {
    }
    public void setParent(Type p) {
      parent=p;
    }
    public boolean hasParent() {
      return parent!=null;
    }

    //==================================================================
    // Getters
    //==================================================================
    public Type getParent() {
      return parent;
    }

    // Signature: should never be called
    public String getSignature() {
      return null;
    }


    //==================================================================
    // Target Specific information
    //==================================================================

    public int sizeof() {return 0;}
    public int alignof() {return 1;}


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
      return("[CHILDTYPE MARKER]");
    }

  }

}
