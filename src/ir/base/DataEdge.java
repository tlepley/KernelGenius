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

/* Edges of the abstract graph model */

package ir.base;

import ir.types.kg.MatrixSize;
import ir.types.kg.MatrixIndexes;

public class DataEdge {
  KernelData sourceData=null;

  //========== Edge target information ==========
  FunctionNode targetFunctionNode=null;
  
  //------- Abstract model information --------
  // Read pattern bounds
  MatrixIndexes readPattern = null;
  // For determining the scheduling shift compared to predecessors in the graph
  MatrixIndexes minReadPattern = null;
  // Border semantics
  BorderMode border=null;
  
  //------- Scheduling graph information --------
  int dep=-1;

 

  //========================================================
  // Build
  //========================================================
  DataEdge(KernelData src, FunctionNode dest) {
    sourceData=src;
    targetFunctionNode=dest;
  }
 
  DataEdge(DataEdge de, KernelData source) {
    sourceData=source;
    targetFunctionNode=de.targetFunctionNode;
    readPattern=de.readPattern;
    minReadPattern=de.minReadPattern;
    border=de.border;
  }

  //========================================================
  // Setters
  //========================================================
  public void setReadPattern(MatrixIndexes m) {
    readPattern=m;
  }
  public void setDependency(int d) {
    dep=d;
  }
 
  public void setBorderMode(BorderMode bm) {
    border=bm;
  }

  //========================================================
  // Main edge information
  //========================================================
  public KernelData getSourceData() {
    return sourceData;
  }
  public FunctionNode getTargetFunctionNode() {
    return targetFunctionNode;
  }
  public MatrixIndexes getReadPattern() {
    return readPattern;
  }
  public MatrixIndexes getMinReadPattern() {
    return minReadPattern;
  } 
  

  //========================================================
  // Borders
  //========================================================
  public BorderMode getBorderMode() {
    return border;
  }
  public boolean hasBorderDefined() {
    return getBorderMode()!=null;
  }

  
  //========================================================
  // Lowered SDF information
  //========================================================
  public int getPeekMax() {
    return getReadPattern().getLastIndex(1);
  }
  public int getPeekMin() {
    return getReadPattern().getFirstIndex(1);
  }
  public int getPop() {
    return getTargetFunctionNode().getInputStridePattern().getNbElements(1);
  }
  
  //========================================================
  // Scheduling information
  //========================================================
  public int getDependency() {
    return dep;
  }
  
  //========================================================
  // Tiling analysis
  //========================================================
  public MatrixSize getTileInGrainConstraint() {
    // Returns ((Tile(target)/stride_out(target))*stride_in(target,src))
    MatrixSize mSucc=new MatrixSize(targetFunctionNode.getTileGrainForSuccessors());
    mSucc.devideDirectBy(targetFunctionNode.getOutputStridePattern());
    mSucc.multiplyDirectBy(targetFunctionNode.getInputStridePattern());
    return mSucc;
  }
    
  public MatrixIndexes getTileInNbhConstraint() {
    // Nbh computed from read access and input stride
    MatrixIndexes nbh=new MatrixIndexes(getReadPattern()).sub(targetFunctionNode.getInputStridePattern());
    return new MatrixIndexes(targetFunctionNode.getTileNbhForSuccessors())
      .devide(targetFunctionNode.getOutputStridePattern())
      .multiply(targetFunctionNode.getInputStridePattern())
      .add(nbh);
  }
  
}
