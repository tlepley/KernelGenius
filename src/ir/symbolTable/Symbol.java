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

/* Symbol of the symbol table */

package ir.symbolTable;

import parser.TNode;
import ir.types.Type;


public class Symbol {
  // ID unique to each compiler thread
  private int id;
  private static final InheritableThreadLocal<Integer> id_counter = new InheritableThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };
  private int incrementIdCounter() {
    int i=id_counter.get();
    id_counter.set(i+1);
    return i;
  }

  //------------------------------
  // Private data
  //------------------------------

  TNode node = null;
  
  // Symbol name
  private String name = null;

  // Scope level
  private int scope_depth = 0;

  // Symbol type
  private Type type = null;
  
  // Storage class
  private StorageClass storageClass=null;

  
  //==================================================================
  // Constructors
  //==================================================================
  public Symbol() {
    id     = incrementIdCounter();
    storageClass=new StorageClass();
  }

  public Symbol(StorageClass sc) {
    id = incrementIdCounter();
    storageClass  = sc;
  }

  public Symbol(String s) {
    id = incrementIdCounter();
    name   = s;
    storageClass=new StorageClass();
  }

  public Symbol(String s, Type t) {
    id = incrementIdCounter();
    name  = s;
    type  = t;
    storageClass=new StorageClass();
  }

  
  public Symbol(Symbol symb) {
    id                   = incrementIdCounter(); // New (unique id)

    // Name
    name                 = symb.name;

    // Scope location
    scope_depth          = symb.scope_depth;

    // Symbol type (future use)
    type                 = symb.type;

    // Storage class
    storageClass     = symb.storageClass;
  }

  
  //==================================================================
  // Setters
  //==================================================================

  //------------------------------------------------------------------
  // setNode
  //------------------------------------------------------------------
  public void setNode(TNode n) {
    node=n;
  }

  //------------------------------------------------------------------
  // setName
  //
  // Sets the original name of the symbol
  //------------------------------------------------------------------
  public void setName(String s) {
    name=s;
  }

  //------------------------------------------------------------------
  // setScopeDepth
  //
  // Sets the scope depth of the symbol (0 is the global scope)
  //------------------------------------------------------------------
  public void setScopeDepth(int d) {
    scope_depth=d;
  }

  //------------------------------------------------------------------
  // setType
  //
  // Sets the type of the symbol
  //------------------------------------------------------------------
  public void setType(Type t) {
    type=t;
  }

  //==================================================================
  // Getters
  //==================================================================

  public int getId() {
    return id;
  }

  public TNode getNode() {
    return(node);
  }

  //------------------------------------------------------------------
  // getName
  //
  // Returns the symbol name
  //------------------------------------------------------------------
  public String getName() {
    return(name);
  }

  //------------------------------------------------------------------
  // getScopeDepth
  //
  // Returns the scope depth of the symbol (0 is the global scope)
  //------------------------------------------------------------------
  public int getScopeDepth() {
    return scope_depth;
  }

  //------------------------------------------------------------------
  // isInTopLevelScope
  //
  // Returns 'true' if the symbol is in the top scope
  //------------------------------------------------------------------
  public boolean isInTopLevelScope() {
    return scope_depth==0;
  }
  
  //------------------------------------------------------------------
  // getType
  //
  // Returns type corresponding to the symbol
  //------------------------------------------------------------------
  public Type getType() {
    return type;
  }

  //------------------------------------------------------------------
  // getStorageClass
  //
  // Returns the storage class corresponding to the symbol
  //------------------------------------------------------------------
  public StorageClass getStorageClass() {
    return storageClass;
  }

  
  //------------------------------------------------------------------
  // Storage class tests
  //------------------------------------------------------------------
    // Returns 'true' if the symbol is declared as 'extern'
  public boolean isExtern() {
    return storageClass.isExtern();
  }
    // Returns 'true' if the symbol is declared as 'static'
  public boolean isStatic() {
    return storageClass.isStatic();
  }
    // Returns 'true' if the symbol is declared as 'register'
  public boolean isRegister() {
    return storageClass.isRegister();
  }
    // Returns 'true' if the symbol is declared as 'auto'
  public boolean isAuto() {
    return storageClass.isAuto();
  }
    // Returns 'true' if the symbol is declared as 'inline'
  public boolean isInline() {
    return storageClass.isInline();
  }
  // Returns 'true' if the symbol is visible only in the C module
  // ('static' or 'inline')
  public boolean isModuleVisibility() {
    return(isStatic()||isInline());
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
    return "symbol '"+ name +"'";
  }


  //------------------------------------------------------------------
  // toString:
  //
  // Dump the symbol to a string
  //------------------------------------------------------------------
  public String toString() {
    StringBuffer buff = new StringBuffer();

    // Name
    buff.append("name=").append(name);

    // Scope depth
    buff.append(", depth=").append(scope_depth);
    
    // Type
    if (type==null) {
      buff.append(", [no type]");
    }
    else {
      buff.append(", type=").append(type.toString());
    }
   
    // Return the final string
    return buff.toString();
  }
}
