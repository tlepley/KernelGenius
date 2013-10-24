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

/* Scalar Literal */


package ir.literals.c;

import ir.literals.Literal;
import ir.types.Type;
import ir.types.c.EnrichedType;



public class ExprLiteral extends Literal {
  //------------------------------
  // Private data
  //------------------------------
  EnrichedType etype=null;

  //==================================================================
  // Constructor
  //==================================================================
  public ExprLiteral(Type t) {
    super(t);
  }


  //------------------------------------------------------------------
  // Setters
  //------------------------------------------------------------------

  //------------------------------------------------------------------
  // setEnrichedType  
  //
  // Sets the enriched type of the literal expression. This enriched
  // enriched type holds the native type of the literal expression
  // and its possible constant value
  //
  //------------------------------------------------------------------
  public void setEnrichedType(EnrichedType et) {
    etype=et;
  }


  //------------------------------------------------------------------
  // Getter
  //------------------------------------------------------------------

  //------------------------------------------------------------------
  // isConstant 
  //
  // Returns 'true' if the literal is a 'compile time' constant
  //
  //------------------------------------------------------------------
  public boolean isConstant() {
    if (etype==null) {
      // Should never happen
      return true;
    }
    return etype.isConstantScalar();
  }

  //------------------------------------------------------------------
  // getEnrichedType  
  //
  // Gets the enriched type of the literal expression. This enriched
  // enriched type holds the native type of the literal expression
  // and its possible constant value
  //
  //------------------------------------------------------------------
  public EnrichedType getEnrichedType() {
    return etype;
  }


  //==================================================================
  // Verbose functions
  //==================================================================

  // public String toString()
  // Use the standard Literal 'toString' function which dumps the AST, because
  // it can be an expression
}
