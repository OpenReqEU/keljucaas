package eu.openreq.keljucaas.domain.release;

import org.chocosolver.solver.Model;

import eu.openreq.keljucaas.domain.release.Relationship4Csp.RelationshipClass;


public class IncompatibleRelationship4Csp extends Relationship4Csp {

	public IncompatibleRelationship4Csp(Element4Csp from, Element4Csp to, Model model, Integer id) {
		super (from, to, model, id);
		determineNameId();
		makeConstraint();
		completeInitialization();
	}


	
	protected void makeConstraint() {
		// from is unaasinged, OR to is unassinged, assigned to different release
		// from.assignedrelease
		setRelationShipConstraint(getModel().or(
				getModel().arithm(getFrom().getIsIncluded(), "=", 0),
				getModel().arithm(getTo().getIsIncluded(), "=", 0),
				getModel().arithm(getTo().getAssignedContainer(), "!=", getFrom().getAssignedContainer())));
	}
	
	protected boolean isSatisfiedWithAssignment(int releaseOfFrom, int releaseOfTo) {
		if (releaseOfFrom == 0)
			return true;
		else if (releaseOfTo == 0)
			return true;
		else if (releaseOfFrom != releaseOfTo)
			return true;
		else
			return false;
	}


	
	public final String getRelationShipName() {
		return "incompatible";
	}
	protected RelationshipClass getRelationshipClass() {
		return RelationshipClass.INCOMPATIBLE;
	}


}


