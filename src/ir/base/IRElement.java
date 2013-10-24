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

package ir.base;


public class IRElement {
  //------------------------------
  // Private data
  //------------------------------

  // ID unique to each compiler thread
  private int id;
  private static final InheritableThreadLocal<Integer> id_counter = new InheritableThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };
  private int incrementIdCounter() {
    int i=id_counter.get();
    id_counter.set(i+1);
    return i;
  }

   
  // Position in the original text
  protected int     lineNum = 0;
  protected String  source = null;

  
  public IRElement() {
    id = incrementIdCounter();
  }
  
  public long getId() {
    return id;
  }
  
  
  public void setLineNum(int lineNum_) { 
    lineNum = lineNum_; 
  }

  public int getLineNum() { 
    return lineNum = 0;
  }
  
  /** Return the source file of this node */
  public String getSource() { 
    return(source); 
  }
  /** Set the source file of this node */
  public void setSource(String source_) { 
    source = source_; 
  }
  
}
