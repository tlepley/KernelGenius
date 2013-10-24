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

package codegen;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import common.CompilerError;

import ir.base.Program;

abstract public class CodeGenerator {
  //=================================================
  // Main entry point
  //=================================================
  public abstract void generate(Program prog, List<File> generatedFiles, File tempDir);
  public abstract void generateReport(Program prog, PrintStream ps);
  
  //=================================================
  // Dynamic generation of code generators from name
  //=================================================
  public static CodeGenerator getNewGeneratorFromName(String s, CompilerError ce) {
    Class<?> t=null;
    try {
      t=Class.forName("codegen."+s+".Generator");
    }
    catch(ClassNotFoundException e) {
      ce.raiseFatalError("no generator for language '"+s+"'");
    }
    
    Class<? extends CodeGenerator>  c=null;
    try {
       c = t.asSubclass(CodeGenerator.class);
    }
    catch(ClassCastException e) {
      ce.raiseInternalError("getNewGeneratorFromName("+s+"), ClassCastException");
    }
       
    CodeGenerator cg=null;
    try {
      cg=c.newInstance();
    }
    catch(InstantiationException e) {
      ce.raiseInternalError("getNewGeneratorFromName("+s+"), InstantiationException");
    }
    catch(IllegalAccessException e) {
      ce.raiseInternalError("getNewGeneratorFromName("+s+"), IllegalAccessException");
    }
    return cg;
  }
  
  public static void checkGeneratorFromName(String s, CompilerError ce) {
    Class<?> t=null;
    try {
      t=Class.forName("codegen."+s+".Generator");
    }
    catch(ClassNotFoundException e) {
      ce.raiseFatalError("no generator for language '"+s+"'");
    }
  }

}
