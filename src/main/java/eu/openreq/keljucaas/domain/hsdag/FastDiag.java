/**
 * Copyright Siemens AG, 2016-2017
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


/**
 * 
 * Adapted by juhaTee / University of Helsinki for OpenReq project  
 * from /JMiniZinc/at.siemens.ct.jminizinc.diag/src/main/java/at/siemens/ct/jmz/diag/FastDiag.java
 * 
 */

package eu.openreq.keljucaas.domain.hsdag;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import eu.openreq.keljucaas.domain.release.Diagnosable;

public class FastDiag {
	
	Predicate<List<Diagnosable>> isConsistent;

	public FastDiag (Predicate<List<Diagnosable>> isConsistent) {
		this.isConsistent = isConsistent;
	}
	
	public List<Diagnosable> fastDiag(List<Diagnosable> C, List<Diagnosable> AC) {

		if (C.isEmpty()) {
			return Collections.emptyList();
		}
		if (isConsistent.test(C)) {
			return Collections.emptyList();
		}

		List<Diagnosable> ACWithoutC = ListUtil.diffListsAsSets(AC, C);
		Boolean searchForDiagnosis = isConsistent.test(ACWithoutC);
		if (!searchForDiagnosis) {
			return Collections.emptyList();
		}
		return fd(Collections.emptyList(), C, AC);
	}


	/**
	 * Function that computes diagnoses in FastDiag Adapted from
	 * /JMiniZinc/at.siemens.ct.jminizinc.diag/src/main/java/at/siemens/ct/jmz/diag/FastDiag.java
	 * 
	 * @param D
	 *            A subset from the user constraints
	 * @param C
	 *            A subset from the user constraints
	 * @param AC
	 *            user constraints
	 * @return a diagnose
	 */
	private List<Diagnosable> fd(List<Diagnosable> D, List<Diagnosable> C, List<Diagnosable> AC) {

		boolean isConsistentNow = isConsistent.test(AC);

		int q = C.size();

		if (!D.isEmpty()) {
			if (isConsistentNow) {
				return Collections.emptyList();
			}
		}

		if (q == 1) {
			return new LinkedList<Diagnosable>(C);
		}

		int k = q / 2;
		List<Diagnosable> C1 = C.subList(0, k);
		List<Diagnosable> C2 = C.subList(k, q);

		List<Diagnosable> ACWithoutC2 = ListUtil.diffListsAsSets(AC, C2);
		List<Diagnosable> D1 = fd(C2, C1, ACWithoutC2);

		List<Diagnosable> ACWithoutD1 = ListUtil.diffListsAsSets(AC, D1);
		List<Diagnosable> D2 = fd(D1, C2, ACWithoutD1);

		return ListUtil.appendListsAsSets(D1, D2);
	}

}

