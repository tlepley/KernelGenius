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

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonErrorNode;


/** A node representing erroneous token range in token stream */
public class TNodeError extends TNode {
        org.antlr.runtime.tree.CommonErrorNode delegate;
 
        public TNodeError(TokenStream input, Token start, Token stop,
                                           RecognitionException e)
        {
                delegate = new CommonErrorNode(input,start,stop,e);
        }
 
        public boolean isNil() { return delegate.isNil(); }
 
        public int getType() { return delegate.getType(); }
 
        public String getText() { return delegate.getText(); }
        public String toString() { return delegate.toString(); }
}
