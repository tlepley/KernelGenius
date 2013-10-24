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

/* Type specifier and qualifier list used of the parser */

package ir.symbolTable;

import parser.TNode;
import ir.types.Type;
import ir.types.c.Void;
import ir.types.c.Array;
import ir.types.c.Bool;
import ir.types.c.FloatingPointScalar;
import ir.types.c.IntegerScalar;
import ir.types.c.Qualifier;
import common.CompilerError;


public class TypeSpecifierQualifier {

  // Type specifiers
  public enum BaseTypeSpecifier {
    NO,
    VOID,BOOL,CHAR,INT,FLOAT,DOUBLE,
    VECTOR,
    COMPLEX,IMAGINARY,
    STRUCT,UNION,ENUM,
    TYPEDEFNAME,TYPEOF,
    VALIST
  };
  public enum SignSpecifier {
    NO, SIGNED, UNSIGNED
  };
  public enum SizeSpecifier {
    NO, SHORT, LONG, LONG_LONG
  };

  // Type qualifier
  private boolean const_type_qualifier       = false;
  private boolean volatile_type_qualifier    = false;
  private boolean restrict_type_qualifier    = false;

  //Type specifiers
  private BaseTypeSpecifier base_type_specifier = BaseTypeSpecifier.NO;
  private SizeSpecifier size_type_specifier 	= SizeSpecifier.NO;
  private SignSpecifier sign_type_specifier 	= SignSpecifier.NO;


  // Sub-type for tags, typedefnames and typeof 
  private Type sub_type = null; // type



  //==================================================================
  // Type specifier management
  //==================================================================

  // Sets the sub-type of the specifier: type of a type tag, a typedefnames
  // or a typeof 
  public void setSubType(Type t) {
    sub_type=t;
  }


  // Check if no sign and size specifier
  private void CheckNoSizeSignTypeSpecifier(TNode tn,CompilerError cp) {
    if (
	(size_type_specifier!=SizeSpecifier.NO) ||
	(sign_type_specifier!=SignSpecifier.NO)
	) {
      cp.raiseError(tn,"long, short, signed or unsigned invalid");
    }
  }


  // Check specifiers and return the type
  public Type getType(TNode tn,CompilerError cp) {
    Type type=null;

    switch(base_type_specifier) {
    case VOID:
      CheckNoSizeSignTypeSpecifier(tn,cp);
      type=Void.Tvoid;
      break;
      
    case BOOL:
      CheckNoSizeSignTypeSpecifier(tn,cp);
      type=Bool.Tbool;
      break;
      
    case CHAR:
      if (size_type_specifier!=SizeSpecifier.NO) {
	cp.raiseError(tn,"long or short specified with char");
      } 
      if(sign_type_specifier==SignSpecifier.UNSIGNED) {
	type=IntegerScalar.Tuchar;
      }
      else {
	type=IntegerScalar.Tschar;
      }
      break;
    
    case NO:
    case INT:
      // Manage the base type specifier
      switch(size_type_specifier) {
      case SHORT:
	if(sign_type_specifier==SignSpecifier.UNSIGNED) {
	  type=IntegerScalar.Tushort;
	}
	else {
	  type=IntegerScalar.Tsshort;
	}
	break;
      case LONG:
	if(sign_type_specifier==SignSpecifier.UNSIGNED) {
	  type=IntegerScalar.Tulong;
	}
	else {
	  type=IntegerScalar.Tslong;
	}
	break;
      case LONG_LONG:
	if (!Type.getSourceABI().isLongLongAllowed()) {
	  cp.raiseError(tn,"long long not allowed");
	}
	if(sign_type_specifier==SignSpecifier.UNSIGNED) {
	  type=IntegerScalar.Tulonglong;
	}
	else {
	  type=IntegerScalar.Tslonglong;
	}
	break;
      default:
	if(sign_type_specifier==SignSpecifier.UNSIGNED) {
	  type=IntegerScalar.Tuint;
	}
	else {
	  type=IntegerScalar.Tsint;
	}
      }
      break;
      
    case FLOAT:
      if (size_type_specifier!=SizeSpecifier.NO) {
	cp.raiseError(tn,"long or short specified with floating type, the only valid combination is `long double'");
      }
      if (sign_type_specifier!=SignSpecifier.NO) {
	cp.raiseError(tn,"signed or unsigned invalid with floating type");
      }
      type=FloatingPointScalar.Tfloat;
      break;
      
    case DOUBLE:
      if ((size_type_specifier!=SizeSpecifier.NO)&&(size_type_specifier!=SizeSpecifier.LONG)) {
	cp.raiseError(tn,"long or short specified with floating type, the only valid combination is `long double'");
      }
      if (sign_type_specifier!=SignSpecifier.NO) {
	cp.raiseError(tn,"signed or unsigned invalid with floating type");
      }
      if (size_type_specifier==SizeSpecifier.LONG) {
	if (!Type.getSourceABI().isLongDoubleAllowed()) {
	  cp.raiseError(tn,"long double not allowed");
	}
	type=FloatingPointScalar.Tlongdouble;
      }
      else {
//	if (!Type.getSourceABI().isDoubleAllowed()) {
//	  cp.raiseError(tn,"double not allowed");
//	}
	type=FloatingPointScalar.Tdouble;
      }
      // Sets the specifier type
      break;
    
    case VECTOR:
    case STRUCT:
    case UNION:
    case ENUM:
     CheckNoSizeSignTypeSpecifier(tn,cp);
     type=sub_type;
     break;
      
    case TYPEOF:
    case TYPEDEFNAME:
      // Sign and size specifiers (which are type specifiers) are not
      // allowed with a typedefname (itself a type specifier)
      if (
	  (size_type_specifier!=SizeSpecifier.NO) || 
	  (sign_type_specifier!=SignSpecifier.NO)
	  ) {
	if (base_type_specifier==BaseTypeSpecifier.TYPEOF) {
	  cp.raiseError(tn,"type specifier not allowed with a typeof");
	}
	else {
	  cp.raiseError(tn,"type specifier not allowed with a typedef name");
	}
      }
      // Other type specifiers (base data type) have been already filtered

      // First take the sub_type as base type
      type=sub_type;

      // Specific case for array
      if (type instanceof Array) {
	Array array=(Array)type;
	if (!array.hasSizeSpecifier()) {
	  // Clone the Array object. This is to manage the case when an
	  // incomplete array is declared as typedef. This typedef can
	  // be used to declare arrays which have different size setting
	  // through their initializer:
	  //
	  //   typedef int T[];
	  //   T tab1= {1, 2};
	  //   T tab2= {3, 4, 5};
	  type=array.clone();
	}
      }

      // Merge type qualifiers
      if (
	  (const_type_qualifier   !=false) || 
	  (volatile_type_qualifier!=false) || 
	  (restrict_type_qualifier!=false) 
	  ) {
	Qualifier new_qualifier;
	
	if (type instanceof Qualifier) {
	  // Clone the qualifier to merge before merging
	  new_qualifier=(Qualifier)(((Qualifier)type).clone());
	}
	else {
	  // Create a new qualifier
	  new_qualifier=new Qualifier(type);
	}
	
	// Merge qualifiers
	if (const_type_qualifier) {
	  new_qualifier.setConst(tn,cp);
	}
	if (volatile_type_qualifier) {
	  new_qualifier.setVolatile(tn,cp);
	}
	if (restrict_type_qualifier) {
	  new_qualifier.setRestrict(tn,cp);
	}

	// Sets the type
	type=(Type)new_qualifier;
      }
      return type;
      
    default:
      // No processing yet (complex, imaginary, valist)
      // -> int by default
      // [TBW]
      type=IntegerScalar.Tsint;
     }

    // Manage type qualifiers (except for typename)
    if (
	(const_type_qualifier   !=false) || 
	(volatile_type_qualifier!=false) || 
	(restrict_type_qualifier!=false) 
	) {
      Qualifier qualifier=new Qualifier(type);
      // Merge qualifiers
      if (const_type_qualifier) {
	qualifier.setConst(tn,cp);
      }
      if (volatile_type_qualifier) {
	qualifier.setVolatile(tn,cp);
      }
      if (restrict_type_qualifier) {
	qualifier.setRestrict(tn,cp);
      }
      
      type=(Type)qualifier;
    }
    return type;
  }


  // On the fly checks
  public void checkSign(TNode tn,CompilerError cp) {
    if (base_type_specifier!=BaseTypeSpecifier.NO) {
       cp.raiseError(tn,"multiple data type in specifier");
    }
  }
  public void checkMultipleDataType(TNode tn,CompilerError cp) {
    if (base_type_specifier!=BaseTypeSpecifier.NO) {
       cp.raiseError(tn,"multiple data type in specifier");
    }
  }

  // 'typeof' type specifier
  public void setTypeof(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.TYPEOF;
  }
  // 'typedef name' type specifier
  public void setTypedefName(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.TYPEDEFNAME;
  }
  // 'void' type specifier
  public void setVoid(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.VOID;
  }
  // '_Bool' type specifier
  public void setBool(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.BOOL;
  }
  // 'char' type specifier
  public void setChar(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.CHAR;
  }
  // 'int' type specifier
  public void setInt(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.INT;
  }
  // 'float' type specifier
  public void setFloat(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.FLOAT;
  }
  // 'double' type specifier
  public void setDouble(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.DOUBLE;
  }
  // vector type specifier
  public void setVector(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.VECTOR;
  }
  // 'complex' type specifier
  public void setComplex(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.COMPLEX;
  }
  // 'imaginary' type specifier
  public void setImaginary(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.IMAGINARY;
  }
  // 'struct' type specifier
  public void setStruct(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.STRUCT;
  }
  // 'union' type specifier
  public void setUnion(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.UNION;
  }
  // 'enum' type specifier
  public void setEnum(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.ENUM;
  }
  // 'valist' type specifier
  public void setValist(TNode tn,CompilerError cp) {
    checkMultipleDataType(tn,cp);
    base_type_specifier=BaseTypeSpecifier.VALIST;
  }

  // 'short' type specifier
  public void setShort(TNode tn,CompilerError cp) {
    if (size_type_specifier==SizeSpecifier.SHORT) {
      cp.raiseWarning(tn,"duplicate 'short'");
    }
    else if (
	     (size_type_specifier==SizeSpecifier.LONG) ||
	     (size_type_specifier==SizeSpecifier.LONG_LONG)
	     ) {
      cp.raiseError(tn,"both long and short specified");
    }
    size_type_specifier=SizeSpecifier.SHORT;
  }
  // 'long' type specifier
  public void setLong(TNode tn,CompilerError cp) {
    if (size_type_specifier==SizeSpecifier.SHORT) {
      cp.raiseError(tn,"both long and short specified");
    }
    else if (size_type_specifier==SizeSpecifier.LONG_LONG) {
      cp.raiseError(tn,"'long long long' is too long");
    }
    else if (size_type_specifier==SizeSpecifier.LONG) {
      size_type_specifier=SizeSpecifier.LONG_LONG;
    }
    else {
      size_type_specifier=SizeSpecifier.LONG;
    }
  }

  // 'signed' type specifier
  public void setSigned(TNode tn,CompilerError cp) {
    if (sign_type_specifier==SignSpecifier.SIGNED) {
      cp.raiseWarning(tn,"duplicate 'signed'");
    }
    else if (sign_type_specifier==SignSpecifier.UNSIGNED) {
      cp.raiseError(tn,"both signed and unsigned specified");
    }
    sign_type_specifier=SignSpecifier.SIGNED;
  }
  // 'unsigned' type specifier
  public void setUnsigned(TNode tn,CompilerError cp) {
    if (sign_type_specifier==SignSpecifier.UNSIGNED) {
      cp.raiseWarning(tn,"duplicate 'unsigned'");
    }
    else if (sign_type_specifier==SignSpecifier.SIGNED) {
      cp.raiseError(tn,"both signed and unsigned specified");
    }
    sign_type_specifier=SignSpecifier.UNSIGNED;
  }


  // 'const' type qualifier
  public void setConst(TNode tn,CompilerError cp) {
    if (const_type_qualifier) {
      cp.raiseWarning(tn,"duplicate 'const'");
    }
    const_type_qualifier=true;
  }
  // 'volatile' type qualifier
  public void setVolatile(TNode tn,CompilerError cp) {
    if (volatile_type_qualifier) {
      cp.raiseWarning(tn,"duplicate 'volatile'");
    }
    volatile_type_qualifier=true;
  }
  // 'restrict' type qualifier
  public void setRestrict(TNode tn,CompilerError cp) {
    if (restrict_type_qualifier) {
      cp.raiseWarning(tn,"duplicate 'restrict'");
    }
    restrict_type_qualifier=true;
  }


  //==================================================================
  // Query of Type specifiers
  //==================================================================


  // 'void' type specifier
  public boolean isTypedefName() {
    return(base_type_specifier==BaseTypeSpecifier.TYPEDEFNAME);
  }
  public boolean isVoid() {
    return(base_type_specifier==BaseTypeSpecifier.VOID);
  }
  // '_Bool' type specifier
  public boolean isBool() {
    return(base_type_specifier==BaseTypeSpecifier.BOOL);
  }
  // 'char' type specifier
  public boolean isChar() {
    return(base_type_specifier==BaseTypeSpecifier.CHAR);
  }
  // 'int' type specifier
  public boolean isInt() {
    return(base_type_specifier==BaseTypeSpecifier.INT);
  }
  // 'float' type specifier
  public boolean isFloat() {
    return(base_type_specifier==BaseTypeSpecifier.FLOAT);
  }
  // 'double' type specifier
  public boolean isDouble() {
    return(base_type_specifier==BaseTypeSpecifier.DOUBLE);
  }
  // vector type specifier
  public boolean isVector() {
    return(base_type_specifier==BaseTypeSpecifier.VECTOR);
  }
  // 'complex' type specifier
  public boolean isComplex() {
    return(base_type_specifier==BaseTypeSpecifier.COMPLEX);
  }
  // 'imaginary' type specifier
  public boolean isImaginary() {
    return(base_type_specifier==BaseTypeSpecifier.IMAGINARY);
  }
  // 'struct' type specifier
  public boolean isStruct() {
    return(base_type_specifier==BaseTypeSpecifier.STRUCT);
  }
  // 'union' type specifier
  public boolean isUnion() {
    return(base_type_specifier==BaseTypeSpecifier.UNION);
  }
  // 'enum' type specifier
  public boolean isEnum() {
    return(base_type_specifier==BaseTypeSpecifier.ENUM);
  }
  // 'valist' type specifier
  public boolean isValist() {
    return(base_type_specifier==BaseTypeSpecifier.VALIST);
  }

  // 'short' type specifier
  public boolean isShort() {
    return(size_type_specifier==SizeSpecifier.SHORT);
  }
  // 'long' type specifier
  public boolean isLong() {
    return(size_type_specifier==SizeSpecifier.LONG);
  }
  // 'long' type specifier
  public boolean isLongLong() {
    return(size_type_specifier==SizeSpecifier.LONG_LONG);
  }
  // 'signed' type specifier
  public boolean isSigned() {
    return((sign_type_specifier==SignSpecifier.SIGNED)||(sign_type_specifier==SignSpecifier.NO));
  }
  // 'unsigned' type specifier
  public boolean isUnsigned() {
    return(sign_type_specifier==SignSpecifier.UNSIGNED);
  }


  // 'const' type qualifier
  public boolean isConst() {
     return const_type_qualifier;
  }
  // 'volatile' type qualifier
  public boolean isVolatile() {
     return volatile_type_qualifier;
  }
  // 'restrict' type qualifier
  public boolean isRestrict() {
     return restrict_type_qualifier;
  }
}
