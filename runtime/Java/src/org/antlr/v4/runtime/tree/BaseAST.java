/*
 [The "BSD license"]
 Copyright (c) 2011 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.antlr.v4.runtime.tree;

import java.util.*;

/** A generic tree implementation with no payload.  You must subclass to
 *  actually have any user data.  ANTLR v3 uses a list of children approach
 *  instead of the child-sibling approach in v2.  A flat tree (a list) is
 *  an empty node whose children represent the list.  An empty, but
 *  non-null node is called "nil".
 */
public abstract class BaseAST implements AST {
	/** Who is the parent node of this node; if null, implies node is root */
	public BaseAST parent;

	protected List<BaseAST> children;

	/** What index is this node in the child list? Range: 0..n-1 */
	public int childIndex = -1;

	public BaseAST() {
	}

	/** Create a new node from an existing node does nothing for BaseTree
	 *  as there are no fields other than the children list, which cannot
	 *  be copied as the children are not considered part of this node.
	 */
	public BaseAST(AST node) {
	}

	public BaseAST getChild(int i) {
		if ( children==null || i>=children.size() ) {
			return null;
		}
		return children.get(i);
	}

	/** Get the children internal List; note that if you directly mess with
	 *  the list, do so at your own risk.
	 */
	public List getChildren() {
		return children;
	}

	public AST getFirstChildWithType(int type) {
		for (int i = 0; children!=null && i < children.size(); i++) {
			AST t = (AST) children.get(i);
			if ( t.getType()==type ) {
				return t;
			}
		}
		return null;
	}

	public int getChildCount() {
		if ( children==null ) {
			return 0;
		}
		return children.size();
	}

	/** Add t as child of this node.
	 *
	 *  Warning: if t has no children, but child does
	 *  and child isNil then this routine moves children to t via
	 *  t.children = child.children; i.e., without copying the array.
	 */
	public void addChild(BaseAST t) {
		//System.out.println("add child "+t.toStringTree()+" "+this.toStringTree());
		//System.out.println("existing children: "+children);
		if ( t==null ) {
			return; // do nothing upon addChild(null)
		}
		BaseAST childTree = (BaseAST)t;
		if ( childTree.isNil() ) { // t is an empty node possibly with children
			if ( this.children!=null && this.children == childTree.children ) {
				throw new RuntimeException("attempt to add child list to itself");
			}
			// just add all of childTree's children to this
			if ( childTree.children!=null ) {
				if ( this.children!=null ) { // must copy, this has children already
					int n = childTree.children.size();
					for (int i = 0; i < n; i++) {
						BaseAST c = childTree.children.get(i);
						this.children.add(c);
						// handle double-link stuff for each child of nil root
						c.setParent(this);
						c.setChildIndex(children.size()-1);
					}
				}
				else {
					// no children for this but t has children; just set pointer
					// call general freshener routine
					this.children = childTree.children;
					this.freshenParentAndChildIndexes();
				}
			}
		}
		else { // child is not nil (don't care about children)
			if ( children==null ) {
				children = createChildrenList(); // create children list on demand
			}
			children.add(t);
			childTree.setParent(this);
			childTree.setChildIndex(children.size()-1);
		}
		// System.out.println("now children are: "+children);
	}

	/** Add all elements of kids list as children of this node */
	public void addChildren(List<BaseAST> kids) {
		if ( kids==null ) return;
		for (int i = 0; i < kids.size(); i++) {
			BaseAST t = kids.get(i);
			addChild(t);
		}
	}

	public void setChild(int i, BaseAST t) {
		if ( t==null ) {
			return;
		}
		if ( ((AST)t).isNil() ) {
			throw new IllegalArgumentException("Can't set single child to a list");
		}
		if ( children==null ) {
			children = createChildrenList();
		}
		children.set(i, t);
		t.setParent(this);
		t.setChildIndex(i);
	}

	public int getChildIndex() {
		return childIndex;
	}

	public AST getParent() {
		return parent;
	}

	public void setParent(BaseAST t) {
		this.parent = t;
	}

	public void setChildIndex(int index) {
		this.childIndex = index;
	}

	public Object deleteChild(int i) {
		if ( children==null ) {
			return null;
		}
		AST killed = (AST)children.remove(i);
		// walk rest and decrement their child indexes
		this.freshenParentAndChildIndexes(i);
		return killed;
	}

	public boolean deleteChild(AST t) {
		for (int i=0; i<children.size(); i++) {
			Object c = children.get(i);
			if ( c == t ) {
				deleteChild(t.getChildIndex());
				return true;
			}
		}
		return false;
	}

	/** Insert child t at child position i (0..n-1) by shifting children
		i+1..n-1 to the right one position. Set parent / indexes properly
	 	but does NOT collapse nil-rooted t's that come in here like addChild.
	 */
	public void insertChild(int i, BaseAST t) {
		if (i < 0 || i >= getChildCount()) {
			throw new IndexOutOfBoundsException(i+" out or range");
		}

		children.add(i, t);
		// walk others to increment their child indexes
		// set index, parent of this one too
		this.freshenParentAndChildIndexes(i);
	}

	/** Delete children from start to stop and replace with t even if t is
	 *  a list (nil-root tree).  num of children can increase or decrease.
	 *  For huge child lists, inserting children can force walking rest of
	 *  children to set their childindex; could be slow.
	 */
	public void replaceChildren(int startChildIndex, int stopChildIndex, Object t) {
		/*
		System.out.println("replaceChildren "+startChildIndex+", "+stopChildIndex+
						   " with "+((BaseTree)t).toStringTree());
		System.out.println("in="+toStringTree());
		*/
		if ( children==null ) {
			throw new IllegalArgumentException("indexes invalid; no children in list");
		}
		int replacingHowMany = stopChildIndex - startChildIndex + 1;
		int replacingWithHowMany;
		BaseAST newTree = (BaseAST)t;
		List<BaseAST> newChildren = null;
		// normalize to a list of children to add: newChildren
		if ( newTree.isNil() ) {
			newChildren = newTree.children;
		}
		else {
			newChildren = new ArrayList(1);
			newChildren.add(newTree);
		}
		replacingWithHowMany = newChildren.size();
		int numNewChildren = newChildren.size();
		int delta = replacingHowMany - replacingWithHowMany;
		// if same number of nodes, do direct replace
		if ( delta == 0 ) {
			int j = 0; // index into new children
			for (int i=startChildIndex; i<=stopChildIndex; i++) {
				BaseAST child = (BaseAST)newChildren.get(j);
				children.set(i, child);
				child.setParent(this);
				child.setChildIndex(i);
                j++;
            }
		}
		else if ( delta > 0 ) { // fewer new nodes than there were
			// set children and then delete extra
			for (int j=0; j<numNewChildren; j++) {
				children.set(startChildIndex+j, newChildren.get(j));
			}
			int indexToDelete = startChildIndex+numNewChildren;
			for (int c=indexToDelete; c<=stopChildIndex; c++) {
				// delete same index, shifting everybody down each time
				children.remove(indexToDelete);
			}
			freshenParentAndChildIndexes(startChildIndex);
		}
		else { // more new nodes than were there before
			// fill in as many children as we can (replacingHowMany) w/o moving data
			for (int j=0; j<replacingHowMany; j++) {
				children.set(startChildIndex+j, newChildren.get(j));
			}
			int numToInsert = replacingWithHowMany-replacingHowMany;
			for (int j=replacingHowMany; j<replacingWithHowMany; j++) {
				children.add(startChildIndex+j, newChildren.get(j));
			}
			freshenParentAndChildIndexes(startChildIndex);
		}
		//System.out.println("out="+toStringTree());
	}

	/** Override in a subclass to change the impl of children list */
	protected List createChildrenList() {
		return new ArrayList();
	}

	public boolean isNil() {
		return false;
	}

	/** Set the parent and child index values for all child of t */
	public void freshenParentAndChildIndexes() {
		freshenParentAndChildIndexes(0);
	}

	public void freshenParentAndChildIndexes(int offset) {
		int n = getChildCount();
		for (int c = offset; c < n; c++) {
			BaseAST child = getChild(c);
			child.setChildIndex(c);
			child.setParent(this);
		}
	}

	public void freshenParentAndChildIndexesDeeply() {
		freshenParentAndChildIndexesDeeply(0);
	}

	public void freshenParentAndChildIndexesDeeply(int offset) {
		int n = getChildCount();
		for (int c = offset; c < n; c++) {
			BaseAST child = (BaseAST)getChild(c);
			child.setChildIndex(c);
			child.setParent(this);
			child.freshenParentAndChildIndexesDeeply();
		}
	}

	public void sanityCheckParentAndChildIndexes() {
		sanityCheckParentAndChildIndexes(null, -1);
	}

	public void sanityCheckParentAndChildIndexes(AST parent, int i) {
		if ( parent!=this.getParent() ) {
			throw new IllegalStateException("parents don't match; expected "+parent+" found "+this.getParent());
		}
		if ( i!=this.getChildIndex() ) {
			throw new IllegalStateException("child index of "+this.toStringTree()+" doesn't match in "+parent.toStringTree()+"; expected "+i+" found "+this.getChildIndex());
		}
		int n = this.getChildCount();
		for (int c = 0; c < n; c++) {
			CommonAST child = (CommonAST)this.getChild(c);
			child.sanityCheckParentAndChildIndexes(this, c);
		}
	}

    /** Walk upwards looking for ancestor with this token type. */
    public boolean hasAncestor(int ttype) { return getAncestor(ttype)!=null; }

    /** Walk upwards and get first ancestor with this token type. */
    public AST getAncestor(int ttype) {
        AST t = this;
        t = t.getParent();
        while ( t!=null ) {
            if ( t.getType()==ttype ) return t;
            t = t.getParent();
        }
        return null;
    }

    /** Return a list of all ancestors of this node.  The first node of
     *  list is the root and the last is the parent of this node.
     */
    public List getAncestors() {
        if ( getParent()==null ) return null;
        List<AST> ancestors = new ArrayList();
        AST t = this;
        t = t.getParent();
        while ( t!=null ) {
            ancestors.add(0, t); // insert at start
            t = t.getParent();
        }
        return ancestors;
    }

	/** Don't use standard tree printing mechanism since ASTs can have nil
	 *  root nodes.
	 */
    public String toStringTree() {
		if ( children==null || children.size()==0 ) {
			return this.toString();
		}
		StringBuffer buf = new StringBuffer();
		if ( !isNil() ) {
			buf.append("(");
			buf.append(this.toString());
			buf.append(' ');
		}
		for (int i = 0; children!=null && i < children.size(); i++) {
			AST t = children.get(i);
			if ( i>0 ) {
				buf.append(' ');
			}
			buf.append(t.toStringTree());
		}
		if ( !isNil() ) {
			buf.append(")");
		}
		return buf.toString();
	}
}