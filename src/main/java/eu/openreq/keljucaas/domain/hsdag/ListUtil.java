package eu.openreq.keljucaas.domain.hsdag;

import java.util.ArrayList;
import java.util.List;

import eu.openreq.keljucaas.domain.release.Diagnosable;

public class ListUtil {
	public static List<Diagnosable> appendListsAsSets(List<Diagnosable> CS1, List<Diagnosable> CS2) {
		List<Diagnosable> union = new ArrayList<>(CS1);
		if (CS2 == null)
			return union;

		for (Diagnosable c : CS2) {
			if (!union.contains(c)) {
				union.add(c);
			}
		}
		return union;
	}


	public static List<Diagnosable> diffListsAsSets(List<Diagnosable> ac, List<Diagnosable> c2) {
		List<Diagnosable> diff = new ArrayList<>();
		for (Diagnosable element : ac) {
			if (!c2.contains(element)) {
				diff.add(element);
			}
		}
		return diff;
	}


}
