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

/* OpenCL ABI information */

package ir.types.abi;

import ir.types.c.IntegerScalar;

import java.math.BigInteger;

public class OCL_ABI implements ABI {

  public static final OCL_ABI abi = new OCL_ABI();

  static final BigInteger INT_MAX    = BigInteger.ONE.shiftLeft(31).subtract(BigInteger.ONE);
  static final BigInteger UINT_MAX   = BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE);
  static final BigInteger LONG_MAX   = BigInteger.ONE.shiftLeft(63).subtract(BigInteger.ONE);
  static final BigInteger ULONG_MAX  = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
  static final BigInteger LLONG_MAX  = BigInteger.ONE.negate();
  static final BigInteger ULLONG_MAX = BigInteger.ONE.negate();

  // Long types support
  public boolean isLongLongAllowed()   {return false;}
  public boolean isDoubleAllowed()     {return false;}
  public boolean isLongDoubleAllowed() {return false;}

  // Sizes in bytes
  public int getCharSize()       { return  1; }
  public int getShortSize()      { return  2; }
  public int getIntSize()        { return  4; }
  public int getLongSize()       { return  8; }
  public int getLonglongSize()   { return -1; }
  public int getFloatSize()      { return  4; }
  public int getDoubleSize()     { return -1; }
  public int getLongdoubleSize() { return -1; }
  public int getPointerSize()    { return  4; }

  // Limits
  public BigInteger getINT_MAX()    { return INT_MAX; }
  public BigInteger getUINT_MAX()   { return UINT_MAX; }
  public BigInteger getLONG_MAX()   { return LONG_MAX; }
  public BigInteger getULONG_MAX()  { return ULONG_MAX; }
  public BigInteger getLLONG_MAX()  { return LLONG_MAX; }
  public BigInteger getULLONG_MAX() { return ULLONG_MAX; }

  // Alignment in bytes
  public int getCharAlignment()       { return 1; }
  public int getShortAlignment()      { return 2; }
  public int getIntAlignment()        { return 4; }
  public int getLongAlignment()       { return 4; }
  public int getLonglongAlignment()   { return 8; }
  public int getFloatAlignment()      { return 4; }
  public int getDoubleAlignment()     { return 8; }
  public int getLongdoubleAlignment() { return 4; }
  public int getPointerAlignment()    { return 4; }

  // Equivalence. Not relevant here since arithmetic types are platform 
  // indepedents => return int
  public IntegerScalar getEquivalent_size_t()    { return IntegerScalar.Tuint; }
  public IntegerScalar getEquivalent_ptrdiff_t() { return IntegerScalar.Tsint; }
  public IntegerScalar getEquivalent_intptr_t()  { return IntegerScalar.Tsint; }
  public IntegerScalar getEquivalent_uintptr_t() { return IntegerScalar.Tuint; }
}
