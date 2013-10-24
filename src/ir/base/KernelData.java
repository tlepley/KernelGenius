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

/* Data of a Kernel (transient, input or output) */

package ir.base;

import java.util.LinkedList;
import java.util.List;

import codegen.CodegenDataPattern;

import parser.TNode;
import ir.literals.Literal;
import ir.types.Type;
import ir.types.kg.MatrixSize;
import ir.types.kg.MatrixIndexes;
import ir.types.kg.TypedMatrixIndexes;

public class KernelData extends IRElement {  
  public boolean flag=false;
    
  // General informations directly derived from the source code
  //-----------------------------------------------------------
  private String name=null;
  private TNode nameNode = null;
  private Literal initializer=null;

  // Information related to computational (array) data
  //---------------------------------------------------
  
  // COntrol or iterative (matrix) data ?
  private boolean isIterative=false;
  
  // Dataset information
  private TNode baseCTypeNode=null;
  private TypedMatrixIndexes matrixType=null;
  
  // Position in the kernel
  boolean isKernelInput=false;
  boolean isKernelOutput=false;
  
  // Border
  private MatrixIndexes outputSkipPattern=new MatrixIndexes(1);
  
  // Abstract Graph model information
  private List<DataEdge> userEdgeList=new LinkedList<DataEdge>();

  
  //========================================================
  // Tile analysis
  //========================================================
  //*** Temporary data ***
  private MatrixSize tileGrainForSuccessors=null; 
  
  //*** Final data ***
  //  Tile grain
  private MatrixSize tileGrainForGraph=null; 
  //  Node execution rate in all dimensions
  private MatrixSize tileExecutionRateForGraph=null; 
  //  Additional nbh for successors (multiple of the node
  // block size)
  private MatrixIndexes tileNbhForSuccessors=null; 

  // Minimal pattern required to serve readers and writer
  private MatrixIndexes accessPatternUnion=null;
  
  // Data pattern for the code generation
  private CodegenDataPattern codegenDataAccess=new CodegenDataPattern();

  
  //========================================================
  // Connectivity modification
  //========================================================
  
  protected void removeUserEdge(DataEdge de) {
    userEdgeList.remove(de);
  }
  
  protected void removeUserEdges() {
    // Inputs
    for(DataEdge de:userEdgeList) {
      de.getTargetFunctionNode().removeInputEdge(de);
    }
    userEdgeList.clear();  
  }
  
  //========================================================
  // Building (General)
  //========================================================

  // Constructors
  public KernelData() {}

  public KernelData(String s, Type t, TNode tn, TNode bn) {
    setName(s);
    nameNode=tn;
    setBaseCTypeNode(bn);
    setType(t);
  }

  // For algorithms
  protected void setName(String s, TNode tn) {
    name=s;
    nameNode=tn;
  }

  // Build function
  protected void setType(Type t) {    
    // Computational data infos
    setMatrixType(new TypedMatrixIndexes(t));
    // The access pattern is for the moment one single element
    // (corresponding to a simple data copy)
    accessPatternUnion=new TypedMatrixIndexes(getBaseCType());
  }
  
  protected void setMatrixType(TypedMatrixIndexes t) {    
    // Computational data infos
    matrixType=t;
    // The access pattern is for the moment one single element
    // (corresponding to a simple data copy)
    accessPatternUnion=new TypedMatrixIndexes(getBaseCType());
  }
  
  public void setInitializer(Literal l) {
    initializer=l;
  }

  protected void addOutputSkipPattern(MatrixIndexes m) {
    getOutputSkipPattern().union(m);
  }
    
  public TNode getBaseCTypeNode() {
    return baseCTypeNode;
  }

  public void setBaseCTypeNode(TNode baseCTypeNode) {
    this.baseCTypeNode = baseCTypeNode;
  }

  public void setOutputSkipPattern(MatrixIndexes outputSkipPattern) {
    this.outputSkipPattern = outputSkipPattern;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setTileGrainForGraph(MatrixSize tileGrainForGraph) {
    this.tileGrainForGraph = tileGrainForGraph;
  }

  public void setTileGrainForSuccessors(MatrixSize tileGrainForSuccessors) {
    this.tileGrainForSuccessors = tileGrainForSuccessors;
  }

  public void setTileExecutionRateForGraph(MatrixSize tileExecutionRateForGraph) {
    this.tileExecutionRateForGraph = tileExecutionRateForGraph;
  }

  public void setTileNbhForSuccessors(MatrixIndexes tileNbhForSuccessors) {
    this.tileNbhForSuccessors = tileNbhForSuccessors;
  }

  
  
  //========================================================
  // Building (Computational data)
  //========================================================

  // Data family
  public void setIterative() {
    isIterative=true;
  }

  protected void addUserEdge(DataEdge a) {
    userEdgeList.add(a);
  }
  
  public void setAsKernelInput() {
    isKernelInput=true;
  }

  public void setAsKernelOutput() {
    isKernelOutput=true;
  }
 
  //========================================================
  // Getters (General)
  //========================================================
  
  public MatrixIndexes getOutputSkipPattern() {
      return outputSkipPattern;
  }

  // Data family which allows understanding how each variable needs
  // to be handled in the generated code
  public boolean isIterative() {
    return getMatrixType().isRealArray() && isIterative;
  }
  public boolean isNonIterativeDataToCache() {
    // For the moment, only arrays are data to cache since all other data 
    // are passed by value in the private address space
    return getMatrixType().isRealArray() && (!isIterative);
  }
  public boolean shouldBeCached() {
    // For the moment, only arrays are data to cache since all other data 
    // are passed by value in the private address space
    return getMatrixType().isRealArray();
  }
  public boolean isControlData() {
    // Types other than arrays
//    return !matrixType.isRealArray();
    return !getMatrixType().isRealArray() || !isIterative();
  }
  
  
  public String getName() {
    return name;
  }
  
  public TNode getNameNode() {
    return nameNode;
  }

  public Type getBaseCType() {
    return getMatrixType().getBaseCType();
  }

  public Literal getInitializer() {
    return initializer;
  }

  
  //========================================================
  // Getters (Computational data)
  //========================================================

  public TypedMatrixIndexes getMatrixType() {
    return matrixType;
  }
 
  // Position in the graph
  public boolean isKernelInputData() {
    return isKernelInput;
  }
  public boolean isKernelOutputData() {
    return isKernelOutput;
  }
  public boolean isKernelIOData() {
    return isKernelInput || isKernelOutput;
  }
  public boolean isTransientdata() {
    return ! (isKernelInput || isKernelOutput);
  }

  // User edges
  public int getNbUserEdges() {
    return userEdgeList.size();
  }
  public DataEdge getUserEdge(int i) {
    return userEdgeList.get(i);
  }
  public List<DataEdge> getUserEdgeList() {
    return userEdgeList;
  }
 
  // Intrinsic data pattern
  public MatrixIndexes getAccessPatternUnion() {
    return accessPatternUnion;
  }

  // Codegen data pattern
  public CodegenDataPattern getCodegenDataPattern() {
    return codegenDataAccess;
  }
 
  
  //========================================================
  // Graph lowering
  //========================================================
  public int getPush() {
    //return push;
    return 1;
  }
  public int getRate() {
    //return rate;
    return getTileExecutionRateForGraph().getNbElements(1);
  }


  //========================================================
  // Analysis
  //========================================================

  public MatrixSize getTileGrainForSuccessors() {
    return tileGrainForSuccessors;
  }
  
  public MatrixSize getTileGrainForGraph() {
    return tileGrainForGraph;
  }
  
  public MatrixSize getTileExecutionRateForGraph() {
    return tileExecutionRateForGraph;
  }
  
  // For KernelData that is not a function node
  protected void computeTileGrainForSuccessors() {
    // Output stride = [1x1]
    setTileGrainForSuccessors(new MatrixSize(MatrixSize.MatrixSingleElement));
    for(DataEdge de:getUserEdgeList()) {
      getTileGrainForSuccessors().lcm(de.getTileInGrainConstraint());
    }
  }
  
  // For KernelData that is not a function node
  protected void computeNbhForSuccessors() {
    // Start with [0:0, 0:0] matrix
    setTileNbhForSuccessors(new MatrixIndexes(MatrixIndexes.MatrixSingleElementCentered));
    for(DataEdge de:getUserEdgeList()) {
      getTileNbhForSuccessors().union(de.getTileInNbhConstraint());
    }
    // OLD: Output stride = [1x1], nothing more to do
    // Multiple of the grain size
    getTileNbhForSuccessors().inflateToMultipleOf(getTileGrainForGraph());
  }

  // For KernelData that is not a function node
  protected void computeTileGrainForGraph() {
    setTileGrainForGraph(getTileGrainForSuccessors());
    setTileExecutionRateForGraph(new MatrixSize(getTileGrainForGraph()));
    
    // Recursive
    for(DataEdge de:getUserEdgeList()) {
      de.getTargetFunctionNode().computeTileGrainForGraph(de);
    }    
  }

  public MatrixIndexes getTileNbhForSuccessors() {
    return tileNbhForSuccessors;
  }
  
  protected MatrixIndexes computeNbNbhGrainsForGraph() {
    // Start with [0:0, 0:0] matrix
    return new MatrixIndexes(getTileNbhForSuccessors()).devideRoundTowardsAbsolute(getTileGrainForGraph());
  } 
           
  //  Manage 2D data only for the moment
  void computeBufferSlots() {
    //==================================================
    // NEW allocation
    //==================================================
    int tokenBuf=0;
    for(DataEdge de:userEdgeList) {
      FunctionNode target=de.getTargetFunctionNode();
      int Ca=getCodegenDataPattern().getSchedulingCycle();
      int Cb=target.getCodegenDataPattern().getSchedulingCycle();
//      int buf=getPush()*getRate()*(Cb-Ca+1)-de.getPeekMin();      
      int buf=getTileGrainForGraph().getNbElements(1)*(Cb-Ca+1)
              -de.getReadPattern().getFirstIndex(1);
      tokenBuf=max(tokenBuf,buf);
    }
    if (this.isKernelOutput) {
      //int Zc=getPush()*getRate()*2;
      int Zc=getTileGrainForGraph().getNbElements(1)*2;
      tokenBuf=max(tokenBuf,Zc);
    }
    
    // We got the number of slots
    getCodegenDataPattern().setNbBufferSlot(tokenBuf);
  }

  int max(int i, int j) {
    return j>i?j:i;
  }
  
 
  public int computeMaxLocalBufferSize() {   
    if (isIterative()) {
      int slot=getCodegenDataPattern().getNbBufferSlot();
      // Size in bytes
      int elementSize=getBaseCType().sizeof();
      int lineSize=getMatrixType().getMaxNbElement(0);
      if (lineSize<0) {
        // Unlimited
        return -1;
      }
      return elementSize*lineSize*slot;
    }
    else {
      return 0;
    }
  }

  
  
  //========================================================
  // Verbose
  //========================================================
  
  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("name '")
    .append(getName())
    .append("'");
    if (isKernelInputData()) {
      sb.append("; kernel input");
    }
    if (isKernelOutputData()) {
      sb.append("; kernel output");
    }
    if (!isKernelInputData()&&isKernelOutputData()) {
      sb.append("; temporary kernel data");
    }
    sb.append("; type=")
    .append(getMatrixType()!=null?getMatrixType().toString():"<not computed>");   
    return sb.toString();
  }



}
