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

/* KernelGenius kernel construct */

package ir.base;


import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import codegen.OpenCL.CLGenKernelData;

import common.CompilerError;
import driver.options.GeneralOptions;

import parser.TNode;


public class Kernel extends IRElement {

  private String name = null;
  private TNode nameNode = null;

  // All type of data
  private final List<KernelData> parameterList = new LinkedList<KernelData>();
  private final List<KernelData> controlParameterList = new LinkedList<KernelData>();

  // Computational data (array input,intermediate or output data )
  private final List<KernelData> computationalDataList = new LinkedList<KernelData>();  // All
  
  private final List<KernelData> computationalInputList = new LinkedList<KernelData>(); // kernel input
  private final List<FunctionNode> functionNodeList = new LinkedList<FunctionNode>();  // transient + outputs
  private final List<KernelData> computationalOutputList = new LinkedList<KernelData>(); // kernel outputs


  //========================================================
  // Building
  //========================================================

  public Kernel(String s, TNode tn) {
    name=s;
    nameNode=tn;
  }

  public void addParameter(KernelData kp) {
    getParameterList().add(kp);
  }

  public void addAlgorithm(FunctionNode a) {
    getFunctionNodeList().add(a);
  }

  public void addOutput(KernelData kd) {
    getComputationalOutputList().add(kd);
  }

  //==================================================================
  // Getters
  //==================================================================

  public String getName() {
    return name;
  }
  
  public TNode getNameNode() {
    return nameNode;
  }

  public int getNbComputationalData() {
    return getComputationalDataList().size();
  }

  public KernelData getComputationalData(int i) {
    return getComputationalDataList().get(i);
  }

  // Only data resulting from always require local buffering
  public int getNbTransientData() {
    return getFunctionNodeList().size();
  }

  public KernelData getTransientData(int i) {
    return getFunctionNodeList().get(i);
  }

  public int getNbFunctionNodes() {
    return getFunctionNodeList().size();
  }

  public FunctionNode getFunctionNode(int i) {
    return getFunctionNodeList().get(i);
  }
  
  public int getNbOutputs() {
    return getComputationalOutputList().size();
  }

  public KernelData getOutput(int i) {
    return getComputationalOutputList().get(i);
  }

  
  //==================================================================
  // Checking 
  //==================================================================
  
  // Check for limitations
  public void completeAndCheck(CompilerError ce) {
    // Node completion
    for (FunctionNode fn: getFunctionNodeList()) {
      fn.completeAndCheck(ce);
      // The IR may partially be created, better to stop here
      ce.exitIfError();
    }
    
    // Split parameters into:
    //  - what is computational (passed by pointer to the Algorithmic node)
    //  - what is control data  (passed by value to the Algorithmic node)
    for(KernelData kd:getParameterList()) {
      // TODO: differentiate Iterative from non iterative data that are passed by pointer
      if (kd.isControlData()) {
        // Data passed by value
        getControlParameterList().add(kd);
      }
      else {
        // Data passed by reference (Arrays)
        getComputationalInputList().add(kd);
      }
    }
    // Fill  dataList (input first and then function nodes in the order they where declared in the input)
    getComputationalDataList().addAll(getComputationalInputList());
    getComputationalDataList().addAll(getFunctionNodeList());
    
    
    // We should have at least one output
    if (getComputationalOutputList().size()==0) {
      ce.raiseError(getNameNode(),"Kernel '"+getName()+"' does not define any output");
    }
  }

  
  //==================================================================
  // Optimization 
  //==================================================================
  
  public void optimize(CompilerError ce) {
    if (GeneralOptions.getOptimizationLevel()>0) {
      // Merge simple sub graphs into a single node
      mergeNodes(ce);
    }
  }

  void mergeNodes(CompilerError ce) {
    List<MacroFunctionNode> macroNodeList=new LinkedList<MacroFunctionNode>();
    Set<FunctionNode> processedNodeSet=new HashSet<FunctionNode>();
    
    for (int i=functionNodeList.size()-1;i>=0;i--) {
      FunctionNode fn=functionNodeList.get(i);
      if (!processedNodeSet.contains(fn)) {
        MacroFunctionNode mfn=new MacroFunctionNode();
        mfn.build(fn);
        processedNodeSet.add(fn);
        processedNodeSet.addAll(mfn.getFunctionNodeList());
        if (mfn.getFunctionNodeList().size()>1) {
          macroNodeList.add(mfn);
        }
      }
    }

    // Complete the creation and insert the macro-nodes into the graph
    for (MacroFunctionNode m:macroNodeList) {
      m.completeAndCheck(ce);
    }

    // Remove sub-nodes from the kernel node list
    for (MacroFunctionNode m:macroNodeList) {
      int i;

      i=functionNodeList.indexOf(m.getHead());
      functionNodeList.add(i,m);

      i=computationalDataList.indexOf(m.getHead());
      computationalDataList.add(i,m);

      // If never the head is a kernel output
      i=computationalOutputList.indexOf(m.getHead());
      if (i>=0) {
        computationalOutputList.add(i,m);
      }

      for(FunctionNode fn:m.getFunctionNodeList()) {
        removeNode(fn);
      }     
    }

    // Verbose
    if (GeneralOptions.getDebugLevel()>0) {
      if (macroNodeList.size()==0) {
        ce.raiseMessage("No nodes merged as a macro-node");
      }
      else {
        ce.raiseMessage("  ... Merged macro-nodes (-O1) :");
        for (MacroFunctionNode m:macroNodeList) {
          m.print("      ",ce);
        }
      }
    }
  }

  void removeNode(FunctionNode fn) {
    // Remove from node lists
    functionNodeList.remove(fn);
    computationalDataList.remove(fn);
    computationalOutputList.remove(fn);
  }

  

  //==================================================================
  // Analysis 
  //==================================================================

  public void analyze(CompilerError ce) {
    // Tiling analysis
    tileAnalysis();

    // Compute the scheduling information
    computeScheduling();
    
    // Compute the Buffering information
    computeBufferSlots();
    
    // (For code generation)
    computeAccessPatternUnion();  
  }
   
  public void tileAnalysis() {   
    // 1- Compute the minimum tile granularity for each node that is deduced by
    //    the 'stride' information and successors
    // 2- Compute the Nbh that is deduced by the 'stride' and the
    //    'read access pattern' information
    // 3- Compute the graph tile granularity that provides the actual minimal
    //    tile size of an execution step to ensure all nodes in the graph
    //    have input and output tiles as that fit their input and output stride
    // 4- Compute the 'execution rate'

    Set<KernelData> to_schedule=new HashSet<KernelData>();
    Set<KernelData> schedulable=new HashSet<KernelData>();
    Set<KernelData> scheduled=new HashSet<KernelData>();

    // ------------------------
    // Backward graph analysis
    // ------------------------    
    
    // Pick all nodes without successors    
    for(KernelData kd:getComputationalDataList()) {
      if (kd.getNbUserEdges()==0) {
        schedulable.add(kd);
      }
      else {
        to_schedule.add(kd);
      }
    }
    
    // Iterate over non computed nodes
    while (!schedulable.isEmpty()) {
      // Schedule schedulable
      for(KernelData kd:schedulable) {
        kd.computeTileGrainForSuccessors();
      }
      scheduled.addAll(schedulable);

      // Compute next schedulable
      schedulable.clear();
      for(KernelData kd:to_schedule) {
        boolean flag=true;
        for(DataEdge de:kd.getUserEdgeList()) {
          if (!scheduled.contains(de.getTargetFunctionNode())) {
            flag=false;
            break;
          }
        }
        if (flag) {
          schedulable.add(kd);
        }
      }   
      to_schedule.removeAll(schedulable);
    }
    
  
    // ------------------------
    // Forward graph analysis
    // ------------------------    

    // Pick all nodes without successors
    for(KernelData kd:getComputationalInputList()) {
      kd.computeTileGrainForGraph();
    }
    // Recursive graph traversal
    for(FunctionNode fn:getFunctionNodeList()) {
      if (fn.getNbInputEdges()==0) {
        fn.computeTileGrainForGraph(null);
      }
    }

    
    // ------------------------
    // Backward graph analysis
    // ------------------------    
    to_schedule.clear();
    schedulable.clear();
    scheduled.clear();
    
    // Pick all nodes without successors
    for(KernelData kd:getComputationalDataList()) {
      if (kd.getNbUserEdges()==0) {
        schedulable.add(kd);
      }
      else {
        to_schedule.add(kd);
      }
    }
    
    // Iterate over non computed nodes
    while (!schedulable.isEmpty()) {
      // Schedule schedulable
      for(KernelData kd:schedulable) {
        kd.computeNbhForSuccessors();        
      }
      scheduled.addAll(schedulable);

      // Compute next schedulable
      schedulable.clear();
      for(KernelData kd:to_schedule) {
        boolean flag=true;
        for(DataEdge de:kd.getUserEdgeList()) {
          if (!scheduled.contains(de.getTargetFunctionNode())) {
            flag=false;
            break;
          }
        }
        if (flag) {
          schedulable.add(kd);
        }
      }   
      to_schedule.removeAll(schedulable);
    }    
  }
    

  void computeAccessPatternUnion() {
    for(FunctionNode a:getFunctionNodeList()) {
      a.applyAccessPatternUnion();
    }
  }
  
  // Scheduling information for Kernel Data
  //***********************************************************************************
  // IMPORTANT: This simple scheduling is not the most efficient for general cases.
  //            in order to limit the computed buffer size, we need to schedule like 
  //            in high level synthesis
  //            -> look at force scheduling
  //***********************************************************************************
  public void computeScheduling() {
    Set<FunctionNode> to_schedule=new HashSet<FunctionNode>();
    to_schedule.addAll(getFunctionNodeList());

    // Initialization
    for(KernelData kd:getComputationalInputList()) {
        kd.getCodegenDataPattern().setSchedulingCycle(0);
    }
    
    Set<FunctionNode> schedulable=new HashSet<FunctionNode>();

    while (!to_schedule.isEmpty()) {
      // Compute schedulable nodes
      schedulable.clear();
      for(FunctionNode fn:to_schedule) {
        boolean ok=true;
        for(DataEdge de:fn.inputEdgeList) { 
          if (!de.getSourceData().getCodegenDataPattern().isScheduled()) {
            ok=false;
            break;
          }
        }
        if (ok) {
          schedulable.add(fn);
        }
      }
        
      // Schedule schedulable nodes
      for(FunctionNode fn:schedulable) {
        to_schedule.remove(fn);
        fn.computeSchedulingForward();
      }
    } 
  }
  
  // Take scheduling infos to compute buffer requirements
  public void computeBufferSlots() {
    for(KernelData kd:getComputationalDataList()) {
      kd.computeBufferSlots();
    }
  }
 
  
  //==================================================================
  // Verbose
  //==================================================================


  public void generateTilingReport() {
    CompilerError.GLOBAL.raiseMessage("** kernel '"+getName()+"'");
    for(KernelData kd:getComputationalDataList()) {
      CompilerError.GLOBAL.raiseMessage("  - Node "+kd.getName()+" :");
      CompilerError.GLOBAL.raiseMessage("     TileGrain = "+kd.getTileGrainForGraph());
      CompilerError.GLOBAL.raiseMessage("     ExecutionRate = "+kd.getTileExecutionRateForGraph());
      CompilerError.GLOBAL.raiseMessage("     NbhForSuccessor (TileGrain) = "+kd.getTileNbhForSuccessors());
    }
  }

  public void generateReportImageMode(PrintStream ps) {
    ps.print("Local memory usage for kernel '");
    ps.print(getName());
    ps.println("'");
    int total_size=0;
    for(KernelData kd:getComputationalDataList()) {
      ps.print("  ");
      ps.print(kd.getName());
      ps.print(": ");
      int size=kd.computeMaxLocalBufferSize();
      if (size<0) {
        ps.print("max is infinite");
      }
      else {
        ps.print("max = ");
        ps.print(((float)size)/1024);
        ps.print(" KB");
      }
      ps.print(" [ ");
      CLGenKernelData.generateLocalBufferSizeFormula(kd, ps);
      ps.println(" ]");
      if (size<0) {
        total_size=-1;
      }
      else {
        if (total_size>=0) {
          total_size+=size;
        }
      }
    }
    ps.print("  ");
    ps.print("TOTAL: ");
    if (total_size<0) {
      ps.print("max is infinite");
    }
    else {
      ps.print("max = ");
      ps.print(((float)total_size)/1024);
      ps.print(" KB");
    }   
    ps.println();
  } 


  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Kernel '").append(getName()).append("' :\n");

    // Inputs
    sb.append("  Control input parameters:\n");
    for (KernelData kd:getControlParameterList()) {
      sb.append("   -").append(kd.toString()).append("\n");
    }
    sb.append("\n");
    sb.append("  Computational input parameters:\n");
    for (KernelData kd:getComputationalInputList()) {
      sb.append("   -").append(kd.toString()).append("\n");
    }
    sb.append("\n");
    sb.append("  Algo generated data:\n");
    for (FunctionNode a:getFunctionNodeList()) {
      sb.append("   -").append(a.toString()).append("\n");
    }
    if (getNbOutputs()==1) {
      sb.append("  Output = ");
    }
    else {
      sb.append("  Outputs = ");
    }
    int i=0;
    for (KernelData kd:getComputationalOutputList()) {
      if (i++ != 0) sb.append(", ");
      sb.append(kd.getName());
      if (kd.isKernelInputData()) {
        sb.append(" (kernel param)");
      }
      else {
        sb.append(" (algo)");
      }
    }

    return sb.toString();
  }

  public List<KernelData> getParameterList() {
    return parameterList;
  }

  public List<KernelData> getControlParameterList() {
    return controlParameterList;
  }

  public List<KernelData> getComputationalDataList() {
    return computationalDataList;
  }

  public List<KernelData> getComputationalInputList() {
    return computationalInputList;
  }

  public List<FunctionNode> getFunctionNodeList() {
    return functionNodeList;
  }

  public List<KernelData> getComputationalOutputList() {
    return computationalOutputList;
  }

}
