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

package ir.types.kg;

import java.io.PrintStream;
import java.util.List;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;

import parser.KernelGeniusEmitter;
import parser.TNode;

import ir.base.KernelData;
import ir.literals.Literal;
import ir.types.Type;

public class TypedMatrixIndexes extends MatrixIndexes {
  Type baseCType=null;
  KernelData indexRangeSpecifier=null;
  // Used to distinguish single element array from non array types
  boolean isAnArray=true;


  public boolean isSame(TypedMatrixIndexes t) {
    if (baseCType.unqualify()!=t.baseCType.unqualify()) return false;
    return super.isSame(t);
  }
  
  //==================================================================
  // Building
  //==================================================================

  public TypedMatrixIndexes(Type t) {    
    baseCType=init(t);
    if (!t.isArray()) {
      isAnArray=false;
    }
  }
 
  public TypedMatrixIndexes(Type t, TypedMatrixIndexes refIter) {    
    super(refIter);
    baseCType=t;
    isAnArray=refIter.isAnArray;
  }

  public TypedMatrixIndexes(Literal literal) {
    baseCType=init(literal.getType());
    if (!literal.getType().isArray()) {
      isAnArray=false;
    }
  }
 
  public TypedMatrixIndexes(TypedMatrixIndexes t) {
    super(t);
    baseCType=t.baseCType; // No duplicate
    isAnArray=t.isAnArray;
  }

  // 1D matrix with index starting from 0
  public TypedMatrixIndexes(Type bt, int nb) {    
    super(nb);
    baseCType=bt;
  }
  
  // 2D matrix with index starting from 0
  public TypedMatrixIndexes(Type bt, int nb0, int nb1) {    
    super(nb0,nb1);
    baseCType=bt;
  }
  
  // ND matrix with fixed indexes.
  // Note: The index is null in case the index is not fixed.
  public TypedMatrixIndexes(Type bt, List<Integer> first, List<Integer> last) {    
    super(first,last);
    baseCType=bt;
  }
  
  
  //==================================================================
  // Getters
  //==================================================================

  public boolean isRealArray() {
    return isAnArray;
  }

  public Type getBaseCType() {
    return baseCType;
  }


  //==================================================================
  // Verbose
  //==================================================================

  public void generate(PrintStream ps, TNode baseCTypeNode, String s) {
    CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)baseCTypeNode);
    KernelGeniusEmitter emitter=new KernelGeniusEmitter(nodes);
    emitter.setNoLineManagement();
    emitter.setPrintStream(ps);
    try {
      emitter.declSpecifiers();
    } catch (RecognitionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
      
    //baseCType.generate(ps);
    
    ps.print(" ");
    ps.print(s);
    super.generate(ps);
  }

  public void generateSizeInBytes(PrintStream ps, TNode baseCTypeNode) {
    ps.print("sizeof(");
    
    CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)baseCTypeNode);
    KernelGeniusEmitter emitter=new KernelGeniusEmitter(nodes);
    emitter.setNoLineManagement();
    emitter.setPrintStream(ps);
    try {
      emitter.declSpecifiers();
    } catch (RecognitionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
      
    //baseCType.generate(ps);
    
    
    ps.print(")");
    ps.print("*");
    super.generateNumberOfElements(ps);
  }

  
  public String toString() {
    StringBuffer sb = new StringBuffer();

    if (!super.hasSingleElement()) {
      sb.append(super.toString());
      sb.append(" of ");
    }
    sb.append(baseCType.toString());

    return sb.toString();
  }

}
