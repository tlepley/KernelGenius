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

import ir.base.IRElement;
import ir.base.KernelData;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import utility.math.Arithmetic;


public class MatrixSize extends IRElement {
  // Array with a single element
  public static final MatrixSize MatrixSingleElement = new MatrixSize(1);
  // Array with no elements
  public static final MatrixSize MatrixNil = new MatrixSize(0);
 
  int nbDims=0;
  public enum IndexType {NOT_SPECIFIED, MAXIMUM, FIXED};
  java.util.Vector<IndexType> indexType=new java.util.Vector<IndexType>();
  java.util.Vector<Integer> nbElems=new java.util.Vector<Integer>();

  // TODO: Index range specifier can be on first and last (ex: convolution coefs)
  java.util.Vector<KernelData> indexRangeSpecifier=new java.util.Vector<KernelData>();


  //==================================================================
  // Building
  //==================================================================

  // By default, empty Array
  public MatrixSize() {
    nbDims=0;
  }
 
  @SuppressWarnings("unchecked")
  public MatrixSize(MatrixSize t) {
    //super(t);
    nbDims=t.nbDims;
    nbElems=(Vector<Integer>) t.nbElems.clone();
    indexType=(Vector<IndexType>) t.indexType.clone();
    indexRangeSpecifier=(Vector<KernelData>)t.indexRangeSpecifier.clone();

  }

  // 1D array with nb elements
  public MatrixSize(int nb) {    
    nbDims=1;
    nbElems.add(nb);
    indexType.add(IndexType.FIXED);
    indexRangeSpecifier.add(null);
  }
  
  // 2D array [nb1,nb2]
  public MatrixSize(int nb0, int nb1) {    
    nbDims=2;
    
    // First dim
    nbElems.add(nb0);
    indexType.add(IndexType.FIXED);
    indexRangeSpecifier.add(null);

    // Second dim
    nbElems.add(nb1);
    indexType.add(IndexType.FIXED);
    indexRangeSpecifier.add(null);
  }

  // ND array with index starting from 0
  public MatrixSize(List<Integer> nb) {
    nbDims=nb.size();
    for (int i=0;i<nb.size();i++) {
      nbElems.add(nb.get(i));
      indexType.add(IndexType.FIXED);
      indexRangeSpecifier.add(null);
    }
  }


  //==================================================================
  // Getters
  //==================================================================
  
  public boolean isEmpty() {
    return nbDims==0;
  }

  public int getNbDims() {
    return nbDims;
  }

  public int getNbElements(int i) {
    if (isEmpty()) { return 0; }
    if (i>=nbDims) {
      return 1;
    }
    return nbElems.get(i);
  }
  
  public boolean hasSingleElement() {
    if (isEmpty()) { return false; }
    for(int i=0;i<nbDims;i++) {
      if (indexType.get(i)==IndexType.NOT_SPECIFIED) {
        return false;
      }
      if (nbElems.get(i)!=1) {
        return false;
      }
    }
    return true;
  }
  
  public boolean hasStaticSize() {
    for(int i=0;i<nbDims;i++) {
      if (
          (indexRangeSpecifier.get(i)!=null) ||
          (indexType.get(i)==IndexType.NOT_SPECIFIED) ) {
        return false;
      }
    }
    return true;
  }
  
  public IndexType getndexType(int i) {
    if (isEmpty()) { return IndexType.NOT_SPECIFIED; }
    if (i>=nbDims) { return IndexType.FIXED; }
    return indexType.get(i);
  }

  public IndexType getLastIndexType(int i) { 
    if (isEmpty()) { return IndexType.NOT_SPECIFIED; }
    if (i>=nbDims) { return IndexType.FIXED; }
    return indexType.get(i);
  }

  public KernelData getIndexRangeSpecifier(int i) {
    if (isEmpty()) { return null; }
    if (i>=nbDims) {
      return null;
    }
    return indexRangeSpecifier.get(i);
  }
    
  public void getIndexRangeSpecifiers(Set<KernelData> kdSet) {
    if (isEmpty()) { return; }
    for(KernelData kd:indexRangeSpecifier) {
      if (kd!=null) {
        kdSet.add(kd);
      }
    }
  }

  
  //==================================================================
  // Queries on dimensions
  //==================================================================
  
  public boolean isSame(MatrixSize t) {
    // Is is myself ?
    if (this==t) return true;
    
    // Same nbDims ?
    if (nbDims!=t.nbDims) {
      return false;
    }
    for (int i=0;i<nbDims;i++) {
      if (indexRangeSpecifier.get(i)!=t.indexRangeSpecifier.get(i)) {
        return false;
      }
      if (indexRangeSpecifier.get(i)!=null) {
        // First index
        if (indexType.get(i)!=t.indexType.get(i)) {
          return false;
        }
        if (((int)nbElems.get(i))!=(int)t.nbElems.get(i)) {
         return false;
        }
      }
    }
    // Everything looks equivalent
    return true;
  }

  public boolean includes(MatrixSize nd)  {
    // Same dims ?
    int n=nd.nbDims;
    if (nbDims < nd.nbDims) {
      if (nd.hasReallyMoreDimensions(nbDims)) {
        return false;
      }
      n=nbDims;
    }

    // Manage dimensions properties
    for (int i=0;i<n;i++) {
      // Variable size specifier
      if (indexRangeSpecifier.get(i)!=nd.indexRangeSpecifier.get(i)) {
        return false;
      }

      // First index
      if (indexType.get(i)!=nd.indexType.get(i)) {
        return false;
      }
      if (((int)nbElems.get(i))>(int)nd.nbElems.get(i)) {
        return false;
      }
   }

    return true;
  }

  boolean hasReallyMoreDimensions(int d) {
    if (isEmpty()) { return false; }
    if (nbDims<d) return false;
    for (int i=d;i<nbDims;i++) {
      // Variable size specifier
      if (indexRangeSpecifier.get(i)!=null) {
        return true;
      }
      // First index
      if (((int)nbElems.get(i))!=0) {
       return true;
      }
    }
    return false;
  }

  
  //==================================================================
  // Operations on dimensions
  //==================================================================

  public void unionMergeWith(MatrixSize nd) throws UnsupportedOperationException {
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;
    
    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd=indexRangeSpecifier.get(i);
      KernelData kd_nd=nd.indexRangeSpecifier.get(i);
      if ((kd!=null) && (kd_nd!=null) && (kd!=kd_nd)) {
        // Error : can not merge
        throw new UnsupportedOperationException("unionMergeWith: not allowed on matrix with different non constant size");
      }
      else if (((kd!=null) && (kd_nd==null)) || ((kd==null) && (kd_nd!=null))) {
        // Error : can not merge
        throw new UnsupportedOperationException("unionMergeWith: not allowed on matrix with different non constant size");
      }
      {  // First index
        IndexType id=indexType.get(i);
        IndexType id_nd=nd.indexType.get(i);

        if (id==IndexType.NOT_SPECIFIED || id_nd==IndexType.NOT_SPECIFIED) {
          indexType.set(i,IndexType.NOT_SPECIFIED);
          nbElems.set(i,0);
        }
        else {
          int fi=nbElems.get(i);
          int fi_nd=nd.nbElems.get(i);
          if (fi_nd<fi) {
            indexType.set(i,id_nd);
            nbElems.set(i,fi_nd);
          }
        }
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        indexType.add(nd.indexType.get(i));
        nbElems.add(nd.nbElems.get(i));
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));

        nbDims++;
      }
    }
  }
  
  public void add(MatrixSize nd) throws UnsupportedOperationException {    
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd=indexRangeSpecifier.get(i);
      KernelData kd_nd=nd.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        // Error : can not merge matrixes with non constant size
        throw new UnsupportedOperationException("add: not allowed on matrix with non constant size");
      }
      else {
        nbElems.set(i,nbElems.get(i)+nd.nbElems.get(i));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        indexType.add(nd.indexType.get(i));
        nbElems.add(nd.nbElems.get(i));
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));

        nbDims++;
      }
    }
  }

 
  public void lcm(MatrixSize nd) throws UnsupportedOperationException {
    
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd   =    indexRangeSpecifier.get(i);
      KernelData kd_nd= nd.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        throw new UnsupportedOperationException("lcm: not allowed on matrix with non constant size");
      }
      else {
        nbElems.set(i,Arithmetic.lcm(nbElems.get(i),nd.nbElems.get(i)));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        indexType.add(nd.indexType.get(i));
        nbElems.add(nd.nbElems.get(i));
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));

        nbDims++;
      }
    }
  }

  public void gcd(MatrixSize nd) throws UnsupportedOperationException {   
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd   =    indexRangeSpecifier.get(i);
      KernelData kd_nd= nd.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        throw new UnsupportedOperationException("gcd: not allowed on matrix with non constant size");
      }
      else {
        nbElems.set(i,Arithmetic.gcd(nbElems.get(i),nd.nbElems.get(i)));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        indexType.add(nd.indexType.get(i));
        nbElems.add(nd.nbElems.get(i));
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));

        nbDims++;
      }
    }
  }
  
  public MatrixSize devideDirectBy(MatrixSize nd) throws UnsupportedOperationException {    
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd   =    indexRangeSpecifier.get(i);
      KernelData kd_nd= nd.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        throw new UnsupportedOperationException("devideDirectBy: not allowed on matrix with non constant size");
      }
      else {
        nbElems.set(i,nbElems.get(i)/nd.nbElems.get(i));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        indexType.add(nd.indexType.get(i));
        nbElems.add( 1/nd.nbElems.get(i) );
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));

        nbDims++;
      }
    }
    
    return this;
  }

  public MatrixSize multiplyDirectBy(MatrixSize nd) throws UnsupportedOperationException {    
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd   =    indexRangeSpecifier.get(i);
      KernelData kd_nd= nd.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        throw new UnsupportedOperationException("multiplyDirectBy: not allowed on matrix with non constant size");
      }
      else {
        nbElems.set(i,nbElems.get(i)*nd.nbElems.get(i));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        indexType.add(nd.indexType.get(i));
        nbElems.add(nd.nbElems.get(i));
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));

        nbDims++;
      }
    }
    
    return this;    
  }

  
  
  //==================================================================
  // Verbose
  //==================================================================
 
  public void generate(PrintStream ps) {
    if (isEmpty()) {
      ps.print("[<EmptyArray>]");
      return;
    }
    for (int i=nbDims-1;i>=0;i--) {
      ps.print("[");
      
      String s=null;
      KernelData kd=indexRangeSpecifier.get(i);
      if (kd!=null) {
        // This is a variable
        s=kd.getName();
      }
      else {
        // This is a constant
        StringBuffer sb=new StringBuffer();
        printIndex(sb, nbElems.get(i), indexType.get(i));
        s=sb.toString();
      }

      ps.print(s);
      ps.print("]");
    }
  }


  //==================================================================
  // Verbose
  //==================================================================
 
  void printIndex(StringBuffer sb, int index, IndexType it) {
    if (it==IndexType.NOT_SPECIFIED) {
      sb.append("(?)");
    }
    else {
      sb.append(Integer.toString(index));
      if (it==IndexType.MAXIMUM) {
        sb.append("(max)");
      }
    }
  }
  
  public String toString() { 
    StringBuffer sb = new StringBuffer();
    sb.append(Integer.toString(nbDims)).append("D Matrix size ");
    if (isEmpty()) {
      sb.append("{<empty>}");
    }
    else {
      for (int i=0;i<nbDims;i++) {
        sb.append("{");
                
        StringBuffer sb2 = new StringBuffer();
        KernelData kd=indexRangeSpecifier.get(i);
        if (kd!=null) {
          sb2.append(kd.getName());
          sb2.append(" /*");
        }

        printIndex(sb2, nbElems.get(i), indexType.get(i));

        if (kd!=null) {
          sb2.append("*/");
        }
        
        sb.append(sb2.toString());
        sb.append("}");
      }
    }
    

    return sb.toString();
  }


}
