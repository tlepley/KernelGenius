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
import ir.literals.Literal;
import ir.literals.c.IntegerLiteral;
import ir.literals.c.RangeLiteral;
import ir.types.Type;
import ir.types.c.ArrayRange;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import utility.math.Arithmetic;


public class MatrixIndexes extends IRElement {
  // Matrix with a single element
  public static final MatrixIndexes MatrixSingleElementCentered = new MatrixIndexes(1);
  // Matrix with no elements
  public static final MatrixIndexes MatrixNil = new MatrixIndexes(0);
 
  int nbDims=0;
  public enum IndexType {NOT_SPECIFIED, MAXIMUM, FIXED};
  java.util.Vector<IndexType> firstIndexType=new java.util.Vector<IndexType>();
  java.util.Vector<IndexType> lastIndexType=new java.util.Vector<IndexType>();
  java.util.Vector<Integer> firstIndex=new java.util.Vector<Integer>();
  java.util.Vector<Integer> lastIndex=new java.util.Vector<Integer>();

  // TODO: Index range specifier can be on first and last (ex: convolution coefs)
  java.util.Vector<KernelData> indexRangeSpecifier=new java.util.Vector<KernelData>();
  
  // Dimension amplification factors
  java.util.Vector<Amplification> amplificationDimList=new java.util.Vector<Amplification>();


  //==================================================================
  // Building
  //==================================================================

  // By default, empty matrix
  public MatrixIndexes() {
    nbDims=0;
  }

  public MatrixIndexes(Type t) {    
    init(t);
  }
  
  @SuppressWarnings("unchecked")
  public MatrixIndexes(MatrixIndexes t) {
    //super(t);
    nbDims=t.nbDims;
    firstIndex=(Vector<Integer>) t.firstIndex.clone();
    firstIndexType=(Vector<IndexType>) t.firstIndexType.clone();
    lastIndex=(Vector<Integer>) t.lastIndex.clone();
    lastIndexType=(Vector<IndexType>) t.lastIndexType.clone();
    indexRangeSpecifier=(Vector<KernelData>)t.indexRangeSpecifier.clone();
    amplificationDimList=new java.util.Vector<Amplification>();
    for(Amplification amp:t.amplificationDimList) {
      amplificationDimList.add(new Amplification(amp));
    }
  }

  // 1D matrix with index starting from 0
  public MatrixIndexes(int nb) {    
    nbDims=1;
    firstIndex.add(0);
    firstIndexType.add(IndexType.FIXED);
    lastIndex.add(nb-1);
    lastIndexType.add(IndexType.FIXED);
    indexRangeSpecifier.add(null);
    amplificationDimList.add(new Amplification());
  }
  
  // 2D matrix with index starting from 0
  public MatrixIndexes(int nb0, int nb1) {    
    nbDims=2;
    
    // First dim
    firstIndex.add(0);
    firstIndexType.add(IndexType.FIXED);
    lastIndex.add(nb0-1);
    lastIndexType.add(IndexType.FIXED);
    indexRangeSpecifier.add(null);
    amplificationDimList.add(new Amplification());

    // Second dim
    firstIndex.add(0);
    firstIndexType.add(IndexType.FIXED);
    lastIndex.add(nb1-1);
    lastIndexType.add(IndexType.FIXED);
    indexRangeSpecifier.add(null);
    amplificationDimList.add(new Amplification());
  }
  
  // ND matrix with fixed indexes
  // Note: The index is null in case the index is not fixed.
  public MatrixIndexes(List<Integer> first, List<Integer> last) {
    nbDims=first.size();
    for (int i=0;i<first.size();i++) {
      firstIndex.add(first.get(i));
      if (first.get(i)==null) {
        firstIndexType.add(IndexType.NOT_SPECIFIED);
      }
      else {
        firstIndexType.add(IndexType.FIXED);
      }
      lastIndex.add(last.get(i));
      if (last.get(i)==null) {
        lastIndexType.add(IndexType.NOT_SPECIFIED);
      }
      else {
        lastIndexType.add(IndexType.FIXED);
      }
      indexRangeSpecifier.add(null);
      amplificationDimList.add(new Amplification());
    }
  } 


  protected Type init(Type t) {
    // In C, dimensions are [z][y][x], with then build lists in the reverse order
    for(nbDims=0;t.isArray();t=((ArrayRange)t).getElementType(),nbDims++) {
      ArrayRange tt=(ArrayRange)t;
      if (tt.isDynamic()) {
        if (tt.hasMaxIndexRange()) {
          firstIndex.add(0,tt.getFirstIndex());
          lastIndex.add(0,tt.getLastIndex());
          firstIndexType.add(0,IndexType.MAXIMUM);
          lastIndexType.add(0,IndexType.MAXIMUM);
        }
        else {
          firstIndex.add(0,0);
          lastIndex.add(0,0);
          firstIndexType.add(0,IndexType.NOT_SPECIFIED);
          lastIndexType.add(0,IndexType.NOT_SPECIFIED);
        }
        indexRangeSpecifier.add(0,tt.getSizeSpecifierData());
        amplificationDimList.add(0,new Amplification());    
      }
      else {
        firstIndex.add(0,tt.getFirstIndex());
        lastIndex.add(0,tt.getLastIndex());
        firstIndexType.add(0,IndexType.FIXED);
        lastIndexType.add(0,IndexType.FIXED);
        indexRangeSpecifier.add(0,tt.getSizeSpecifierData());
        amplificationDimList.add(0,new Amplification());    
      }
    }

    if (nbDims==0) {
      // Not an array c type, we consider an array of one element
      nbDims=1;
      firstIndex.add(0);
      lastIndex.add(0);
      firstIndexType.add(IndexType.FIXED);
      lastIndexType.add(IndexType.FIXED);
      indexRangeSpecifier.add(null);   
      amplificationDimList.add(new Amplification());    
    }

    return t;
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
    return getLastIndex(i)-getFirstIndex(i)+1;
  }
  
  public boolean hasSingleElement() {
    if (isEmpty()) { return false; }
    for(int i=0;i<nbDims;i++) {
      if ((firstIndexType.get(i)==IndexType.NOT_SPECIFIED) ||
          (lastIndexType.get(i)==IndexType.NOT_SPECIFIED)) {
        return false;
      }
      if (firstIndex.get(i)!=lastIndex.get(i)) {
        return false;
      }
    }
    return true;
  }
  
  public boolean hasStaticSize() {
    for(int i=0;i<nbDims;i++) {
      if (
          (indexRangeSpecifier.get(i)!=null) ||
          (firstIndexType.get(i)==IndexType.NOT_SPECIFIED) ||
          (lastIndexType.get(i) ==IndexType.NOT_SPECIFIED) ) {
        return false;
      }
    }
    return true;
  }
  
  public IndexType getFirstIndexType(int i) {
    if (isEmpty()) { return IndexType.NOT_SPECIFIED; }
    if (i>=nbDims) { return IndexType.FIXED; }
    return firstIndexType.get(i);
  }

  public IndexType getLastIndexType(int i) { 
    if (isEmpty()) { return IndexType.NOT_SPECIFIED; }
    if (i>=nbDims) { return IndexType.FIXED; }
    return firstIndexType.get(i);
  }

  public int getFirstIndex(int i) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("getFirstIndex: not allowed on amplified matrix");
    }
    
    if (isEmpty()) { return 0; }
    if (i>=nbDims) {
      return 0;
    }
    return firstIndex.get(i);
  }

  public int getLastIndex(int i) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("getLastIndex: not allowed on amplified matrix");
    }

    if (isEmpty()) { return -1; }
    if (i>=nbDims) {
      return 0;
    }
    return lastIndex.get(i);
  }
  
  public KernelData getIndexRangeSpecifier(int i) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("getIndexRangeSpecifier: not allowed on amplified matrix");
    }

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

  public boolean isAmplified() {
    for(Amplification amp:amplificationDimList) {
      if (amp.hasCoefficients()) {
        return true;
      }
    }
    return false;
  }

  
  //==================================================================
  // Queries on dimensions
  //==================================================================
  
  public boolean isSame(MatrixIndexes t) {
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
        if (firstIndexType.get(i)!=t.firstIndexType.get(i)) {
          return false;
        }
        if (((int)firstIndex.get(i))!=(int)t.firstIndex.get(i)) {
         return false;
        }
        // Last index
        if (lastIndexType.get(i)!=t.lastIndexType.get(i)) {
          return false;
        }
        if (((int)lastIndex.get(i))!=(int)t.lastIndex.get(i)) {
          return false;
        }
      }
      if (!amplificationDimList.get(i).isSame(t.amplificationDimList.get(i))) {
        return false;
      }
    }
    // Everything looks equivalent
    return true;
  }

  public boolean includes(MatrixIndexes nd) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified() || nd.isAmplified()) {
      throw new UnsupportedOperationException("includes: not allowed on amplified matrix");
    }

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
      if (firstIndexType.get(i)!=nd.firstIndexType.get(i)) {
        return false;
      }
      if (((int)firstIndex.get(i))>(int)nd.firstIndex.get(i)) {
        return false;
      }
      // Last index
      if (lastIndexType.get(i)!=nd.lastIndexType.get(i)) {
        return false;
      }
      if (((int)lastIndex.get(i))<(int)nd.lastIndex.get(i)) {
        return false;
      }
    }

    return true;
  }
  
  public boolean includes(MatrixSize nd) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("includes: not allowed on amplified matrix");
    }

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
      // Fix size
      if (((int)lastIndex.get(i)-(int)firstIndex.get(i)+1)<(int)nd.nbElems.get(i)) {
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
      if (((int)firstIndex.get(i))!=0) {
       return true;
      }
      // Last index
      if (((int)lastIndex.get(i))!=0) {
        return true;
      }
    }
    return false;
  }

  
  //==================================================================
  // Operations on dimensions
  //==================================================================

  // Matrix size modifications
  public void amplifyMulBy(int i, int c, int nom, boolean ceil) {
    if (i>=nbDims) {
      return;
    }
    // Ceiling as default
    amplificationDimList.get(i).multiplyBy(c,nom,ceil);
 }
  public void amplifyDivBy(int i, int c,  int denom, boolean ceil) {
    if (i>=nbDims) {
      return;
    }
    // Ceiling as default
    amplificationDimList.get(i).devideBy(c,denom,ceil);
 }


  public MatrixIndexes add(MatrixIndexes nd) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified() || nd.isAmplified()) {
      throw new UnsupportedOperationException("add: not allowed on amplified matrix");
    }
    
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
        firstIndex.set(i,firstIndex.get(i)+nd.firstIndex.get(i));
        lastIndex.set(i,lastIndex.get(i)+nd.lastIndex.get(i));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        firstIndexType.add(nd.firstIndexType.get(i));
        firstIndex.add(nd.firstIndex.get(i));
        lastIndexType.add(nd.lastIndexType.get(i));
        lastIndex.add(nd.lastIndex.get(i));
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));
        amplificationDimList.add(new Amplification(nd.amplificationDimList.get(i)));    

        nbDims++;
      }
    }
    
    return this;
  }
  
  public MatrixIndexes sub(MatrixIndexes nd) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified() || nd.isAmplified()) {
      throw new UnsupportedOperationException("sub: not allowed on amplified matrix");
    }
    
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd=indexRangeSpecifier.get(i);
      KernelData kd_nd=nd.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        // Error : can not merge matrixes with non constant size
        throw new UnsupportedOperationException("sub: not allowed on matrix with non constant size");
      }
      else {
        firstIndex.set(i,firstIndex.get(i)-nd.firstIndex.get(i));
        lastIndex.set(i,lastIndex.get(i)-nd.lastIndex.get(i));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        firstIndexType.add(nd.firstIndexType.get(i));
        firstIndex.add(-nd.firstIndex.get(i));
        lastIndexType.add(nd.lastIndexType.get(i));
        lastIndex.add(-nd.lastIndex.get(i));
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));
        amplificationDimList.add(new Amplification(nd.amplificationDimList.get(i)));    

        nbDims++;
      }
    }
    
    return this;
  }

  
 
  public MatrixIndexes sub(MatrixSize da) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("sub: not allowed on amplified matrix");
    }
    
    int n = nbDims > da.nbDims ? da.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd=indexRangeSpecifier.get(i);
      KernelData kd_nd=da.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        // Error : can not merge matrixes with non constant size
        throw new UnsupportedOperationException("minus: not allowed on matrix with non constant size");
      }
      else {
        firstIndex.set(i,firstIndex.get(i));
        lastIndex.set(i,firstIndex.get(i)+Arithmetic.clampToPositive((lastIndex.get(i)-firstIndex.get(i)+1)-da.nbElems.get(i)));
      }
    }

    // Manage additional dimensions
    if (nbDims>da.nbDims) {
      for (int i=da.nbDims;i<nbDims;i++) {
        KernelData kd=indexRangeSpecifier.get(i);
        if ((kd!=null)  ) {
          // Error : can not merge matrixes with non constant size
          throw new UnsupportedOperationException("sub: not allowed on matrix with non constant size");
        }    
        else {
          firstIndex.set(i,firstIndex.get(i));
          lastIndex.set(i,firstIndex.get(i)+Arithmetic.clampToPositive(lastIndex.get(i)-firstIndex.get(i)));
        }
      }
    }
    
    return this;
  }
  
  
  public MatrixIndexes devide(MatrixIndexes nd) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified() || nd.isAmplified()) {
      throw new UnsupportedOperationException("devideDirectBy: not allowed on amplified or non statically defined matrix");
    }
    
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd   =    indexRangeSpecifier.get(i);
      KernelData kd_nd= nd.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        throw new UnsupportedOperationException("devideDirectBy: not allowed on matrix with non constant size");
      }
      else {
        firstIndex.set(i,firstIndex.get(i)/nd.firstIndex.get(i));
        lastIndex.set(i,lastIndex.get(i)/nd.lastIndex.get(i));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        firstIndexType.add(nd.firstIndexType.get(i));
        firstIndex.add( 1/nd.firstIndex.get(i) );
        lastIndexType.add(nd.lastIndexType.get(i));
        lastIndex.add( 1/nd.lastIndex.get(i) );
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));
        amplificationDimList.add(new Amplification(nd.amplificationDimList.get(i)));    

        nbDims++;
      }
    }
    
    return this;
  }

  
  public MatrixIndexes devide(MatrixSize da) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("devideDirectBy: not allowed on amplified or non statically defined matrix");
    }
    
    int n = nbDims > da.nbDims ? da.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd   =    indexRangeSpecifier.get(i);
      KernelData kd_nd= da.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        throw new UnsupportedOperationException("devideDirectBy: not allowed on matrix with non constant size");
      }
      else {
        firstIndex.set(i,firstIndex.get(i)/da.getNbElements(i));
        lastIndex.set(i,lastIndex.get(i)/da.getNbElements(i));
      }
    }

    // No need to manage additional dimensions

    return this;
  }

  
  public MatrixIndexes multiply(MatrixIndexes nd) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified() || nd.isAmplified()) {
      throw new UnsupportedOperationException("multiplyDirectBy: not allowed on amplified or non statically defined matrix");
    }
    
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd   =    indexRangeSpecifier.get(i);
      KernelData kd_nd= nd.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        throw new UnsupportedOperationException("multiplyDirectBy: not allowed on matrix with non constant size");
      }
      else {
        firstIndex.set(i,firstIndex.get(i)*nd.firstIndex.get(i));
        lastIndex.set(i,lastIndex.get(i)*nd.lastIndex.get(i));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        firstIndexType.add(nd.firstIndexType.get(i));
        firstIndex.add(0);
        lastIndexType.add(nd.lastIndexType.get(i));
        lastIndex.add(0);
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));
        amplificationDimList.add(new Amplification(nd.amplificationDimList.get(i)));    

        nbDims++;
      }
    }
    
    return this;
  }

  
  public MatrixIndexes multiply(MatrixSize da) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("multiplyDirectBy: not allowed on amplified or non statically defined matrix");
    }
    
    int n = nbDims > da.nbDims ? da.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd   =    indexRangeSpecifier.get(i);
      KernelData kd_nd= da.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        throw new UnsupportedOperationException("multiplyDirectBy: not allowed on matrix with non constant size");
      }
      else {
        firstIndex.set(i,firstIndex.get(i)*da.getNbElements(i));
        lastIndex.set(i,lastIndex.get(i)*da.getNbElements(i));
      }
    }

    // No need to manage additional dimensions
    
    return this;
  }
   
  
  public MatrixIndexes devideRoundTowardsAbsolute(MatrixSize da) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("devideRoundTowardsAbsolute: not allowed on amplified matrix");
    }
    
    // Normalize to positive values the 'grain'
      
    int n = nbDims > da.nbDims ? da.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd=indexRangeSpecifier.get(i);
      KernelData kd_da=da.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_da!=null) ) {
        // Error : can not merge matrixes with non constant size
        throw new UnsupportedOperationException("devideRoundTowardsAbsolute: not allowed on matrix with non constant size");
      }
      else {
        // Note: In java, the modulo takes the sign of the denominator
        int g=da.getNbElements(i);
        if (g<0) { g=-g; }
        firstIndex.set(i,(firstIndex.get(i)-(g-1))/g);
        lastIndex.set(i,(lastIndex.get(i)+(g-1))/g);
      }
    }
    
    // No need to manage additional dimensions

    return this;
  }
  
  
  public void lcm(MatrixIndexes nd) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified() || nd.isAmplified()) {
      throw new UnsupportedOperationException("lcm: not allowed on amplified or non statically defined matrix");
    }
    
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd   =    indexRangeSpecifier.get(i);
      KernelData kd_nd= nd.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        throw new UnsupportedOperationException("lcm: not allowed on matrix with non constant size");
      }
      else {
        firstIndex.set(i,Arithmetic.lcm(firstIndex.get(i),nd.firstIndex.get(i)));
        lastIndex.set(i,Arithmetic.lcm(lastIndex.get(i),nd.lastIndex.get(i)));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        firstIndexType.add(nd.firstIndexType.get(i));
        firstIndex.add(nd.firstIndex.get(i));
        lastIndexType.add(nd.lastIndexType.get(i));
        lastIndex.add(nd.lastIndex.get(i));
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));
        amplificationDimList.add(new Amplification(nd.amplificationDimList.get(i)));    

        nbDims++;
      }
    }
  }

  public void gcd(MatrixIndexes nd) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified() || nd.isAmplified()) {
      throw new UnsupportedOperationException("gcd: not allowed on amplified or non statically defined matrix");
    }
    
    int n = nbDims > nd.nbDims ? nd.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd   =    indexRangeSpecifier.get(i);
      KernelData kd_nd= nd.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_nd!=null) ) {
        throw new UnsupportedOperationException("gcd: not allowed on matrix with non constant size");
      }
      else {
        firstIndex.set(i,Arithmetic.gcd(firstIndex.get(i),nd.firstIndex.get(i)));
        lastIndex.set(i,Arithmetic.gcd(lastIndex.get(i),nd.lastIndex.get(i)));
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        firstIndexType.add(nd.firstIndexType.get(i));
        firstIndex.add(nd.firstIndex.get(i));
        lastIndexType.add(nd.lastIndexType.get(i));
        lastIndex.add(nd.lastIndex.get(i));
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));
        amplificationDimList.add(new Amplification(nd.amplificationDimList.get(i)));    

        nbDims++;
      }
    }
  }
  
  
  public MatrixIndexes inflateToMultipleOf(MatrixSize da) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("inflateToGrain: not allowed on amplified matrix");
    }
    
    // Normalize to positive values the 'grain'
      
    int n = nbDims > da.nbDims ? da.nbDims : nbDims;

    // Manage common dimensions
    for (int i=0;i<n;i++) {
      KernelData kd=indexRangeSpecifier.get(i);
      KernelData kd_da=da.indexRangeSpecifier.get(i);
      if ((kd!=null) || (kd_da!=null) ) {
        // Error : can not merge matrixes with non constant size
        throw new UnsupportedOperationException("inflateToGrain: not allowed on matrix with non constant size");
      }
      else {
        // Note: In java, the modulo takes the sign of the denominator
        int g=da.getNbElements(i);
        if (g<0) { g=-g; }
        // First index (towards -infinit)
        {
          int index=firstIndex.get(i);
          if (index<0) {
            firstIndex.set(i,((index-(g-1))/g)*g);
          }
          else {
            firstIndex.set(i,(index/g)*g);
          }
        }
        // Last index (towards +infinit)
        {
          int index=lastIndex.get(i);
          if (index<0) {
            lastIndex.set(i,(index/g)*g);
          }
          else {
           lastIndex.set(i,((index+(g-1))/g)*g);
          }
        }
      }
    }
    
    // No need to manage additional dimensions
   
    return this;
  }
  
  public void union(MatrixIndexes nd) throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified() || nd.isAmplified()) {
      throw new UnsupportedOperationException("unionMergeWith: not allowed on amplified matrix");
    }

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
        IndexType id=firstIndexType.get(i);
        IndexType id_nd=nd.firstIndexType.get(i);

        if (id==IndexType.NOT_SPECIFIED || id_nd==IndexType.NOT_SPECIFIED) {
          firstIndexType.set(i,IndexType.NOT_SPECIFIED);
          firstIndex.set(i,0);
        }
        else {
          int fi=firstIndex.get(i);
          int fi_nd=nd.firstIndex.get(i);
          if (fi_nd<fi) {
            firstIndexType.set(i,id_nd);
            firstIndex.set(i,fi_nd);
          }
        }
      }
      {  // Last index
        IndexType id=lastIndexType.get(i);
        IndexType id_nd=nd.lastIndexType.get(i);

        if (id==IndexType.NOT_SPECIFIED || id_nd==IndexType.NOT_SPECIFIED) {
          lastIndexType.set(i,IndexType.NOT_SPECIFIED);
          lastIndex.set(i,0);
        }
        else {
          int fi=lastIndex.get(i);
          int fi_nd=nd.lastIndex.get(i);
          if (fi_nd>fi) {
            lastIndexType.set(i,id_nd);
            lastIndex.set(i,fi_nd);
          }
        }
      }
    }

    // Manage additional dimensions
    if (nd.nbDims>nbDims) {
      for (int i=nbDims;i<nd.nbDims;i++) {
        firstIndexType.add(nd.firstIndexType.get(i));
        firstIndex.add(nd.firstIndex.get(i));
        lastIndexType.add(nd.lastIndexType.get(i));
        lastIndex.add(nd.lastIndex.get(i));
        indexRangeSpecifier.add(nd.indexRangeSpecifier.get(i));
        amplificationDimList.add(new Amplification(nd.amplificationDimList.get(i)));    

        nbDims++;
      }
    }
  }
  
  public void setLastIndexesAsZero() throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("setLastIndexesAsZero: not allowed on amplified matrix");
    }

    for (int i=0;i<nbDims;i++) {
      // Last index to 0
      lastIndex.setElementAt(0, i);
    }
  }
  
  public void setFirstIndexesAsZero() throws UnsupportedOperationException {
    // Does not work on amplified matrixes
    if (isAmplified()) {
      throw new UnsupportedOperationException("setFirstIndexesAsZero: not allowed on amplified matrix");
    }

    for (int i=0;i<nbDims;i++) {
      // Last index to 0
      firstIndex.setElementAt(0, i);
    }
  }
  
  public void symmetryUnion() throws UnsupportedOperationException {
    if (this.isAmplified()) {
      throw new UnsupportedOperationException("symmetryUnion not allowed on amplified matrix");
    }
    for (int i=0;i<nbDims;i++) {
      // Last index to 0
      int max=(-firstIndex.get(i))>lastIndex.get(i)?-firstIndex.get(i):lastIndex.get(i);
      firstIndex.setElementAt(-max, i);
      lastIndex.setElementAt(max, i);
    }
  }
  
   
  //==================================================================
  // Code generation
  //==================================================================

  public String getLastIndexPlusOneString(int i) {
    String s=null;
    KernelData kd=indexRangeSpecifier.get(i);
    if (kd!=null) {
      s=kd.getName();
    }
    else {
      s=Integer.toString(lastIndex.get(i)+1);
    }

    return amplificationDimList.get(i).generateString(s);
  }
  
  public void generateLastIndexPlusOne(int i,PrintStream ps) {
    String s=null;
    KernelData kd=indexRangeSpecifier.get(i);    
    if (kd!=null) {
      s=kd.getName();
    }
    else {
      s=Integer.toString(lastIndex.get(i)+1);
    }
    
    amplificationDimList.get(i).generate(s,ps);
  }
  
  public void generateMaxNbElementFormula(int i,PrintStream ps) {
    String s=null;
    KernelData kd=indexRangeSpecifier.get(i);
    if (kd!=null) {
      ps.print(kd.getName());
    }
    else {
      ps.print(Integer.toString(lastIndex.get(i)+1));
    }
    
    amplificationDimList.get(i).generate(s,ps);
  }

  public int getMaxNbElement(int dim) {
    KernelData kd=indexRangeSpecifier.get(dim);
    int base_size=0;
    if (kd!=null) {
      Literal init=kd.getInitializer();
      if (init==null) {
        return -1;
      }
      // Must be a range literal
      else if (init instanceof RangeLiteral) {
        RangeLiteral rl=(RangeLiteral)init;
        base_size=(int)rl.getLastValue();
      }
      else if (init instanceof IntegerLiteral) {
        IntegerLiteral il=(IntegerLiteral)init;
        base_size=(int)il.getValue();
      }
      else {
        // Internal error
        // ce.raiseError("variable array size specifier variable must have range literal");
      }
    }
    else {
      base_size=lastIndex.get(dim);
    }
    for(int i=amplificationDimList.size()-1;i>=0;i--) {
      Amplification a=amplificationDimList.get(i);
      base_size=a.applyTo(base_size);
    }
    return base_size;
  }
   
  public String getLastIndexString(int i) {
    String s=null;
    KernelData kd=indexRangeSpecifier.get(i);
    if (kd!=null) {
      s=kd.getName()+"-1";
    }
    else {
      s=Integer.toString(lastIndex.get(i));
    }

    return amplificationDimList.get(i).generateString(s);
  }

  public void generateLastIndex(int i,PrintStream ps) {
    String s=null;
    KernelData kd=indexRangeSpecifier.get(i);
    if (kd!=null) {
      s=kd.getName()+"-1";   
    }
    else {
      s=Integer.toString(lastIndex.get(i));
    }
 
    amplificationDimList.get(i).generate(s,ps);
  }

  //==================================================================
  // Verbose
  //==================================================================
 
  public void generate(PrintStream ps) {
    if (isEmpty()) {
      ps.print("[<EmptyMatrix>]");
      return;
    }
    for (int i=nbDims-1;i>=0;i--) {
      ps.print("[");
      Amplification amp=amplificationDimList.get(i);

      String s=null;
      KernelData kd=indexRangeSpecifier.get(i);
      if (kd!=null) {
        // This is a variable
        s=kd.getName();
      }
      else {
        // This is a constant
        StringBuffer sb=new StringBuffer();
        if (firstIndex.get(i)==0) {
          printIndex(sb, amp.applyTo(lastIndex.get(i)+1), lastIndexType.get(i));
        }
        else {
          printIndex(sb, amp.applyTo(firstIndex.get(i)), firstIndexType.get(i));
          sb.append("..");
          printIndex(sb, amp.applyTo(lastIndex.get(i)+1)-1, lastIndexType.get(i));
        }
        s=sb.toString();
      }

      amp.generate(s,ps);

      ps.print("]");
    }
  }

  public void generateNumberOfElements(PrintStream ps) {
    for (int i=nbDims-1;i>=0;i--) {
      if (i!=nbDims-1) { ps.print("*"); }
      ps.print("(");
      Amplification amp=amplificationDimList.get(i);
     
      String s=null;
      KernelData kd=indexRangeSpecifier.get(i);
      if (kd!=null) {
        // This is a variable
        s=kd.getName();
      }
      else {
        // This is a constant
        StringBuffer sb=new StringBuffer();
        if (firstIndex.get(i)==0) {
          printIndex(sb, amp.applyTo(lastIndex.get(i)+1), lastIndexType.get(i));
        }
        else {
          // TODO: Looks like a bug here !
          printIndex(sb, amp.applyTo(firstIndex.get(i)), firstIndexType.get(i));
          sb.append("..");
          printIndex(sb, amp.applyTo(lastIndex.get(i)+1)-1, lastIndexType.get(i));
        }
        s=sb.toString();
      }

      amp.generate(s,ps);

      ps.print(")");
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
    sb.append(Integer.toString(nbDims)).append("D Matrix indexes ");
    if (isEmpty()) {
      sb.append("{<empty>}");
    }
    else {
      for (int i=0;i<nbDims;i++) {
        sb.append("{");
        
        Amplification amp=amplificationDimList.get(i);
        
        StringBuffer sb2 = new StringBuffer();
        KernelData kd=indexRangeSpecifier.get(i);
        if (kd!=null) {
          sb2.append(kd.getName());
          sb2.append(" /*");
        }

        printIndex(sb2, amp.applyTo(firstIndex.get(i)), firstIndexType.get(i));
        sb2.append("..");
        printIndex(sb2, amp.applyTo(lastIndex.get(i)+1)-1, lastIndexType.get(i));

        if (kd!=null) {
          sb2.append("*/");
        }
        
        sb.append(amp.toString(sb2.toString()));

        sb.append("}");
      }
    }
    
    return sb.toString();
  }


}
