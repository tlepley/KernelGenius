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

package ir.base;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.RecognitionException;

import common.CompilerError;

import parser.KernelGeniusEmitter;
import parser.TNode;
import ir.literals.Literal;
import ir.literals.c.FloatingPointLiteral;
import ir.literals.c.IntegerLiteral;
import ir.types.kg.KernelDataCoordinate;

public class BorderMode {
  // Default mode
  static public BorderMode Default=new BorderMode();
  
  // AST top node (for CONST_VALUE and EXP)
  TNode node=null;

  // Border 
  public enum Type {CONST_VALUE, DUPLICATE, MIRROR, SKIP, EXP, UNDEF};
  Type mode=Type.UNDEF; // Default

  // For CONST_VALUE
  Literal constBorderLiteral=null;
  
  // For EXP
  String borderFunction=null; // Border function raw string
  List<Object> borderFunctionList=new LinkedList<Object>(); // Processed border function



  //##########################################################################################
  //# Constructors
  //##########################################################################################

  private BorderMode() {}

  // Main constructor
  public BorderMode(String ident) throws Exception {
    // Clear semantics
    if (ident.equals("skip")) {
      mode=Type.SKIP;
    }
    else if (ident.equals("duplicate")) {
      mode=Type.DUPLICATE;
    }
    else if (ident.equals("mirror")) {
      mode=Type.MIRROR;
    }
    else if (ident.equals("undef")) {
      mode=Type.UNDEF;
    }
    else {
      throw new Exception();
    }
  }
  
  // Constructor of CONST_VALUE
  public BorderMode(Literal l, TNode tn) throws Exception {
    mode=Type.CONST_VALUE;
    constBorderLiteral=l;
    node=tn;
    if (!l.isConstant()) {
      throw new Exception("the value must be a constant literal");
    }
  }
  
  // Constructor of EXP
  public BorderMode(String f, TNode tn, CompilerError ce) throws Exception {
    mode=Type.EXP;
    borderFunction=f;
    node=tn;
    
    processBorderFunction(borderFunction,borderFunctionList,tn,ce);
  }
  
  //##########################################################################################
  //# Getters
  //##########################################################################################

  public Type getType() {
    return mode;
  }
 
  public List<Object> getBorderFunctionList() {
    return borderFunctionList;
  }
  public TNode getASTNode() {
    return node;
  }

  public boolean isSkip() {
    return mode==Type.SKIP;
  }
  public boolean isUndef() {
    return mode==Type.UNDEF;
  }
  public boolean isSkipOrUndef() {
    return isSkip()||isUndef();
  }
  public boolean isDuplicate() {
    return mode==Type.DUPLICATE;
  }
  public boolean isMirror() {
    return mode==Type.MIRROR;
  }
  public boolean isExp() {
    return mode==Type.EXP;
  }
  public boolean isConstValue() {
    return mode==Type.CONST_VALUE;
  }
 
  public boolean isSame(BorderMode bm) {
    if (mode!=bm.mode) return false;
    if (mode==Type.CONST_VALUE) {
      // Note: Only scalar literals are supported
      if ((constBorderLiteral instanceof IntegerLiteral)&&(bm.constBorderLiteral instanceof IntegerLiteral)) {
        IntegerLiteral il=(IntegerLiteral)constBorderLiteral;
        IntegerLiteral ilbm=(IntegerLiteral)bm.constBorderLiteral;
        if (il.getValue()!=ilbm.getValue()) { return false; }
      }
      else if ((constBorderLiteral instanceof FloatingPointLiteral)&&(bm.constBorderLiteral instanceof FloatingPointLiteral)) {
        FloatingPointLiteral fpl=(FloatingPointLiteral)constBorderLiteral;
        FloatingPointLiteral fplbm=(FloatingPointLiteral)bm.constBorderLiteral;
        if (fpl.getValue()!=fplbm.getValue()) { return false; }
      }
      else {
        return false;
      }
    }
    else if (mode==Type.EXP) {
      // TODO: not necessarily correct ?
      if (borderFunction.compareTo(bm.borderFunction)!=0) {
        return false;
      }
    }
    return true;
  }
  
  
  //##########################################################################################
  //# CONST_VALUE specific
  //##########################################################################################
  Literal getConstLiteral() {
    return constBorderLiteral;
  }

  //##########################################################################################
  //# EXP specific
  //##########################################################################################

  public String getBorderFunction() {
    return borderFunction;
  }

  // Process the string function and compute the input read pattern
  int relativeLine;
  void updateRelativeLine(String s) {
    int index=0;
    int i;
    while ((i=s.indexOf("\n",index))>=0) {
      index=i+1;
      relativeLine++;
    }
  }
  void resetRelativeLine() {
    relativeLine=0;
  }
  int getRelativeLine() {
    return relativeLine;
  }
  
  //------------------------------------------------------------------------
  // Get the full string corresponding to an array access.
  //------------------------------------------------------------------------
  static int getEndOfReference(String s, int start, TNode fNode, CompilerError ce) {
    int size=s.length();
    int i;
    
    // Remove spaces
    for (i=start;(i<size)&&Character.isWhitespace(s.charAt(i));i++);
    // Get the identifier
    for (;i<size;i++) {
      char c=s.charAt(i);
      if (((c<'0')||(c>'9')) && ((c<'a')||(c>'z')) && ((c<'A')||(c>'Z')) && (c!='_')) {
        break;
      }
    }

    // Get 
    for (;(i<size)&&Character.isWhitespace(s.charAt(i));i++);
    while ((i<size) && (s.charAt(i)=='[')) {
      // Look for the other
      i = s.indexOf("]",i);
      if (i<0) {
        ce.raiseError(fNode,"while parsing the border function (missing ']')");
        return size;
      }
      for (i++;(i<size)&&Character.isWhitespace(s.charAt(i));i++);
    }
    
    return i;
  }

  
  // Process the border string function
  protected void processBorderFunction(final String s, List<Object> fList, TNode fNode, CompilerError ce) {
    int coord=0;
    resetRelativeLine();

    int i= s.indexOf("$");
    int j=0;
    while (i>=0) {

      // Get the string before
      String subS=s.substring(j, i);
      fList.add(subS);
      updateRelativeLine(subS);

      // Take the end of the input reference
      j = getEndOfReference(s,i+1,fNode,ce);

      final String variableAccessReferenceFull = s.substring(i + 1, j);
      final String variableAccessReference = variableAccessReferenceFull.trim();

      // Get the input name
      int a = variableAccessReference.indexOf("[");
      if (a<0) {
        ce.raiseError(fNode, getRelativeLine(),"$"+variableAccessReference+" should reference a [] index in the border function");
      }
      else if (variableAccessReference.charAt(0)!='[') {
        String name=variableAccessReference.substring(0, a);
        ce.raiseWarning(fNode, getRelativeLine(),"$"+variableAccessReference+" should not reference a variable ("+name+
            ") but only a [] index in the border function. Ignoring the variable");
      }

//      KernelData kdReference = this;

      // Get coordinates
      {
        int b = variableAccessReference.indexOf("]",a);
        if (b<0) {
          ce.raiseError(fNode, getRelativeLine(),"while parsing the border function (missing ']')");
          break;
        }
        // Get the index
        String sIndex=variableAccessReference.substring(a + 1, b);
        int index=0;
        try {
          index=Integer.parseInt(sIndex);
        } catch (NumberFormatException e) {
          // This is not a number, so it is considered as an expression
          // -> TBW should not raise an error
          ce.raiseError(fNode, getRelativeLine(),"bad index  '"+sIndex+"' in the border function");
        }
        coord=index;

        // Next index
        a = variableAccessReference.indexOf("[",b);
        if (a>=0) {
          ce.raiseError(fNode, getRelativeLine(),"$"+variableAccessReference+" should reference only one [] dimension.");  
        }
      }

      updateRelativeLine(variableAccessReferenceFull);

      // Add the index object
      // fList.add(new DataIndexReference(kdReference,coord));
      fList.add(new KernelDataCoordinate(null,coord));

      // Next reference
      i = s.indexOf("$",j);  
    }

    // Get the Last string
    fList.add(s.substring(j, s.length()));
  }
  
  
  //##########################################################################################
  //# Generation
  //##########################################################################################

  public String toString() {
    switch(mode) {
    case CONST_VALUE:
      return "<const>";
    case DUPLICATE:
      return "duplicate";
    case MIRROR:
      return "mirror";
    case SKIP:
      return "skip";
    case EXP:
      return "<expression>";
    case UNDEF:
      return "undef";
    }
    return "<UNKNOWN>";
  }

  public void generateValueLiteral(PrintStream ps) {
    KernelGeniusEmitter emitter=new KernelGeniusEmitter(ps,node);
    try {
      emitter.literalNoType();
    } catch (RecognitionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}


