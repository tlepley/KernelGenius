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

/* Storage class of a symbol */


package ir.symbolTable;

import parser.TNode;
import common.CompilerError;


public class StorageClass {
  // Storage class
  private boolean typedef_storage_class  = false;
  private boolean static_storage_class   = false;
  private boolean extern_storage_class   = false;
  private boolean auto_storage_class     = false;
  private boolean register_storage_class = false;
  //-> C99, only for functions
  private boolean inline_storage_class   = false;


  //==================================================================
  // Setting
  //==================================================================

  // 'typedef' storage class
  public void setTypedef(TNode tn,CompilerError cp) {
    if (typedef_storage_class) {
      cp.raiseWarning(tn,"duplicate 'typedef'");
    }
    typedef_storage_class=true;
  }
  // 'extern' storage class
  public void setExtern(TNode tn,CompilerError cp) {
    if (extern_storage_class) {
      cp.raiseWarning(tn,"duplicate 'extern'");
    }
    extern_storage_class=true;
  }
  // 'static' storage class
  public void setStatic(TNode tn,CompilerError cp) {
    if (static_storage_class) {
      cp.raiseWarning(tn,"duplicate 'static'");
    }
    static_storage_class=true;
  }
  // 'register' storage class
  public void setRegister(TNode tn,CompilerError cp) {
    if (register_storage_class) {
      cp.raiseWarning(tn,"duplicate 'register'");
    }
    register_storage_class=true;
  }
  // 'auto' storage class
  public void setAuto(TNode tn,CompilerError cp) {
    if (auto_storage_class) {
      cp.raiseWarning(tn,"duplicate 'auto'");
    }
    auto_storage_class=true;
  }
  // 'inline' storage class
  public void setInline(TNode tn,CompilerError cp) {
    if (inline_storage_class) {
      cp.raiseWarning(tn,"duplicate 'inline'");
    }
    inline_storage_class=true;
  }


  //==================================================================
  // Query
  //==================================================================

  // 'typedef' type specifier
  public boolean isTypedef() {
    return(typedef_storage_class);
  }
  // Returns 'true' if the symbol is declared as 'extern'
  public boolean isExtern() {
    return(extern_storage_class);
  }
  // Returns 'true' if the symbol is declared as 'static'
  public boolean isStatic() {
    return(static_storage_class);
  }
  // Returns 'true' if the symbol is declared as 'auto'
  public boolean isAuto() {
    return(auto_storage_class);
  }
  // Returns 'true' if the symbol is declared as 'register'
  public boolean isRegister() {
    return(register_storage_class);
  }

  // Returns 'true' if the symbol is declared as 'inline'
  public boolean isInline() {
    return(inline_storage_class);
  }


  //==================================================================
  // Check
  //==================================================================

  // Returns 'true' multiple storage classes have been specified
  // Note: 'inline' is not considered as a storage class for this check
  public boolean isMultipleStorageClass() {
    if (typedef_storage_class) {
      if (extern_storage_class || static_storage_class ||
          auto_storage_class || register_storage_class ) {
        return true;
      }
    }
    else if (extern_storage_class) {
      if ( static_storage_class ||
          auto_storage_class || register_storage_class ) {
        return true;
      }
    }
    else if (static_storage_class) {
      if ( auto_storage_class || register_storage_class ) {
        return true;
      }
    }
    else if ( auto_storage_class && register_storage_class) {
      return true;
    }

    return false;
  }


}
