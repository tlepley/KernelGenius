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

/* Arithmetic helper functions */

package utility.math;

public abstract class Arithmetic {
  
  // Recursive Euclidean algorithm
  public static int gcd (int a, int b) { 
    if(a<b) {
      return (gcd(b,a));
    }
    else if(b==0) { 
      return (a);
    }
    else {
      return (gcd(b,a%b));
    }
  }

  public static int lcm(int nbr1, int nbr2){
    if ((nbr1==0)||(nbr2==0)) return 0;
    int gcd = gcd(nbr1,nbr2);
    int lcm = (nbr1*nbr2)/gcd;
    return lcm;
  }

  public static int min(int nbr1, int nbr2) {
    return nbr1<nbr2?nbr1:nbr2;
  }
  
  public static int max(int nbr1, int nbr2) {
    return nbr1>nbr2?nbr1:nbr2;
  }
  
  public static int clampToPositive(int nbr) {
    return nbr<0?0:nbr;
  }

}
