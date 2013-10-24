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

tree grammar KernelGeniusEmitter;

options {
  ASTLabelType=TNode;
  tokenVocab=KernelGenius;
}


@header {
  package parser;
  
  import java.io.PrintStream;
  import java.util.Stack;
  import java.util.List;
}

@members {
  // Constructors
  public KernelGeniusEmitter(PrintStream ps, TNode treeToEmit) {
    this((TreeNodeStream)new CommonTreeNodeStream((Tree)treeToEmit));
    setNoLineManagement();
    setPrintStream(ps);
  }

  // Internal members
  protected int tabs = 0;
  protected PrintStream currentOutput = System.out;
  protected int lineNum = 1;
  protected String currentSource = "";
  protected LineObject trueSourceFile;
  protected final int lineDirectiveThreshold = Integer.MAX_VALUE;
  protected PreprocessorInfoChannel preprocessorInfoChannel = null;
  protected Stack sourceFiles = new Stack();
  boolean lineManagement=true;
  

  public void setNoLineManagement() {
    lineManagement=false;
  }

  public void setPrintStream(PrintStream ps) {
    currentOutput=ps;
  }

  public void setPreprocInfoChannel(PreprocessorInfoChannel preprocChannel) {
    preprocessorInfoChannel = preprocChannel;
  }

  protected void initializePrinting() {
    if (preprocessorInfoChannel!=null) {
      List<Object> preprocs = preprocessorInfoChannel.extractLinesPrecedingTokenNumber( new Integer(1) );
      printPreprocs(preprocs);
      /*    if ( currentSource.equals("") ) {
      trueSourceFile = new LineObject(currentSource);
      currentOutput.println("# 1 \"" + currentSource + "\"");
      sourceFiles.push(trueSourceFile);
      } 
      */
    }
  }

  protected void finalizePrinting() {
    // flush any leftover preprocessing instructions to the stream
    if (preprocessorInfoChannel!=null) {
      printPreprocs( 
        preprocessorInfoChannel.extractLinesPrecedingTokenNumber( 
        new Integer( preprocessorInfoChannel.getMaxTokenNumber() + 1 ) ));
      //print a newline so file ends at a new line
      currentOutput.println();
    }
  }

  protected void printPreprocs( List<Object> preprocs )  {
    // if there was a preprocessingDirective previous to this token then
    // print a newline and the directive, line numbers handled later
    if ( preprocs.size() > 0 ) {  
      currentOutput.println();  //make sure we're starting a new line unless this is the first line directive
      lineNum++;
      for (Object o:preprocs) {
  if ( o instanceof LineObject ) {
    LineObject l = (LineObject) o;
      
    // we always return to the trueSourceFile, we never enter it from another file
    // force it to be returning if in fact we aren't currently in trueSourceFile
    //if (( trueSourceFile != null ) //trueSource exists
    //  && ( !currentSource.equals(trueSourceFile.getSource()) ) //currently not in trueSource
    //  && ( trueSourceFile.getSource().equals(l.getSource())  ) ) { //returning to trueSource
    //    l.setEnteringFile( false );
    //   l.setReturningToFile( true );
    // }
    
    
    // print the line directive
    currentOutput.println(l);
    lineNum = l.getLine();
    currentSource = l.getSource();
    
    
    // the very first line directive always represents the true sourcefile
    if ( trueSourceFile == null ) {
      trueSourceFile = new LineObject(currentSource);
      sourceFiles.push(trueSourceFile);
    }
    
    // keep our own stack of files entered
    if ( l.getEnteringFile() ) {
      sourceFiles.push(l);
    }
    
    // if returning to a file, pop the exited files off the stack
    if ( l.getReturningToFile() ) {
      LineObject top = (LineObject) sourceFiles.peek();
      while (( top != trueSourceFile ) && (! l.getSource().equals(top.getSource()) )) {
        sourceFiles.pop();
        top = (LineObject) sourceFiles.peek();
      }
    }
  }
  else {
    // it was a #pragma or such
    currentOutput.println(o);
    lineNum++;
  }
      }
    }
    
  }


  // Manage preprocessing directives
  protected void moveToNode( TNode t ) {
    if (lineManagement) {
      int tLineNum = t.getLine();
      if ( tLineNum == 0 ) tLineNum = lineNum;

      if (preprocessorInfoChannel!=null) {
        List<Object> preprocs = preprocessorInfoChannel.extractLinesPrecedingTokenNumber(t.getTokenNumber());
        printPreprocs(preprocs);
      }

      if ( (lineNum != tLineNum) ) {
        // we know we'll be newlines or a line directive or it probably
        // is just the case that this token is on the next line
        // either way start a new line and indent it
        currentOutput.println();
        lineNum++;      
        printTabs();
      }

      if ( lineNum == tLineNum ){
        // do nothing special, we're at the right place
      }
      else {  
        int diff = tLineNum - lineNum;
        if ( lineNum < tLineNum ) {
            // print out the blank lines to bring us up to right line number
            for ( ; lineNum < tLineNum ; lineNum++ ) {
                currentOutput.println();
            }
            printTabs();
        }
        else { // just reset lineNum
            lineNum = tLineNum; 
           currentOutput.println("# "+lineNum+" \"" + currentSource + "\"");
        }
      }
    }
  }


  protected void print( TNode t ) {
    // Manage preprocessing directives and line positioning
    moveToNode(t);
    currentOutput.print( t.getText() + " " );
  }


  /** It is not ok to print newlines from the String passed in as 
      it will screw up the line number handling **/
  protected void print( String s ) {
    currentOutput.print( s + " " );
  }
  
  protected void printTabs() {
    for ( int i = 0; i< tabs; i++ ) {
      currentOutput.print( "\t" );
    }
  }
    
  protected void comma( ) {
    print( "," );
  }
  
}


program:
  ^( PROGRAM programStatementList )
  ;

programStatementList :
  ( programStatement )+ ;

programStatement : 
  ( kernelDeclaration
  | nativeProgramStatement
  | declaration
  )
  ;
 
nativeProgramStatement :
  C_SECTION
  ;


//========================================================
// declarations
//========================================================

declaration : 
   ^( NDeclaration declaration_body )
   { print ( ";" ); }
   ;

declaration_body :
   declSpecifiers
   ( initDeclList )?
   ;

initDeclList :
    initDecl     
    ( 
     { print( "," ); }
      initDecl
     )*
   ;

initDecl:
   declarator
   ;


//========================================================
// kernel
//========================================================

kernelDeclaration :
   ^( KERNEL ID 
     kernelParams
     kernelBody
   )
  ;

kernelParams :
   ^( LPAREN 
     kernelParamList
   )
   ;

kernelParamList : ( kernelParamDeclaration )*;

kernelParamDeclaration :
   kernelParamDeclarator ;

kernelParamDeclarator :
  scalarTypeSpecifier ID 
  (arrayDecl)?
  (kernelParamInitializer)?
;

kernelParamInitializer :
   ASSIGN top_literal
;

kernelBody : 
  ^( LCURLY
     kernelBodyStatement* 
   )
   ;
         
kernelBodyStatement :  
  (
    kernelAlgo
  | kernelReturn
  ) SEMI ;

kernelReturn :
  RETURN symbolReference 
;

numberValue :
  a=IntegralNumber { print( $a ); }  ;



//========================================================
// Node
//========================================================

paramList : param paramList2 ;
paramList2 : COMMA param paramList2 | ;
param :  symbolReference ;

kernelAlgo 
: kernelAlgoType ID 
  algoParams
  algoBody?
  ;

algoParams :
  LPAREN 
  paramList
  RPAREN
  ;

kernelAlgoType 
: ID LT typeDecl GT;

algoBody : 
  LCURLY
  propertyStatement*
  RCURLY
;

propertyStatement :
  DOT propertyDeclarator 
  propertyDeclaratorParam
  ( propertyAssignment
  |
  )
  SEMI ;

propertyDeclaratorParam
: ( 
    LPAREN
    ID
    RPAREN
    ( COMMA ID )*
  )?
 ;

propertyDeclarator :
  ID ;

propertyAssignment :
  ASSIGN  propertyInit
  ;

propertyInit : 
     propertyInitID
   | literalNoType
   | arrayDecl
   | propertyInitString
   | propertyInitCSection
   ;
   
propertyInitID :
  ID 
  ;

propertyInitString  :
  StringLiteral 
  ;

propertyInitCSection :
  C_SECTION 
  ;

symbolReference :
  ID 
  ;

// TODO: should be removed
typeDefinition :
  scalarTypeSpecifier 
  arrayDecl ?
  ;


//========================================================
// Types
//========================================================


typeDecl :
  scalarTypeSpecifier
  arrayDecl?
;

//=================== scalar type ========================

declSpecifiers
        :
          ( storageClassSpecifier
          | typeSpecifier
          )+
        ;
        
specifierQualifierList
        : ( typeSpecifier )+
        ;
        
storageClassSpecifier
        :   a=TYPEDEF  { print( $a ); }
        ;

typeSpecifier
        :       
            a=CHAR      { print( $a ); }
        |   b=SHORT     { print( $b ); }
        |   c=INT       { print( $c ); }
        |   d=LONG      { print( $d ); }
        |   e=FLOAT     { print( $e ); }
        |   f=DOUBLE    { print( $f ); }
        |   g=SIGNED    { print( $g ); }
        |   h=UNSIGNED  { print( $h ); }

        |   structSpecifier
        |   unionSpecifier
        |   enumSpecifier
        |   typedefName
        ;

typedefName
        :  ^(TYPEDEFNAME i=ID { print( $i ); } ) 
        ;

structSpecifier
        : ^( a=STRUCT { print( $a ); }
            structorUnionBody
        )
 ;

unionSpecifier
        : ^( a=UNION { print( $a ); }
            structorUnionBody
        )
 ;
 
 
structorUnionBody
        : 
          ( 
            ( ID LCURLY )=> 
              i1=ID lc1=LCURLY  { print( $i1 ); print ( $lc1 ); tabs++; }
              ( structOrUnionDeclarationList )? rc1=RCURLY  { tabs--; print( $rc1 ); }
            | lc2=LCURLY { print( $lc2); tabs++; }
              ( structOrUnionDeclarationList )? rc2=RCURLY { tabs--; print( $rc2 ); }
            | i2=ID { print( $i2 ); }
           )
        ;

structOrUnionDeclarationList
        : ( structOrUnionDeclaration )+
        ;

structOrUnionDeclaration
        : specifierQualifierList
          structDeclaratorList { print( ";" ); }
        ;

structDeclaratorList
        :  structDeclarator
           ( { print(","); } structDeclarator )*
        ;

structDeclarator
        : declarator
        ;


enumSpecifier
       :       
       ^( a=ENUM { print( $a ); }
       ( 
         ( ID LCURLY )=>
           i1=ID lc1=LCURLY  { print( $i1 ); print ( $lc1 ); tabs++; }
           enumList rc1=RCURLY  { tabs--; print( $rc1 ); }
                  
         | lc2=LCURLY { print( $lc2); tabs++; } 
           enumList rc2=RCURLY { tabs--; print( $rc2 ); }
             
         | i2=ID { print( $i2 ); }
       )
       )
       ;
             
        
// TODO: manage enum values ?
enumList
       :  enumerator
           ( { print(","); } enumerator )*
   ;

enumerator
      : 
   id=ID {print($id);} (a=ASSIGN {print($a);} numberValue)?
;


declarator
        :      
          i=ID  { print( $i ); } 
          ( arrayDecl )?
        ;

scalarTypeSpecifier : 
    a=CHAR    { print( $a ); }
  | b=SHORT   { print( $b ); }
  | c=INT     { print( $c ); }
  | d=FLOAT   { print( $d ); }
  | e=DOUBLE  { print( $e ); }
;


//================= standard array type =====================

arrayDecl : 
  ( arrayDeclDimension )+
  ;


arrayDeclDimension : 
  ^( lb=LBRACKET   { print( $lb ); }
     arrayDeclValue
     rb=RBRACKET  { print( $rb ); }
   )
  ;

arrayDeclValue : 
  (
   value | 
   idValue | 
   rangeValue
  )
  ;

value :
  numberValue 
  ;

idValue :
  symbolReference 
  ;

rangeValue :
  numberValue 
  c=COLON { print( $c ); }
  numberValue 
  ;



//========================================================
// Literals
//========================================================

//--- Self-contained literals (do not take any type as input) ---

literalNoType :
    compoundLiteral
  | intArrayLiteral
  | scalarLiteralNoType
 ;

compoundLiteral
: lp=LPAREN   { print( $lp ); }
  typeDefinition
  rp=RPAREN   { print( $rp ); }
  top_literal;
  
intArrayLiteral 
: curlyLiteral ;

scalarLiteralNoType
:   integralLiteral 
  | floatingPointLiteral
;

//--- Initializer literals (for compound literals or declaration) ---
top_literal :
  literal
  ;

literal :    
     scalarLiteral
   | curlyLiteral
   | rangeLiteral
;


//=================== range literal ========================

rangeLiteral :
   ^( lb=LBRACKET   { print( $lb ); }
      numberValue
      c=COLON       { print( $c ); }
      numberValue
      rb=RBRACKET   { print( $rb ); }
    )
;


//=================== curly literal ========================


curlyLiteral :
  ^( lc=LCURLY    { print( $lc ); }
     initializerList       
     rc=RCURLY    { print( $rc ); }
   )
;

initializerElementLabel :
  ( i1=ID c=COLON         { print( $i1 );  print( $c ); }
  | d=DOT i2=ID a=ASSIGN  { print( $d ); print( $i2 );  print( $a ); }
  )
  ;

initializerList :
   ( 
      ( c=COMMA  { print( $c ); } )?
      ( initializerElementLabel )? 
      literal
   )*
   ;


//=================== scalar literal ========================

scalarLiteral :
  (
    integralLiteral
  | floatingPointLiteral
  )
;

integralLiteral :
  in=IntegralNumber { print( $in ); }
  ;

floatingPointLiteral :
  fn=FloatingPointNumber { print( $fn ); }
  ;

