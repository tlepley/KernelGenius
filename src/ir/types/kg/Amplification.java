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

/* Set of rational numbers determining the ratio of data */

package ir.types.kg;


import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import utility.math.LinearCoefficient;


public class Amplification {
    List<LinearCoefficient> fractionList=new LinkedList<LinearCoefficient>();
        
    //==================================================================
    // Constructors
    //==================================================================

    public Amplification() { }

    public Amplification(Amplification amp) {
      fractionList=new LinkedList<LinearCoefficient>(amp.fractionList);
    }
    
    //========================================================
    // Setting
    //========================================================
    
    public void MultiplyBy(LinearCoefficient rn) {
      if (!rn.isNeutral()) {
        fractionList.add(rn);
      }
    }

    public void multiplyBy(int c, int num) {
    	if ((c!=0)||(num!=1)) {
    		fractionList.add(new LinearCoefficient(c, num,1));
    	}
    }

    public void multiplyBy(int c, int num, boolean ceil) {
    	if ((c!=0)||(num!=1)) {
    		fractionList.add(new LinearCoefficient(c, num, 1, ceil));
    	}
    }

    public void devideBy(int c, int denom, boolean ceil) {
    	if ((c!=0)||(denom!=1)) {
    		fractionList.add(new LinearCoefficient(c, 1, denom, ceil));
    	}
    }

    
    //========================================================
    // Getting Value
    //========================================================
    
    public boolean isSame(Amplification a) {
      if (fractionList.size()!=a.fractionList.size()) {
        return false;
      }
      int i=0;
      for(LinearCoefficient lc:fractionList) {
        LinearCoefficient lc2=a.fractionList.get(i++);
        if (!lc.isSame(lc2)) {
          return false;
        }
      }
      return true;
    }

    
    public boolean hasCoefficients() {
      return fractionList.size()!=0;
    }
    public int applyTo(int value) {
      // Do it in the order of the list in order to keep integer accuracy
      for (LinearCoefficient rn:fractionList) {
        value=rn.applyTo(value);
      }
      return value;
    }
        
    //========================================================
    // Code generation
    //========================================================

    public void generate(String val, PrintStream ps) {
      ps.print(generateString(val));
    }

    public String generateString(String val) {
      String s=val;
      for(LinearCoefficient lc:fractionList) {
        s=lc.generateString(s);
      }
      return s;
    }

    //========================================================
    // Verbose
    //========================================================  
    
    public String toString(String val) {
      String s=val;
      for(LinearCoefficient lc:fractionList) {
        s=lc.toString(s);
      }
      return s;
    }
    
}
