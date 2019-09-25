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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import eu.openreq.keljucaas.domain.release.Diagnosable;

/**
 * This class stores all diagnoses for a configuration problem.
 */
public class DiagnosesCollection extends ArrayList<List<Diagnosable>> {
	private static final long serialVersionUID = 7L;

	public DiagnosesCollection() {
		super();
	}

	public DiagnosesCollection(Collection<? extends List<Diagnosable>> c) {
		super(c);
	}

	public DiagnosesCollection(int initialCapacity) {
		super(initialCapacity);
	}

	public DiagnosisMetadata Contains(List<Diagnosable> diagnose) {
		for (List<Diagnosable> d : this) {
			DiagnosisMetadata diagnoseMetadata = compareDiagnose(diagnose, d);
			if (diagnoseMetadata != DiagnosisMetadata.Min)
				return diagnoseMetadata;
		}
		return DiagnosisMetadata.Min;
	}

	private DiagnosisMetadata compareDiagnose(List<Diagnosable> newDiagnose, List<Diagnosable> existingDiagnose) {
		for (Diagnosable c : existingDiagnose) {
			if (!newDiagnose.contains(c))
				return DiagnosisMetadata.Min;
		}
		
		if (newDiagnose.size() == existingDiagnose.size())
			return DiagnosisMetadata.AlreadyExists;
		return DiagnosisMetadata.NotMin;
	}

	public java.lang.String toString() {
		List<String> lines = new ArrayList<String>();

		for (List<Diagnosable> diagnose : this) {

			List<String> constraints = new ArrayList<String>();
			for (Diagnosable c : diagnose) {
				constraints.add(c.getNameId());
			}

			Collections.sort(constraints);
			StringBuilder line = new StringBuilder();
			line.append("{ ");
			for (String c : constraints) {
				line.append(c);
				if (constraints.indexOf(c) != constraints.size() - 1) {
					line.append(", ");
				}
			}
			line.append(" }");
			lines.add(line.toString());
		}

		StringBuilder sb = new StringBuilder();
		Collections.sort(lines);
		for (String line : lines) {
			sb.append(line);
			if (lines.indexOf(line) != lines.size() - 1) {
				sb.append(System.lineSeparator());
			}
		}

		return sb.toString();
	}

}
