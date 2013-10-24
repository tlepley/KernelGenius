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

/* Relevant information for the target processor for C */

package ir.types.abi;

import ir.types.c.IntegerScalar;

import java.math.BigInteger;

public interface ABI {

  // Long types support
  boolean isLongLongAllowed();
  boolean isDoubleAllowed();
  boolean isLongDoubleAllowed();

  // Sizes in bytes
  int getCharSize();
  int getShortSize();
  int getIntSize();
  int getLongSize();
  int getLonglongSize();
  int getFloatSize();
  int getDoubleSize();
  int getLongdoubleSize();
  int getPointerSize();

  // Limits
  BigInteger getINT_MAX();
  BigInteger getUINT_MAX();
  BigInteger getLONG_MAX();
  BigInteger getULONG_MAX();
  BigInteger getLLONG_MAX();
  BigInteger getULLONG_MAX();

  // Alignment in bytes
  int getCharAlignment();
  int getShortAlignment();
  int getIntAlignment();
  int getLongAlignment();
  int getLonglongAlignment();
  int getFloatAlignment();
  int getDoubleAlignment();
  int getLongdoubleAlignment();
  int getPointerAlignment();

  // Equivalence
  IntegerScalar getEquivalent_size_t();
  IntegerScalar getEquivalent_ptrdiff_t();
  IntegerScalar getEquivalent_intptr_t();
  IntegerScalar getEquivalent_uintptr_t();
}
