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

/* C type checking function */

package ir.types;

import java.math.BigInteger;

import common.CompilerError;

import ir.types.abi.ABI;
import ir.types.c.ChildType;
import ir.types.c.EnrichedType;
import ir.types.c.FloatingPointScalar;
import ir.types.c.Function;
import ir.types.c.IntegerScalar;
import ir.types.c.StructOrUnion;
import ir.types.ocl.AddressSpace;
import parser.TNode;



public class TypeManager {

  // ##################################################################
  // Input language management
  // ##################################################################

  private boolean oclOption = false;

  public void setOclLanguage() {
    oclOption=true;
  }


  // ##################################################################
  // Miscellaneous Type checking
  // ##################################################################

  public static void checkChildAsFunction(TNode id_node, CompilerError cp,
      ChildType parent) {
    if (parent.isFunction()) {
      // Not necessarily the symbol which is declared as function
      // returning function, but it is in its declaration
      // Note: gcc does the same simplification
      cp.raiseError((TNode)id_node,
          "function `"+id_node.getText()+
          "' declared as function returning a function");
    }
    if (parent.isArray()) {
      // Not necessarily the symbol which is declared as array of
      // function, but it is in its declaration
      // Note: gcc does the same simplification
      cp.raiseError((TNode)id_node,
          "declaration of `"+id_node.getText()+
          "' as array of functions");
    }
  }
  public static void checkChildAsFunctionAbstract(TNode node, CompilerError cp,
      ChildType parent) {
    if (parent.isFunction()) {
      cp.raiseError((TNode)node,
          "declaration of a function returning a function");
    }
    if (parent.isArray()) {
      cp.raiseError((TNode)node,
          "declaration of an array of functions");
    }
  }

  public static void checkChildAsArray(TNode id_node, CompilerError cp,
      ChildType parent) {
    if (parent.isFunction()) {
      // Not necessarily the symbol which is declared as function
      // returning function, but it is in its declaration
      // Note: gcc does the same simplification
      cp.raiseError((TNode)id_node,
          "function `"+id_node.getText()+
          "' declared as function returning an array");
    }
  }
  
  public static void checkChildAsArrayAbstract(TNode node, CompilerError cp,
      ChildType parent) {
    if (parent.isFunction()) {
      cp.raiseError((TNode)node,
          "declaration of a function returning an array");
    }
  }


  // ##################################################################
  // Expression type management
  // ##################################################################

  // ******************************************************************
  // getIntegralCommonTypeNoQualifier :
  //
  // Check that operands are integral (raise a fatal error otherwise),
  // promote operands and return the resulting type
  // Note: It assumes that operand types are not qualified
  // ******************************************************************
  private static Type getIntegralCommonTypeNoQualifier(Type left, Type right) {
    // Here, no pointer nor float, this is arithmetic statement

    // Performs operands promotion (only remaining int, long int, long long int,
    // float, double, long double)
    left =left.promote();
    right=right.promote();

    // Step 2: Integer operands
    if ( (right==IntegerScalar.Tulonglong) || (left==IntegerScalar.Tulonglong)) {
      return IntegerScalar.Tulonglong;
    }
    if ( (right==IntegerScalar.Tslonglong) || (left==IntegerScalar.Tslonglong)) {
      return IntegerScalar.Tslonglong;
    }
    if ( (right==IntegerScalar.Tulong) || (left==IntegerScalar.Tulong)) {
      return IntegerScalar.Tulong;
    }
    if ( (right==IntegerScalar.Tslong) || (left==IntegerScalar.Tslong)) {
      return IntegerScalar.Tslong;
    }
    if ( (right==IntegerScalar.Tuint) || (left==IntegerScalar.Tuint)) {
      return IntegerScalar.Tuint;
    }
    return IntegerScalar.Tsint;
  }

  // ******************************************************************
  // getArithmeticCommonTypeNoQualifier :
  //
  // Check that operands are arithmetic (raise a fatal error otherwise),
  // promote operands and return the resulting type
  // Note: It assumes that operand types are not qualified
  // ******************************************************************
  public static Type getArithmeticCommonTypeNoQualifier(Type left, Type right) {
    // Here, no pointer, this is arithmetic statement

    // Performs operands promotion (only remaining int, long int, long long int,
    // float, double, long double)
    left =left.promote();
    right=right.promote();

    // Step 1: Floating points operands
    if (right.isFloatingPointScalar()) {
      if (left.isFloatingPointScalar()) {
        if (
            (right==FloatingPointScalar.Tlongdouble) ||
            (left==FloatingPointScalar.Tlongdouble)
            ) {
          return FloatingPointScalar.Tlongdouble;
        }
        if (
            (right==FloatingPointScalar.Tdouble) ||
            (left==FloatingPointScalar.Tdouble)
            ) {
          return FloatingPointScalar.Tdouble;
        }
        // By default, return a float
        return FloatingPointScalar.Tfloat;
      }
      else {
        // The integer type must be converted to floating point
        // [Node insertion TBW]
        return right;
      }
    }
    else {
      if (left.isFloatingPointScalar()) {
        // The integer type must be converted to floating point
        // [Node insertion TBW]
        return left;
      }
      // Here, only integer operands
    }


    // Step 2: Integral operands
    return getIntegralCommonTypeNoQualifier(left,right);
  }

  // ******************************************************************
  //  getTypeAdditiveBinaryOperator:
  //
  // Check that operands of a + or - operation are coherent (raise a
  // fatal error otherwise), promote operands and return the resulting
  // type
  // Note: If the type is a qualified pointer, the qualification is
  //       propagated
  // ******************************************************************
  public Type getTypeAdditiveBinaryOperator(TNode node, CompilerError cp,
      Type l, Type r, boolean is_minus) {
    Type left, right;

    // Remove potential qualifier
    left  = l.unqualify();
    right = r.unqualify();

    // From void cast
    if (left.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return right;
    }
    if (right.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return left;
    }

    // Check for operand type conformance
    // Check operands
    if ( (!left.isScalarOrLabel() ) ||
        (!right.isScalarOrLabel() ) 
        ) {
      cp.raiseFatalError(node,"invalid operands to binary " + node.getText());
    }

    // Manage pointers
    if (right.isPointerOrLabel()) {
      Type tmp=left;left=right;right=tmp;
    }
    if (left.isPointerOrLabel()) {
      if (right.isPointerOrLabel()) {
        if (is_minus) {
          // Subtraction of two pointers is allowed, it gives ptrdiff_t     
          // Get it from the ABI
          return Type.getSourceABI().getEquivalent_ptrdiff_t();
        }
        else {
          cp.raiseFatalError(node,"invalid operands to binary " + node.getText());
        }
      }
      else {
        // The resulting type is a pointer

        // Check for correct pointer arithmetic
        if (left.isPointer()) {
          if (left.getPointedType().isIncomplete()) {
            cp.raiseFatalError(node,"arithmetic of pointer to incomplete type");
          }
        }
        else if (left.isArray()) {
          // Should never happen since arrays are checked at declaration
          if (left.getElementType().isIncomplete()) {
            cp.raiseFatalError(node,"arithmetic of pointer to incomplete type");
          }
        }
        // else, its a function prototype (pointer to code)
        // No error

        return left;
      }
    }

    // Here, no pointer, this is arithmetic statement
    // -> no need to propagate the qualifier
    return getArithmeticCommonTypeNoQualifier(left,right);
  }

  // ******************************************************************
  // getTypeArithmeticBinaryOperator :
  //
  // Check that operands of an arithmetic binary operation are coherent
  // (raise a fatal error otherwise), promote operands and return the
  // resulting type
  // Note: potential qualifiers of operands are not propagated
  // ******************************************************************
  public Type getTypeArithmeticBinaryOperator(TNode node, CompilerError cp,
      Type l, Type r) {
    Type left, right;

    // Remove potential qualifier
    left  = l.unqualify();
    right = r.unqualify();

    // From void cast
    if (left.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return right;
    }
    if (right.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return left;
    }

    // Check for operand type conformance
    if ( (!left.isArithmeticScalar()) ||
        (!right.isArithmeticScalar()) 
        ) {
      cp.raiseFatalError(node,"invalid arithmetic operands to binary " + node.getText());
    }

    // Here operands are Arithmetic
    // -> no need to propagate the qualifier
    return getArithmeticCommonTypeNoQualifier(left,right);
  }

  // ******************************************************************
  // getTypeShiftOperator :
  //
  // Check that operands of a shift operation are coherent (raise a
  // fatal error otherwise), promote operands and return the resulting
  // type
  // Note: potential qualifiers of operands are not propagated
  // ******************************************************************
  public Type getTypeShiftOperator(TNode node, CompilerError cp,
      Type l, Type r) {
    Type left, right;

    // Remove potential qualifier
    left  = l.unqualify();
    right = r.unqualify();

    // From void cast
    if (left.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return left;
    }
    if (right.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return left;
    }

    if ( (!left.isIntegralScalar()) ||
        (!right.isIntegralScalar()) 
        ) {
      cp.raiseFatalError(node,"invalid integer operands to binary " + node.getText());
      return null;
    }

    // Promote the left operand
    return left.promote();
  }

  // ******************************************************************
  // getTypeIntegralBinaryOperator :
  //
  // Check that operands of an integral binary operation are coherent
  // (raise a fatal error otherwise), promote operands and return the
  // resulting type
  // Note: potential qualifiers of operands are not propagated
  // ******************************************************************
  public Type getTypeIntegralBinaryOperator(TNode node, CompilerError cp,
      Type l, Type r) {
    Type left, right;

    // Remove potential qualifier
    left  = l.unqualify();
    right = r.unqualify();

    // From void cast
    if (left.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return right;
    }
    if (right.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return left;
    }

    // Check operands
    if ( (!left.isIntegralScalar()) ||
        (!right.isIntegralScalar()) 
        ) {
      cp.raiseFatalError(node,"invalid integral operands to binary " + node.getText());
      return null;
    }

    // -> no need to propagate the qualifier
    return getIntegralCommonTypeNoQualifier(left,right);
  }

  // ******************************************************************
  // getTypeLogicalUnaryOperator :
  //
  // Check that the operand of a logical unary operator is valid
  // (raise an error otherwise), and return the resulting type
  // ******************************************************************
  public static Type getTypeLogicalUnaryOperator(TNode node, CompilerError cp,
      Type l, String s) {
    Type left= l.unqualify();

    if (!left.isScalarOrLabel()) {
      cp.raiseError(node,"wrong type argument to logical unary "+s);
    }

    // The type is integer
    return IntegerScalar.Tsint;
  }

  // ******************************************************************
  // getTypeLogicalBinaryOperator :
  //
  // Check that operands of a logical binary operator is scalar 
  // (raise an error otherwise), and return the resulting type
  // ******************************************************************
  public Type getTypeLogicalBinaryOperator(TNode node, CompilerError cp,
      Type l, Type r,
      TNode e_left, TNode e_right) {
    // Remove potential qualifier
    Type left, right;
    left  = l.unqualify();
    right = r.unqualify();

    // From void cast
    if (left.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return IntegerScalar.Tsint;
    }
    if (right.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return IntegerScalar.Tsint;
    }

    if ( (!left.isScalarOrLabel()) ||
        (!right.isScalarOrLabel()) 
        ) {
      cp.raiseError(node,"invalid operands to logical binary " + node.getText());
    }

    // The type is integer
    return IntegerScalar.Tsint;
  }

  // ******************************************************************
  // getTypeRelationalOperator :
  //
  // Check that both operands of a relational operator are compatible
  // ((raise an error otherwise) and return the type of the expression
  // ******************************************************************
  public Type getTypeRelationalOperator(TNode node, CompilerError cp,
      EnrichedType el, EnrichedType er,
      TNode e_left, TNode e_right) {
    // NOTE: More tests should be done with pointers, in particular regarding
    // 'object compatibility' [TBW]

    // Remove potential qualifier
    Type left, right;
    left  = el.getType().unqualify();
    right = er.getType().unqualify();

    // From void cast
    if (left.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return IntegerScalar.Tsint;
    }
    if (right.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return IntegerScalar.Tsint;
    }

    if (left.isArithmeticScalar()) {
      if (!right.isArithmeticScalar()) {
        // Both should be arithmetic, but a pointer can be compared to an integral
        if (left.isIntegralScalar() && right.isPointerOrLabel()) {
          if (!el.isConstantZero()) {
            cp.raiseWarning(node,"comparison between pointer and integer");
          }
        }
        else {
          cp.raiseError(node,"invalid operands to binary " + node.getText());
        }
      }
      // else OK
    }

    else if (left.isPointerOrLabel()) {
      if (!right.isPointerOrLabel()) {
        // Both should be pointer, but a pointer can be compared to an integral
        if (right.isIntegralScalar()) {
          if (!er.isConstantZero()) {
            cp.raiseWarning(node,"comparison between pointer and integer");
          }
        }
        else {
          cp.raiseError(node,"invalid operands to binary " + node.getText());
        }
      }
      //else {
      // Do not generate compatibility warning yet, because compatibility for function
      // prototypes and array is not just reference equality like for arithmetic types
      // struct or union
      // [TBW]
      //if (left.isPointer() && right.isPointer()) {
      //  if (left.getPointedType().unqualify()!=right.getPointedType().unqualify()) {
      //    cp.raiseWarning(node,"comparison of distinct pointer types lacks a cast");
      //  }
      //	}
      //else if (left.isArray() && right.isArray()) {
      //  if (left.getElementType().unqualify()!=right.getElementType().unqualify()) {
      //    cp.raiseWarning(node,"comparison of distinct pointer types lacks a cast");
      //  }
      //	}
      //	else if (left.isPointer() && right.isArray()) {
      //  if (left.getPointedType().unqualify()!=right.getElementType().unqualify()) {
      //    cp.raiseWarning(node,"comparison of distinct pointer types lacks a cast");
      //  }
      //}
      //else if (left.isArray() && right.isPointer()) {
      //  if (left.getElementType().unqualify()!=right.getPointedType().unqualify()) {
      //    cp.raiseWarning(node,"comparison of distinct pointer types lacks a cast");
      //  }
      //}
      //}
      // else OK
    }

    else {
      cp.raiseError(node,"invalid operands to binary " + node.getText());
    }

    // Relational always returns signed int
    return IntegerScalar.Tsint;
  }

  // ******************************************************************
  // getTypeConditionalOperator :
  //
  // Check that operands of a conditional operator are
  // coherent (raise a fatal error otherwise), promote operands
  // and return the resulting type
  //
  // Note: qualifiers are not propagated
  // ******************************************************************
  public Type getTypeConditionalOperator(TNode node, CompilerError cp,
      Type t, Type l, Type r) {
    // NOTE: More tests should be done in particular regarding
    // 'object compatibility' [TBW]

    // Left optional (GNU-C ??)
    if (l==null) {
      // It will returns the type of the right operand
      l=r;
    }

    // Remove potential qualifier
    Type test, left, right;
    test  = t.unqualify();
    left  = l.unqualify();
    right = r.unqualify();


    // The test must be a scalar
    if (!test.isScalarOrLabel()) {
      cp.raiseError(node,"invalid test operand to conditional operator");
    }

    // Check other operands
    if (left.isArithmeticScalar()) {
      if (!right.isArithmeticScalar()) {
        // Both should be arithmetic, but a pointer can be compared to an integral
        if (left.isIntegralScalar() && (right.isPointerOrLabel())) {
          cp.raiseWarning(node,"pointer/integer type mismatch in conditional expression");
          // Returns the pointer type
          return right;
        }
        else {
          cp.raiseFatalError(node,"type mismatch in conditional expression");
          return null;
        }
      }
      // else OK
      return getArithmeticCommonTypeNoQualifier(left,right);
    }
    else if (left.isPointerOrLabel()) {
      if (!right.isPointerOrLabel()) {
        // Both should be pointer, but a pointer can be compared to an integral
        if (right.isIntegralScalar()) {
          cp.raiseWarning(node,"pointer/integer type mismatch in conditional expression");
          // Returns the pointer type
          return left;
        }
        else {
          cp.raiseFatalError(node,"type mismatch in conditional expression");
          return null;
        }
      }
      // else OK
      return left;
    }
    else if (left.isStruct()) {
      if (!right.isStruct()) {
        // Both should be a compatible structure
        cp.raiseFatalError(node,"type mismatch in conditional expression");
        return null;
      }
      else if (left!=right) {
        // It must be the same structure
        cp.raiseFatalError(node,"struct type mismatch in conditional expression");
        return null;
      }
      // else OK
      return left;
    }
    else if (left.isUnion()) {
      if (!right.isUnion()) {
        // Both should be a compatible structure
        cp.raiseFatalError(node,"type mismatch in conditional expression");
        return null;
      }
      else if (left!=right) {
        // It must be the same union
        cp.raiseFatalError(node,"union type mismatch in conditional expression");
        return null;
      }
      // else OK
      return left;
    }
    else if (left.isVoid()) {
      if (!right.isVoid()) {
        // Both should be void
        cp.raiseFatalError(node,"type mismatch in conditional expression");
        return null;
      }
      // else OK
      return left;
    }
    else {
      // Should not be correct [TBW]
      cp.raiseFatalError(node,"type mismatch in conditional expression");
      return null;
    }
  }

  // ******************************************************************
  // checkAssignOperands :
  //
  // Check that both operands of an assign operator are compatible
  // It raises an error in case of non correct check
  // ******************************************************************
  public void checkAssignOperands(TNode node, CompilerError cp,
      Type left, EnrichedType eright, String s) {
    Type right=eright.getType();
    if (left.isPointerOrLabel() &&
        right.isIntegralScalar() && eright.isConstantZero()) {
      // It is a common situation which should not generates warning
      // int *i=0;
      return;
    }
    checkAssignOperands(node,cp,left,right,s);
  }

  // ******************************************************************
  // checkAssignOperands :
  //
  // Check that both operands of an assign operator are compatible
  // It raises an error in case of non correct check
  // ******************************************************************
  public void checkAssignOperands(TNode node, CompilerError cp,
      Type left, Type right, String s) {
    // NOTE: More tests should be done in particular regarding
    // 'object compatibility' [TBW]

    // From void cast
    if (right.isVoid()) {
      cp.raiseError(node,"void value not ignored as it ought to be");
      return;
    }


    if (left.isArithmeticScalar()) {
      if (!right.isArithmeticScalar()) {
        // Both should be arithmetic, but a pointer can be compared to an integral
        if (left.isIntegralScalar() && (right.isPointerOrLabel())) {
          cp.raiseWarning(node,s+" makes integer from pointer without a cast");
        }
        else {
          cp.raiseError(node,"incompatible types in "+s);
        }
      }
      // else OK
    }
    else if (left.isPointerOrLabel()) {
      if (!right.isPointerOrLabel()) {
        // Both should be pointer, but a pointer can be compared to an integral
        if (right.isIntegralScalar()) {
          cp.raiseWarning(node,s+" makes pointer from integer without a cast");
        }
        else {
          cp.raiseError(node,"incompatible types in "+s);
        }
      }
      else {
        // [OCL] Check for address space coherency
        if (oclOption) {
          AddressSpace left_as, right_as;
          left_as=getPointerOrLabelAddressSpace(cp,left);
          right_as=getPointerOrLabelAddressSpace(cp,right);

          // By default, a non qualified object if private static in kernels
          // (except images which are global but not supported here)
          if (left_as==AddressSpace.NO) {
            left_as=AddressSpace.PRIVATE;
          }
          if (right_as==AddressSpace.NO) {
            right_as=AddressSpace.PRIVATE;
          }

          if (left_as!=right_as) {
            //cp.raiseError(node,"[OCL] incompatible address space for pointer types in "+s);
          }
        }
      }
    }
    else if (left.isStruct()) {
      if (!right.isStruct()) {
        // Both should be a compatible structure
        cp.raiseFatalError(node,"incompatible types in "+s);
      }
      else if (left.unqualify()!=right.unqualify()) {
        // It must be the same structure
        cp.raiseError(node,"incompatible struct types in "+s);
      }
      // else OK
    }
    else if (left.isUnion()) {
      if (!right.isUnion()) {
        // Both should be a compatible structure
        cp.raiseFatalError(node,"incompatible types in "+s);
      }
      else if (left.unqualify()!=right.unqualify()) {
        // It must be the same union
        cp.raiseError(node,"incompatible union types in "+s);
      }
      // else OK
    }
    else {
      // Should not be correct [TBW]
      cp.raiseError(node,"type mismatch in "+s);
    } 
  }

  // ******************************************************************
  // isConvertible :
  //
  // Check that the source can be converted to the destination
  // Note: the result is equivalent to 'checkAssignOperands'
  // ******************************************************************
  public boolean isConvertible(CompilerError cp, Type target, Type source) {
    // NOTE: More tests should be done in particular regarding
    // 'object compatibility' [TBW]

    // From void cast
    if (source.isVoid()) {
      return false;
    }

    if (target.isArithmeticScalar()) {
      if (!source.isArithmeticScalar()) {
        // Both should be arithmetic, but a pointer can be compared to an integral
        if (target.isIntegralScalar() && (source.isPointerOrLabel())) {
          // OK
        }
        else {
          return false;
        }
      }
      // else OK
    }
    else if (target.isPointerOrLabel()) {
      if (!source.isPointerOrLabel()) {
        // Both should be pointer, but a pointer can be compared to an integral
        if (source.isIntegralScalar()) {
          // OK
        }
        else {
          return false;
        }
      }
      else {
        // [OCL] Check for address space coherency
        if (oclOption) {
          AddressSpace target_as, source_as;
          target_as=getPointerOrLabelAddressSpace(null,target);
          source_as=getPointerOrLabelAddressSpace(null,source);

          // By default, a non qualified object if private static in kernels
          // (except images which are global but not supported here)
          if (target_as==AddressSpace.NO) {
            target_as=AddressSpace.PRIVATE;
          }
          if (source_as==AddressSpace.NO) {
            source_as=AddressSpace.PRIVATE;
          }

          if (target_as!=source_as) {
            return false;
          }
        }
      }
    }
    else if (target.isStruct()) {
      if (!source.isStruct()) {
        // Both should be a compatible structure
        return false;
      }
      else if (target.unqualify()!=source.unqualify()) {
        // It must be the same structure
        return false;
      }
      // else OK
    }
    else if (target.isUnion()) {
      if (!source.isUnion()) {
        // Both should be a compatible structure
        return false;
      }
      else if (target.unqualify()!=source.unqualify()) {
        // It must be the same union
        return false;
      }
      // else OK
    }
    else {
      // Should not be correct [TBW]
      return false;
    }
    return true;
  }

  // ******************************************************************
  // checkCastOperands :
  //
  // Check that both operands of an cast operator are compatible
  // It raises an error in case of non correct check
  // ******************************************************************
  public void checkCastOperands(TNode node, CompilerError cp,
      Type left, Type right) {
    // NOTE: More tests should be done in particular regarding
    if (left.isVoid()) {
      // Simply voids the type of 'right'
      // Nothing to do
      return;
    }

    if (!left.isScalarOrLabel()) {
      if (left.unqualify()==right.unqualify()) {
        cp.raiseWarning(node,"ISO C forbids casting non-scalar to the same type");
      }
      else {
        cp.raiseError(node,"conversion to non-scalar type requested");
      }
    }
    else if (!right.isScalarOrLabel()) {
      if (right.isVoid()) {
        cp.raiseError(node,"void value not ignored as it ought to be");
      }
      else {
        cp.raiseError(node,"conversion of non-scalar type requested");
      }
    }

    // Here, left and right are either arithmetic, either pointer or label
    else if (left.isArithmeticScalar()) {
      if (left.isFloatingPointScalar() && right.isPointerOrLabel()) {
        cp.raiseError(node,"casting a pointer into floating point");
      }
      // else OK
    }
    else if (left.isPointerOrLabel()) {
      if (right.isFloatingPointScalar()) {
        cp.raiseError(node,"casting a floating point into pointer");
      }
      else if (oclOption && 
          right.isPointerOrLabel()) {
        // [OCL] Check for address space coherency
        AddressSpace left_as, right_as;
        left_as=getPointerOrLabelAddressSpace(cp,left);
        right_as=getPointerOrLabelAddressSpace(cp,right);

        // By default, a non qualified object if private static in kernels
        // (except images which are global but not supported here)
        if (left_as==AddressSpace.NO) {
          left_as=AddressSpace.PRIVATE;
        }
        if (right_as==AddressSpace.NO) {
          right_as=AddressSpace.PRIVATE;
        }
        // Address space must be the same
        if (left_as!=right_as) {
          // Temporary patch for PGIacc
          //cp.raiseError(node,"[OCL] casting a pointer to an other address space is forbidden");
        }
      }
      // else OK
    }
    else {
      // Should never come here
      cp.raiseInternalError("checkCastOperands");
    }
  }

  // ******************************************************************
  // checkLvalue :
  //
  // Check that 'etype' is a correct lvalue
  // It raises an error in case of non correct check
  // ******************************************************************
  public static void checkLvalue(TNode node, CompilerError cp,
      EnrichedType etype, String s) {
    if (!etype.designateAnObject()) {
      cp.raiseError(node,"invalid lvalue in "+s);
    }

    if (etype.getType().isVoid()) {
      cp.raiseError(node,"invalid lvalue in "+s);
    }
  }

  // ******************************************************************
  // checkModifiableLvalue :
  //
  // Check that 'etype' is a correct modifiable lvalue
  // It raises an error in case of non correct check
  // ******************************************************************
  public void checkModifiableLvalue(TNode node, CompilerError cp,
      EnrichedType etype, String s) {
    // Array labels are not object references
    if (!etype.designateAnObject()) {
      cp.raiseError(node,"invalid lvalue in "+s);
    }

    Type t=etype.getType();
    // Array type is not modifiable
    if (t.isIncompleteOrVoid()||t.isArray()) {
      cp.raiseError(node,"invalid lvalue in "+s);
    }
    // A 'const' qualified type is not modifiable
    else if (t.isConstQualified()) {
      cp.raiseError(node,s+" of read-only object");
    }
    // An empty aggregate is not a lvalue
    // An aggregate with a const field is not modifiable
    else if (t.isStructOrUnion()) {
      if (
          (((StructOrUnion)(t.unqualify())).hasEmptyBody()) 
          // [TBW]
          //	  ||(((StructOrUnion)t).hasConstQualifiedElements())
          ) {
        cp.raiseError(node,"invalid lvalue in "+s);
      }
    }

    // [OCL] checks
    if (oclOption) {
      if (t.isQualified()) {
        // __constant data must be initialized
        if ((t.getQualifier()).getAddressSpace()==AddressSpace.CONSTANT) {
          cp.raiseError(node,"[OCL] "+s+
              " of variables declared in the __constant address space is forbidden");
        }
      }
    }
  }

  // ******************************************************************
  // getPointerOrLabelAddressSpace :
  //
  // Return the address space qualifier of the object referenced by
  // a 'pointer' type
  // ******************************************************************
  private static AddressSpace getPointerOrLabelAddressSpace(CompilerError cp, Type t) {
    if (t.isPointer()) {
      Type subType=t.getPointedType();
      if (subType.isQualified()) {
        return (subType.getQualifier()).getAddressSpace();
      }
      return AddressSpace.NO;
    }
    else if (t.isArray()) {
      if (t.isQualified()) {
        return (t.getQualifier()).getAddressSpace();
      }
      return AddressSpace.NO;
    }
    else if (t.isFunction()) {
      return AddressSpace.CODE;
    }
    else {
      // Internal error
      cp.raiseInternalError("(getPointerOrLabelAddressSpace)");
      return AddressSpace.NO;
    }
  }



  // ##################################################################
  // Function call management
  // ##################################################################

  // ******************************************************************
  // checkFunctionCall :
  //
  // Check that arguments of a function call are compatible with the
  // function prototype. It raises an error in case of non correct
  // check.
  // Note:if functionName is be 'null', it means that the function name
  //      is not available (ex: function pointer)
  // ******************************************************************
  public void checkFunctionCall(TNode node, CompilerError cp,
      String functionName,
      Function prototype,
      Function call) {
    String functionToPrint;

    // Check parameter list
    if ( (!prototype.hasParameter()) && (!prototype.isVoidParameterList())) {
      // The function prototype is compatible with all function calls
      return;
    }

    if (functionName==null) {
      functionToPrint="";
    }
    else {
      functionToPrint=" '"+functionName+"'";
    }

    // Check that both prototypes have the same number of parameters
    if (call.getNbParameters()>prototype.getNbParameters()) {
      if (!prototype.hasVararg()) {
        cp.raiseError(node,"too many arguments to function" + functionToPrint);
      }
      return;
    }
    if (call.getNbParameters()<prototype.getNbParameters()) {
      cp.raiseError(node,"too few arguments to function" + functionToPrint);
      return;
    }

    // Check parameters one by one
    int i;
    for (i=0;i<prototype.getNbParameters();i++) {

      checkAssignOperands(node, cp,
          prototype.getParameterType(i),
          call.getParameterType(i),
          "argument "+(i+1)+" of call to function" + functionToPrint
          );
    }

    return;
  }



  // ##################################################################
  // Constant type management
  // ##################################################################

  // ******************************************************************
  // getIntegralType :
  //
  // Returns the type of an integer terminal number
  // ******************************************************************
  @SuppressWarnings("unused")
  private static Type getIntegralType(String s) {
    String s_num=s.toLowerCase();

    if (s_num.endsWith("ull")) {
      return IntegerScalar.Tulonglong;
    }
    if (s_num.endsWith("llu")) {
      return IntegerScalar.Tulonglong;
    }
    if (s_num.endsWith("ll")) {
      return IntegerScalar.Tslonglong;
    }
    if (s_num.endsWith("ul")) {
      return IntegerScalar.Tulong;
    }
    if (s_num.endsWith("lu")) {
      return IntegerScalar.Tulong;
    }
    if (s_num.endsWith("l")) {
      return IntegerScalar.Tslong;
    }
    if (s_num.endsWith("u")) {
      return IntegerScalar.Tuint;
    }
    return IntegerScalar.Tsint;
  }


  // ******************************************************************
  // getFloatType :
  //
  // Returns the type of a floating point terminal number
  // ******************************************************************
  private static Type getFloatType(String s) {
    String s_num=s.toLowerCase();

    if (s_num.endsWith("l")) {
      return FloatingPointScalar.Tlongdouble;
    }
    if (s_num.endsWith("f")) {
      return FloatingPointScalar.Tfloat;
    }
    return FloatingPointScalar.Tdouble;
  }


  // ******************************************************************
  // getIntegralNumberEnrichedType :
  //
  // Returns the type of a terminal number
  // ******************************************************************

  // Enum to characterize integer constant literals
  enum INTEGER_CONSTANT_TYPE {DECIMAL, OCTAL, HEXADECIMAL};
  enum SUFFIX {NO, U, L, UL, LL, ULL};

  public static EnrichedType getIntegralNumberEnrichedType(TNode node, CompilerError cp, String s) {
    // To lower case to simplify the parsing
    // [TBW] note that in C, 2lL is forbidden in C99
    String str=s.toLowerCase();


    //------------------------
    // Determinate the suffix
    //------------------------
    SUFFIX suffix;
    if (str.endsWith("ull")) {
      suffix=SUFFIX.ULL;
      str=str.substring(0,str.length()-3);
    }
    else if (str.endsWith("llu")) {
      suffix=SUFFIX.ULL;
      str=str.substring(0,str.length()-3);
    }
    else if (str.endsWith("ll")) {
      suffix=SUFFIX.LL;
      str=str.substring(0,str.length()-2);
    }
    else if (str.endsWith("ul")) {
      suffix=SUFFIX.UL;
      str=str.substring(0,str.length()-2);
    }
    else if (str.endsWith("lu")) {
      suffix=SUFFIX.UL;
      str=str.substring(0,str.length()-2);
    }
    else if (str.endsWith("l")) {
      suffix=SUFFIX.L;
      str=str.substring(0,str.length()-1);
    }
    else if (str.endsWith("u")) {
      suffix=SUFFIX.U;
      str=str.substring(0,str.length()-1);
    }
    else {
      suffix=SUFFIX.NO;
    }


    //------------------------------------------------
    // Determinate the kind of constant and its value
    //------------------------------------------------
    BigInteger value = BigInteger.ZERO;
    INTEGER_CONSTANT_TYPE constant_type;

    // -> Hexadecimal integer
    if (str.startsWith("0x")) {
      constant_type=INTEGER_CONSTANT_TYPE.HEXADECIMAL;
      try {
        //System.out.println( "  STRING: " + str);
        value=new BigInteger(str.substring(2),16);
        //System.out.println( "  VALUE: " + value);
      }
      catch (NumberFormatException ex) {
        cp.raiseInternalError("Wrong hexadecimal integral constant format");
      }
    }

    // -> Octal integer and 0 value
    else if (str.startsWith("0")) {
      constant_type = INTEGER_CONSTANT_TYPE.OCTAL;
      try {
        //System.out.println( "  STRING: " + str);
        value=new BigInteger(str,8);
        //System.out.println( "  VALUE: " + value);
      }
      catch (NumberFormatException ex) {
        cp.raiseInternalError("Wrong octal integral constant format");
      }
    }

    // -> Decimal integer and 0 value
    else {
      constant_type = INTEGER_CONSTANT_TYPE.DECIMAL;
      try {
        //System.out.println( "  STRING: " + str);
        value=new BigInteger(str);
        //System.out.println( "  VALUE: " + value);
      }
      catch (NumberFormatException ex) {
        cp.raiseInternalError("Wrong decimal integral constant format");
      }
    }


    //--------------------------------------
    // Determinate the type of the constant
    //--------------------------------------
    Type the_type=IntegerScalar.Tsint;
    ABI abi=Type.getSourceABI();

    switch (constant_type) {
    //  Decimal
    case DECIMAL:
      switch (suffix) {
      case NO:
        if (value.compareTo(abi.getINT_MAX())<=0) {
          the_type=IntegerScalar.Tsint;
        }
        else if (value.compareTo(abi.getLONG_MAX())<=0) {
          the_type=IntegerScalar.Tslong;
        }
        else if (value.compareTo(abi.getLLONG_MAX())<=0) {
          the_type=IntegerScalar.Tslonglong;
        }
        else {
          the_type=IntegerScalar.Tslonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      case U:
        if (value.compareTo(abi.getUINT_MAX())<=0) {
          the_type=IntegerScalar.Tuint;
        }
        else if (value.compareTo(abi.getULONG_MAX())<=0) {
          the_type=IntegerScalar.Tulong;
        }
        else if (value.compareTo(abi.getULLONG_MAX())<=0) {
          the_type=IntegerScalar.Tulonglong;
        }
        else {
          the_type=IntegerScalar.Tulonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      case L:
        if (value.compareTo(abi.getLONG_MAX())<=0) {
          the_type=IntegerScalar.Tslong;
        }
        else if (value.compareTo(abi.getLLONG_MAX())<=0) {
          the_type=IntegerScalar.Tslonglong;
        }
        else {
          the_type=IntegerScalar.Tslonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      case UL:
        if (value.compareTo(abi.getULONG_MAX())<=0) {
          the_type=IntegerScalar.Tulong;
        }
        else if (value.compareTo(abi.getULLONG_MAX())<=0) {
          the_type=IntegerScalar.Tulonglong;
        }
        else {
          the_type=IntegerScalar.Tulonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      case LL:
        if (value.compareTo(abi.getLLONG_MAX())<=0) {
          the_type=IntegerScalar.Tslonglong;
        }
        else {
          the_type=IntegerScalar.Tslonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      case ULL:
        if (value.compareTo(abi.getULLONG_MAX())<=0) {
          the_type=IntegerScalar.Tulonglong;
        }
        else {
          the_type=IntegerScalar.Tulonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      }
      break;

      //  Octal and Hexadecimal
    case OCTAL:
    case HEXADECIMAL:
      switch (suffix) {
      case NO:
        if (value.compareTo(abi.getINT_MAX())<=0) {
          the_type=IntegerScalar.Tsint;
        }
        else if (value.compareTo(abi.getUINT_MAX())<=0) {
          the_type=IntegerScalar.Tuint;
        }
        else if (value.compareTo(abi.getLONG_MAX())<=0) {
          the_type=IntegerScalar.Tslong;
        }
        else if (value.compareTo(abi.getULONG_MAX())<=0) {
          the_type=IntegerScalar.Tulong;
        }
        else if (value.compareTo(abi.getLLONG_MAX())<=0) {
          the_type=IntegerScalar.Tslonglong;
        }
        else if (value.compareTo(abi.getULLONG_MAX())<=0) {
          the_type=IntegerScalar.Tulonglong;
        }
        else {
          the_type=IntegerScalar.Tulonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      case U:
        if (value.compareTo(abi.getUINT_MAX())<=0) {
          the_type=IntegerScalar.Tuint;
        }
        else if (value.compareTo(abi.getULONG_MAX())<=0) {
          the_type=IntegerScalar.Tulong;
        }
        else if (value.compareTo(abi.getULLONG_MAX())<=0) {
          the_type=IntegerScalar.Tulonglong;
        }
        else {
          the_type=IntegerScalar.Tulonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      case L:
        if (value.compareTo(abi.getLONG_MAX())<=0) {
          the_type=IntegerScalar.Tslong;
        }
        else if (value.compareTo(abi.getULONG_MAX())<=0) {
          the_type=IntegerScalar.Tulong;
        }
        else if (value.compareTo(abi.getLLONG_MAX())<=0) {
          the_type=IntegerScalar.Tslonglong;
        }
        else if (value.compareTo(abi.getULLONG_MAX())<=0) {
          the_type=IntegerScalar.Tulonglong;
        }
        else {
          the_type=IntegerScalar.Tulonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      case UL:
        if (value.compareTo(abi.getULONG_MAX())<=0) {
          the_type=IntegerScalar.Tulong;
        }
        else if (value.compareTo(abi.getULLONG_MAX())<=0) {
          the_type=IntegerScalar.Tulonglong;
        }
        else {
          the_type=IntegerScalar.Tulonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      case LL:
        if (value.compareTo(abi.getLLONG_MAX())<=0) {
          the_type=IntegerScalar.Tslonglong;
        }
        else if (value.compareTo(abi.getULLONG_MAX())<=0) {
          the_type=IntegerScalar.Tulonglong;
        }
        else {
          the_type=IntegerScalar.Tulonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      case ULL:
        if (value.compareTo(abi.getULLONG_MAX())<=0) {
          the_type=IntegerScalar.Tulonglong;
        }
        else {
          the_type=IntegerScalar.Tulonglong;
          cp.raiseWarning(node,"integer constant is too large");
        }
        break;
      }
      break;
    }


    //--------------------------
    // Create the enriched type
    //--------------------------
    EnrichedType etype=new EnrichedType(the_type);
    etype.setConstantIntegral(value);

    return etype;
  }


  // ******************************************************************
  // getFloatingPointNumberEnrichedType :
  //
  // Returns the type of a terminal number
  // ******************************************************************
  public static EnrichedType getFloatingPointNumberEnrichedType(CompilerError cp, String s) {
    Type the_type;

    int i;

    the_type=getFloatType(s);
    EnrichedType etype=new EnrichedType(the_type);

    String str=s.toLowerCase();

    // Removes the suffix
    for(i=str.length()-1;
        (i>=0)&&((str.charAt(i)=='f')||(str.charAt(i)=='l'));
        i--);
    if (i<0) {
      cp.raiseInternalError("getNumberEnrichedType - 2");
    }
    str=str.substring(0,i+1);

    // Get the number
    try {
      //System.out.println( "  STRING: " + str);
      etype.setConstantFloatingpoint(Double.parseDouble(str));
      //System.out.println( "  VALEUR: " + etype.getConstantFloatingpointValue());
    }
    catch (NumberFormatException ex) {
      cp.raiseInternalError("Wrong floating point constant format");
    }
    return etype;
  }

}
