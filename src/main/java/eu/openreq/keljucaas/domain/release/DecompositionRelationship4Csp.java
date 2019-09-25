package eu.openreq.keljucaas.domain.release;

import org.chocosolver.solver.Model;

public class DecompositionRelationship4Csp extends Relationship4Csp {

	
	public DecompositionRelationship4Csp(Element4Csp from, Element4Csp to, Model model, Integer id) {
		super (from, to, model, id);
		determineNameId();
		makeConstraint();
		completeInitialization();
	}

	@Override
	protected void makeConstraint() {
		// from is included implies (to is included and to.assignedRelease <=
		// from.assignedrelease
		Model model = getModel();
		setRelationShipConstraint(model.or(
				model.and(model.arithm(getFrom().getIsIncluded(), "=", 0), model.arithm(getTo().getIsIncluded(), "=", 0)),
				model.arithm(getFrom().getIsIncluded(), "=", 0),
				model.and(
						model.arithm(getFrom().getIsIncluded(), "=", 1),
						model.arithm(getTo().getIsIncluded(), "=", 1),
						model.or(
								model.arithm(getTo().getAssignedContainer(), "<=", getFrom().getAssignedContainer()),
								model.arithm(getTo().getPriority(), ">", getFrom().getPriority()))
				)));
	}
	
	@Override
	protected boolean isSatisfiedWithAssignment(int releaseOfFrom, int releaseOfTo) {
		if (releaseOfTo == 0) //parent will not be complete without child TODO check if prioriity affectd
			if (releaseOfFrom == 0)
				return true;
			else
				return false;
		else if ((releaseOfTo > releaseOfFrom) && (getTo().getPriority().getLB() <= getFrom().getPriority().getLB()))
			return false;
		else
			return true;
	}

	
	@Override
	public final String getRelationShipName() {
		return "decomposition";
	}
	
	protected RelationshipClass getRelationshipClass() {
		return RelationshipClass.DECOMPOSITION;
	}

}
