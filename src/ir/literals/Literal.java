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

/* Generic Literals (mother class of literals) */

package ir.literals;


import ir.types.Type;


public abstract class Literal {
  // Type of the current literal
  Type type=null;

  public Literal(Type t) {
    type=t;
  }

  //------------------------------------------------------------------
  // Setters
  //------------------------------------------------------------------

  //------------------------------------------------------------------
  // setType
  //
  // Sets the declared type of the literal (can be different from
  // its native type in case of scalar)
  //
  //------------------------------------------------------------------
  protected void setType(Type t) {
    // Sets the type
    type=t;
  }


  //------------------------------------------------------------------
  // Getters
  //------------------------------------------------------------------

  //------------------------------------------------------------------
  // isConstant 
  //
  // Returns 'true' if the literal is a 'compile time' constant
  //
  //------------------------------------------------------------------
  abstract public boolean isConstant();

  //------------------------------------------------------------------
  // getType
  //
  // Gets the declared type of the literal (can be different from
  // its native type in case of scalar)
  //
  //------------------------------------------------------------------
  public Type getType() {
    // Sets the type
    return type;
  }


  //==================================================================
  // Verbose functions
  //==================================================================

  public String toString() {
    StringBuffer buff = new StringBuffer();

    // Constant
    if (isConstant()) {
      // Compile time constant
      buff.append("compile-time constant literal");
    }
    else {
      // Non compile time constant
      buff.append("Non compile-time constant literal");
    }

    // Return the final string
    return buff.toString();
  }
}
