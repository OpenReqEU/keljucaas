package eu.openreq.keljucaas.domain.release;

import org.chocosolver.solver.Model;

import eu.openreq.keljucaas.domain.release.Relationship4Csp.RelationshipClass;


public class ExcludesRelationship4Csp extends Relationship4Csp {

	public ExcludesRelationship4Csp(Element4Csp from, Element4Csp to, Model model, Integer id) {
		super (from, to, model, id);
		determineNameId();
		makeConstraint();
		completeInitialization();
	}

	protected void makeConstraint() {
		setRelationShipConstraint(getModel().or(getModel().arithm(getTo().getIsIncluded(), "=", 0),
				getModel().arithm(getFrom().getIsIncluded(), "=", 0)));
	}
	
	
	protected boolean isSatisfiedWithAssignment(int releaseOfFrom, int releaseOfTo) {
		return (releaseOfFrom == 0) ||(releaseOfTo == 0); 
	}
	
	public final String getRelationShipName() {
		return "excludes";
	}
	
	protected RelationshipClass getRelationshipClass() {
		return RelationshipClass.EXLUDES;
	}

}

