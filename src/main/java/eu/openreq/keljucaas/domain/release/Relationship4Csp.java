package eu.openreq.keljucaas.domain.release;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;

import fi.helsinki.ese.murmeli.Relationship;


public abstract class Relationship4Csp implements Diagnosable {
	
	private Element4Csp from;
	private Element4Csp to;
	private Relationship relationship;
	private BoolVar isIncluded;
	private boolean denyPosted = false;
	private boolean requirePosted = false;
	private Constraint requireCstr;
	private Constraint denyCstr;
	private Constraint relationShipConstraint = null;
	private Model model;
	private String nameId;
	
	public Relationship4Csp(Element4Csp from, Element4Csp to, Model model, Relationship relationship) {
		this.from = from;
		this.to = to;
		this.model = model;
		this.relationship = relationship;
	}
	
	protected void completeInitialization() {
		isIncluded = getRelationShipConstraint().reify();
		requireCstr = getModel().arithm(isIncluded, "=", 1);
		denyCstr = getModel().arithm(isIncluded, "=", 0);
		getModel().post(requireCstr);
		requirePosted = true;
	}


	public void require(boolean include) {
		if (include) {
			if (requirePosted) {
				return;
			}
			requireCstr.post();
			requirePosted = true;
			if (denyPosted) {
				getModel().unpost(denyCstr);
				denyPosted = false;
			}
		} else { // not include = deny
			if (denyPosted) {
				return;
			}
			denyCstr.post();
			denyPosted = true;
			if (requirePosted) {
				getModel().unpost(requireCstr);
				requirePosted = false;
			}

		}
	}

	public void unRequire() {
		if (denyPosted) {
			getModel().unpost(denyCstr);
			denyPosted = false;
		}
		if (requirePosted) {
			getModel().unpost(requireCstr);
			requirePosted = false;
		}
	}

	public BoolVar getIsIncluded() {
		return isIncluded;
	}
	
	public final Relationship getRelationship() {
		return relationship;
	}

	protected abstract void makeConstraint();
	
	public final String getRelationShipName() {
		return getRelationshipName(relationship);
	}
	
	protected abstract boolean isSatisfiedWithAssignment(int releaseOfFrom, int ReleaseOfTo);

	@Override
	public String toString() {
		return getFrom() + " " + getRelationShipName() + " "+ getTo();
	}
	
//	@Override
//	public String toString() {
//		return "Relationship4Csp [from=" + getFrom() + ", relationshipType=" + getRelationShipName() + ", to=" + getTo() + "]";
//	}


	public Constraint getRelationShipConstraint() {
		return relationShipConstraint;
	}

	public void setRelationShipConstraint(Constraint relationShipConstraint) {
		this.relationShipConstraint = relationShipConstraint;
	}

	public Model getModel() {
		return model;
	}

	public Element4Csp getFrom() {
		return from;
	}

	public Element4Csp getTo() {
		return to;
	}

	public Integer getId() {
		return relationship.getID();
	}

	public String getNameId() {
		return nameId;
	}

	public void setNameId(String nameId) {
		this.nameId = nameId;
	}
	
	protected void determineNameId() {
		this.setNameId("rel_" + this.getFrom().getNameId()+"_" + getRelationShipName() + "_" + this.getTo().getNameId());
	}
	
	public static String getRelationshipName (Relationship relationship) {
		switch (relationship.getNameType()) {
		case CONTRIBUTES:
			return "contributes";
		case DAMAGES:
			return "damages";
		case DECOMPOSITION:
			return "decomposition";
		case DUPLICATES:
			return "duplicates";
		case EXCLUDES:
			return "excludes";
		case IMPLIES:
			return "implies";
		case INCOMPATIBLE:
			return "incompatible";
		case REFINES:
			return "refines";
		case REPLACES:
			return "replaces";
		case REQUIRES:
			return "requires";
		case SIMILAR:
			return "similar";
		}
		return "";
	}
	
}
