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
import ir.base.Kernel;
import ir.base.KernelData;
import java.io.PrintStream;
import java.util.LinkedHashSet;

public class CGenKernel {

  public static void generateC_MainKernelFunction(Kernel k, PrintStream ps) {
    // Compute function prototype
    ps.print("void "+k.getName()+"(");
    // Output first
    int n=0;
    for(KernelData kd:k.getComputationalOutputList()) {
      if (n++!=0) {
        ps.print(", ");
      }
      CGenKernelData.generateKernelParamDeclaration(kd, ps);
    }
    // Then inputs (global)
    for(KernelData kd:k.getParameterList()) {
      if (n++!=0) {
        ps.print(", ");
      }
      CGenKernelData.generateKernelParamDeclaration(kd, ps);
    }

    // Then temporary variables
    for(KernelData kd:k.getComputationalDataList()) {
    	n+=CGenKernelData.generateKernelTempParamDeclaration(kd,ps, n);
    }

    ps.print(")");

    //=============
    // Kernel body
    //=============
    ps.println(" {");

    // Execute nodes
    for(FunctionNode fn:k.getFunctionNodeList()) {
    	CGenKernel.generateImageComputeCall(fn,"    ",ps);
    }

    // === end of kernel ====
    ps.println("}");
  }

  static public void generateImageComputeCall(FunctionNode fn, 
		  String prefix,
		  PrintStream ps) {

	  // Function call
	  ps.print(prefix+"  ");
	  CGenVarNames.generateComputeFunctionName(fn, ps);
	  ps.print("(");

	  // 1- Output first
	  ps.print(prefix+fn.getName());

	  // 2- Inputs
	  for(int j=0;j<fn.getNbInputData();j++) {
		  KernelData kd=fn.getInputData(j);
		  ps.print(", ");
		  ps.print(prefix+kd.getName());
	  }

	  // 3- All implicit control data (size of iterative input)
	  LinkedHashSet<KernelData> controlParamSet=new LinkedHashSet<KernelData>();
	  fn.getImplicitControlParamList(controlParamSet);
	  for(KernelData kd:controlParamSet) {
		  ps.print(", ");
		  ps.print(kd.getName());
	  }

	  ps.println(");");
  }

  static public void generateImageComputeFunctionPrototype(FunctionNode fn, PrintStream ps) {
    // Compute function prototype
    ps.print("void ");
    CGenVarNames.generateComputeFunctionName(fn, ps);
    ps.print("(");
    // 1- Output first
    CGenKernelData.generateComputeFunctionParamDeclaration(fn,fn.getWritePattern(), ps);
    // 2- Inputs
    for(KernelData kd:fn.getInputDataList()) {
      ps.print(", ");
      if (kd.isIterative()) {
        CGenKernelData.generateComputeFunctionParamDeclaration(kd, fn.getInputEdge(kd).getReadPattern(), ps);
      }
      else {
        CGenKernelData.generateComputeFunctionParamDeclaration(kd, null, ps);
      }
    }
    // 3- All implicit control data (size of iterative input)
    LinkedHashSet<KernelData> controlParamSet=new LinkedHashSet<KernelData>();
    fn.getImplicitControlParamList(controlParamSet);
    for(KernelData kd:controlParamSet) {
      ps.print(", ");
      CGenKernelData.generateComputeFunctionParamDeclaration(kd,null, ps);   
    }
    ps.print(")");
  }

  static public void generateImageComputeFunction(FunctionNode fn, PrintStream ps) {
    // Function prototype
    generateImageComputeFunctionPrototype(fn,ps);

    // Function body
    ps.println("{");
    fn.generateCImageComputeFunctionBody(ps);
    ps.println("}");
    ps.println();
  }
  
}
