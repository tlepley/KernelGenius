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

/* Convolution predefined algorithmic node */

package ir.algorithms;

import java.util.List;
import parser.TNode;

import common.CompilerError;
import ir.base.KernelData;
import ir.literals.Literal;
import ir.literals.c.ArrayLiteral;
import ir.literals.c.ScalarLiteral;
import ir.literals.c.FloatingPointLiteral;
import ir.literals.c.IntegerLiteral;
import ir.types.Type;
import ir.types.c.Array;
import ir.types.c.IntegerScalar;
import ir.types.kg.TypedMatrixIndexes;

public class Convolution extends Filter {
  static IntegerLiteral defaultMulDiv=new IntegerLiteral(1,IntegerScalar.Tsint);      
  
  // Coefficients
  TypedMatrixIndexes coefType=null;
  ArrayLiteral coefLiteral=null;

  // Post Operation
  ScalarLiteral multiplier=null;
  ScalarLiteral divider=null;


  //==================================================================
  // Setters specific to algos
  //==================================================================
  
  public void setCoefficients(ArrayLiteral c, TNode tn, CompilerError ce) {
    if (coefLiteral!=null) {
      ce.raiseError(tn,"redefining the 'coefficient' property");
    }
    coefLiteral=c;
  }
  public void setMultiplier(ScalarLiteral c, TNode tn, CompilerError ce) {
    if (multiplier!=null) {
      ce.raiseError(tn,"redefining the 'multiplier' property");
    }
    multiplier=c;
  }
  public void setDivider(ScalarLiteral c, TNode tn, CompilerError ce) {
    if (divider!=null) {
      ce.raiseError(tn,"redefining the 'divider' property");
    }
    divider=c;
  }
  
  
  //==================================================================
  // Properties management
  //==================================================================

  public void setProperty(String prop, List<KernelData> paramList, TNode tn, CompilerError ce) {
    raiseUnknownIdentifierPropertyError(prop,tn,ce);
  }

  public void setPropertyWithIdentifier(String prop, List<KernelData> paramList, String ident, TNode tn, CompilerError ce) {
    if (prop.equals("border")) {
      super.setPropertyWithIdentifier(prop,paramList,ident,tn,ce);
    }
    else {
      raiseUnknownIdentifierPropertyError(prop,tn,ce);
    }
  }
  
  public void setPropertyWithLiteral(String prop, List<KernelData> paramList, Literal l, TNode tn, CompilerError ce) {
    if (prop.equals("border")) {
      if (prop.equals("border")) {
        super.setPropertyWithLiteral(prop,paramList,l,tn,ce);
      }
    }
    else if (prop.equals("coefficients")) {
      if (paramList.size()!=0) {
        raiseParamPropertyError(prop,tn,ce);
      }
      if (!(l instanceof ArrayLiteral)) {
        ce.raiseError(tn,"The 'coefficients' property must be an mono- or multi-dimensional array");
        return;
      }
      setCoefficients((ArrayLiteral)l,tn,ce);
    }
    else if (prop.equals("multiplier")) {
      if (paramList.size()!=0) {
        raiseParamPropertyError(prop,tn,ce);
      }
     if (!(l instanceof ScalarLiteral)) {
        ce.raiseError(tn,"The 'multiplier' property must be a scalar");
        return;
      }
      setMultiplier((ScalarLiteral)l,tn,ce);
    }
    else if (prop.equals("divider")) {
      if (paramList.size()!=0) {
        raiseParamPropertyError(prop,tn,ce);
      }
      if (!(l instanceof ScalarLiteral)) {
        ce.raiseError(tn,"The 'divider' property must be a scalar");
        return;
      }
      setDivider((ScalarLiteral)l,tn,ce);
    }
    else {
      raiseUnknownLiteralPropertyError(prop,tn,ce);
    }
  }

  public void setPropertyWithArrayRange(String prop, List<KernelData> paramList, Array array, TNode tn, CompilerError ce) {
    raiseUnknownArrayRangePropertyError(prop,tn,ce);
  } 
  
  public void setPropertyWithString(String prop, List<KernelData> paramList, String s, TNode tn, CompilerError ce) {
    // border 'EXP' non valid with Convolution (Specific to Filter)
    raiseUnknownStringPropertyError(prop,tn,ce);
  }

  
  //==================================================================
  // Check
  //==================================================================

  public boolean completeAndCheckNode(CompilerError ce) {
    boolean error=false;

    //-- Check coefficients
    if (coefLiteral==null) {
      ce.raiseError(getNameNode(),"coefficient definition missing for ");
    }
    else {
      coefType=new TypedMatrixIndexes(coefLiteral);

      // Check the base type
      if (!coefType.getBaseCType().isArithmeticScalar()) {
        ce.raiseError(getNameNode(),"the base type of coefficients must be an arithmetic type");
      }
    }
    
    // Multiplier/divider : default is 1
    if (multiplier==null) {
      multiplier=defaultMulDiv;      
    }
    if (divider==null) {
      divider=defaultMulDiv;      
    }
    
    //-- Check output type
    if (!getOutputBaseCType().isArithmeticScalar()) {
      ce.raiseError(getNameNode(),"the base output type should be a scalar arithmetic type");
    }

    //-- Check the input
    if (getNbInputData()!=1) {
      ce.raiseError(getNameNode(),"must have one input parameter");
      error=true;
    }
    Type t=getFirstInputData().getMatrixType().getBaseCType();
    if (!t.isArithmeticScalar()) {
      ce.raiseError(getNameNode(), "the base type of input "+getFirstInputData().getName()+" should be a scalar arithmetic type");
      error=true;
    } 
    if (getFirstInputData().getMatrixType().getNbDims() < coefType.getNbDims()) {
      ce.raiseError(getNameNode(),"coefficients have the more dimensions than input '"+getFirstInputData().getName()+"' "); 
      error=true;
    }
    
    // No need to continue in case of error
    if (error) { return true; }
    
    // Set the filter function
    setFunction();
    
    // Call the Filter function
    return super.completeAndCheckNode(ce);
  }

  
  //==================================================================
  // Filter function generation
  //==================================================================
  
  // Sets the function expected by Filter
  public void setFunction() {
    int nbDims=getFirstInputData().getMatrixType().getNbDims();
    StringBuffer sb=new StringBuffer();

    // Assignment
    sb.append("@");
    sb.append(getName());
    for(int i=0;i<nbDims;i++) {
      sb.append("[0]");
    }
    sb.append("=");

    // Convolution expression
    generateFixedCoefExpression(sb);
    
    // End of expression
    sb.append(";");
    
    //System.err.print(sb);
    
    // Filter function
    setFunction(sb.toString(),null);
  }
  
  // Dim 1
  void generateFixedCoefExpression(StringBuffer sb) {    
    sb.append("((");

    boolean first=true;
    //---------------- Fixed coef generation --------------
    ArrayLiteral al=(ArrayLiteral) coefLiteral;
    for(int y=coefType.getFirstIndex(1),yy=0;y<=coefType.getLastIndex(1);y++,yy++) {
      // TODO: should check
      ArrayLiteral aly;
      if (coefType.getNbDims()==1) {
        aly=al;
      }
      else {
        aly = (ArrayLiteral)(al.getAtIndex(yy));
      }

      //***** center *******
      first=generateFixedCoefExpression0(aly,y,first,sb);
    }
    sb.append(")");

    // Multiplier
    if (!multiplier.isOne()) {
      sb.append("*");
      sb.append(multiplier.toString());
    }       
    sb.append(")");

    // Divider
    if (!divider.isOne()) {
      sb.append("/");
      sb.append(divider.toString());
    }
  }


  // Dim 0
  boolean generateFixedCoefExpression0(
      ArrayLiteral aly, int y, boolean first,
      StringBuffer sb) {
    String inputName=getFirstInputData().getName();

    // In case the literal is not fully initialized
    if (aly==null) {
      // Consider 0 as coefficients
      return first;
    }

    for(int x=coefType.getFirstIndex(0),xx=0;
        x<=coefType.getLastIndex(0);
        x++,xx++) {   
      // Coef
      boolean flag = generateCoef(aly.getAtIndex(xx),first,sb);  
      // Array
      if (flag) {
        sb.append("$").append(inputName);
        sb.append("[").append(y).append("][").append(x).append("]");
        first=false;
      }
    }

    return first;
  }


  // Coefficient generation
  boolean generateCoef(Literal alx, boolean first, StringBuffer sb) {
    // In case the literal is not fully initialized
    if (alx==null) {
      return false;
    }
    if (alx instanceof FloatingPointLiteral) {
      double d=((FloatingPointLiteral)alx).getValue();
      if (d!=0) {
        if (d<0) {
          if (d==-1) {
            sb.append("-");
          }
          else {
            sb.append(""+d+"*");
          }
        }
        else {
          if (!first) {
            sb.append("+");
          }
          if (d!=1) {
            sb.append(""+d+"*");
          }
        }
        return true;
      }       
    }
    else if (alx instanceof IntegerLiteral) {
      long l=((IntegerLiteral)alx).getValue();
      if (l!=0) {
        if (l<0) {
          if (l==-1) {
            sb.append("-");
          }
          else {
            sb.append(""+l+"*");
          }
        }
        else {
          if (!first) {
            sb.append("+");
          }
          if (l!=1) {
            sb.append(""+l+"*");
          }
        }
        return true;
      }   
    }
    else {
      System.err.println("Internal Error, coef is not a Scalar Literal : "+alx+","+coefLiteral);
      CompilerError.exitWithError();      
    }
    return false;
  }


  
  
  boolean hasCoefValues() {
    return !((ArrayLiteral)coefLiteral).isEmpty();
  }


  //========================================================
  // Verbose
  //========================================================

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(super.toString());
    sb.append("; coefficients=").append(coefLiteral.toString());
    return sb.toString();
  }

}
