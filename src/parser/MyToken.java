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

package parser;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;


@SuppressWarnings("serial")
public class MyToken extends CommonToken {
  String source = "";
  public String getSource() { return source; }
  public void setSource(String src) { source = src; }
  
  public MyToken(CharStream input, int type, int channel,
                 int start, int stop) {
      super(input, type, channel, start, stop);
  }
  
  public MyToken(int type, String text) {
    super(type,text);
  }
  
  public MyToken(Token t) {
    super(t);
    setSource(((MyToken)t).getSource());
  }

  public String toString() {
      return "MyToken:" +"(" + hashCode() + ")" + "[" + getType() + "] "+ getText() + " line:" + getLine() + " source:" + source ;
  }
}
