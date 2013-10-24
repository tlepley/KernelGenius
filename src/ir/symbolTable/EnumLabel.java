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

/* Enum tag symbol */


package ir.symbolTable;

import java.util.Vector;

public class EnumLabel extends TagLabel {

  //==================================================================
  // Constructors
  //==================================================================

  public EnumLabel(String name) {
    super(name);
  }


  //==================================================================
  // 'Children' management
  // 
  // -> Enum specific.Children of an enum tag are enum fields that it
  //    defines 
  //------------------------------------------------------------------

  // For ENUM symbols
  Vector<EnumConstantLabel> children = null;

  // Setting
  public void setChildren(Vector<EnumConstantLabel> v) {
    children=v;
  }
  public void addChild(EnumConstantLabel symb) {
    if (children==null) {
      children=new Vector<EnumConstantLabel>(10);
    }
    children.addElement(symb);
  }

  // Query
  public int getNbChildren() {
    if (children==null) {
      return(0);
    }
    return(children.size());
  }
  public Vector<EnumConstantLabel> getChildren() {
    return children;
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
    return "enum '"+ getName() +"'";
  }

  //------------------------------------------------------------------
  // toString:
  //
  // Dump the symbol to a string
  //------------------------------------------------------------------
  public String toString() {
    StringBuffer buff = new StringBuffer();

    buff.append("Enum: ");

    // Common symbol info
    buff.append(super.toString());

    // 'Children'
    if (children!=null) {
      int i=0;
      buff.append(", children=[");
      for(EnumConstantLabel obj:children) {
	if (i==0) {
	  i=1;
	}
	else {
	  buff.append(" ");
	}
	buff.append(obj.getId());
      }
      buff.append("]");
    }


    // Return the final string
    return buff.toString();
  }
}