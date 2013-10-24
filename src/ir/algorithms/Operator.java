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

/* Operator programmable algorithmic node */

package ir.algorithms;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import codegen.OpenCL.CLGenVarNames;

import parser.TNode;

import common.CompilerError;
import ir.base.DataEdge;
import ir.base.FunctionNode;
import ir.base.KernelData;
import ir.literals.Literal;
import ir.types.c.Array;
import ir.types.kg.MatrixSize;
import ir.types.kg.MatrixIndexes;

public class Operator extends FunctionNode {

  String function=null; // Function raw string
  TNode functionNode=null; // AST node for the function
  List<Object> functionList=new LinkedList<Object>(); // Processed function

  
  
  //==================================================================
  // Building
  //==================================================================
  
  public void applyAccessPatternContraints() {
    // Merge the codegen pattern between all inputs
    //mergeCodegenDataPatternInputs();
  }

 
  //==================================================================
  // Setters
  //==================================================================
  
  void setFunction(String f, TNode tn) {
    // Should check the coherence of the function or better parse it
    function=f;
    functionNode=tn;
  }
  
  public void setProperty(String prop, List<KernelData> param, TNode tn, CompilerError ce) {
    raiseUnknownIdentifierPropertyError(prop,tn,ce);
  }

  public void setPropertyWithIdentifier(String prop, List<KernelData> param, String ident, TNode tn, CompilerError ce) {
    raiseUnknownIdentifierPropertyError(prop,tn,ce);
  }
  
  public void setPropertyWithLiteral(String prop, List<KernelData> param, Literal array, TNode tn, CompilerError ce) {
    raiseUnknownLiteralPropertyError(prop,tn,ce);
  }

  public void setPropertyWithArrayRange(String prop, List<KernelData> param, Array array, TNode tn, CompilerError ce) {
    raiseUnknownArrayRangePropertyError(prop,tn,ce);
  } 

  public void setPropertyWithString(String prop, List<KernelData> param, String s, TNode tn, CompilerError ce) {
    if (param.size()!=0) {
      raiseParamPropertyError(prop,tn,ce);
    }
    if (prop.equals("function")) {
      setFunction(s,tn);
    }
    else {
      raiseUnknownStringPropertyError(prop,tn,ce);
    }
  }
  
  
  //==================================================================
  // Check
  //==================================================================

  protected boolean completeAndCheckNode(CompilerError ce) {
    boolean error=false;
    
    if (function==null) {
      ce.raiseError(getNameNode(), "missing function definition for Operator '"+getName()+"'");
      return true;
    }

    // This node can have any number of parameters (even 0 for a constant or random generator)
    
    // [TBW] Should be a statically sized type (no dynamic array) 
    // Only support arithmetic scalar today
    //if (!outputBaseCType.isArithmeticScalar()) {
    //  ce.raiseError(getNameNode(), "base output type should be a scalar arithmetic for Operator '"+getName()+"'");
    //}
    
    // Do not need to process the function if errors
    ce.exitIfError();

    // Process the function and determines iterative inputs
    if (processFunction(function,functionList,functionNode,ce)) {
      return true;
    }

    // If there is no iterative input, it may be a mistake
    if (getNbInputEdges()<1) {
      ce.raiseWarning(1,getNameNode(),getName()+": no iterative input");
    }
    
    // Node type already set by the function processing
 
    // Set access patterns
    for(DataEdge de:getInputEdgeList()) {     
      // Input
      de.setReadPattern(new MatrixIndexes(MatrixIndexes.MatrixSingleElementCentered));
      setInputStrideNoCheck(new MatrixSize(MatrixSize.MatrixSingleElement));
      // Output
      setWritePattern(new MatrixIndexes(MatrixIndexes.MatrixSingleElementCentered));
      setOutputStrideNoCheck(new MatrixSize(MatrixSize.MatrixSingleElement));
    }   
    
    // [TBW] Iterative inputs should be a statically sized type (no dynamic array) 
    // Sanity check on the input type of iterative inputs (only supports scalar today)
    //for (int i=0;i<getNbIterativeInputs();i++) {
    //  KernelData kd=getIterativeInput(i);
    //  if (!kd.getMatrixType().getBaseCType().isArithmeticScalar()) {
    //    ce.raiseError(kd.getNameNode(), "base output type should be a scalar arithmetic for input '"+kd.getName()+"' of Operator '"+getName()+"'");
    //  }
    //}
    
    return error;
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
  

  // Process the string function and compute the input read pattern
  protected boolean processFunction(final String s, List<Object> fList, TNode fNode, CompilerError ce) {
    boolean error=false;
    // Map of inputs
    // Set of iterative inputs
    Set<KernelData> iterationSet=new HashSet<KernelData>();
    resetRelativeLine();

    int i,i1,i2;
    int j=0;
    
    i1=s.indexOf("$"); i2=s.indexOf("@");
    if (i1<0) {i=i2;} else { if (i2<0) {i=i1;} else { if (i1<i2) {i=i1;} else {i=i2;} } }

    MatrixIndexes iterationSpace=null;
    
    while (i>=0) {
      // Get the string before
      String subS=s.substring(j, i);
      fList.add(subS);
      updateRelativeLine(subS);

      // Read or write ?
      boolean isReadMarker=s.charAt(i) == '$' ;

      // Take the end of the input reference
      j = getEndOfReference(s,i+1,fNode,ce);
      final String variableAccessReferenceFull = s.substring(i + 1, j);
      final String variableAccessReference = variableAccessReferenceFull.trim();

      // Get the input name
      int a = variableAccessReference.indexOf("[");
      String name;
      if (a<0) {
        name=variableAccessReference;
      }
      else {
        name=variableAccessReference.substring(0, a);
        ce.raiseError(fNode, getRelativeLine(), "variable '"+name+"' should not be referenced as an array element in the function of Operator '"+getName()+"'");
      }
      KernelData kdReference = getInputData(name);
      if (kdReference == null) {
        // It may be the output
        if (name.equals(getName())) {
          if (isReadMarker) {
            ce.raiseError(fNode, getRelativeLine(), "read of output '"+name+"' is forbidden");  
            error=true;
          }
          kdReference=this;
        }
        else {
          // Error, unknown variable
          ce.raiseError(fNode, getRelativeLine(), "unknown  reference '"+name+"' in the function of Operator '"+getName()+"'");
          error=true;
        }
      }
      
      else {
        // It is an input (that becomes an iterative input)
        // Inputs can only be read
        if (!isReadMarker) {
          ce.raiseError(fNode, getRelativeLine(), "write of node input '"+name+"' is forbidden");  
          error=true;
        }

        if (iterationSet.size()==0) {
          iterationSet.add(kdReference);
          addInputEdge(kdReference);
          iterationSpace=kdReference.getMatrixType();
        }
        else if (!iterationSet.contains(kdReference)) {
          // Checks that types are compatibles
          if (!iterationSpace.isSame(kdReference.getMatrixType())) {
            ce.raiseError(fNode, getRelativeLine(), "iterating space for input variables is not compatible for Operator '"+getName()+"'");
            System.err.println("A1:"+iterationSpace.toString());
            System.err.println("A2:"+kdReference.getMatrixType().toString());

          }
          iterationSet.add(kdReference);
          addInputEdge(kdReference);
        }
        // Else nothing to do, already in
      }
      updateRelativeLine(variableAccessReferenceFull);

      // Add the index object
      fList.add(kdReference);

      // Next
      i1=s.indexOf("$",j);i2=s.indexOf("@",j);
      if (i1<0) {i=i2;} else { if (i2<0) {i=i1;} else { if (i1<i2) {i=i1;} else {i=i2;} } }
    }
    // Get the Last string
    fList.add(s.substring(j, s.length()));
    
    
    return error;
  }

  
  int getEndOfReference(String s, int start, TNode fNode, CompilerError ce) {
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
        ce.raiseError(fNode,"while parsing the function of Filter '"+getName()+"' (missing ']')");
        return size;
      }
      for (i++;(i<size)&&Character.isWhitespace(s.charAt(i));i++);
    }
    
    return i;
  }


  //========================================================
  // Code generation
  //========================================================
  
  public void generateConstLiterals(PrintStream ps) {
    // Nothing
  }

  // Overrides the FunctionNode method
  public void generateImageComputeFunctionBody(PrintStream ps) {
    // OpenCL variables declarations
    CLGenVarNames.generateLocalInfoVarDeclaration(0,ps,"  ");
    ps.println();

    // main loop
    ps.print("  for (int ");
    CLGenVarNames.generateComputeLoopBlockCounter(ps);
    ps.print("=");
    ps.print("-");
    CLGenVarNames.generateMyWGSliceLeftExtentBlockUnit(this,0,ps);
    ps.print("+");
    ps.print(CLGenVarNames.getLocalIdVarName(0));
    ps.print(";");
    CLGenVarNames.generateComputeLoopBlockCounter(ps);
    ps.print("<");
    CLGenVarNames.generateMyWGSliceSizeBlockUnit(this,0, ps);
    ps.print("+");
    CLGenVarNames.generateMyWGSliceRightExtentBlockUnit(this,0,ps);
    ps.print(";");
    CLGenVarNames.generateComputeLoopBlockCounter(ps);
    ps.print("+=");
    ps.print(CLGenVarNames.getLocalSizeVarName(0));
    ps.println(") {");
    
    // Expression
    generateFunctionList(new HashSet<KernelData>(), ps);
    
    // End of loop
    ps.println();
    ps.println("  }");
  }
  
  protected void generateNodeComputeFunction(
      List<Integer> firstIndexList,
      List<Integer> lastIndexList,
      List<String> lastIndexStringPlusOneList, 
      Set<KernelData> internalData,
      PrintStream ps,
      String prefix) {
    ps.print(prefix);
    generateFunctionList(internalData,ps);
    ps.println();
  }
 
  
  void generateFunctionList(Set<KernelData> internalData, PrintStream ps) {
    // The function node may be null for predefined nodes
    if (functionNode!=null) {
      functionNode.generatePreproDirective(ps);
    }
    for(Object o:functionList) {
      if (o instanceof KernelData) {
        ps.print(((KernelData)o).getName());
        if (!internalData.contains(o)) {
          ps.print("[0][");
          CLGenVarNames.generateComputeLoopBlockCounter(ps);
          ps.print("]");
        }
      }
      else {
        ps.print(o);
      }
    }
  }
  
//  void generateComputeFunctionAssign(PrintStream ps) {
//    ps.print("    " + getName() + "[0][_kg_i] = ");
//  }

  public void generateRuntimeFunctions(PrintStream ps) { }
  
  //==================================================================
  // Verbose
  //==================================================================
  
   public String toString() {
     StringBuffer sb = new StringBuffer();
      sb.append("Operator ")
       .append(super.toString())
       .append("; function '").append(function).append("'");
      return sb.toString();
   }
}
