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


import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import eu.openreq.keljucaas.domain.release.Diagnosable;


public class QuickXPlain {

/**
 * @author Copyright Siemens AG, 2016
 */

/**
 * Function that computes minimal conflict sets in QuickXPlain
 * 
 * @param D  A subset from the user constraints set
 * @param C  A subset from the user constraints set
 * @param AC user constraints
 * @return a minimal conflict set
 * @throws DiagnosisException
 */
	
	Predicate<List<Diagnosable>> checkConsistent;

private List<Diagnosable> quickXPlain(List<Diagnosable> D, List<Diagnosable> C, List<Diagnosable> B) {

	if (!D.isEmpty()) {
		boolean isConsistent = checkConsistent.test(B);
		if (!isConsistent) {
			return Collections.emptyList();
		}
	}

	int q = C.size();
	if (q == 1) {

		return C;
	}

	int k = q / 2;
	List<Diagnosable> C1 = C.subList(0, k);
	List<Diagnosable> C2 = C.subList(k, q);
	List<Diagnosable> CS1 = quickXPlain(C2, C1, ListUtil.appendListsAsSets(B, C2));
	List<Diagnosable> CS2 = quickXPlain(CS1, C2, ListUtil.appendListsAsSets(B, CS1));
	List<Diagnosable> tempCS = ListUtil.appendListsAsSets(CS1, CS2);

	return tempCS;
}


  public QuickXPlain(Predicate<List<Diagnosable>> checkConsistent) {
	  this.checkConsistent = checkConsistent;
  }

	
  public List<Diagnosable> getMinConflictSet(List<Diagnosable> constraintsSetC) {

    if (constraintsSetC.isEmpty() || checkConsistent.test(constraintsSetC)) {
      return Collections.emptyList();
		}

    return quickXPlain(Collections.emptyList(), constraintsSetC, Collections.emptyList());
	}

	
  }
