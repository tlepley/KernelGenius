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

package ir.symbolTable;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import common.CompilerError;


public class SymbolTable {

  //------------------------------
  // Private data
  //------------------------------

  // Scope management
  private Vector<String> scopeStack;
  private int unnamedScopeCounter = 0;
  private int scope_depth         = 0;

  // Ordinary name space
  //--------------------
  private Map<String,Symbol> symTable;


  //==================================================================
  // Constructor
  //==================================================================
  public SymbolTable()  {
    scopeStack             = new Vector<String>(10);
    symTable               = new LinkedHashMap<String,Symbol>(200);
  }



  //==================================================================
  // Mangling management
  //==================================================================

  // ******************************************************************
  // Mangling counter management 
  // ******************************************************************
  private int mangling_counter  = 0;

  public int getManglingCounter() {
    return(mangling_counter);
  }
  public void setManglingCounter(int m) {
    mangling_counter=m;
  }

  // ******************************************************************
  // getMangling :
  //
  // Returns the mangled version of 's', from a counter which is
  // incremented. The resulting name is unique
  // ******************************************************************
  public String getMangling(String s) {
    mangling_counter++;
    StringBuffer str = new StringBuffer("_M" + mangling_counter + "_" + s);
    return(str.toString());
  }

  // ******************************************************************
  // getSameMangling :
  //
  // Returns the mangled version of 's', from a counter which is not
  // incremented
  // ******************************************************************
  public String getSameMangling(String s) {
    StringBuffer str = new StringBuffer("_M" + mangling_counter + "_" + s);
    return(str.toString());
  }


  // ******************************************************************
  // getMangling :
  //
  // Returns a new symbol name from a counter which is incremented.
  // The resulting name is unique
  // ******************************************************************
  public String getNewName() {
    mangling_counter++;
    StringBuffer str = new StringBuffer("_N" + mangling_counter);
    return(str.toString());
  }


  //==================================================================
  // Scope management
  //==================================================================

  //------------------------------------------------------------------
  //  isTopLevel 
  //
  //  Returns true is the current global scope is the top level one
  //
  //------------------------------------------------------------------
  public boolean isTopLevel() {
    return scope_depth==0;
  }

  // Returns the current scope depth
  public int getCurrentScopeDepth() {
    return(scope_depth);
  }


  //------------------------------------------------------------------
  // Push scope
  //------------------------------------------------------------------

  //------------------------------------------------------------------
  //  PushScope 
  //
  //  Adds one level of the global scope (named scope)
  //
  //------------------------------------------------------------------
  public void pushScope(String s) {
    //System.out.println("push scope:" + s);
    scope_depth++;
    scopeStack.addElement(s);
  }

  //------------------------------------------------------------------
  //  pushScope 
  //
  //  Adds one level of the global scope (unnamed scope)
  //
  //------------------------------------------------------------------
  public void pushScope() {
    //System.out.println("push scope:" + s);
    scope_depth++;
    scopeStack.addElement("" + unnamedScopeCounter++);
  }


  //------------------------------------------------------------------
  // Pop scope
  //------------------------------------------------------------------

  //------------------------------------------------------------------
  //  popScope 
  //
  //  Removes one level of the global scope
  //
  //------------------------------------------------------------------
  public void popScope() {
    int size = scopeStack.size();
    if(size>0) {
      scopeStack.removeElementAt(size-1);
    }
    scope_depth--;
  }

  //------------------------------------------------------------------
  //  currentScopeAsString 
  //
  //  Returns the current global scope as a string (identifiers
  //  separated by ':')
  //
  //------------------------------------------------------------------
  public String currentScopeAsString() {
    StringBuffer buf = new StringBuffer(100);
    boolean first    = true;
    Enumeration<String> e = scopeStack.elements();

    while(e.hasMoreElements()) {
      if(first) {
        first = false;
      }
      else {
        buf.append(":");
      }
      buf.append(e.nextElement());
    }
    return(buf.toString());
  }



  //==================================================================
  // Symbol table building
  //==================================================================


  private void raiseRedefinitionError(String name, Symbol new_symbol, Symbol symbol_in_table, CompilerError ce) {
    // Redefinition
    ce.raiseError(new_symbol.getNode(),"redefinition of '"+name+"'");
    ce.raiseMessage(symbol_in_table.getNode(),"previous declaration of '"+name+"' was here");
  }


  //------------------------------------------------------------------
  //  add 
  //
  //  Add a new ordinary symbol definition related to the current scope
  //  The current function performs compatibility checks and raises
  //  potential errors
  //
  //------------------------------------------------------------------
  public void add(String name, Symbol new_symbol, CompilerError ce) {
    new_symbol.setScopeDepth(scope_depth);
    String scopedName=addCurrentScopeToName(name);
    Symbol symbolInTable=symTable.get(scopedName);

    if (symbolInTable!=null) {
      // A symbol with this name already exists
      raiseRedefinitionError(name,new_symbol,symbolInTable,ce);
    }

    symTable.put(scopedName,new_symbol);
  }


  //==================================================================
  // Symbols lookup
  //==================================================================

  //------------------------------------------------------------------
  // getListOfKernels
  //
  // Returns the list of symbols (accessible, not flushed) defining a
  // data or a function 
  //------------------------------------------------------------------
  public ArrayList<KernelLabel> getListOfKernels() {
    ArrayList<KernelLabel> symbol_list = new ArrayList<KernelLabel>();

    // Symbols defining a data or a function are only in the ordinary
    // name space
    for(Symbol symb:symTable.values()) {
      // Note: kernels shall not be mangled
      if (symb instanceof KernelLabel) {
        KernelLabel func_symb=(KernelLabel)symb;
        symbol_list.add(func_symb);
      }
    }

    return(symbol_list);
  }


  //------------------------------------------------------------------
  // Ordinary symbol lookup
  //------------------------------------------------------------------

  //------------------------------------------------------------------
  // lookupName
  //
  // lookup an unscoped name in the table by prepending
  // the current scope.
  // - if not found, pop scopes and look again
  // - returns null if no symbol found
  //------------------------------------------------------------------
  public Symbol lookupName(String name) {
    String scope  = currentScopeAsString();
    String scopedName;
    Symbol symbol = null;

    while ( (symbol==null) && (scope != null) ) {
      scopedName = addScopeToName(scope, name);
      symbol = symTable.get(scopedName);
      scope = removeOneLevelScope(scope);
    }
    return(symbol);
  }

  //------------------------------------------------------------------
  // lookupNameInTopLevelScope
  //
  // lookup an unscoped name in the table at top level
  // - if not found, pop scopes and look again
  // - returns null if no symbol found
  //------------------------------------------------------------------
  public Symbol lookupNameInTopLevelScope(String name) {
    return symTable.get(name);
  }


  //==================================================================
  // Debug functions
  //==================================================================

  //------------------------------------------------------------------
  // toString
  //
  // Converts the symbol table to a string
  //------------------------------------------------------------------
  public String toString() {
    StringBuffer buff = new StringBuffer(300);

    buff.append("SymbolTable { \nCurrentScope: ");

    String current_scope=currentScopeAsString();
    if (current_scope.compareTo("")==0) {
      buff.append("  <top level>");
    }
    else {
      buff.append(currentScopeAsString());
    }
    // Ordinary name space
    buff.append("\n");
    Iterator<Map.Entry<String,Symbol>> iter=symTable.entrySet().iterator();
    while (iter.hasNext()) {
      // Get Map informations
      Map.Entry<String,Symbol> entry = iter.next();
      String s    = entry.getKey();
      Symbol symb = entry.getValue();

      // Generate symbol information
      int id = symb.getId();
      buff.append("  ");
      if (id<10) {
        buff.append("  ");
      }
      else if (id<100) {
        buff.append(" ");
      }
      buff.append("").append(id).append("  ").append(s.toString())
      .append("\n       -> ").append(symb.toString()).append("\n");
    }
    buff.append("}\n");

    // Return the string
    return(buff.toString());
  }


  //==================================================================
  // Private functions for managing scoped names
  //==================================================================

  // lookup a fully scoped name in the symbol table
  private Symbol lookupScopedName(String scopedName) {
    return(symTable.get(scopedName));
  }

  // given a name for a type, append it with the 
  // current scope.
  private String addCurrentScopeToName(String name) {
    String currScope = currentScopeAsString();
    return(addScopeToName(currScope, name));
  }

  // given a name for a type, append it with the 
  // given scope.
  private String addScopeToName(String scope, String name) {
    if( (scope==null) || (scope.length()>0) ) {
      return(scope + ":" + name);
    }
    else {
      return(name);
    }
  }

  // remove one level of scope from name
  private String removeOneLevelScope(String scopeName) {
    int index = scopeName.lastIndexOf(":");
    if (index>0) {
      return(scopeName.substring(0,index));
    }
    if (scopeName.length() > 0) {
      return("");
    }
    return(null);
  }

};
