/**
 * Copyright Siemens AG, 2016
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/**
 * 
 * Adapted by juhaTee / University of Helsinki for OpenReq project  
 * 
 */

package eu.openreq.keljucaas.domain.hsdag;

import java.util.List;

import eu.openreq.keljucaas.domain.release.Diagnosable;

/**
 * Class for a node from the HSDAG tree
 * @author z003pczy (Rosu Mara)
 */
public class TreeNode implements Comparable<TreeNode> {	
	/**
	 *  List with constraints for minimal conflict set. Can be null.
	 */
	private List<Diagnosable> data;
	
	private List<Diagnosable> initialConstraintsSet;
		
	/**
	 *  parent node	
	 */
	private TreeNode parent;
	
	private int depth;
	
	/**
	 * This is the constraint associated to the arch which comes to this node. Can be null for parent nodes.
	 */
	private Diagnosable constraint = null;

	public String name; 
	
	public TreeNode(List<Diagnosable> data, List<Diagnosable> initialConstraintsSet, String name, int depth) {
		super();
		this.data = data;		
		this.initialConstraintsSet = initialConstraintsSet;
		this.name = name;
		this.depth = depth;
		
		
	}

	public void addChild(Diagnosable c, TreeNode child) {		
		child.parent = this;
		child.constraint = c;	
		if(this.name!=null)
		{
			child.name = this.name + "." + child.name;
		}
		
	}

	public TreeNode getParentNode() {
		return this.parent;
	}
	
	public List<Diagnosable> getData() {
		return this.data;
	}
		
	public String toString(){		
		StringBuilder sb = new StringBuilder();		
		sb.append("Data: ");
		if (data == null){
			sb.append("-");
		} else {
			for(Diagnosable c : data){
				sb.append(c.getNameId()).append("; ");
			}
		}
		sb.append("\n");

		sb.append("Depth: ");
		sb.append(depth);
		sb.append("\n");

		
		sb.append("InitialConstraintsSet: ");
		if (initialConstraintsSet == null){
			sb.append("-");
		} else {
			for(Diagnosable c : initialConstraintsSet){
				sb.append(c.getNameId()).append("; ");
			}
		}
		sb.append("\n");
		
		if (constraint == null){
			sb.append("Constraint: null");
		} else {
			sb.append("Constraint: " + constraint.getNameId());
		}
		
		return sb.toString();
	}
	
	public Diagnosable getConstraint(){
		return constraint;  
	}

	public List<Diagnosable> getInitialConstraintsSet() {
		return initialConstraintsSet;
	}

	public final int getDepth() {
		return depth;
	}

	@Override
	public int compareTo(TreeNode other) {
		return this.depth - other.depth;
	}	
	
	
}
