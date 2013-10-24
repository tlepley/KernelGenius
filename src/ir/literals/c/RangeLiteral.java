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

public class RangeLiteral extends ScalarLiteral {
  // TODO: use BigInteger
  long first = 0;
  long last = 0;


  //==================================================================
  // Building
  //==================================================================

  // [v1,v2]
  public RangeLiteral(long v1, long v2, IntegerScalar t) {
    super(t);
    first=v1;
    last=v2;
  }

  // <= v
  public RangeLiteral(long v, IntegerScalar t) {
    super(t);
    first=0;
    last=v;
  }


  //==================================================================
  // Getters
  //==================================================================

  // Returns the number of elements
  public long getRangeSize() {
    return last-first+1;
  }

  public long getFirstValue() {
    return first;
  }

  public long getLastValue() {
    return last;
  }

  public boolean isZero() {
    return (first==0) && (last==0);
  }
  public boolean isOne() {
    return (first==1) && (last==1);
  }
  public boolean isMinusOne() {
    return (first==-1) && (last==-1);
  }

  public boolean isGreaterThanZero() {
    return (first>0) && (last>0);
  }
  public boolean isGreaterEqualZero() {
    return (first>=0) && (last>=0);
  }
  public boolean isLowerThanZero() {
    return (first<0) && (last<0);
  }
  public boolean isLowerEqualZero(){
    return (first<=0) && (last<=0);
  }

  @Override
  public boolean isConstant() {
    return false;
  }


  //==================================================================
  // Verbose functions
  //==================================================================

  public String toString() {
    StringBuffer buff = new StringBuffer();
    buff.append("[");
    buff.append(first);
    buff.append(",");
    buff.append(last);
    buff.append("]");

    return buff.toString();
  }
}
