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

grammar DeviceConfig;

options {
  output=AST;
  ASTLabelType=CommonTree;
}

tokens {
    // Main keywords
    COMPUTEDEVICE  = 'computeDevice' ;
    COMPUTEUNIT    = 'computeUnit' ;
    COMPUTEELEMENT = 'computeElement' ;
    
    // types
    FLOAT  = 'float';
    DOUBLE = 'double';
    LONG   = 'long';
    INT    = 'int';
    SHORT  = 'short';
    CHAR   = 'char';
    
    // Language Operators
    ASSIGN   = '=' ;
    COMMA    = ',' ;
    SEMI     = ';' ;

    LPAREN   = '(' ;
    RPAREN   = ')' ;
    LCURLY   = '{' ;
    RCURLY   = '}' ;
    LBRACKET = '[' ;
    RBRACKET = ']' ;
    
    POINT = '.' ;
    TWOPOINTS = '..' ;

    LTE      = '<=' ;
    LT       = '<' ;
}

@lexer::header {
  package parser;
}

@parser::header {
  package parser;
  import target.*;
  import ir.types.*;
  import ir.types.c.*;
  import ir.literals.*;
  import ir.literals.c.*;
  import ir.symbolTable.*;
  import common.*;
  
}
 
@parser::members {
  // ##################################################################
  // IR
  // ##################################################################
  ComputeDevice computeDevice=null;
  
  public ComputeDevice getComputeDevice() {
    return computeDevice;
  }
 
  // ##################################################################
  // Error management
  // ##################################################################
  private CompilerError compilerError = new CompilerError();

  public void setCompilerError(CompilerError cp) {
    compilerError = cp;
  }
}

//========================================================
// Grammar
//========================================================

module
@after{computeDevice=$d.device;}
: d=computeDevice ;

computeDevice returns [ComputeDevice device] 
@init{ComputeDevice d=null;}
@after{
  d.finalCheck(compilerError);
  $device=d;
}
:
COMPUTEDEVICE id=ID { d=new ComputeDevice($id.getText()); }
LCURLY computeDeviceBody[d] RCURLY
;

computeDeviceBody [ComputeDevice device]
: (
  propertyStatement[$device]
| a=computeUnit    {$device.setComputeUnit($a.computeUnit,compilerError);}
| b=computeElement {$device.setComputeElement($b.computeElement,compilerError);}
  )*
;

computeUnit returns [ComputeUnit computeUnit]
@init{ComputeUnit cu=null;}
@after{
  cu.finalCheck(compilerError);
  $computeUnit=cu;
  }
:
COMPUTEUNIT id=ID {cu=new ComputeUnit($id.getText());}
LCURLY computeBody[cu] RCURLY
;

computeElement returns [ComputeElement computeElement]
@init{ComputeElement ce=null;}
@after{
  ce.finalCheck(compilerError);
  $computeElement=ce;
  }
:
COMPUTEELEMENT id=ID {ce=new ComputeElement($id.getText());}
LCURLY computeBody[ce] RCURLY
;


computeBody[GenericDevice gd] : propertyStatement[$gd]*;


propertyStatement[GenericDevice gd] :
    POINT decl=propertyDeclarator 
    (
      propertyAssignment[$gd,$decl.string]
    |
      { $gd.setProperty($decl.string,compilerError); }
    )
    SEMI ;

propertyDeclarator returns [String string] 
@after { $string=$id.getText(); }
: id=ID ;

propertyAssignment[GenericDevice gd, String name]  : 
ASSIGN 
propertyInitList[$gd,$name]
;

propertyInitList[GenericDevice gd, String name] :
propertyInit[$gd,$name] propertyInitList2[$gd,$name];

propertyInitList2[GenericDevice gd, String name] :
COMMA propertyInit[$gd,$name] propertyInitList2[$gd,$name] | ;

propertyInit[GenericDevice gd, String name] : 
     pi=propertyInitID       {$gd.SetPropertyWithIdentifier($name,$pi.name,compilerError);}
   | pt=propertyInitInteger  {$gd.SetPropertyWithInteger($name,$pt.val,compilerError);}
   | pit=propertyInitLiteral {$gd.SetPropertyWithLiteral($name,$pit.literal,compilerError);}
   | ps=propertyInitString   {$gd.SetPropertyWithString($name,$ps.string,compilerError);}
   ;
   
propertyInitID returns [String name]
@after { $name=$id.getText(); }
: id=ID ;

propertyInitInteger returns [long val]
@after { $val=$vn.val;}
: vn=longValueWithUnit ;

propertyInitLiteral returns [Literal literal]
@after { $literal=$c.literal;}
: s=LPAREN t=typeDefinition RPAREN c=literal[$t.type] ;

propertyInitString returns [String string]
@after {$string=$sl.getText().substring(1,$sl.getText().length()-1);}
: sl=StringLiteral ;

typeDefinition returns [Type type] :
s=scalarTypeSpecifier 
 ( a=arrayRangeDecl[$s.type]
     {$type=$a.type;}
  |  {$type=$s.type;}
 )
;

//========================================================
// Literals
//========================================================

// TODO: type checking with the literal
// TODO: manage all kinds of literals

literal[Type type] returns [Literal literal] 
@init{ Literal l=null; }
@after{ $literal=l;} :
     a=arrayLiteral[$type]{l=$a.literal;}
   | b=scalarLiteral[$type] {l=$b.literal;}
   
;

arrayLiteral[Type type] returns [ArrayLiteral literal]
@init{
  ArrayRange arrayType=(ArrayRange)$type;
  ArrayLiteral al=new ArrayLiteral(arrayType); }
@after{ $literal=al; }
: LCURLY 
  (a=arrayLiteralElementList[arrayType.getChild(),al])?
  RCURLY
;

arrayLiteralElementList[Type type, ArrayLiteral literal] :
  a=literal[$type] 
    { $literal.add($a.literal);}
  arrayLiteralElementList2[$type,$literal]
  ;

arrayLiteralElementList2[Type type, ArrayLiteral literal] : 
 COMMA
 a=literal[$type] 
   {$literal.add($a.literal);}
 arrayLiteralElementList2[$type,$literal]
 | 
 ;

// TODO: manage all scalar literals
scalarLiteral[Type type] returns [Literal literal] 
@init{ Literal il=null;}
@after{$literal=il;}
:  n=integralValue
    { il=new IntegerLiteral($n.value,(IntegerScalar)$type);}
  | m=floatingPointValue
    { il=new FloatingPointLiteral($m.value,(FloatingPointScalar)$type);}  
;

integralValue returns [long value] 
@after{$value=Long.parseLong($n.getText());}
: n=IntegralNumber;

floatingPointValue returns [double value] 
@after{$value=Double.parseDouble($n.getText());}
: n=FloatingPointNumber;


//========================================================
// Types
//========================================================

// Scalar
scalarTypeSpecifier returns [Type type]: 
  CHAR {$type= IntegerScalar.Tschar;}
| SHORT {$type= IntegerScalar.Tsshort;}
| INT {$type= IntegerScalar.Tsint;}
| FLOAT {$type= FloatingPointScalar.Tfloat;}
| DOUBLE {$type= FloatingPointScalar.Tdouble;}
;

// Arrays
arrayDecl[Type baseType] returns [Type type]
@after{$type=$b.type;}
: a=arrayDeclDimension[$baseType] b=arrayDeclDimensionList[$a.type] ;

arrayDeclDimensionList[Type baseType] returns [Type type]
@init{Type t=$baseType;}
@after{$type=t;}
: a=arrayDeclDimension[$baseType] b=arrayDeclDimensionList[$a.type] {t=$b.type;} | ;

arrayDeclDimension[Type baseType] returns [Type type]
@init{ ArrayRange a=new ArrayRange($baseType); }
@after {$type=a;}
: LBRACKET arrayDeclValue[a] RBRACKET ;

arrayDeclValue[ArrayRange array] : 
      value[$array] | lowerValue[$array] | novalue[$array] ;


arrayRangeDecl[Type baseType] returns [Type type]
@after{$type=$b.type;}
: a=arrayRangeDeclDimension[$baseType] b=arrayRangeDeclDimensionList[$a.type] ;

arrayRangeDeclDimensionList[Type baseType] returns [Type type]
@init{Type t=$baseType;}
@after{$type=t;}
: a=arrayRangeDeclDimension[$baseType] b=arrayRangeDeclDimensionList[$a.type] {t=$b.type;} | ;

arrayRangeDeclDimension[Type baseType] returns [Type type]
@init{ ArrayRange a=new ArrayRange($baseType); }
@after {$type=a;}
: LBRACKET arrayRangeDeclValue[a] RBRACKET ;

arrayRangeDeclValue[ArrayRange array] : arrayDeclRangeValue[$array] | arrayDeclValue[$array];

arrayDeclRangeValue[ArrayRange array] 
@after{$array.setIndexRange($n1.val,$n2.val);}
: n1=intValue TWOPOINTS n2=intValue ;

value[ArrayRange array] 
@after{$array.setNbElements($n.val);}
: n=intValue;

lowerValue[ArrayRange array] 
@init{Boolean lt=true;}
@after{
  if (lt) {
    $array.setMaxNbElements($n.val-1);
  }
  else {
    $array.setMaxNbElements($n.val);
  }
}
: ( LT | LTE {lt=false;} )  n=intValue ;

novalue[ArrayRange array] 
@after{$array.setVariableSize();} :
;

intValue returns [int val] 
@after{$val=Integer.parseInt(n.getText());}
: n=IntegralNumber;

longValue returns [long val] 
@after{$val=Long.parseLong(n.getText());}
: n=IntegralNumber;


longValueWithUnit returns [long val] 
@init{long l;}
@after{$val=l;}
: n=IntegralNumber {l=Long.parseLong(n.getText());}
  (u=unit {l*=$u.val;} )?
;

unit returns [long val]
:  n=ID
     {
       String s=$n.getText().toLowerCase();
       if (s.equals("k")) {
         $val=1000;
       }
       else if (s.equals("kb")) {
         $val=1024;
       }
       else if (s.equals("m")) {
         $val=1000000;
       }
       else if (s.equals("mb")) {
         $val=1048576;
       }
       else if (s.equals("g")) {
         $val=1000000000;
       }
       else if (s.equals("gb")) {
         $val=1073741824;
       }
       else {
         compilerError.raiseError("unknown unit '"+s+"'");
         $val=1;
       }
     }
;


//========================================================
// Lexer
//========================================================

CPPComment : '//' ( ~('\n') )* { $channel=HIDDEN; } ;
 
ID     :       ( 'a'..'z' | 'A'..'Z' | '_' | '$')
               ( 'a'..'z' | 'A'..'Z' | '_' | '$' | '0'..'9' )*
        ;
 
StringLiteral : '"'
                ( ('\\' ~('\n'))=> Escape
                | ( '\r'  
                  | '\n' 
                  | '\\' '\n'
                  )
                | ~( '"' | '\r' | '\n' | '\\' )
                )*
                '"'
        ;
 
fragment Escape : '\\'
                (
                  ~('0'..'7' | 'x')
                | ('0'..'3')  ( DIGIT )*
                | ('4'..'7')  ( DIGIT )*
                | 'x' ( DIGIT | 'a'..'f' | 'A'..'F' )+
                )
        ;

//NUMBER : ('-'|)(DIGIT)+ ;
 
WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+    { $channel = HIDDEN; } ;
 
fragment DIGIT  : '0'..'9' ;


IntegralNumber:
        ('-'|) ( DIGIT )+ 
        ;

FloatingPointNumber :
      ('-'|)  ( DIGIT )+ '.' ( DIGIT )*
      ;


fragment Space:
        ( ' ' | '\t')
        ;

PREPROC_DIRECTIVE
        :
        '#'
        ( ( 'line' || ( (Space)+ '0'..'9') ) => LineDirective      
          | (~'\n')* 
        )
         { $channel=HIDDEN; }
        ;


fragment LineDirective :
  ('line')?  // this would be for if the directive started "#line",
             // but not there for GNU directives
  (Space)+
  IntegralNumber
  ( ((Space)+ ( StringLiteral | ID )) =>  (Space)+ ( StringLiteral | ID ) 
   |  )
   EatRestOfLine
  '\n'
;

fragment EatRestOfLine : ~('\n')* ;






