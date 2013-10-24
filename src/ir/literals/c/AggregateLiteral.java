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

/* Aggregate Literals (vector, array, struct, union) */


package ir.literals.c;


import ir.literals.Literal;
import ir.types.Type;
import ir.types.c.*;
import ir.types.ocl.Vector;


public class AggregateLiteral extends Literal {

  //------------------------------
  // Private data
  //------------------------------
  private boolean isConstant=true;

  // Elements management
  private java.util.Vector<Literal> elementVector=null;
  private int index=0;
  private int size=0;


  //==================================================================
  // Constructor
  //==================================================================

  public AggregateLiteral(Type t) {
    super(t);

    int alloc_size;

    // The type here is necessarily unqualified (StrutOrUnion or Array)
    if ((t.isStructOrUnion()) && (t.isComplete())) {
      size=((StructOrUnion)t).getNbFields();
      alloc_size=size;
    }
    else if ( (t instanceof Array) && (((Array)(t)).hasSizeSpecifier()) ) {
      size=((Array)t).getNbElements();
      alloc_size=size;
    }
    else if (t instanceof Vector) {
      size=((Vector)t).getNbElements();
      alloc_size=size;
    }
    else {
      // Incomplete aggregate, no size defined (yet)
      size=0;
      alloc_size=20;
    }

    // Allocate and initialize the vector
    elementVector=new java.util.Vector<Literal>(alloc_size);
    elementVector.setSize(size);
  }


  //------------------------------------------------------------------
  // Setters
  //------------------------------------------------------------------


  //------------------------------------------------------------------
  // add :
  //
  // Add a new element at the current index and increment this index
  //
  //------------------------------------------------------------------
  public void add(Literal l) {
    if (index>=size) {
      // The vector must be resized
      size=index+1;
      elementVector.setSize(size);
    }
    elementVector.setElementAt(l,index);

    // Sets the new index
    index++;

    // Propagate the constant property
    if (!l.isConstant()) {
      isConstant=false;
    }
  }


  //------------------------------------------------------------------
  // addAtIndex :
  //
  // Add an elements at a specified index
  //
  //------------------------------------------------------------------
  public void addAtIndex(int new_index, Literal l) {
    index=new_index;
    add(l);
  }


  //------------------------------------------------------------------
  // addAtIndexRange :
  //
  // Add a new element to the specified index range
  //
  //------------------------------------------------------------------
  public void addAtIndexRange(int inf, int sup, Literal l) {
    if (sup>=size) {
      // The vector must be resized
      size=sup+1;
      elementVector.setSize(size);
    }

    // Put the value for the whole range
    int i;
    for(i=inf;i<=sup;i++) {
      elementVector.setElementAt(l,i);
    }

    // Sets the new index
    index=sup+1;

    // Propagate the constant property
    if (!l.isConstant()) {
      isConstant=false;
    }
  }



  //------------------------------------------------------------------
  // Getters
  //------------------------------------------------------------------

  //------------------------------------------------------------------
  // isConstant :
  //
  // Returns 'true' if the literal is a 'compile time' constant
  //
  //------------------------------------------------------------------
  public boolean isConstant() {
    return isConstant;
  }

  //------------------------------------------------------------------
  // getSize : 
  //
  // Returns the (current for uncomplete arrays) size of the aggregate
  //
  //------------------------------------------------------------------
  public int getSize() {
    return size;
  }

  //------------------------------------------------------------------
  // sEmpty : 
  //
  // Returns true if no element has been set
  //
  //------------------------------------------------------------------
  public boolean isEmpty() {
    return index==0;
  }

  //------------------------------------------------------------------
  // getAtIndex :
  //
  // Add an elements at a specified index
  //
  //------------------------------------------------------------------
  public Literal getAtIndex(int i) {
    if (i>=size) {
      // Should never occur
      return null;
    }
    return elementVector.elementAt(i);
  }




  //==================================================================
  // Verbose functions
  //==================================================================

  public String toString() {
    int i;
    Literal l;
    StringBuffer buff = new StringBuffer();

    buff.append("{");
    for(i=0;i<size;i++) {
      if (i!=0) {
        buff.append(", ");
      }
      l=elementVector.get(i);
      if (l==null) {
        buff.append("<null>");
      }
      else {
        buff.append(l.toString());
      }
    }
    buff.append("}");

    return buff.toString();
  }

}
