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

/* Vector */

package ir.literals.ocl;

import ir.literals.Literal;
import ir.literals.c.AggregateLiteral;
import ir.literals.c.ExprLiteral;
import ir.types.ocl.Vector;


public class VectorLiteral extends AggregateLiteral {

  private int nb_def_elements=0;

  //==================================================================
  // Constructor
  //==================================================================

  public VectorLiteral(Vector t) {
    // Initialize the aggregate 
    super(t);
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
    nb_def_elements++;
    super.add(l);
  }

  //------------------------------------------------------------------
  // addAtIndex :
  //
  // Add an elements at a specified index
  //
  //------------------------------------------------------------------
  public void addAtIndex(int new_index, Literal l) {
    nb_def_elements++;
    
    super.addAtIndex(new_index,l);
  }

  //------------------------------------------------------------------
  // addAtIndexRange :
  //
  // Add a new element to the specified index range
  //
  //------------------------------------------------------------------
  public void addAtIndexRange(int inf, int sup, Literal l) {
    if (sup>=inf) {
      nb_def_elements+=sup-inf+1;
    }

    super.addAtIndexRange(inf,sup,l);
  }


  //------------------------------------------------------------------
  // Getters
  //------------------------------------------------------------------

  //------------------------------------------------------------------
  // getNbDefinitionElements  : 
  //
  // Returns the number of elements given in the definition of the
  // literal. This number may differ from the 'size' in the case of
  // vectors.
  // Ex: (int4)(1,2,(int2)(3,4)) has a size of 4 but 3*elements in
  //     the definition
  //
  //------------------------------------------------------------------
  public int getNbDefinitionElements() {
    return nb_def_elements;
  }

  //------------------------------------------------------------------
  // isScalarDefined : 
  //
  // Returns true is the vector literal is defined with one unique
  // scalar
  // Ex: (int4)(1)
  //
  //------------------------------------------------------------------
  public boolean isScalarDefined() {
    if (nb_def_elements==1) {
      if (getAtIndex(0) instanceof ExprLiteral) {
	return true;
      }
    }
    return false;
  }

  //------------------------------------------------------------------
  // isComplexDefined : 
  //
  // Returns true is the vector literal is defined with at lease
  // a sub-vector
  // Ex: (int4)(1, 2, (int2)(3,4))
  //
  //------------------------------------------------------------------
  public boolean isComplexDefined() {
    if ((nb_def_elements<getSize()) && (!isScalarDefined())) {
      return true;
    }
    return false;
  }


  //==================================================================
  // Verbose functions
  //==================================================================
  public String toString() {
    return "Vector = "+ super.toString();

  }
}
