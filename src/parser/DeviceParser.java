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

package parser;

import java.io.File;
import java.io.IOException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import target.ComputeDevice;

import common.CompilerError;

public class DeviceParser {
  // Module information
  ComputeDevice device=null; // IR
  File inputFile=null; // Original file

  // Parser
  DeviceConfigLexer lex = null;
  CommonTokenStream tokens = null;
  DeviceConfigParser parser = null;
  
  // Error management
  CompilerError compilerError = new CompilerError();
  int verbose=0;
  
  
  // ##################################################################
  // Building
  // ##################################################################

  public DeviceParser(int v, File f) {
    inputFile=f;
    if (!f.isFile()) {
      compilerError.raiseFatalError("Target device configuration file '"+f.getPath()+"' does not exist");
    }
    compilerError = new CompilerError(v,f);
  }
  
  // ##################################################################
  // Parsing
  // ##################################################################

  public void parse() {
    try {
      lex = new DeviceConfigLexer(new ANTLRFileStream(inputFile.getPath()));
    } catch (IOException e1) {
      // Should never happen
      e1.printStackTrace();
    }

    tokens = new CommonTokenStream(lex);
    parser = new DeviceConfigParser(tokens);
    parser.setCompilerError(compilerError);

    try {
      parser.module();
    } catch (RecognitionException e)  {
      e.printStackTrace();
    }
    
    device=parser.getComputeDevice();
  }


  // ##################################################################
  // Getters
  // ##################################################################

  public File getInputFile() {
    return inputFile;
  }
    
  public ComputeDevice getDevice() {
    return device;
  }
  
  public CompilerError getCompilerError() {
   return compilerError; 
  }

}
