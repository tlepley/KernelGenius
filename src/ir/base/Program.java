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

/* KernelGenius Program construct */

package ir.base;

import java.util.LinkedList;
import java.util.List;



import common.CompilerError;


import parser.TNode;

public class Program extends IRElement {
  String name=null;
  
  private final List<Kernel> kernelList=new LinkedList<Kernel>();
  private final List<String> nativeStatementList=new LinkedList<String>();
  private final List<TNode>  declarationList = new LinkedList<TNode>();
  
  private final List<Object> statementList=new LinkedList<Object>();

  //===========================================================================
  // Building
  //===========================================================================

  public Program(String s) {
    name=s;
  }

  public void addKernel(Kernel k) {
    getKernelList().add(k);
    getStatementList().add(k);
  }

  public void addNativeStatement(String s) {
    nativeStatementList.add(s);
    getStatementList().add(s);
  }

  public void addDeclaration(TNode d) {
    declarationList.add(d);
    getStatementList().add(d);
  }
  
  //===========================================================================
  // Getters
  //===========================================================================
  
  public String getName() {
    return name;
  }
  
  public int getNbKernels() {
    return getKernelList().size();
  }

  public Kernel getKernel(int i) {
    return getKernelList().get(i);
  }

  public List<Object> getStatementList() {
    return statementList;
  }

  public List<Kernel> getKernelList() {
    return kernelList;
  }

  
  //===========================================================================
  // Check for correct IR
  //===========================================================================
  
  public void completeAndCheck(CompilerError ce) {
    if (getKernelList().size()==0) {
      ce.raiseWarning("no kernel defined in the program");
    }
    
    // Final check of nodes
    for (Kernel k: getKernelList()) {
      k.completeAndCheck(ce);
    }
  }

  //===========================================================================
  // Verbose
  //===========================================================================

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Program '").append(getName()).append("' :\n");

    for (Kernel k:getKernelList()) {
      sb.append(k.toString());
    }

    return sb.toString();
  }

}
