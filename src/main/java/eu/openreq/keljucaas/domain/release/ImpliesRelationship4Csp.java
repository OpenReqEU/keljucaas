package eu.openreq.keljucaas.domain.release;

import org.chocosolver.solver.Model;

import fi.helsinki.ese.murmeli.Relationship;

public class ImpliesRelationship4Csp extends Relationship4Csp {

	public ImpliesRelationship4Csp(Element4Csp from, Element4Csp to, Model model, Relationship relationship) {
		super (from, to, model, relationship);
		determineNameId();
		makeConstraint();
		completeInitialization();
	}

	protected void makeConstraint() {
		// from is included implies (to is included and to.assignedRelease <
		// from.assignedrelease
		setRelationShipConstraint(getModel().or(
				getModel().arithm(getFrom().getIsIncluded(), "=", 0),
				getModel().and(getModel().arithm(getTo().getIsIncluded(), "=", 1),
						getModel().arithm(getTo().getAssignedContainer(), "<", getFrom().getAssignedContainer()))));
	}
	
	protected boolean isSatisfiedWithAssignment(int releaseOfFrom, int releaseOfTo) {
		if (releaseOfFrom == 0)
			return true;
		else if ((releaseOfTo >0) &&  (releaseOfFrom < releaseOfTo))
			return true;
		else
			return false;
	}

}

