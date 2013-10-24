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

/* OperatorMulti configurable algorithmic node */

package ir.algorithms;

import parser.TNode;
import ir.base.KernelData;
import common.CompilerError;

abstract public class OperatorMulti extends Operator {
  String operation=null;
  String finalOperation=null;

  void setOperation(String s) {
    operation=s;
  }
  
  void setFinalOperation(String s) {
    finalOperation=s;
  }

  public void SetPropertyWithString(String prop, String s, TNode tn, CompilerError ce) {
    raiseUnknownStringPropertyError(prop,tn,ce);
  }

  public boolean completeAndCheckNode(CompilerError ce) {
    StringBuffer sb=new StringBuffer();
    boolean first=true;

    sb.append("@");
    sb.append(getName());
    sb.append("=");

    if (finalOperation!=null) {
      sb.append(finalOperation);
      sb.append("(");
    }
    for(KernelData kd:getInputDataList()) {
      if (!first) {
        sb.append(operation);
      }
      sb.append("$");
      sb.append(kd.getName());
      first=false;
    }
    if (finalOperation!=null) {
      sb.append(")");
    }
    sb.append(";");

    function=sb.toString();

    return super.completeAndCheckNode(ce);
  }
}
