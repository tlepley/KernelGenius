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

package ir.literals.c;

import ir.types.c.IntegerScalar;

//import java.math.BigInteger;

public class IntegerLiteral extends ScalarLiteral {
  // TODO: use BigInteger
  long value;

  public IntegerLiteral(long v, IntegerScalar t) {
    super(t);
    value=v;
  }

  public long getValue() {
    return value;
  }

  public boolean isZero() {
    return value==0;
  }
  public boolean isOne() {
    return value==1;
  }
  public boolean isMinusOne() {
    return value== -1;

  }

  public boolean isGreaterThanZero() {
    return value>0;
  }
  public boolean isGreaterEqualZero() {
    return value>=0;
  }
  public boolean isLowerThanZero() {
    return value<0;
  }
  public boolean isLowerEqualZero(){
    return value<=0;
  }


  @Override
  public boolean isConstant() {
    // TODO Auto-generated method stub
    return true;
  }

  public String toString() {
    return Long.toString(value);
  }

}
