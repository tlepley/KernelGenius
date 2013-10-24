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

/* Union tag symbol */


package ir.symbolTable;

public class UnionLabel extends TagLabel {

  //==================================================================
  // Constructors
  //==================================================================

  public UnionLabel(String name) {
    super(name);
  }


  //==================================================================
  // Verbose functions
  //==================================================================

   //------------------------------------------------------------------
  // getMessageName:
  //
  // Return the symbol reference name as i should appear in a message
  // or error
  //------------------------------------------------------------------
  public String getMessageName() {
    return "union '"+ getName() +"'";
  }

  //------------------------------------------------------------------
  // toString:
  //
  // Dump the symbol to a string
  //------------------------------------------------------------------
  public String toString() {
    StringBuffer buff = new StringBuffer();

    buff.append("Union: ");

    // Common symbol info
    buff.append(super.toString());

    // Return the final string
    return buff.toString();
  }
}