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

/* Node structure that abstracts a merged sub-graph */

package ir.base;

import ir.literals.Literal;
import ir.types.c.Array;
import ir.types.kg.MatrixIndexes;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import codegen.OpenCL.CLGenKernelData;

import parser.TNode;

import common.CompilerError;

public class MacroFunctionNode extends FunctionNode {
  Set<FunctionNode> nodeSet=new HashSet<FunctionNode>();
  LinkedList<FunctionNode> nodeList=new LinkedList<FunctionNode>();
  FunctionNode head=null;
  
  //==================================================================
  // Building functions
  //==================================================================
  
  public MacroFunctionNode() {
    // Nothing special
  }
  
  public void build(FunctionNode fnHead) {
    HashMap<KernelData, DataEdge> edgeExtList=new HashMap<KernelData, DataEdge>();
    
    // Look at output: 1x1 output stride only for the moment
    if (!fnHead.getOutputStridePattern().hasSingleElement()) {
      return;
    }
    // For the moment, for sake of simplicity, limit the input stride of admitted nodes to 1
    // In this case, there is no need to check for border mode compatibility
    if (!fnHead.getInputStridePattern().hasSingleElement()) {
      return; 
    }
    
    Set<FunctionNode> admitted=new HashSet<FunctionNode>();
    
    addNode(fnHead);
    admitted.add(fnHead);

    while(admitted.size()>0) {
      admitted.clear();

      for (FunctionNode fn:nodeList) {
        for (DataEdge de:fn.getInputEdgeList()) {
          KernelData kdIn=de.getSourceData();
          if (kdIn instanceof FunctionNode) {
            FunctionNode fnIn=(FunctionNode)kdIn;
            if (!nodeSet.contains(fnIn)) {
              boolean internalNode=true;
              for (DataEdge deOut:fnIn.getUserEdgeList()) {
                if (!nodeSet.contains(deOut.getTargetFunctionNode()) ||
                    !deOut.getReadPattern().hasSingleElement()) {
                  internalNode=false;
                  break;
                }
              }   
              if (internalNode) {
                if (fnIn.getOutputStridePattern().hasSingleElement() && 
                    fnIn.getInputStridePattern().hasSingleElement()) {
                  boolean admissible=true;
                  for (DataEdge deInIn:fnIn.getInputEdgeList()) {
                    KernelData kdInIn=deInIn.getSourceData();
                    if (edgeExtList.containsKey(kdInIn)) {
                      DataEdge deRef=edgeExtList.get(kdInIn);
                      if (deInIn.getReadPattern().hasSingleElement()) {
                        // OK, compatible, keep the existing edge
                      }
                      else {
                        if (deRef.getReadPattern().hasSingleElement() ) {
                          // OK, compatible, exchange edges
                          edgeExtList.put(kdInIn,new DataEdge(deInIn,this));
                        }
                        else {
                          // Check border mode compatibility
                          if (deRef.getBorderMode().isSame(deInIn.getBorderMode())) {
                            // OK, compatible, simply merge the read pattern
                            deRef.getReadPattern().union(deInIn.getReadPattern());
                          }
                          else {
                            admissible=false;
                            break;
                          }
                        }
                      }                       
                    }
                    else {
                      edgeExtList.put(kdInIn,new DataEdge(deInIn,this));
                    }
                  }
                  if (admissible) {
                    admitted.add(fnIn);
                  }
                }
              }
            }
          }
        }
      }

      // Add admitted nodes to the macro-node
      for (FunctionNode fn:admitted) {
        addNode(fn);
      }
    }

  }

 
  void addNode(FunctionNode fn) {
    nodeSet.add(fn);
    nodeList.addFirst(fn);
  }
  
  @Override
  protected boolean completeAndCheckNode(CompilerError ce) {
    boolean error=false;
    LinkedHashSet<KernelData> inputData=new LinkedHashSet<KernelData>();

    //System.err.print("Nodes in the Macro-Node: ");
    //for(FunctionNode fn:nodeSet) {  
    //  System.err.print(" "+fn.getName());
    //}
    //System.err.println();

    //** Iterative inputs
    for(FunctionNode fn:nodeSet) {     
      // Manage iterative inputs
      for(DataEdge deIn:fn.getInputEdgeList()) {     
        KernelData kdIn=deIn.getSourceData();
        if (nodeSet.contains(kdIn)) {
          // Internal data, it is necessarily a function node. We need to check
          // the composition compatibility, for the moment:
          //  - 1x1 input stride
          //  - 1x1 read pattern
          //  - 1x1 output stride (for the predecessor)
          FunctionNode fnIn=(FunctionNode)kdIn;
          if (!fnIn.getOutputStridePattern().hasSingleElement()) {
            // Error
            ce.raiseInternalError(getNameNode(),"can not merge node '"+kdIn.getName()+"' with '"+
            fn.getName()+"', incorrect output stride");
            error=true;
          }
          else if (!deIn.getReadPattern().hasSingleElement()) {
            // Error
            ce.raiseInternalError(getNameNode(),"can not merge node '"+kdIn.getName()+"' with '"+
                fn.getName()+"', incorrect read pattern");
            error=true;
          }
          else if (!fn.getInputStridePattern().hasSingleElement()) {
            // Error
            ce.raiseInternalError(getNameNode(),"can not merge node '"+kdIn.getName()+"' with '"+
                fn.getName()+"', incorrect input stride");
            error=true;
          }
        }
        else {
          // External data
          DataEdge edgeExisting;
          if ((edgeExisting=getInputEdge(kdIn))!=null) {
            //-> Already the input of an other node of the macro-block
            boolean admissible=true;
            // Check compatibility of border management
            if (deIn.getReadPattern().hasSingleElement()) {
              // OK, compatible, keep the existing edge
            }
            else {
              if (edgeExisting.getReadPattern().hasSingleElement() ) {
                // OK, compatible, exchange edges informations
                edgeExisting.getReadPattern().union(deIn.getReadPattern());
                edgeExisting.setBorderMode(deIn.getBorderMode());
              }
              else {
                // Check border mode compatibility
                if (edgeExisting.getBorderMode().isSame(deIn.getBorderMode())) {
                  // OK, compatible, simply merge the read pattern
                  edgeExisting.getReadPattern().union(deIn.getReadPattern());
                }
                else {
                  admissible=false;
                  break;
                }
              }
            }                       
            if (!admissible) {
              // Error
              ce.raiseInternalError(getNameNode(),"can not merge node '"+
                  fn.getName()+"', inconsistent border mode on input '"+kdIn.getName()+"'");
              error=true;
            }
          }
          else {
            // Setup the macro-node input stride information
            setInputStrideNoCheck(fn.getInputStridePattern());
            // Create a macro-node input edge
            DataEdge newEdge=new DataEdge(kdIn,this);
            newEdge.setReadPattern(new MatrixIndexes(deIn.getReadPattern()));
            newEdge.setBorderMode(deIn.getBorderMode());
            addInputEdge(newEdge);
          }
          
          // Disconnect for the external predecessor
          kdIn.removeUserEdge(deIn);
        }
      } // for input edge

      //** Input data list (iterative and control) **
      for(KernelData kdIn:fn.getInputDataList()) {
        // Only external data become input of the macro-node
        if (!nodeSet.contains(kdIn)) {
          inputData.add(kdIn);
        }
      }

      //** Output **
      boolean out=false;
      if (fn.isKernelOutput) { out=true; }
      for(DataEdge deOut:fn.getUserEdgeList()) {     
        FunctionNode fnOut=deOut.getTargetFunctionNode();
        if (!nodeSet.contains(fnOut)) { out=true; }
      }
      if (out) {
        if (head!=null) {
          // More than 1 output
          ce.raiseInternalError(getNameNode(),"can not merge node '"+fn.getName()+"' with '"+
              head.getName()+"', more than one output");
          error=true;
        }
        else {
          // Setup the macro node information related to the output
          head=fn;
          setOutputStrideNoCheck(fn.getOutputStridePattern());
          setWritePattern(fn.getWritePattern());
          setOutputBaseCType(fn.getOutputBaseCType(), fn.getBaseCTypeNode());
          setMatrixType(fn.getMatrixType());
          setIterative();
          if (fn.isKernelOutputData()) setAsKernelOutput();

          // Set output edges
          for(DataEdge deOut:fn.getUserEdgeList()) {     
            FunctionNode fnOut=deOut.getTargetFunctionNode();
            if (!nodeSet.contains(fnOut)) {
              // Out of the macroNode
              DataEdge de=new DataEdge(deOut,this);
              fnOut.addInputEdge(de);
              // Disconnect the sub-node
              fnOut.removeInputEdge(deOut);
            }
          }
        }
      }
    } // for node
        
    // Add inputs parameters to the macro-node
    for (KernelData kd:inputData) {
      addInputData(kd);
    }
    
    // Take the name of the head node
    setName(head.getName());

    return error;
  }
  
  //==================================================================
  // Getters
  //==================================================================
  
  public List<FunctionNode> getFunctionNodeList() {
    return nodeList;
  }
  public FunctionNode getHead() {
    return head;
  }
 
  //==================================================================
  // Building function (useless for macro nodes)
  //==================================================================
 
  @Override
  public void setProperty(String prop, List<KernelData> param, TNode tn,
      CompilerError ce) {
    ce.raiseError(this.getClass().getSimpleName()+": setProperty");
  }

  @Override
  public void setPropertyWithIdentifier(String prop, List<KernelData> param,
      String ident, TNode tn, CompilerError ce) {
    ce.raiseError(this.getClass().getSimpleName()+": setPropertyWithIdentifier");
  }

  @Override
  public void setPropertyWithArrayRange(String prop, List<KernelData> param,
      Array array, TNode tn, CompilerError ce) {
    ce.raiseError(this.getClass().getSimpleName()+": setPropertyWithArrayRange");
  }

  @Override
  public void setPropertyWithLiteral(String prop, List<KernelData> param,
      Literal array, TNode tn, CompilerError ce) {
    ce.raiseError(this.getClass().getSimpleName()+": setPropertyWithLiteral");
  }

  @Override
  public void setPropertyWithString(String prop, List<KernelData> param,
      String s, TNode tn, CompilerError ce) {
    ce.raiseError(this.getClass().getSimpleName()+": setPropertyWithString");
  }

  @Override
  public void applyAccessPatternContraints() {
    CompilerError.GLOBAL.raiseError(this.getClass().getSimpleName()+": applyAccessPatternContraints");
  }

  
  // ==================================================================
  // Code generation
  // ==================================================================

  @Override
  public void generateConstLiterals(PrintStream ps) {
    for (FunctionNode fn:nodeList) {
      fn.generateConstLiterals(ps);
    }
  }

  @Override
  public void generateRuntimeFunctions(PrintStream ps) {
    for (FunctionNode fn:nodeList) {
      fn.generateConstLiterals(ps);
    }
  }
  
  @Override
  protected void generateNodeComputeFunction(
      List<Integer> firstIndexList,
      List<Integer> lastIndexList,
      List<String> lastIndexStringPlusOneList, 
      Set<KernelData> itd,
      PrintStream ps,
      String prefix) {
    
    Set<KernelData> internalData=new HashSet<KernelData>(nodeSet);
    internalData.addAll(itd);
    internalData.remove(head);
    
    for (FunctionNode fn:nodeList) {
      if (fn!=head) {
        // Allocate a local variable
        ps.print(prefix);
        CLGenKernelData.generateBaseCType(fn,ps);
        ps.print(" ");
        ps.print(fn.getName());
        ps.println(";");
      }
      ps.print(prefix);ps.println("{");
      fn.generateNodeComputeFunction(firstIndexList,lastIndexList,lastIndexStringPlusOneList,internalData,ps,prefix+"  ");
      ps.println();
      ps.print(prefix);ps.println("}");
     }
  }


  // ==================================================================
  // Verbosing
  // ==================================================================

  public void print(String prefix, CompilerError ce) {
    StringBuffer buf=new StringBuffer();
    buf.append(prefix);
    buf.append(getName()).append(": ");
    boolean first=true;
    for(FunctionNode fn:nodeList) {
      if (!first) buf.append(", ");
      first=false;
      buf.append(fn.getName());
      if (fn==head) {
        buf.append(" (head)");
      }
    }
    ce.raiseMessage(buf.toString());
  }
  
}
