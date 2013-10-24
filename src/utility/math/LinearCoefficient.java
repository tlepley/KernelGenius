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

package utility.math;

// Linear equation: a(x+b)
public class LinearCoefficient  {
  RationalNumber coefA=null;
  public int coefB=0;

  
  //==================================================================
  // Constructor
  //==================================================================

  public LinearCoefficient(int b, int aNum, int aDenom) {
    coefA=new RationalNumber(aNum,aDenom);
    coefB=b;
  }
  
  public LinearCoefficient(int b, int aNum, int aDenom, boolean ceil) {
    coefA=new RationalNumber(aNum,aDenom,ceil);
    coefB=b;
  }

  public boolean isNeutral() {
    return (coefB==0) && coefA.equalsToOne();
  }
  
  public boolean isSame(LinearCoefficient lc) {
    return (coefB==lc.coefB)&&(coefA.isSame(lc.coefA));
  }
  
  //========================================================
  // appliance to a number
  //========================================================

  public int applyTo(int value) {
    return coefA.applyTo(value+coefB);
  }
  

  //========================================================
  // Code generation
  //========================================================

  public String generateString(String value) {
    StringBuffer sb = new StringBuffer();
    generate(value,sb);
    return sb.toString();
  }

  void generate(String value, StringBuffer sb) {
    StringBuffer s=new StringBuffer();
    s.append("(");
    s.append(value);
    s.append(")");
    if (coefB!=0) {
      if (coefB>=0) {
        s.append("+");
      }
      s.append(coefB);  
    }

    coefA.generate(s.toString(), sb);
  }

  //========================================================
  // Code generation / Verbose
  //========================================================

  public String toString(String value) {
    StringBuffer sb = new StringBuffer();
    generate(value,sb);
    return sb.toString();
  }
 

}
