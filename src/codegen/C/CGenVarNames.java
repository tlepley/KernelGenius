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

package codegen.C;

import ir.base.FunctionNode;
import ir.base.KernelData;

import java.io.PrintStream;

public class CGenVarNames {

  // KernelGenius prefix
  static String getKGPrefix() {
    return("__kg_");
  }

  //------------------------------------
  //         Function nodes
  //------------------------------------
  
  public static void generateComputeLoopBlockCounter(PrintStream ps) {
    ps.print(getKGPrefix()+getComputeLoopCounterBaseName()+"Block");
  }
  public static void generateComputeLoopCounterPix(KernelData kd, PrintStream ps) {
    ps.print(getKGPrefix()+getComputeLoopCounterBaseName()+"Pix_"+kd.getName());
  }
  public static void appendComputeLoopCounterPix(KernelData kd, StringBuffer ps) {
    ps.append(getKGPrefix()+getComputeLoopCounterBaseName()+"Pix_"+kd.getName());
  }
  static String getComputeLoopCounterBaseName() {
    return "counter";
  }
  
  static String getIndexVarName(int dim) {
    return getKGPrefix()+"index"+dim;
  }
  public static void appendIndex(StringBuffer sb, int dim) {
    sb.append(getIndexVarName(dim));
  }
  public static void generateIndexDeclaration(int dim, PrintStream ps, String prefix) {
    ps.print(prefix);
    ps.print("int ");
    ps.print(getIndexVarName(dim));
    ps.println(";");
  }
  

  //------------------------------------
  //       Main kernel function
  //------------------------------------
  
  public static void generateSchedulerLoopCounter(PrintStream ps) {
    ps.print(getKGPrefix()+getSchedulerLoopCounter());
  }

  static String getSchedulerLoopCounter() {
    return "i";
  }
  
  // External tiling mode
  static String getKernelTileBaseName() {
    return "kernelTile";
  }
  static public void generateKernelTilePtrName(KernelData kd, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getKernelTileBaseName());
    ps.print("_ptr_");
    ps.print(kd.getName());
  }
  public static void generateKernelTilePosition(int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getKernelTileBaseName());
    ps.print("_pos"+dim);
  }
  public static void generateKernelTileSize(int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getKernelTileBaseName());
    ps.print("_size"+dim);
  }

  
  // WG slicing
  static String getWGSliceBaseName() {
    return "WGSlice_";
  }
  static String getMyWGSliceBaseName() {
    return "MyWGSlice_";
  }
  static String getWGFullSliceBaseName() {
    return "WGFullSlice_";
  }
  static String getMyWGFullSliceBaseName() {
    return "MyWGFullSlice_";
  }
  
  // --> global to the main kernel function
  static public void generateWGSlicePtrName(KernelData kd, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getWGSliceBaseName());
    ps.print("ptr_");
    ps.print(kd.getName());
  }
  public static void generateMyWGSlicePositionGrainUnit(int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("PosGrain"+dim);
  }
  public static void generateWGSliceSizeGrainUnit(int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getWGSliceBaseName());
    ps.print("size"+dim);
  }
  public static void generateMyWGSliceSizeGrainUnit(int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("SizeGrain"+dim);
  }
  public static void generateNbGrains(int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print("nbGrain"+dim);
  }
  
  // --> Specific to a compute function
  public static void generateMyWGSlicePositionBlockUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("PosBlock"+dim+"_"+kd.getName());
  }
  public static void generateMyWGSliceSizeBlockUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("SizeBlock"+dim+"_"+kd.getName());
  }
  public static void generatePositionPixUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix()+"posPix");
    ps.print(dim+"_"+kd.getName());
  }
  public static void generatePositionBlockUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix()+"posBlock");
    ps.print(dim+"_"+kd.getName());
  }
  public static void generateSkipBeginBlockUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix()+"skipBeginBlock");
    ps.print(dim+"_"+kd.getName());  
  }
  public static void generateSkipEndBlockUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix()+"skipEndBlock");
    ps.print(dim+"_"+kd.getName());  
  }
  public static void generateSkipBeginPixUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix()+"skipBeginPix");
    ps.print(dim+"_"+kd.getName());  
  }
  public static void generateSkipEndPixUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix()+"skipEndPix");
    ps.print(dim+"_"+kd.getName());  
  }

  
  // --> Specific to a data
  public static void generateMyWGSliceLeftExtentGrainUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("LeftExtentGrain"+dim+"_"+kd.getName());
  }
  public static void generateMyWGSliceRightExtentGrainUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("RightExtentGrain"+dim+"_"+kd.getName());
  }
  public static void generateMyWGSliceLeftExtentBlockUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("LeftExtentBlock"+dim+"_"+kd.getName());
  }
  public static void generateMyWGSliceRightExtentBlockUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("RightExtentBlock"+dim+"_"+kd.getName());
  }
  public static void generateMyWGSliceLeftExtentPixUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("LeftExtentPix"+dim+"_"+kd.getName());
  }
  public static void generateMyWGSliceSizePixUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("SizePix"+dim+"_"+kd.getName());
  }
  public static void generateFullSliceSizePixUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getWGFullSliceBaseName());
    ps.print("sizePix"+dim+"_"+kd.getName());
  }
  public static void generateMyFullSliceSizePixUnit(KernelData kd, int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGFullSliceBaseName());
    ps.print("SizePix"+dim+"_"+kd.getName());
  }
  public static void generateMySlicePositionPixUnit(KernelData kd,int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGSliceBaseName());
    ps.print("PosPix"+dim+"_"+kd.getName());
  }
  public static void generateMyFullSlicePositionPixUnit(KernelData kd,int dim, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print(getMyWGFullSliceBaseName());
    ps.print("PosPix"+dim+"_"+kd.getName());
  }


  
  
  static public void generateKernelEventName(KernelData kd, int offset, PrintStream ps) {
    generateKernelEventName(kd,ps);
    ps.print("[");
    ps.print(offset);
    ps.print("]");
  }
  static public void generateKernelEventName(KernelData kd, PrintStream ps) {
    ps.print(getKGPrefix()+"event_");
    ps.print(kd.getName());
  }
  static public void generateKernelLocalName(KernelData kd, PrintStream ps) {
    ps.print(getKernelLocalName(kd));
  }

  static public String getKernelLocalName(KernelData kd) {
    return getKGPrefix()+"local_"+
        kd.getName();
  }

  static public void generateKernelLocalBufferName(KernelData kd, PrintStream ps) {
    ps.print(getKGPrefix()+"buffer_");
    ps.print(kd.getName());
  }
  static public void generateKernelLocalCircularName(KernelData kd, PrintStream ps) {
    ps.print(getKGPrefix()+"localcirc_");
    ps.print(kd.getName());
  }

  static public void generateComputeFunctionName(FunctionNode fn, PrintStream ps) {
    ps.print(getKGPrefix());
    ps.print("Compute_");
    ps.print(fn.myId);
    ps.print("_");
    ps.print(fn.getName());
  }
 
  
  //------------------------------------
  //             Generic
  //------------------------------------

  public static String getLocalIdVarName(int dim) {
    return getKGPrefix()+"ID"+dim;
  }
  public static String getLocalSizeVarName(int dim) {
    return getKGPrefix()+"nbWI"+dim;
  }
  public static String getGroupIdVarName(int dim) {
    return getKGPrefix()+"gID"+dim;
  }
  public static String getNbWGVarName(int dim) {
    return getKGPrefix()+"nbWG"+dim;
  }

  
  public static void generateLocalInfoVarDeclaration(int dim, PrintStream ps, String prefix) {
    ps.print(prefix);
    ps.print("int ");
    ps.print(getLocalIdVarName(dim));
    ps.println("= get_local_id("+dim+");");
    
    ps.print(prefix);
    ps.print("int ");
    ps.print(getLocalSizeVarName(dim));
    ps.println("= get_local_size("+dim+");");
  }
  
  public static void generateWGInfoVarDeclaration(int dim, PrintStream ps, String prefix) {
    ps.print(prefix);
    ps.print("int ");
    ps.print(getGroupIdVarName(dim));
    ps.println("= get_group_id("+dim+");");
    
    ps.print(prefix);
    ps.print("int ");
    ps.print(getNbWGVarName(dim));
    ps.println("= get_num_groups("+dim+");");
    ps.println();
  }

}
