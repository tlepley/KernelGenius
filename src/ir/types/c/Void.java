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

/* C void type */

package ir.types.c;

import ir.types.Type;

import java.util.HashSet;
import java.util.HashMap;

public class Void extends Type {
  public static Void Tvoid=new Void();


  //==================================================================
  // Private constructor
  //==================================================================
  private Void() {
    ;
  }


  //==================================================================
  // Type management
  //==================================================================

  public boolean isVoid() {return true;}


  //==================================================================
  // Target Specific information
  //==================================================================

  public int sizeof() {return 0;}
  public int alignof() {return 1;}
  

  //==================================================================
  // Signature management (for arguments of function prototypes)
  //==================================================================

  public String getSignature() {
    return "v";
  }


  //==================================================================
  // Verbose functions
  //==================================================================

  //------------------------------------------------------------------
  // toString :
  //
  // Returns string a textual representation of the type
  //------------------------------------------------------------------
  public String toStringInternal(HashSet<Type> ts, HashMap<Type,Integer> cs) {
    return("void");
  }

}
