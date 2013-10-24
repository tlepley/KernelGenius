/*
  This file is part of KernelGenius.
  
  Authors: Monty Zukoski, Thierry Lepley
*/

/* Node of the compiler AST */

package parser;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;

/** 
  Class TNode is an implementation of the AST interface
  and adds many useful features:

  It is double-linked for reverse searching.
  (this is currently incomplete, in that method doubleLink() must
  be called after any changes to the tree to maintain the 
  reverse links).

  It can store a definition node (defNode), so that nodes such
  as scoped names can refer to the node that defines the name.

  It stores line numbers for nodes.

  Searches for parents and children of a tree can be done
  based on their type.

  The tree can be printed to System.out using a lisp-style syntax.



 */
public class TNode extends CommonTree {
  protected Integer tokenNumber = null;
  protected String  source=null;
  protected Map<String,Object> attributes = null;
  
  
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
  // Constructor
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

  public TNode() {
     super();
     //System.err.println("Create a TNode without token");
  }

  // Constructor from a token
  public TNode(Token token) {
    super(token);

    MyToken tok = (MyToken) token;

    // Origin for non NIL token
    if (tok!=null) {
      source      = tok.getSource();
    }
    
    //System.err.println("Create a TNode with token : "+token+", "+this);
  }
  
  // Copy constructor
  public TNode(CommonTree tr) {
    TNode t = (TNode) tr;

    // Copy informations
    tokenNumber = t.getTokenNumber(); // Integer immutable

    // origin
    source      = t.getSource();

    // Attributes
    if (t.attributes==null) {
      attributes=null;
    }
    else {
      attributes = new HashMap<String,Object>(t.attributes);
    }
  }


  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
  // Other Information
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


  /** Return the source file of this node */
  public String getSource() { 
    return(source); 
  }
  /** Set the source file of this node */
  public void setSource(String source_) { 
    source = source_; 
  }

  /** Return the token number of this node */
  public Integer getTokenNumber() { 
    return(tokenNumber);
  }
  /** Set the token number of this node */
  public void setTokenNumber(Integer tokenNumber_) { 
    tokenNumber=tokenNumber_;
  }

  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
  // Attributes
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
  
  /** get the Map that holds attribute values.
   */  
  public Map<String,Object> getAttributesTable() {
    if(attributes == null) {
      attributes = new HashMap<String,Object>(7);
    }
    return(attributes);
  }
  
  /** lookup the attribute name in the attribute table.
    If the value does not exist, it returns null.
    */
  public Object getAttribute(String attrName) {
    if(attributes == null) {
      return(null);
    }
    else {
      return(attributes.get(attrName));
    }
  }
  
  /** lookup the attribute name in the attribute table.
    If the value does not exist, true.
    */
  public boolean isAttribute(String attrName) {
    if(attributes == null) {
      return(false);
    }
    else {
      return(attributes.containsKey(attrName));
    }
  }
  
  /** set an attribute in the attribute table.
   */
  public void setAttribute(String attrName, Object value) {
    if(attributes == null) {
      attributes = new HashMap<String,Object>(7);
    }
    attributes.put(attrName,value);
  }
  
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
  // Generation
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
  public void generatePreproDirective(PrintStream ps) {
    ps.println();
    ps.print("#line ");
    ps.print(getLine());
    ps.print("\"");
    ps.print(getSource());
    ps.print("\"");
    ps.println();
  }
    

  
}


