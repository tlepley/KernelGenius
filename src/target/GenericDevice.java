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

package target;

import common.CompilerError;

import ir.literals.Literal;

public abstract class GenericDevice {
  String name=null;
  
  GenericDevice(String s) {
    name=s;
  }
  
  //==================================================================
  // Getters
  //==================================================================

  public String getName() {
    return name;
  }

  //==================================================================
  // Setters specific to object building
  //==================================================================

  abstract public void setProperty(String prop, CompilerError ce);
  abstract public void SetPropertyWithIdentifier(String prop, String ident, CompilerError ce);
  abstract public void SetPropertyWithInteger(String prop, long i, CompilerError ce);
  abstract public void SetPropertyWithLiteral(String prop, Literal array, CompilerError ce);
  abstract public void SetPropertyWithString(String prop, String s, CompilerError ce);


  //==================================================================
  // Error management
  //==================================================================
  
  public void raiseUnknownPropertyError(String prop, CompilerError ce) {
    ce.raiseError(this.getClass().getName()+"': unknown property '"+prop+"'");
  }
  public void raiseUnknownIdentifierPropertyError(String prop, CompilerError ce) {
    ce.raiseError(this.getClass().getName()+"': unknown identifier property '"+prop+"'");
  }
  public void raiseUnknownStringPropertyError(String prop, CompilerError ce) {
    ce.raiseError(this.getClass().getName()+": unknown string property '"+prop+"'");
  }
  public void raiseUnknownIntegerPropertyError(String prop, CompilerError ce) {
    ce.raiseError(this.getClass().getName()+": unknown integer property '"+prop+"'");
  }
  public void raiseUnknownLiteralPropertyError(String prop, CompilerError ce) {
    ce.raiseError(this.getClass().getName()+"': unknown literal property '"+prop+"'");
  }
  public void raiseUnknownPropertyValueError(String prop, String value, CompilerError ce) {
    ce.raiseError(this.getClass().getName()+"': for property '"+prop+"', unknown value '"+value+"'");
  }
  public void raiseRedefinePropertyError(String prop, CompilerError ce) {
    ce.raiseError(this.getClass().getName()+"': redefining property '"+prop+"'");
  }
  public void raiseMessagePropertyError(String prop, String message, CompilerError ce) {
    ce.raiseError(this.getClass().getName()+"': property '"+prop+"' "+message);
  }

  //==================================================================
  // Check
  //==================================================================

  abstract public void finalCheck(CompilerError ce);
  
  
  //========================================================
  // Output
  //========================================================

  abstract public String toString();
}
