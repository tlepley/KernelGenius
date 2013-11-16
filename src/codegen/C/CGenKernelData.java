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

import ir.base.KernelData;
import ir.types.kg.MatrixIndexes;
import java.io.PrintStream;
import org.antlr.runtime.RecognitionException;
import parser.KernelGeniusEmitter;
import codegen.CodegenDataPattern;
import common.CompilerError;
import driver.options.CodegenOptions;

public class CGenKernelData {

  static public void generateKernelParamDeclaration(KernelData kd, PrintStream ps) {
	  if (!kd.getMatrixType().hasSingleElement()) {
		  generateBaseCType(kd,ps);
		  ps.print(" * ");
		  ps.print(kd.getName());
	  }
	  else {
		  // It will be passed by value
		  generateBaseCType(kd,ps);
		  ps.print(" ");
		  ps.print(kd.getName());    
	  }
  }

  static public int generateKernelTempParamDeclaration(KernelData kd, PrintStream ps, int n) {
	  if (n!=0) {
		  ps.print(", ");
	  }
	  generateBaseCType(kd, ps);
	  ps.print(" * ");
	  ps.print(kd.getName());
	  return 1;
  }

  static public void generateComputeFunctionParamDeclaration(KernelData kd, MatrixIndexes pattern, PrintStream ps) {
	  if (kd.isControlData()) {
		  if (kd.isNonIterativeDataToCache()) {
			  // Non iterative data to cache
			  kd.getMatrixType().generate(ps, kd.getBaseCTypeNode(), kd.getName());
		  }
		  else {
			  // This is a control parameter copied by value
			  generateBaseCType(kd,ps);
			  ps.print(" ");
			  ps.print(kd.getName());
		  }
	  }
	  else {
		  // This is a iterative computational data
		  generateBaseCType(kd,ps);
		  ps.print(" *");
		  ps.print(kd.getName());
	  }
  }


  static public void generateIOPointerDeclaration(KernelData kd, String prefix, PrintStream ps) {
    CodegenDataPattern outputPattern=kd.getCodegenDataPattern();
    if (outputPattern==null) {
      // This is a control parameter
    }
    else {
      // This is a computational data
      ps.print(prefix);
      ps.print("global ");
      generateBaseCType(kd,ps);
      ps.print(" *");
      ps.print(kd.getName());
      ps.print("_IO_pointer;");
    }
  }



  static public void generateBaseCType(KernelData kd, PrintStream ps) {
    KernelGeniusEmitter emitter=new KernelGeniusEmitter(ps,kd.getBaseCTypeNode());
    try {
      emitter.declSpecifiers();
    } catch (RecognitionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  
}
