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

package ir.types.kg;

import java.util.List;

import ir.base.IRElement;

public class MatrixCoordinate extends IRElement {
  public enum IndexType {NOT_SPECIFIED, MAXIMUM, FIXED};

  // Notes: Indexes can be an integer or a String
  int nbDims=0;
  java.util.Vector<Object> indexList=new java.util.Vector<Object>();
  // 1D matrix
  public MatrixCoordinate(int i0) { 
    nbDims=1;
    indexList.add(i0);
  }

  // 2D matrix
  public MatrixCoordinate(int i0, int i1) {    
    nbDims=2;
    indexList.add(i0);
    indexList.add(i1);
  }

  public MatrixCoordinate(List<Object> l) {
    nbDims=l.size();
    for (Object i:l) {
      indexList.add(i);
    }
  }
  
  
  //==================================================================
  // Getters
  //==================================================================
  
  public int getNbDims() {
    return nbDims;
  }

  public Object getIndex(int i) {
    if (i>=nbDims) {
      return 0;
    }
    return indexList.get(i);
  }

  
  //==================================================================
  // Verbose
  //==================================================================
 
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (Object index:indexList) {
      sb.append("[");
      sb.append(index.toString());
      sb.append("]");
    }
    return sb.toString();
  }

}
