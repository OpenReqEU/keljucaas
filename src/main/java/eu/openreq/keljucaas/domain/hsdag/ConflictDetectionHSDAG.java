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
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import eu.openreq.keljucaas.domain.release.Diagnosable;

public class ConflictDetectionHSDAG extends HSDAG {

	private DiagnosesCollection conflictSets;
	private QuickXPlain conflictDetection;
	private Integer solutionFoundAtDepth = null;

	public ConflictDetectionHSDAG(List<Diagnosable> userConstraints, Predicate<List<Diagnosable>> checkConsistent) {
		super(userConstraints);
		conflictSets = new DiagnosesCollection();
		this.conflictDetection = new QuickXPlain(checkConsistent);
	}

	@Override
	protected void buildDiagnosesTree(TreeNode root, DiagnosesCollection diagnosesCollection, int maxDepth) {
		List<Diagnosable> minCS;
		List<Diagnosable> difference;
		TreeNode treeNode;

		List<Diagnosable> rootData = root.getData();
		List<Diagnosable> rootInitialConstraintsSet = root.getInitialConstraintsSet();

		for (Diagnosable constraint : rootData) {
			difference = removeConstraintFromList(rootInitialConstraintsSet, constraint);
			minCS = conflictDetection.getMinConflictSet(difference);

			if (minCS.isEmpty()) {
				treeNode = new TreeNode(null, null, null, root.getDepth() + 1);
				root.addChild(constraint, treeNode);

				List<Diagnosable> diagnose = getDiagnose(treeNode);
				Collections.reverse(diagnose);

				DiagnosisMetadata diagnoseMetadata = diagnosesCollection.Contains(diagnose);
				if (diagnoseMetadata == DiagnosisMetadata.Min) {
					diagnosesCollection.add(diagnose);
					setSolutionFoundDepth(root.getDepth() + 1);

				} else {

				}
			} else {
				if (!conflictSets.contains(minCS)) {
					conflictSets.add(minCS);
				}
				treeNode = new TreeNode(minCS, difference, String.valueOf(indexOfConstraint), root.getDepth() + 1);
				root.addChild(constraint, treeNode);
				queue.add(treeNode);
			}
		}

		while (!queue.isEmpty()) {
			TreeNode node = queue.poll();
			if (!mayHaveMinimalSolution(node))
				return;
			buildDiagnosesTree(node, diagnosesCollection, maxDepth);
		}
	}

	private List<Diagnosable> getDiagnose(TreeNode node) {
		List<Diagnosable> diagnoses = new ArrayList<Diagnosable>();

		if (node.getConstraint() != null) {
			diagnoses.add(node.getConstraint());
		}
		if (node.getParentNode() != null) {
			diagnoses.addAll(getDiagnose(node.getParentNode()));
		}
		return diagnoses;
	}

	@Override
	public DiagnosesCollection diagnose() {

		List<Diagnosable> minCS = conflictDetection.getMinConflictSet(userConstraints);
		if (minCS.isEmpty()) {
			return new DiagnosesCollection();
		}

		if (!conflictSets.contains(minCS)) {
			conflictSets.add(minCS);
		}
		TreeNode rootNode = new TreeNode(minCS, userConstraints, null, 0);
		// Here are stored diagnoses
		DiagnosesCollection diagnosesCollection = new DiagnosesCollection(); 

		buildDiagnosesTree(rootNode, diagnosesCollection, userConstraints.size());
		return diagnosesCollection;
	}

	private void setSolutionFoundDepth(int depth) {
		if (solutionFoundAtDepth == null)
			solutionFoundAtDepth = Integer.valueOf(depth);
		else if (depth < solutionFoundAtDepth.intValue())
			solutionFoundAtDepth = Integer.valueOf(depth);
	}

	private boolean mayHaveMinimalSolution(TreeNode treeNode) {
		if (solutionFoundAtDepth == null)
			return true;
		return treeNode.getDepth() <= solutionFoundAtDepth.intValue();
	}
}
