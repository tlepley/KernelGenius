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

/* Rational Number */

package utility.math;

public class RationalNumber {
  private int numerator=1;
  private int denominator=1;
  // By default, floor is taken
  private boolean ceil=false;

  
  //==================================================================
  // Constructor
  //==================================================================

  public RationalNumber(int a, int b) {
    numerator=a;
    denominator=b;  
    normalize();
  }
  public RationalNumber(int a, int b, boolean c) {
    this(a,b);
    ceil=c;
  }

  public RationalNumber(RationalNumber rn) {
    numerator=rn.getNumerator();
    denominator=rn.getDenominator();
    ceil=rn.ceil;
  }

  //========================================================
  // Query
  //========================================================
  
  public int getNumerator() {
    return numerator;
  }

  public int getDenominator() {
    return denominator;
  }

  public boolean equalsToOne() {
    return (getNumerator()==getDenominator());
  }

  public boolean isStrictlyNegative() {
    return (getNumerator()*getDenominator()<0);
  }
  public boolean isStrictlyPositive() {
    return (getNumerator()*getDenominator()>0);
  }
  
  public boolean isSame(RationalNumber rn) {
    return (getNumerator()==rn.getNumerator()) && (getDenominator()==rn.getDenominator());
  }

  
  //========================================================
  // Operation
  //========================================================

  public RationalNumber MultiplyBy(int value) {
    numerator=getNumerator() * value;
    normalize();
    return this;
  }
  public RationalNumber DevideBy(int value) {
    denominator=getDenominator() * value;
    normalize();
    return this;
  }
  
  public boolean equals(RationalNumber rn) {
    return getNumerator()*rn.getDenominator()==getDenominator()*rn.getNumerator();
  }

  //========================================================
  // appliance to a number
  //========================================================

  public int applyTo(int value) {
    return (value*getNumerator())/getDenominator();
  }

  //========================================================
  // Internal functions
  //========================================================

  public void normalize() {
    if (getDenominator()<0) {
      numerator=-getNumerator();
      denominator=-getDenominator();
    }
    int gcd = Arithmetic.gcd(getNumerator(),getDenominator());
    if (gcd!=1) {
      numerator=getNumerator() / gcd;
      denominator=getDenominator() / gcd;
    }
  }


  //==================================================================
  // Generation
  //==================================================================

  
  void generate(String value, StringBuffer sb) {
    // Prefix
    sb.append("(");
    
    // Numerator
    if (getNumerator()!=1) {
      sb.append(numerator);
      sb.append("*(");
    }

    if (getDenominator()!=1) {
      if (ceil) {
        sb.append("(");
      }
    }

    sb.append("(");
    sb.append(value);
    sb.append(")");

    // Denominator
    if (getDenominator()!=1) {
      if (ceil) {
        // Denominator assumed positive here
        sb.append("+");
        sb.append(denominator);
        sb.append("-1");
        sb.append(")");
      }
      sb.append("/");
      sb.append(denominator);
    }

    if (getNumerator()!=1) {
      sb.append(")");
    }
    
    sb.append(")");
  }

  
  //==================================================================
  // Verbose
  //==================================================================
  
  public String toString() {
    StringBuffer sb = new StringBuffer();

    if ((getDenominator()==1)) {
      sb.append(getNumerator());
    }
    else {
      if (ceil) {
        sb.append("ceil(");
        sb.append(getNumerator());
        sb.append("/");
        sb.append(getDenominator());
        sb.append(")");
      }
      else {
        // Floor (standard java semantics)
        sb.append("floor(");
        sb.append(getNumerator());
        sb.append("/");
        sb.append(getDenominator());
        sb.append(")");
      } 
    }

    return sb.toString();
  }

}
