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

grammar KernelGenius;

options {
  output=AST;
  ASTLabelType=TNode;
}

tokens {
    // KernelGenius tokens
    PROGRAM    = 'program' ;
    KERNEL     = 'kernel' ;
    PROPERTIES = 'properties';
    RETURN     = 'return';
    
    C_SECTION_BEGIN = '${' ; 
    C_SECTION_END = '}$' ; 
 
    
    // C types
    FLOAT  = 'float';
    DOUBLE = 'double';
    LONG   = 'long';
    INT    = 'int';
    SHORT  = 'short';
    CHAR   = 'char';
    SIGNED = 'signed';
    UNSIGNED = 'unsigned';
    
    TYPEDEF = 'typedef';
    STRUCT  = 'struct';
    UNION   = 'union';
    ENUM    = 'enum';   
    
    // C Language Operators
    ASSIGN   = '=' ;
    COLON    = ':' ;
    COMMA    = ',' ;
    QUESTION = '?' ;
    SEMI     = ';' ;

    LPAREN   = '(' ;
    RPAREN   = ')' ;
    LBRACKET = '[' ;
    RBRACKET = ']' ;
    LCURLY   = '{' ;
    RCURLY   = '}' ;
    
    DOT = '.' ;

    LTE      = '<=' ;
    LT       = '<' ;
    GT       = '>' ;
    GTE      = '>=' ;
    
    
    // Virtual nodes
    PROGRAM;
    TYPEDEFNAME;
    NDeclaration;
}


@lexer::header {
  package parser;
  import parser.*;
}

@parser::header {
  package parser;
  import ir.base.*;
  import ir.types.*;
  import ir.types.c.*;
  import ir.literals.*;
  import ir.literals.c.*;
  import ir.symbolTable.*;
  import common.*;
  import java.util.LinkedList;
}
 
@parser::members {
  // ##################################################################
  // Error management
  // ##################################################################
  private CompilerError compilerError = new CompilerError();

  public void setCompilerError(CompilerError cp) {
    compilerError = cp;
  }
  
  public CompilerError getCompilerError() {
    return compilerError;
  }

  String programName;
  public void setProgramName(String s) {
    programName=s;
  }

  // Capture of syntax errors
  public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {
      String hdr = getErrorHeader(e);
      String msg = getErrorMessage(e, tokenNames);
      
      compilerError.raiseSyntaxError((MyToken)e.token,msg);
    }

  private TypeManager typeManager = new TypeManager();


  // ##################################################################
  // Symbol table
  // ##################################################################
  SymbolTable symbolTable=new SymbolTable();
  
  public SymbolTable getSymbolTable() {
    return symbolTable;
  }
  
  public boolean isTypedefName(String name) { 
   Symbol s=symbolTable.lookupName(name);
   if (s==null) { return false;}  
   if (s instanceof TypedefLabel) { return true; }
   return false;
  }

  // ******************************************************************
  // lookup(Tag)AndSetReference :
  //
  // Look for (tag) symbol named 'tn.getText()' in the scope hierarchy
  // of the symbol table (starting from the current one).
  // The symbol exists ?
  //   - YES: put a reference to the symbol as 'reference' attributes 
  //          of AST node 'tn' (attribute with name "REFERENCE")
  //   - NO : raise an error
  //
  // ******************************************************************
  private Symbol lookupAndSetReference(TNode tn, String str) {
    Symbol symbol = symbolTable.lookupName(str);
    if (symbol==null) {
      compilerError.raiseFatalError(tn, " symbol '" + str + "' not defined");
    }
    return(symbol);
  }

  
  // ******************************************************************
  // Add([Program|Kernel|KernelParameter|FunctionNode])Label:
  //
  // KernelGenius types
  //
  // ******************************************************************
  private void addProgramLabel(ProgramLabel symbol) {
    symbolTable.add(symbol.getName(),symbol,compilerError);
  }
  private void addAlgorithmLabel(AlgorithmLabel symbol) {
    symbolTable.add(symbol.getName(),symbol,compilerError);
  }
  private void addKernelLabel(KernelLabel symbol) {
    symbolTable.add(symbol.getName(),symbol,compilerError);
  }
  private void addKernelParameterLabel(KernelParameterLabel symbol) {
    symbolTable.add(symbol.getName(),symbol,compilerError);
  }
  
  // ******************************************************************
  // C types supported by KernelGenius
  // ******************************************************************
  private void addTypedefLabel(TypedefLabel symbol) {
    symbolTable.add(symbol.getName(),symbol,compilerError);
  }
  private void addTagLabel(TagLabel symbol) {
    symbolTable.add(symbol.getName(),symbol,compilerError);
  }
  private void addEnumConstantLabel(EnumConstantLabel symbol) {
    symbolTable.add(symbol.getName(),symbol,compilerError);
  }
  
}

@lexer::members {
  boolean sectionMode = false;
  
  LineObject lineObject = new LineObject();
  String originalSource = "";
  PreprocessorInfoChannel preprocessorInfoChannel = new PreprocessorInfoChannel();
  int tokenNumber = 0;
  boolean countingTokens = true;
  int deferredLineCount = 0;

  // override standard token emission
  public Token emit() {
    if ( countingTokens) {
      tokenNumber++;
    }
  
    MyToken t =
        new MyToken(input, state.type, state.channel,
                    state.tokenStartCharIndex, getCharIndex()-1);
                                        
    // Set the actual source information (filename + line)                
    //t.setLine(state.tokenStartLine);
    t.setLine(lineObject.line);
    t.setSource(lineObject.source);
    
    // Add other standard information
    t.setText(state.text);
    t.setCharPositionInLine(state.tokenStartCharPositionInLine);

    emit(t);
   
    // Additional custom actions
    lineObject.line += deferredLineCount;
    deferredLineCount = 0;
    
    return t;
  }

  public void setCountingTokens(boolean ct) {
    countingTokens = ct;
    if ( countingTokens ) {
      tokenNumber = 0;
    }
    else {
      tokenNumber = 1;
    }
  }

  public void setOriginalSource(String src) {
    originalSource = src;
    lineObject.setSource(src);
  }

  public void setSource(String src) {
    lineObject.setSource(src);
  }

  public String getOriginalSource(String src) {
    return originalSource;
  }

  public PreprocessorInfoChannel getPreprocessorInfoChannel() {
    return preprocessorInfoChannel;
  }

  public void setPreprocessingDirective(String pre) {
    preprocessorInfoChannel.addLineForTokenNumber( pre, new Integer(tokenNumber) );
  }
  
  // For immediate line increment
  public void newline() { 
    lineObject.newline();
  }
  
  // For line increment after the token creation
  public void deferredNewline() { 
    deferredLineCount++;
  }

}


//========================================================
// Grammar
//========================================================

program returns [Program program] 
@init{ $program=new Program(programName); }
@after{ 
//  $program.check(compilerError);
}
: pl=programStatementList[$program] 
  -> ^(PROGRAM $pl)
;

programStatementList[Program program]
: ( programStatement[$program] )+ ;

programStatement[Program program]
: ( kd=kernelDeclaration
    { $program.addKernel($kd.kernel); }
  | ns=nativeProgramStatement
    { $program.addNativeStatement($ns.section);}
  | d=declaration
    { $program.addDeclaration($d.tree);}
  )
  ;
 
nativeProgramStatement returns [String section]
@after{ $section=$cs.getText().substring(2,$cs.getText().length()-2); }
: cs=C_SECTION ;


//========================================================
// declarations
//========================================================

declaration :     
        db=declaration_body
        SEMI     
        -> ^(NDeclaration $db)
        ;

declaration_body
@init {
  TypeSpecifierQualifier specifier_qualifier= new TypeSpecifierQualifier();
  StorageClass storageclass=new StorageClass();
}
        :       ds=declSpecifiers[specifier_qualifier,storageclass]
                ( initDeclList[specifier_qualifier.getType($ds.tree,compilerError),storageclass,true] )?
        ;

initDeclList[Type specifier_qualifier_type,StorageClass storageclass, boolean globalScope]
        :       initDecl[specifier_qualifier_type,storageclass,globalScope]
                 ( COMMA! initDecl[specifier_qualifier_type,storageclass,globalScope] )*                
                ( COMMA! )?
        ;


initDecl[Type specifier_qualifier_type,StorageClass storageclass, boolean globalScope]
returns [Symbol symbol]
@init{
    Symbol decl_symbol=new Symbol(storageclass);
    $symbol=decl_symbol;
}
@after{
  // sets the symbol name
  decl_symbol.setName($d.id_node.getText());
   
   if (globalScope) {
     // Create the final symbol and put it in the symbol table
     if (decl_symbol.getStorageClass().isTypedef()) {       
       TypedefLabel td=new TypedefLabel(decl_symbol);
       addTypedefLabel(td);
     }
     else if (decl_symbol.getType().isArray()) {
       compilerError.raiseError($d.id_node, " declaration of array '" + decl_symbol.getName()
                                 + "' forbidden ");
     }
     else {
        compilerError.raiseError($d.id_node, " declaration of variable '" + decl_symbol.getName()
                                 + "' forbidden ");   
     }
   }
   else {
     if (decl_symbol.getStorageClass().isTypedef()) {
         compilerError.raiseError($d.id_node, " declaration of typedef  '" + decl_symbol.getName()
                                 + "' in parameter list forbidden ");  
     }
   }
}
        : d=declarator[decl_symbol,specifier_qualifier_type]
        ;


//========================================================
// kernel
//========================================================

kernelDeclaration returns [Kernel kernel]
@init{ Kernel kernel=null;}
@after{ 
//  kernel.completeCreation();
//  kernel.check(compilerError);
  $kernel=kernel;
}
: KERNEL^ id=ID 
          {
            kernel=new Kernel($id.getText(),$id.tree);
            addKernelLabel(new KernelLabel(kernel.getName(),kernel));
          }
  kernelParams[kernel]
  kernelBody[kernel]
  ;

kernelParams[Kernel kernel]  :
   LPAREN^ {symbolTable.pushScope($kernel.getName());}
   kernelParamList[kernel]
   RPAREN! { symbolTable.popScope();}
   ;

kernelParamList[Kernel kernel]  : kernelParamDeclaration[kernel] kernelParamList2[kernel] ;
kernelParamList2[Kernel kernel] : COMMA kernelParamDeclaration[kernel] kernelParamList2[kernel] | ;

kernelParamDeclaration[Kernel kernel] 
@after{kernel.addParameter($a.param);}
: a=kernelParamDeclarator ;

kernelParamDeclarator returns[KernelData param] 
@init {
//  Type t=null;
  TypeSpecifierQualifier specifier_qualifier= new TypeSpecifierQualifier();
  StorageClass storageclass=new StorageClass();
}
@after{
  KernelData kp=new KernelData($decl.symbol.getName(),$decl.symbol.getType(),$decl.symbol.getNode(), $ds.tree);
  kp.setAsKernelInput();
  if ($init.literal!=null) {
    kp.setInitializer($init.literal);
  }
  addKernelParameterLabel(new KernelParameterLabel($decl.symbol.getName(),kp,$decl.symbol.getType()));
  $param=kp;
}
: 
   ds=declSpecifiers[specifier_qualifier,storageclass]
   decl=initDecl[specifier_qualifier.getType($ds.tree,compilerError),storageclass,false]
   //scalar=scalarTypeSpecifier {t=$scalar.type;} 
   //id=ID  (array=arrayDecl[$scalar.type,false] {t=$array.type;})?
   
   (init=kernelParamInitializer[$decl.symbol.getType()])?
;

kernelParamInitializer[Type type] returns [Literal literal]
@after { $literal=$c.literal;}
: ASSIGN c=top_literal[$type]
;


kernelBody[Kernel kernel] : 
    LCURLY^ {symbolTable.pushScope($kernel.getName());}
    kernelBodyStatement[kernel]* 
    RCURLY! {symbolTable.popScope();} 
    ;
         
kernelBodyStatement[Kernel kernel] :  
  (
    a=kernelAlgo {$kernel.addAlgorithm($a.algo);} 
  | 
    kernelReturn[$kernel]
  ) SEMI ;

kernelReturn[Kernel kernel] : RETURN s=symbolReference 
{
  Symbol symb=$s.symbol;
  if (symb!=null) {
    if (symb instanceof KernelParameterLabel) {
      KernelData kd=((KernelParameterLabel)symb).getKernelData();
      kd.setAsKernelOutput();
      $kernel.addOutput(kd);
    }
    else if (symb instanceof AlgorithmLabel) {
      KernelData kd=((AlgorithmLabel)symb).getAlgorithm();
      kd.setAsKernelOutput();
      $kernel.addOutput(kd);
    }
    else {
      compilerError.raiseError($s.tree, "wrong type for kernel output '"+symb.getName()+"'");
    }
  }
}
;

numberValue returns [int val] 
@after{$val=Integer.parseInt(n.getText());}
: n=IntegralNumber;
//: n=NUMBER;



//========================================================
// Node
//========================================================

paramList[FunctionNode algo]  : param[$algo] paramList2[$algo] ;
paramList2[FunctionNode algo]  : COMMA param[$algo] paramList2[$algo] | ;
param[FunctionNode algo]  :  s=symbolReference 
{
  Symbol symb=$s.symbol;
  if (symb!=null) {
    if (symb instanceof KernelParameterLabel) {
      algo.addInputData(((KernelParameterLabel)symb).getKernelData());
    }
    else if (symb instanceof AlgorithmLabel) {
       algo.addInputData(((AlgorithmLabel)symb).getAlgorithm());
    }
    else {
      compilerError.raiseError($s.tree,"wrong type for node parameter '"+symb.getName()+"'");
    }
  }
}
;

kernelAlgo returns [FunctionNode algo] 
@init{ FunctionNode a=null; }
@after {
//  a.finalCheck(compilerError);
  $algo=a;
}
: t=kernelAlgoType {a=$t.algo;} id=ID 
  {
    a.setName($id.getText(), $id.tree);
    addAlgorithmLabel(new AlgorithmLabel($id.getText(),a));
    }
  algoParams[a]
  algoBody[a]?
  ;

algoParams[FunctionNode algo] :
  LPAREN { symbolTable.pushScope($algo.getName());}
  paramList[$algo] 
  RPAREN {symbolTable.popScope();}
  ;

kernelAlgoType returns [FunctionNode algo] 
@after{
  $algo=FunctionNode.getNewAlgorithmFromName($id.getText(),$id.tree,compilerError);
  $algo.setOutputBaseCType($type.type,$type.tree);
}
: id=ID LT type=typeName GT;

algoBody[FunctionNode algo] : 
  LCURLY{ symbolTable.pushScope($algo.getName());}
  propertyStatement[$algo]*
  RCURLY {symbolTable.popScope();}
;

propertyStatement[FunctionNode algo] :
    DOT decl=propertyDeclarator 
    p=propertyDeclaratorParam[$algo]
    (
      propertyAssignment[$algo,$decl.string,$p.paramList]
    |
      { $algo.setProperty($decl.string,$p.paramList,$decl.tree,compilerError); }
    )
    SEMI ;

propertyDeclaratorParam[FunctionNode algo] returns [List<KernelData> paramList]
@init { $paramList=new LinkedList<KernelData>(); }
: ( 
   LPAREN
   id=ID { 
    // TODO: may preferably use the symbol table (?)
    KernelData kd=$algo.getInputData($id.getText());
    if (kd==null) { compilerError.raiseError($id.tree,"property attribute '"+$id.getText()+"' is not a node parameter "); }
    else { $paramList.add(kd); }
   }
   ( COMMA id2=ID
      { 
        KernelData kd=$algo.getInputData($id2.getText());
        if (kd==null) { compilerError.raiseError($id2.tree,"property attribute '"+$id2.getText()+"' is not a node parameter "); }
        else { $paramList.add(kd); }
      }
   )*
   RPAREN
  )?
 ;

propertyDeclarator returns [String string] 
@after { $string=$id.getText(); }
  : id=ID 
  ;

propertyAssignment[FunctionNode algo, String name, List<KernelData> paramList]  :
  ASSIGN propertyInit[$algo,$name,$paramList]
  ;
  
propertyInit[FunctionNode algo, String name, List<KernelData> paramList] : 
     pi=propertyInitID       {$algo.setPropertyWithIdentifier($name,$paramList,$pi.name,$pi.tree,compilerError);}
   | lnt=literalNoType       {$algo.setPropertyWithLiteral($name,$paramList,$lnt.literal,$lnt.tree,compilerError);}
   | ad=arrayDecl[IntegerScalar.Tsint,true] {$algo.setPropertyWithArrayRange($name,$paramList,$ad.type,$ad.tree,compilerError);}
   | ps=propertyInitString   {$algo.setPropertyWithString($name,$paramList,$ps.string,$ps.tree,compilerError);}
   | pc=propertyInitCSection {$algo.setPropertyWithString($name,$paramList,$pc.string,$pc.tree,compilerError);}
   ;
   
propertyInitID returns [String name]
@after { $name=$id.getText(); }
: id=ID ;

propertyInitString returns [String string]
@after {$string=$sl.getText().substring(1,$sl.getText().length()-1);}
: sl=StringLiteral ;

propertyInitCSection returns [String string]
@after {$string=$scs.getText().substring(2,$scs.getText().length()-2);}
: scs=C_SECTION ;


// Returns the symbol from the symbol table and raise an error if the symbol
// is not found
symbolReference returns [Symbol symbol]
@init{$symbol=null;}
:  id=ID 
{
  $symbol=symbolTable.lookupName($id.getText());
  if ($symbol==null) {
    compilerError.raiseError($id.tree, "Unknown symbol '"+$id.getText()+"'");
  }
}
;

// TODO: should be removed
typeDefinition returns [Type type] :
s=scalarTypeSpecifier 
 ( a=arrayDecl[$s.type,true]
     {$type=$a.type;}
  |  {$type=$s.type;}
 )
;


//========================================================
// Types
//========================================================

//=================== scalar type ========================

declSpecifiers[TypeSpecifierQualifier specifier_qualifier, StorageClass storageclass]
        :
          (  storageClassSpecifier[storageclass]
          | typeSpecifier[specifier_qualifier]
          )+
        ;
        
specifierQualifierList[TypeSpecifierQualifier specifier_qualifier] 
        : ( typeSpecifier[specifier_qualifier] )+
        ;
        
storageClassSpecifier[StorageClass storageclass] 
        :   t=TYPEDEF   {  storageclass.setTypedef($t.tree,compilerError); }
        ;

typeSpecifier[TypeSpecifierQualifier specifier_qualifier] 
        :       
            c=CHAR      { specifier_qualifier.setChar    ($c.tree,compilerError); }
        |   s=SHORT     { specifier_qualifier.setShort   ($s.tree,compilerError); }
        |   i=INT       { specifier_qualifier.setInt     ($i.tree,compilerError); }
        |   l=LONG      { specifier_qualifier.setLong    ($l.tree,compilerError); }
        |   f=FLOAT     { specifier_qualifier.setFloat   ($f.tree,compilerError); }
        |   d=DOUBLE    { specifier_qualifier.setDouble  ($d.tree,compilerError); }
        |   si=SIGNED   { specifier_qualifier.setSigned  ($si.tree,compilerError); }
        |   un=UNSIGNED { specifier_qualifier.setUnsigned($un.tree,compilerError); }

        |   tag=structorUnionSpecifier
          {
            if ($tag.isStruct) {
              specifier_qualifier.setStruct($tag.tree,compilerError);
            }
            else {
              specifier_qualifier.setUnion($tag.tree,compilerError);            
            }
            specifier_qualifier.setSubType($tag.symbol.getType());
          }
         |   en=enumSpecifier
          {
             specifier_qualifier.setEnum($en.tree,compilerError);
             specifier_qualifier.setSubType($en.symbol.getType());       
          }                 
        |   td=typedefName
          {
            specifier_qualifier.setTypedefName($td.tree,compilerError);
            specifier_qualifier.setSubType($td.symbol.getType());
          }        
        ;

typedefName returns [Symbol symbol]

        :   { isTypedefName ( input.LT(1).getText() ) }? =>
            (
              i=ID 
              { $symbol=lookupAndSetReference($i.tree, $i.getText()); }
              -> ^(TYPEDEFNAME $i)
            )         
        ;

structorUnionSpecifier returns [TagLabel symbol, boolean isStruct]
@init {
  $symbol=null;
  StructOrUnion structorunion_type=null;
  boolean error=false;
}
        : (
            STRUCT^ {
              structorunion_type=(StructOrUnion)new Struct();
              $isStruct=true;
            }
          | UNION^ {
              structorunion_type=(StructOrUnion)new Union();
              $isStruct=false;
            }         
          )
          ( 
            ( ID LCURLY )=> 
              id=ID LCURLY  
               {
                 Symbol symb=symbolTable.lookupName($id.getText());
                 if (symb!=null) {
                    compilerError.raiseError($id.tree, " symbol '" + $id.getText()
                                             + "' redefined, previous declaration line "
                                             + symb.getNode().getLine());
                    error=true;
                 }
                 // Create a new symbol (by default incomplete)
                 $symbol = $isStruct? new StructLabel($id.getText()): new UnionLabel($id.getText()); 
                 $symbol.setNode($id.tree);
                 $symbol.setType(structorunion_type);
                 addTagLabel($symbol);
               }                          
              ( structOrUnionDeclarationList[structorunion_type,$isStruct] )? RCURLY
              { if (!error) { structorunion_type.setComplete(); } }
            | LCURLY  
              {
                 String declName = symbolTable.getNewName();
                 $symbol = $isStruct? new StructLabel(declName): new UnionLabel(declName); 
                 $symbol.setType(structorunion_type);
                 addTagLabel($symbol);             
              }
              ( structOrUnionDeclarationList[structorunion_type,$isStruct] )? RCURLY
              { if (!error) { structorunion_type.setComplete(); } }             
            | id_ref=ID
              {
                Symbol tag_symbol=symbolTable.lookupName($id_ref.getText());
                if (tag_symbol==null) {
                  compilerError.raiseError($id_ref.tree, ($isStruct?" struct '":" union '")+
                                           $id_ref.getText() + "' non defined ");  
                  error=true;                                             
                }
                else {
                  if ( ( ( $isStruct) && (!(tag_symbol instanceof StructLabel)) ) ||
                       ( (!$isStruct) && (!(tag_symbol instanceof UnionLabel )) ) ) {
                   compilerError.raiseError($id_ref.tree,"'"+$id_ref.getText()+"'"+
                                            " not defined as "+ ($isStruct?"struct":"union"));
                   error=true;                                             
                  }
                  else {
                    $symbol=(TagLabel)tag_symbol;
                  }
                }               
              }
           )
        ;

structOrUnionDeclarationList[StructOrUnion structorunion_type, boolean isStruct]
        : ( s=structOrUnionDeclaration[structorunion_type,isStruct] )+
        ;

structOrUnionDeclaration[StructOrUnion structorunion_type, boolean isStruct]
@init{
    TypeSpecifierQualifier specifier_qualifier=new TypeSpecifierQualifier();
}
        : ds=specifierQualifierList[specifier_qualifier] 
         // Disable unamed fields
         structDeclaratorList[specifier_qualifier.getType($ds.tree,compilerError),structorunion_type,isStruct]
         ( SEMI! )+
        ;

structDeclaratorList[Type specifier_qualifier_type, StructOrUnion structorunion_type, boolean isStruct]
        : structDeclarator[specifier_qualifier_type,structorunion_type,isStruct]
        ( COMMA! structDeclarator[specifier_qualifier_type,structorunion_type,isStruct] )*
        ;

structDeclarator[Type specifier_qualifier_type, StructOrUnion structorunion_type, boolean isStruct]
@init {
   Symbol decl_symbol=new Symbol();
}
@after{
  // sets the symbol name
  decl_symbol.setName($d.id_node.getText());
   
   Type fieldType=decl_symbol.getType();
   if (fieldType.isVoid()) {
       compilerError.raiseError($d.id_node,"field '"+$d.id_node.getText()+"' declared void");
   }
   // Adds a new field to the struct or union
   if (structorunion_type.addField($d.id_node.getText(),fieldType)==false) {
      compilerError.raiseError($d.id_node,"duplicate member '" +$d.id_node.getText()+"'" );      
   }
}
        : d=declarator[decl_symbol,specifier_qualifier_type]
        ;


enumSpecifier returns [EnumLabel symbol]
@init {
  $symbol=null;
  Enumerate enum_type=new Enumerate();
  boolean error=false;
}
       :       
       ENUM^
       ( 
         ( ID LCURLY )=>
          id=ID LCURLY
            {
               Symbol symb=symbolTable.lookupName($id.getText());
               if (symb!=null) {
                  compilerError.raiseError($id.tree, " symbol '" + $id.getText()
                                           + "' redefined, previous declaration line "
                                           + symb.getNode().getLine());
                  error=true;
               }
               // Create a new symbol (by default incomplete)
               $symbol = new EnumLabel($id.getText()); 
               $symbol.setNode($id.tree);
               $symbol.setType(enum_type);
               addTagLabel($symbol);
             }     
           enumList[$symbol,enum_type] RCURLY
             { if (!error) { enum_type.setComplete(); } }
                  
         | LCURLY 
             {
               String declName = symbolTable.getNewName();
                $symbol =  new EnumLabel(declName); 
                $symbol.setType(enum_type);
                addTagLabel($symbol);             
              }             
            enumList[$symbol,enum_type] RCURLY
              { if (!error) { enum_type.setComplete(); } }             
             
          | id_ref=ID
             {
                Symbol tag_symbol=symbolTable.lookupName($id_ref.getText());
                if (tag_symbol==null) {
                  compilerError.raiseError($id_ref.tree, " enum '"+
                                           $id_ref.getText() + "' non defined ");  
                  error=true;                                             
                }
                else {
                  if ( !(tag_symbol instanceof EnumLabel) ) {
                   compilerError.raiseError($id_ref.tree,"'"+$id_ref.getText()+"' not defined as enum");
                   error=true;                                             
                  }
                  else {
                    $symbol=(EnumLabel)tag_symbol;
                  }
                }               
              }
           )
        ;
        
// TODO: manage enum values ?
enumList[Symbol parent_symbol, Enumerate enum_type]
   :
   enumerator[parent_symbol,enum_type] ( COMMA! enumerator[parent_symbol,enum_type] )* ( COMMA! )?
   ;

enumerator[Symbol parent_symbol, Enumerate enum_type]
@after{
  EnumConstantLabel enum_field_symbol = new EnumConstantLabel($id.getText());
  enum_field_symbol.setType(IntegerScalar.Tsint);
  //enum_field_symbol.setValue(counter);
  //new_counter=counter+1;
  enum_field_symbol.setNode($id.tree);
  
  addEnumConstantLabel(enum_field_symbol);
  
  enum_type.addElement($id.getText());
}
: 
   id=ID (ASSIGN n=numberValue)?
;


declarator[Symbol symbol,Type input_type] returns [TNode id_node]
@after{
  // Work-around for code regeneration: keep the tree of the base type
  
}
        :      
          id=ID  
          { symbol.setType(input_type); 
            $id_node=$id.tree;
          }
          ( ad=arrayDecl[input_type,false] 
            { 
              symbol.setType($ad.type); 
            }
          ) ?
        ;


scalarTypeSpecifier returns [Type type]: 
  CHAR {$type= IntegerScalar.Tschar;}
| SHORT {$type= IntegerScalar.Tsshort;}
| INT {$type= IntegerScalar.Tsint;}
| FLOAT {$type= FloatingPointScalar.Tfloat;}
| DOUBLE {$type= FloatingPointScalar.Tdouble;}
;



// Type declaration
typeName returns [Type type]
@init {
  TypeSpecifierQualifier specifier_qualifier= new TypeSpecifierQualifier();
}
  :
     ds=specifierQualifierList[specifier_qualifier]
     ad=nonemptyAbstractDeclarator[specifier_qualifier.getType($ds.tree,compilerError)] 
     { $type=$ad.type;} 
  ;

nonemptyAbstractDeclarator[Type input_type] returns [Type type]
@init {
  $type=input_type;
}
        :      
          ( ad=arrayDecl[input_type,false] 
            { $type=$ad.type; }
          ) ?
        ;


//================= standard array type =====================

arrayDecl[Type baseType, boolean range] returns [ArrayRange type]
@init{
  ChildType.Marker marker=new ChildType.Marker();
  ArrayRange t=null,t_current=null;
}
: 
  ( 
    { t_current=t; }
    a=arrayDeclDimension[marker,range]
    {
      t=$a.type;
      if (t_current==null) { $type=t; }
      else { t_current.setChild(t); }
    }
  )+
  {
    // Set the base array type
    ChildType parent=((ChildType)(marker.getParent()));
    parent.setChild(baseType);
  }
  ;

arrayDeclDimension[Type baseType, boolean range] returns [ArrayRange type]
@init{ ArrayRange a=new ArrayRange($baseType); }
@after {$type=a;}
: LBRACKET^ arrayDeclValue[a,range] RBRACKET ;

arrayDeclValue[ArrayRange array, boolean range] : 
      value[$array] | 
      idValue[$array] | 
      { range }? rangeValue[$array];

value[ArrayRange array] 
@after{$array.setNbElements($n.val);}
: n=numberValue ;

idValue[ArrayRange array] 
: s=symbolReference 
  {
    if (!($s.symbol instanceof KernelParameterLabel)) {
        compilerError.raiseError($s.tree,"array size specifier variable must be a kernel parameter");
    }
    else {
      $array.setNbElements(((KernelParameterLabel)$s.symbol).getKernelData(), compilerError);
    }
  }
;

rangeValue[ArrayRange array] 
@after{
  if ($n1.val>$n2.val) {
    compilerError.raiseError($c.tree,"invalid array index range ("+$n1.val+">"+$n2.val+")");
  }
  $array.setIndexRange($n1.val,$n2.val);
}
: n1=numberValue c=COLON n2=numberValue ;



//========================================================
// Literals
//========================================================

//--- Self-contained literals (do not take any type as input) ---

literalNoType returns [Literal literal] :
    cl   = compoundLiteral     {$literal=$cl.literal;}
  | ial  = intArrayLiteral     {$literal=$ial.literal;}
  | slnt = scalarLiteralNoType {$literal=$slnt.literal;}
 ;

compoundLiteral returns [Literal literal]
@after { $literal=$c.literal;}
: s=LPAREN t=typeDefinition RPAREN c=top_literal[$t.type] ;

intArrayLiteral returns [Literal literal]
@init { Array t=new Array(IntegerScalar.Tsint); }
@after { $literal=$c.literal;}      
: c=curlyLiteral[t,0] ;

scalarLiteralNoType returns [ScalarLiteral literal] 
:   n=integralLiteral {
      $literal=new IntegerLiteral($n.etype.getConstantIntegralValue().longValue(),(IntegerScalar)$n.etype.getType());
    }
  | m=floatingPointLiteral { 
      $literal=new FloatingPointLiteral($m.etype.getConstantFloatingpointValue(),(FloatingPointScalar)$m.etype.getType());
    }  
;


//--- Initializer literals (for compound literals or declaration) ---
top_literal[Type type] returns [Literal literal]
@after{ $literal=$l.literal;} 
:
   l=literal[type,0]
;

literal[Type type, int level] returns [Literal literal] 
@init{ Literal l=null; }
@after{ $literal=l;} 
:    
     a=scalarLiteral[$type]       {l=$a.literal;}
   | b=curlyLiteral[$type,level]  {l=$b.literal;}
   | c=rangeLiteral[$type]        {l=$c.literal;}
;


//=================== range literal ========================

rangeLiteral[Type type] returns [RangeLiteral literal]
@init{ RangeLiteral l=null; }
@after{ $literal=l;}
: LBRACKET^ n1=numberValue c=COLON n2=numberValue RBRACKET 
{
  if ($type instanceof IntegerScalar) {
    IntegerScalar is=(IntegerScalar)$type;
    if ($n1.val>$n2.val) {
      compilerError.raiseError($c.tree, "invalid range literal");
    }
    l=new RangeLiteral($n1.val,$n2.val,is);
  }
  else {
    compilerError.raiseError($c.tree, "range literal can only be used with integral scalar types");
  }
}
;

//=================== curly literal ========================
curlyLiteral[Type type, int level] returns [Literal literal]
@init{
  // Use this object as a trick for coping with a ANTLR bug
  Boolean first=true;
}
: LCURLY^ 
  (
  { type.isArray() }? 
    a=arrayLiteralElementList[(Array)(type.unqualify()),level,first] { $literal=$a.literal; }
  | {type.isStruct()}?
    b=structLiteralElementList[(Struct)(type.unqualify()),level,first] { $literal=$b.literal; }
  | {type.isUnion()}?
    c=unionLiteralElementList[(Union)(type.unqualify()),level,first] { $literal=$c.literal; }
  | 
    d=scalarLiteralElementList[type.unqualify(),first] { $literal=$d.literal; }
  )  
  RCURLY
;

arrayLiteralElementList[Array type, int level, Boolean first] returns [AggregateLiteral literal]
@init{
  int index=0;
  int nb_elements=0;
  Type subtype=type.getElementType();
  $literal=new ArrayLiteral(type);
}
@after{
    // Complete the incomplete array declaration
    if ((level==0) && (!type.hasSizeSpecifier())) {
        type.setNbElements(nb_elements);
    }
}
:
  ( ( {!first}? COMMA | )
     subLiteral=literal[subtype,level+1]
      { 
        // Check that we are not out of bound (only for arrays with
        // size defined)
        if (type.hasSizeSpecifier()) {
          if (index>=type.getNbElements()) {
            compilerError.raiseWarning($subLiteral.tree, "excess elements in array initializer");
          }
        }
        $literal.addAtIndex(index++,$subLiteral.literal); 
        if (index>nb_elements) {
          nb_elements=index;
        }
        first=false;
      } 
  )*
  ;

structLiteralElementList[Struct type, int level, Boolean first] returns [AggregateLiteral literal]
@init{
  Type subtype=null;
  int nb_elements=0;
  int fieldNumber=0;
  TNode fieldTree=null;
  String field_name=null;
  $literal=new StructOrUnionLiteral(type);
}
:
  ( ( {!first}? COMMA | )
    ( 
      (
        id1=ID COLON
         {
           field_name=$id1.text;
           fieldTree=$id1.tree;
          }
       | DOT id2=ID ASSIGN
          {
            field_name=$id2.text;
            fieldTree=$id1.tree;            
          }
       )
       {
          fieldNumber=type.getFieldNumber(field_name);     
          if (fieldNumber<0) {
            compilerError.raiseError(fieldTree,"unknown field '" + field_name +"' specified in struct initializer");
          }
       }
    )?  
    { subtype=type.getFieldType(fieldNumber); }
    ( 
     {subtype!=null}?  subLiteral=literal[subtype,level+1]
       { $literal.addAtIndex(fieldNumber,$subLiteral.literal); } 
     | literal[null,level+1]
       { compilerError.raiseWarning("excess elements in struct initializer"); }
    )
    {
      fieldNumber++;
      nb_elements++;
      first=false;
    }
  )*
  ;

unionLiteralElementList[Union type, int level, Boolean first] returns [AggregateLiteral literal]
@init{
  Type subtype=null;
  int count=0;
  int nb_elements=0;  
  int fieldNumber=0;
  TNode fieldTree=null;
  String field_name=null;
  $literal=new StructOrUnionLiteral(type);
}
:
  ( ( {!first}? COMMA | )
    ( 
      (
        id1=ID COLON
         {
           field_name=$id1.text;
           fieldTree=$id1.tree;
          }
       | DOT id2=ID ASSIGN
          {
            field_name=$id2.text;
            fieldTree=$id1.tree;            
          }
       )
       {
          fieldNumber=type.getFieldNumber(field_name);     
          if (fieldNumber<0) {
            compilerError.raiseError(fieldTree,"unknown field '" + field_name +"' specified in struct initializer");
          }
       }
    )?  
    { subtype=type.getFieldType(fieldNumber); }
    ( 
     {subtype!=null}?  subLiteral=literal[subtype,level+1]
       {
         if (count>0) {
           compilerError.raiseWarning("excess elements in union initializer");
         }
         $literal.addAtIndex(fieldNumber,$subLiteral.literal);
        } 
     | literal[null,level+1]
       { compilerError.raiseWarning("excess elements in union initializer"); }
    )
    {
      count++;
      fieldNumber++;
      nb_elements++;
      first=false;
    }
  )*
  ;

scalarLiteralElementList[Type type, Boolean first] returns [Literal literal]
@init{
  int count=0;
  int nb_elements=0;
  $literal=null;
}
:
  ( ( {!first}? COMMA | )
    ( id1=ID COLON 
      { compilerError.raiseError($id1.tree,"field name not in struct or union initializer"); }
      | DOT id2=ID ASSIGN
      { compilerError.raiseError($id2.tree,"field name not in struct or union initializer"); }
    )?
    ( 
     {count==0}?  subLiteral=literal[type,0]
       {
         $literal=$subLiteral.literal;
       } 
     | literal[null,0]
       { compilerError.raiseWarning("excess elements in scalar initializer"); }
    )
    {
      count++;
      nb_elements++;
      first=false;
    }
  )*
  ;
 

//=================== scalar literal ========================

// TODO: manage all scalar literals
scalarLiteral[Type type] returns [ScalarLiteral literal] 
@init{
  TNode t=null;
}
@after{
  typeManager.checkAssignOperands(t,compilerError,type,$literal.getType(),"initialization");  
}
:   n=integralLiteral
    {
      t=$n.tree;
      $literal=new IntegerLiteral($n.etype.getConstantIntegralValue().longValue(),(IntegerScalar)$n.etype.getType());
    }
  | m=floatingPointLiteral
    { 
      t=$m.tree;
      $literal=new FloatingPointLiteral($m.etype.getConstantFloatingpointValue(),(FloatingPointScalar)$m.etype.getType());
    }  
;

integralLiteral returns [EnrichedType etype] 
@after{
  $etype=TypeManager.getIntegralNumberEnrichedType($n.tree,compilerError,$n.getText());
}
: n=IntegralNumber;

floatingPointLiteral returns [EnrichedType etype] 
@after{
 $etype=TypeManager.getFloatingPointNumberEnrichedType(compilerError,$n.getText());
}
: n=FloatingPointNumber;









//################################################################################
// Lexer
///################################################################################

CPPComment : '//' ( ~('\n') )* { $channel=HIDDEN; } ;
 
ID     :       ( 'a'..'z' | 'A'..'Z' | '_' | '$')
               ( 'a'..'z' | 'A'..'Z' | '_' | '$' | '0'..'9' )*
        ;
 
StringLiteral : '"'
                ( ('\\' ~('\n'))=> Escape
                | ( '\r'  
                  | '\n' 
                  | '\\' '\n'
                  ) { newline(); }
                | ~( '"' | '\r' | '\n' | '\\' )
                )*
                '"'
        ;
 
fragment Escape : '\\'
                (
                  ~('0'..'7' | 'x')
                | ('0'..'3')  ( Digit )*
                | ('4'..'7')  ( Digit )*
                | 'x' ( Digit | 'a'..'f' | 'A'..'F' )+
                )
        ;
 
Whitespace : ( ('\t' | ' ' | '\u000C')
               | '\r\n'          { newline(); }  
               | ('\r' | '\n')   { newline(); }    
             )   { $channel = HIDDEN; }
            ;
 
fragment Space:
        ( ' ' | '\t')
        ;
        
 
fragment
VARARGS:;
 
 
 
//================== Numeric Constants  ===================
 
fragment
Digit :  '0'..'9'
      ;

fragment IntSuffix
        : (   'L' | 'l' | 'U' | 'u'
        // Complex numbers ?
            | 'I' | 'i' | 'J' | 'j' 
          ) 
        ;
fragment FloatSuffix
        : 'F' | 'f' | 'L' | 'l'
        ;

fragment Exponent
        :  ( 'e' | 'E' ) ( '+' | '-' )? ( Digit )+
        ;

fragment BinaryExponent
        : ( 'p' | 'P' ) ( '+' | '-' )? ( Digit )+
        ;

fragment HexadecimalPrefix :
  '0' ( 'x' | 'X' )
  ;
fragment HexadecimalDigit :
   'a'..'f' | 'A'..'F' | Digit
  ;

// Floating point Literals
fragment DecimalFloatingConstant1 : 
      ( Digit )+ '.' ( Digit )* ( Exponent )? ( FloatSuffix )?
      ;
fragment DecimalFloatingConstant2 : 
      '.' ( Digit )+ ( Exponent )? ( FloatSuffix )?
      ;
fragment DecimalFloatingConstant3 : 
      ( Digit )+ Exponent ( FloatSuffix )?
      ;
fragment HexadecimalFloatingConstant1 : 
      HexadecimalPrefix
      ( HexadecimalDigit )+
      '.' ( HexadecimalDigit )*
      ( BinaryExponent )?
      ( FloatSuffix )?
      ;
fragment HexadecimalFloatingConstant2 : 
      HexadecimalPrefix
      '.' ( HexadecimalDigit )+
      ( BinaryExponent )?
      ( FloatSuffix )?
      ;
fragment HexadecimalFloatingConstant3 : 
      HexadecimalPrefix
      ( HexadecimalDigit )+
      BinaryExponent
      ( FloatSuffix )?
      ;

fragment
FloatingPointNumber:;

IntegralNumber
        :
  // includes the sign since C expressions are not supported by the parser
        '-'?
        (
  // Floating point Literals
          ( ( Digit )+ '.' )
          => DecimalFloatingConstant1
            { $type = FloatingPointNumber; }
        | ( '.' ( Digit )+ )
           => DecimalFloatingConstant2
           { $type = FloatingPointNumber; }
        | ( ( Digit )+ Exponent )
           => DecimalFloatingConstant3
           { $type = FloatingPointNumber; }
        | ( HexadecimalPrefix ( HexadecimalDigit )+ '.' )
           => HexadecimalFloatingConstant1
           { $type = FloatingPointNumber; }
        | ( HexadecimalPrefix '.' ( HexadecimalDigit )+ )
           => HexadecimalFloatingConstant2
           { $type = FloatingPointNumber; }
        | ( HexadecimalPrefix ( HexadecimalDigit )+ BinaryExponent )
           => HexadecimalFloatingConstant3
           { $type = FloatingPointNumber; }

  // Other
        | ( '...' )=> '...' { $type = VARARGS;     }
        |  '.'              { $type = DOT; }

  // Integral Literals
        | '0' ( '0'..'7' )*    ( IntSuffix | FloatSuffix )*
        | '1'..'9' ( Digit )*  ( IntSuffix | FloatSuffix )*
        |  HexadecimalPrefix ( HexadecimalDigit )+ ( IntSuffix )*
        )
        ;


//================== Native sections  ===================

fragment DUAL : . ( ( '\r' | '\n' ) { deferredNewline(); } | ~( '\r' | '\n' ));

C_SECTION : C_SECTION_BEGIN
             ( 
               ( '\r' | '\n' ) { deferredNewline(); }
              | ('}' ~('$')) => DUAL
              | ~( '}' | '\r' | '\n' )
             )*
            C_SECTION_END ;



//================== Preprocessing ===================
PREPROC_DIRECTIVE
        :
        '#'
        ( ( 'line' || ( (Space)+ '0'..'9') ) => LineDirective      
          | (~'\n')* { setPreprocessingDirective(state.text); }
        )
         { $channel=HIDDEN; }
        ;


fragment LineDirective 
@init{
  boolean oldCountingTokens = countingTokens;
  countingTokens = false;
}
:
  {
    lineObject = new LineObject();
    deferredLineCount = 0;
  }
  ('line')?  // this would be for if the directive started "#line",
             // but not there for GNU directives
  (Space)+
  n=IntegralNumber { lineObject.setLine(Integer.parseInt($n.text));  } 
    (Space)+
  ( fn=StringLiteral
    {
      try { lineObject.setSource($fn.text.substring(1,$fn.text.length()-1)); } 
      catch (StringIndexOutOfBoundsException e) { /*not possible*/ } 
    }
    | fi=ID { lineObject.setSource($fi.text); }
  )?
  (
      '1'      { lineObject.setEnteringFile(true); }
    | '2'      { lineObject.setReturningToFile(true); }
    | '3'      { lineObject.setSystemHeader(true); }
    | '4'      { lineObject.setTreatAsC(true); }
    |  (~('\r\n' | '\r' | '\n'))
  )*
  ('\r\n' | '\r' | '\n')
  {
    preprocessorInfoChannel.addLineForTokenNumber(new LineObject(lineObject), new Integer(tokenNumber));
    countingTokens = oldCountingTokens;   
  }
;

//fragment EatRestOfLine : ~('\n')* ;






