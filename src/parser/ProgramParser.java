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

import ir.base.Program;
import ir.symbolTable.SymbolTable;
import java.io.File;
import java.io.IOException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.TokenStream;

import common.CompilerError;
import parser.KernelGeniusLexer;
import parser.KernelGeniusParser;

public class ProgramParser {
  // Module information
  Program program=null; // IR
  String programName=null;
  File inputFile=null; // Original file
  CommonTree astTree=null; 

  // Parser
  KernelGeniusLexer lex = null;
  CommonTokenStream tokens = null;
  KernelGeniusParser parser = null;
  CommonTreeAdaptor treeAdaptor = null;
  
  // Error management
  CompilerError compilerError = new CompilerError();
  int verbose=0;
  
  
  // ##################################################################
  // Building
  // ##################################################################

  public ProgramParser(int v, File f, String s) {
    inputFile=f;
    programName=s;
    if (!f.isFile()) {
      compilerError.raiseFatalError("input file '"+f.getPath()+"' does not exist");
    }
    compilerError = new CompilerError(v,f);
  }
   
  
  // ##################################################################
  // Parsing
  // ##################################################################

  //TLexer lexer = new TLexer(new ANTLRStringStream(source));
  //TParser parser = new TParser(new CommonTokenStream(lexer));
  //parser.setTreeAdaptor(new CommonTreeAdaptor(){

  

  public void parse() {
    // Lexer build
    try { lex = new KernelGeniusLexer(new ANTLRFileStream(inputFile.getPath())); }
    catch (IOException e1) { e1.printStackTrace(); }
    tokens = new CommonTokenStream(lex);

    // Parser tree adaptor build
    treeAdaptor=new CommonTreeAdaptor() {
      
      // Standard Nodes
      public Object create(Token token) {
        return new TNode(token);
      }
      
      // Imaginary nodes
      public Object create(int type, String text) {
        return new TNode(new MyToken(type,text));
      }
     
      public Object dupNode(Object t) {
        if ( t==null ) {
          return null;
        }
        return create(((TNode)t).token);
      }
      
      public Object errorNode(TokenStream input, Token start, Token stop,
          RecognitionException e)
      {
        TNodeError t = new TNodeError(input, start, stop, e);
        return t;
      }
    };
    
    // Parser build
    parser = new KernelGeniusParser(tokens);
    parser.setProgramName(programName);
    parser.setCompilerError(compilerError);
    parser.setTreeAdaptor(treeAdaptor);


    // Run the parser
    KernelGeniusParser.program_return ret=null;
    try {
      ret=parser.program();
    } catch (Exception e)  {
      e.printStackTrace();
    }
    
    // Sets AST informations
    astTree=(CommonTree)ret.getTree();
    program=ret.program;
  }


  // ##################################################################
  // Getters
  // ##################################################################

  public File getInputFile() {
    return inputFile;
  }
  
  public SymbolTable getSymbolTable() {
    return(parser.getSymbolTable());
  };
  
  public CommonTree getAST() {
    return astTree;
  }
  
  public Program getProgram() {
    return program;
  }
  
  public CompilerError getCompilerError() {
   return compilerError; 
  }

}
