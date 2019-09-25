package eu.openreq.keljucaas.domain.release;

import java.util.LinkedList;
import java.util.List;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import eu.openreq.keljucaas.services.CSPPlanner;
import eu.openreq.keljucaas.services.CSPPlanner.DiagnosableClass;
import fi.helsinki.ese.murmeli.Container;
import fi.helsinki.ese.murmeli.Element;
import fi.helsinki.ese.murmeli.ElementModel;

/*
 * Element is transformed so that Choco solver can understand it.
 * 
 */
public class Element4Csp implements Diagnosable, Comparable<Element4Csp> {
	private BoolVar isIncluded;
	private IntVar assignedContainer;
	private int originalContainerAssigment;
	private IntVar[] effortInContainer;
	private IntVar priority;
	private boolean denyPosted = false;
	private boolean requirePosted = false;
	private Constraint requireCstr;
	private Constraint denyCstr;
	private Model model;
	private int id;
	private String nameID;
	private ElementModel elementModel;
	private Element element = null;
	private List<Relationship4Csp> referringRels= new LinkedList<>();

	public Element4Csp(Element element, CSPPlanner planner, Model model, ElementModel elementModel) {
		this.model = model;
		this.elementModel = elementModel;
		this.element = element;
		this.id = element.getID();
		this.nameID= element.getNameID();

		isIncluded = model.boolVar(element.getNameID() + "_in");

		requireCstr = model.arithm(isIncluded, "=", 1);
		denyCstr = model.arithm(isIncluded, "=", 0);
		model.post(requireCstr);
		requirePosted = true;
		//in terms of Index. Container 1 =0, Container 0 = UNASSIGNED
		originalContainerAssigment = getElementsContainer(element);
		
		priority = model.intVar(element.getNameID() + "_priority", getPriorityOfElement(planner));

		setAssignedContainer(planner);
		createEffortVariables(planner);
		createConstraints(planner);
		//model.ifThenElse(isIncluded, model.arithm(assignedContainer, ">", 0), model.arithm(assignedContainer, "=", 0));
	}

	
	/**
	 * 
	 * @param element
	 * @param planner
	 */
	private void setAssignedContainer(CSPPlanner planner) {
		if (getOriginallyAssignedRelease() == CSPPlanner.UNASSIGNED_RELEASE) {
			assignedContainer = model.intVar(id + "_assignedTo", 
					0, planner.getNReleases() );
		} else {
			assignedContainer = model.intVar(id + "_assignedTo",
					getOriginallyAssignedRelease());
		}
	}


	public int getOriginallyAssignedRelease() {
		return this.originalContainerAssigment;
	}


	private int getElementsContainer(Element element) {
		for (Container container : this.elementModel.getsubContainers()) {
			if (container.getElements().contains(element.getNameID())) {
				return container.getID();
			}
		}
		return 0;
	}


	//TODO JT: remove doubles? Or is there some reason
	public int getEffortOfElement() {
		Double d = (Double) elementModel.getAttributeValues().get(element.getAttributes().get("effort")).getValue();
		return d.intValue();
	}

	public int getPriorityOfElement(CSPPlanner planner) {
		Double d = (Double) elementModel.getAttributeValues().get(element.getAttributes().get("priority")).getValue();
		if (d != null)
			return d.intValue();
		else
			return planner.getMaxElementPriority() +1;
	}

	/**
	 * Create choco variables for representing effort in each container
	 * 
	 * @param element
	 * @param planner
	 */
	private void createEffortVariables(CSPPlanner planner) {
		effortInContainer = new IntVar[planner.getNReleases() + 1]; 
		int[] effortDomain = new int[2];
		effortDomain[0] = 0;
		effortDomain[1] = getEffortOfElement(); 

		for (int releaseIndex = 0; releaseIndex <= planner.getNReleases(); releaseIndex++) {
			String varName = "req_" + element.getNameID() + "_" + (releaseIndex); //e.g req_REQ1_1 (element 1 in release 1)

			if (getOriginallyAssignedRelease() == CSPPlanner.UNASSIGNED_RELEASE) { // not assigned
				effortInContainer[releaseIndex] = model.intVar(varName, effortDomain); // effort in each release is 0 or the effort	(never divided to several releases)																		
			} else { // assigned to release
				if ((getOriginallyAssignedRelease() == releaseIndex) || (releaseIndex == CSPPlanner.UNASSIGNED_RELEASE)) {
					effortInContainer[releaseIndex] = model.intVar(varName, effortDomain); //e.g for REQ2_1 (meaning REQ2 in release 1) effortInRelease is req_REQ2_1 = {0,2}
				} else {
					effortInContainer[releaseIndex] = model.intVar(varName, 0); // domain is fixed 0 in other releases (e.g for REQ2_2 (meaning REQ2 in release 2) effortInRelease is req_REQ2_2 = 0
				}
			}
		}
	}


	/**
	 * Create constraints that enforce If the effort in assigned release if release
	 * is assigned, connect only the affected release
	 * 
	 * @param element
	 * @param planner
	 */
	private void createConstraints(CSPPlanner planner) {
		if (getEffortOfElement() == 0) { 
			// effort of element is not specified (=0). 
			// Require effort allocated to a release is 0 in every release.
			// No need for constraint, if capacity of rfelease is already 0
			for (int releaseIndex = 0; releaseIndex <= planner.getNReleases(); releaseIndex++) {
				IntVar effortInRelease= effortInContainer[releaseIndex];
				if (effortInRelease.getUB() > 0) {
					model.arithm(effortInRelease, "=", 0);
				}
			}
		}
		else {
			if (getOriginallyAssignedRelease() == CSPPlanner.UNASSIGNED_RELEASE) { //not assigned to any release
				for (int releaseIndex = 1; releaseIndex <= planner.getNReleases(); releaseIndex++) {
					// effectively forces others to 0 because domain size is 2, and the non-0 gets
					// forbidden //?????????????????????????????
					// Could try if adding explicit constraints would be faster
					model.ifOnlyIf(
							model.and(model.arithm(isIncluded, "=", 1), model.arithm(assignedContainer, "=", releaseIndex)),
							model.arithm(effortInContainer[releaseIndex], "=", getEffortOfElement()));
					// "ifOnlyIf(Constraint cstr1, Constraint cstr2)"
					// "Posts an equivalence constraint stating that cstr1 is satisfied <=> cstr2 is satisfied, BEWARE : it is automatically posted (it cannot be reified)"
					// Source: http://www.choco-solver.org/apidocs/org/chocosolver/solver/constraints/IReificationFactory.html
				}
				model.ifThen(model.arithm(isIncluded, "=", 0),
						model.arithm(effortInContainer[CSPPlanner.UNASSIGNED_RELEASE], "=", getEffortOfElement()));
				
			} else { //assigned to a release
				model.ifThenElse(model.arithm(isIncluded, "=", 1),
						model.arithm(effortInContainer[getOriginallyAssignedRelease()], "=", getEffortOfElement()),
						model.arithm(effortInContainer[getOriginallyAssignedRelease()], "=", 0));
				model.ifThenElse(model.arithm(isIncluded, "=", 1),
						model.arithm(effortInContainer[ CSPPlanner.UNASSIGNED_RELEASE], "=", 0),
						model.arithm(effortInContainer[CSPPlanner.UNASSIGNED_RELEASE], "=", getEffortOfElement()));
				// if isIncluded, Then model.arithm(effortInRelease[element.getAssignedRelease() - 1], "=",element.getEffort()),
				// and if Not isIncluded, Then model.arithm(effortInRelease[element.getAssignedRelease() - 1], "=", 0)
				// "IReificationFactory.ifThenElse(BoolVar ifVar, Constraint thenCstr, Constraint elseCstr)"
				// "Posts an implication constraint: ifVar => thenCstr && not(ifVar) => elseCstr."
				// See http://www.choco-solver.org/apidocs/org/chocosolver/solver/variables/class-use/BoolVar.html

			}
		}
	}


	public IntVar getEffortOfContainer(int releaseIndex) {
		return effortInContainer[releaseIndex]; 
	}
	
	public final IntVar getPriority() {
		return priority;
	}


	public IntVar getAssignedContainer() {
		return assignedContainer;
	}


	public BoolVar getIsIncluded() {
		return isIncluded;
	}


	public void require(boolean include) {
		//System.out.println("Require " + include + " " + getNameId());
		if (include) {
			if (requirePosted) {
				return;
			}
			requireCstr.post();
			requirePosted = true;
			if (denyPosted) {
				model.unpost(denyCstr);
				denyPosted = false;
			}
			//When a requirement is included, also relationships referring to it must be included
			//This should make fastdiag able to make better/correct  diagnoses in case of relationship and requirement diagnosis.
			//requireDependencies();
		} else { // not include = deny
			if (denyPosted) {
				return;
			}
			denyCstr.post();
			denyPosted = true;
			if (requirePosted) {
				model.unpost(requireCstr);
				requirePosted = false;
			}

		}
	}


	public void unRequire() {
		//System.out.println("Unequire " + getNameId());
		if (denyPosted) {
			model.unpost(denyCstr);
			denyPosted = false;
		}
		if (requirePosted) {
			model.unpost(requireCstr);
			requirePosted = false;
		}
	}
	
	public void requireDependencies() {
		for (Relationship4Csp rel: getReferringRels()) {
			rel.require(true);
		}
	}


	public Integer getId() {
		return id;
	}


	public String getNameId() {
		return nameID;
	}


	@Override
	public String toString() {
		return "Element4Csp [id=" + id + ", nameID=" + nameID + "]";
	}
		
	public void addRereferringRel(Relationship4Csp rel) {
		referringRels.add(rel);
	}


	public final List<Relationship4Csp> getReferringRels() {
		return referringRels;
	}


	@Override
	public int compareTo(Element4Csp other) {
		//for java, smaller first is the default sort order
		//for OpenReq, smaller priority is higher
		//so we want to get smallest priority number first
		int diff = this.priority.getLB() - other.priority.getLB();
		if (diff != 0)
			return diff;
		//if an element has many relationships, it might be more important
		diff = other.referringRels.size() - this.referringRels.size();
		if (diff != 0)
			return diff;
		if (this.nameID == null) {
			if (other.nameID ==null)
				return 0;
			else
				return 1;
		}
		else {
			if (other.nameID ==null)
				return -1;
			else
				return this.getNameId().compareTo(other.getNameId());
		}
	}

	@Override
	public DiagnosableClass getDiagnosableClass () {
		return DiagnosableClass.REQUIREMENT;
	}

}

