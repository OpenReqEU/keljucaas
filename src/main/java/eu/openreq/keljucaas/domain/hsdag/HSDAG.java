/**
 * Copyright Siemens AG, 2016-2017
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

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import eu.openreq.keljucaas.domain.release.Diagnosable;

/**
 * This class implements HSDAG algorithm for detecting all diagnoses.
 */
public abstract class HSDAG {

	protected List<Diagnosable> userConstraints;
	protected PriorityQueue<TreeNode> queue;
	protected int indexOfConstraint;

	/**
   * Prepares HSDAG for diagnosing {@code userConstraints}, where {@code mznFullFileName} (optional) is regarded as fixed (i.e. the knowledge base).
   * 
   * @param userConstraints
   *            constraint that are sets (variable assignments)
   */


  public HSDAG(List<Diagnosable> userConstraints) {
    this.userConstraints = userConstraints;
    queue = new PriorityQueue<TreeNode>();
  }

  /**
   * Function that gets the diagnoses.
   * 
   * @return A collection with all diagnoses.
   * @throws DiagnosisException 
   */
  public abstract DiagnosesCollection diagnose();

  protected abstract void buildDiagnosesTree(TreeNode root, DiagnosesCollection diagnosesCollection, int maxDepth);
      

	protected String diagnoseToString(List<Diagnosable> diagnose) {
		StringBuilder sb = new StringBuilder();
		int count = diagnose.size();
		for (int i = count - 1; i >= 0; i--) {
			Diagnosable c = diagnose.get(i);
			sb.append(c.getNameId() + "; ");
		}
		return sb.toString();
	}

	protected List<Diagnosable> removeConstraintFromList(List<Diagnosable> constraints, Diagnosable constraint) {
		List<Diagnosable> returnList = new ArrayList<Diagnosable>();
		if (constraint != null) {
			for (Diagnosable ct : constraints) {
				if (!ct.getId().equals(constraint.getId())) {
					returnList.add(ct);
				}

			}
		}
		return returnList;
	}
	
	protected void displayNodeConstraint(TreeNode node, Diagnosable constraint)
	{
		indexOfConstraint = node.getData().indexOf(constraint) + 1;
		String constraintIndex = String.valueOf(indexOfConstraint);
		String index = constraintIndex + ")";
		String rootName = node.name;

		if (rootName != null) {
			index = String.format("%s.%s)", rootName, constraintIndex);
		}
	}

}