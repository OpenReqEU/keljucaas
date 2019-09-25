package eu.openreq.keljucaas.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

import eu.openreq.keljucaas.domain.hsdag.ConflictDetectionHSDAG;
import eu.openreq.keljucaas.domain.hsdag.DiagnosesCollection;
import eu.openreq.keljucaas.domain.hsdag.FastDiag;
import eu.openreq.keljucaas.domain.hsdag.ListUtil;
import eu.openreq.keljucaas.domain.release.DecompositionRelationship4Csp;
import eu.openreq.keljucaas.domain.release.Diagnosable;
import eu.openreq.keljucaas.domain.release.Element4Csp;
import eu.openreq.keljucaas.domain.release.ExcludesRelationship4Csp;
import eu.openreq.keljucaas.domain.release.ImpliesRelationship4Csp;
import eu.openreq.keljucaas.domain.release.IncompatibleRelationship4Csp;
import eu.openreq.keljucaas.domain.release.Relationship4Csp;
import eu.openreq.keljucaas.domain.release.ReleaseInfo;
import eu.openreq.keljucaas.domain.release.ReleasePlanAnalysisDefinition;
import eu.openreq.keljucaas.domain.release.ReleasePlanInfo;
import eu.openreq.keljucaas.domain.release.RequiresRelationship4Csp;
import fi.helsinki.ese.murmeli.Container;
import fi.helsinki.ese.murmeli.Element;
import fi.helsinki.ese.murmeli.ElementModel;
import fi.helsinki.ese.murmeli.Relationship;
import fi.helsinki.ese.murmeli.Relationship.NameType;

/**
 * This class contains a private class Element4Csp
 * This class is used to check consistency and do diagnosis.
 * 
 * Mapping from OpenReq domain:
 * Requirement = Element
 * Release = (Sub-)container
 * Project = (Root)container
 * 
 * Requirements can be assigned to releases. 
 * 
 * Releases are arranged in a linear sequence (ordered by release date)
 * In external view, the first release is number 1, second number 2 etc.
 * 
 * A requirement can be assigned to a release. If assigned, 
 * the requirement specifies the number of the release.
 * If a requirement is not assigned, it specifies release 0.
 * 
 * The implmentation used to use -1 for designating unassigned release, but his became confusing.
 * Unassigned release is always presented as release 0, including the choco model. It now includes 
 * release 0 for unallocated requirements.  
 * Some analysis tasks require that release 1 is set to internal index 0.
 * Some data structures can conveniently include "unassigned release" at 
 * data structure index 0., release 1 at 1 etc.
 * For some data structures, index 0 must represent release 1.
 * This causes some difficulties in understanding the code.
 *  * 
 */
public class CSPPlanner {
	
	public enum DiagnosableClass {
		REQUIREMENT,
		RELATIONSHIP
	}

	public final static int UNASSIGNED_RELEASE = 0; 

	private ElementModel elementModel;
	private List<ReleasePlanAnalysisDefinition> wantedplans;

	private LinkedHashMap<String, Integer> elementIDToIndex;
	private LinkedHashMap<Integer, String> indexToElementID;

	private final int nContainers;
	private final int nElements;
	private Element4Csp[] element4CSPs = null;
	private ArrayList<Relationship4Csp> relationship4Csps = new ArrayList<>();
	private boolean diagnoseElements; //if false, diagnosis requirements setting does not clear these requirements 
	private boolean diagnoseRelations; //if false, diagnosis requirements setting does not clear these requirements

	private LinkedHashMap <String, ReleasePlanInfo> releaseStates = new LinkedHashMap<>();
	
	private int maxElementPriority;

	Model model = null;
	//Solution solution;

	public CSPPlanner(ElementModel elementModel, List<ReleasePlanAnalysisDefinition> wantedplans) {
		this.elementModel = elementModel;
		this.wantedplans = wantedplans;
		nContainers = elementModel.getsubContainers().size();
		nElements = elementModel.getElements().size();
		initialize();
	}

	private void initialize() {
		elementIDToIndex = new LinkedHashMap<>(nElements);
		indexToElementID = new LinkedHashMap<>(nElements);
		initializeElementIndexMaps();

		this.element4CSPs = new Element4Csp[nElements];
		maxElementPriority = getMaximumPriority();

		generateCSP();

		initializeReleaseStates();
		generateDependencyLists();
//		ReleasePlanInfo submittedReleasePlan = releaseStates.get("submitted");
		//allocateOriginallyAsssignedElements(submittedReleasePlan);
		//releaseStates.put(submittedReleasePlan.getIdString(), submittedReleasePlan);
//		System.out.println("");
//		System.out.println("Initialize: submmitted "+ submittedReleasePlan.getReleasePlanMessage());
	}

	private Element4Csp getElement4Csp(String elementId) {
		Integer index = elementIDToIndex.get(elementId);
		if (index == null)
			return null;
		return element4CSPs[index.intValue()];

	}

	private void initializeElementIndexMaps() {
		int index1 = 0;
		for (Element element : elementModel.getElements().values()) {
			Integer index2 = Integer.valueOf(index1++);
			elementIDToIndex.put(element.getNameID(), index2);
			indexToElementID.put(index2, element.getNameID());
		}
	}

	public ReleasePlanInfo getReleasePlan(String planName) {
		return releaseStates.get(planName);
	}

	public final int getNReleases() {
		return nContainers;
	}


	public final int getNelements() {
		return nElements;
	}
		
	public final int getMaxElementPriority() {
		return maxElementPriority;
	}

	/**
	 * Generate Constraint Satisfaction Problem model
	 */
	public void generateCSP() {
		model = new Model("ReleasePlanner"); // NOTE change this?

		initializeElementCSPs();
		setEffortConstraints();
		initializeRelationship4CSPs();
		//solution = new Solution(model);
		//TODO: modify so that it can be controlled if efforts and capacities are used. 
		//If they are not, the mode check boils down to checking dependencies, 
		//which should be more straightforward. Byt maybe it is just easier to make minor modifications

		//TODO Think if support for unassigned requirements should be dropped for now?
	}


	/**
	 * Initialize Element4Csp[] elementCSPs
	 */
	private void initializeElementCSPs() {
		for (Element element : elementModel.getElements().values()) {
			Element4Csp element4csp = new Element4Csp(element, this, model, this.elementModel);
			element4CSPs[elementIDToIndex.get(element.getNameID())] = element4csp;
		}
	}
	
	private int getMaximumPriority() {
		double maximumPriority =0;
		for (Element element : elementModel.getElements().values()) {
			Double d = (Double) elementModel.getAttributeValues().get(element.getAttributes().get("priority")).getValue();
			if (d != null)
				if (d > maximumPriority)
					maximumPriority = d;
		}
		return (int)maximumPriority;
		
	}

	/**
	 * Initialize Element4Csp[] elementCSPs
	 */
	private void initializeReleaseStates() {
		for (ReleasePlanAnalysisDefinition wantedPlan : wantedplans) {
			ReleasePlanInfo releasePlan = new ReleasePlanInfo(wantedPlan.getPlanName(), wantedPlan);
			createInitialState (releasePlan);
			//just check, no analysis
			if (!wantedPlan.isDiagnoseRequirements() && !wantedPlan.isDiagnoseRelationships()) {
				allocateOriginallyAsssignedElements(releasePlan);
				setOriginallyEnabledRelationships(releasePlan);
				releasePlan.determineConsistency();
			}
			releaseStates.put(releasePlan.getIdString(), releasePlan);
		}
	}

	private void initializeRelationship4CSPs() {

		for (Relationship relation : this.elementModel.getRelations()) {
			if (isSupported(relation.getNameType())) {
				Element4Csp from = getElement4Csp(relation.getFromID());
				Element4Csp to = getElement4Csp(relation.getToID());
				if ((from!= null) && (to != null)) {
					switch(relation.getNameType()) {
					case REQUIRES:
						relationship4Csps.add(
								new RequiresRelationship4Csp(from, to, model, Integer.valueOf(relation.getID())));
						break;
					case INCOMPATIBLE:
						relationship4Csps.add(
								new IncompatibleRelationship4Csp(from, to, model,Integer.valueOf(relation.getID())));
						break;

					case EXCLUDES:
						relationship4Csps.add(
								new ExcludesRelationship4Csp(from, to, model, Integer.valueOf(relation.getID())));
						break;
					case IMPLIES:
						relationship4Csps.add(
								new ImpliesRelationship4Csp(from, to, model, Integer.valueOf(relation.getID())));
						break;
					case DECOMPOSITION:
						relationship4Csps.add(
								new DecompositionRelationship4Csp(from, to, model, Integer.valueOf(relation.getID())));
						break;
					//following not supported	
					case CONTRIBUTES:
					case DAMAGES:
					case DUPLICATES:
					case REFINES:
					case REPLACES:
					case SIMILAR:
						break;

					}
				}
			}
		}
	}

	private void generateDependencyLists() {
		for (Relationship4Csp rel : relationship4Csps) {
			rel.getFrom().addRereferringRel(rel);
			rel.getTo().addRereferringRel(rel);
		}
	}

	protected ReleasePlanInfo createInitialState (ReleasePlanInfo releasePlanInfo) {

		ReleaseInfo unAllocatedRelease = new ReleaseInfo(CSPPlanner.UNASSIGNED_RELEASE, "unassigned");
		releasePlanInfo.addReleaseInfo(unAllocatedRelease);

		int index = CSPPlanner.UNASSIGNED_RELEASE +1;
		for (Container container : elementModel.getsubContainers()) {
			ReleaseInfo releaseInfo = new ReleaseInfo(index++, container.getNameID());
			int capacityOfRelease = determineCapacityAvailable(container);
			releaseInfo.setCapacityAvailable(capacityOfRelease);
			releasePlanInfo.addReleaseInfo(releaseInfo);
		}
		return releasePlanInfo;
	}

	protected void allocateOriginallyAsssignedElements(ReleasePlanInfo releasePlanInfo) {
		for (Element4Csp element4Csp:element4CSPs) {
			int originalRelease= element4Csp.getOriginallyAssignedRelease();
			ReleaseInfo assignedRelease = releasePlanInfo.getReleaseInfo(originalRelease);
			releasePlanInfo.assignElementToRelease(element4Csp, assignedRelease);
		}
	}

	protected void setOriginallyEnabledRelationships(ReleasePlanInfo releasePlanInfo) {
		for (Relationship4Csp relationship4Csp: relationship4Csps) {
			releasePlanInfo.addEnabledRelationsShip(relationship4Csp);
		}
	}


	protected void allocateDiagnosed(ReleasePlanInfo releasePlanInfo) {
		for (Element4Csp element4Csp:element4CSPs) {
			int allocatedRelease=  element4Csp.getAssignedContainer().getValue();
			ReleaseInfo assignedRelease;
			if (element4Csp.getIsIncluded().getValue() != 0) 
				assignedRelease = releasePlanInfo.getReleaseInfo(allocatedRelease);
			else 
				assignedRelease = releasePlanInfo.getReleaseInfo(CSPPlanner.UNASSIGNED_RELEASE);
			releasePlanInfo.assignElementToRelease(element4Csp, assignedRelease);
			//assignedRelease.addAssignedElement(element4Csp);
		}

		for (Relationship4Csp relationship4Csp: relationship4Csps) {
			if (relationship4Csp.getIsIncluded().getValue() != 0) 
				releasePlanInfo.addEnabledRelationsShip(relationship4Csp);
			else
				releasePlanInfo.addDisabledRelationsShip(relationship4Csp);
		}
	}



	/**
	 * Set constraints for ensuring enough effort per release
	 */
	private void setEffortConstraints() {
		for (Container container : elementModel.getsubContainers()) {
			ArrayList<IntVar> containerEffortVars = new ArrayList<>();
			for (Element element : elementModel.getElements().values()) {
				Element4Csp element4Csp = element4CSPs[elementIDToIndex.get(element.getNameID())];

				IntVar effortVar = element4Csp.getEffortOfContainer(container.getID());

				if (effortVar.getUB() >= 0) {// do not add variable to be summed if the variable cannot be > 0
					containerEffortVars.add(effortVar);
				}
			}
			if (containerEffortVars.size() > 0) {
				IntVar[] effortVarArray = containerEffortVars.toArray(new IntVar[0]);
				int capacityOfRelease = determineCapacityAvailable(container);
				model.sum(effortVarArray, "<=", capacityOfRelease).post(); // TODO: What if no capacity?
			}
		}
	}





	public boolean isReleasePlanConsistent() {
		requireAllElements();
		requireAllRelations();

		model.getSolver().reset();

		Solver solver = model.getSolver();
		boolean solvable = solver.solve();
		//		if (solvable)
		//			solution.record();
		return solvable;
	}


	public void performDiagnoses() {

		List<Diagnosable> all= new ArrayList<>();
		for (int req = 0; req < nElements; req++) {
			all.add(element4CSPs[req]);
		}

		for (Relationship4Csp relationship4Csp: relationship4Csps)
			all.add(relationship4Csp);

		for (ReleasePlanInfo releasePlanInfo : releaseStates.values()) {
			ReleasePlanAnalysisDefinition wanted = releasePlanInfo.getWantedAnalysis();
			this.diagnoseElements = wanted.isDiagnoseRequirements();
			this.diagnoseRelations= wanted.isDiagnoseRelationships();
			boolean isAnalysisRequired = diagnoseElements || diagnoseRelations;
			ReleasePlanInfo requireFailedForDiagnosis = releaseStates.get(wanted.getAnalyzeOnlyIfIncosistentPlan());
			if ((requireFailedForDiagnosis != null) && requireFailedForDiagnosis.isConsistent())
					isAnalysisRequired = false;
			if (isAnalysisRequired) {
				List<Diagnosable> diagnosis = getDiagnosisFastDiag(diagnoseElements, diagnoseRelations);
				List<Diagnosable> included = ListUtil.diffListsAsSets(all, diagnosis);
				DiagnosesCollection fooCollection = getDiagnosesHSDAG(diagnoseElements, diagnoseRelations);
				setRequirementsToList(included);
				boolean OK = consistent(included);
				releasePlanInfo.setConsistent(OK);
				if (OK) {
					//use the current, determined release plan
					allocateDiagnosed(releasePlanInfo);
					releasePlanInfo.setAppliedDiagnosis(diagnosis);
				}
				else {
					//no no diagnosis helped
					//so do not set a diagnosis to utilize
					
					// how to return relevant information to user?
					// If we take out all requirements, relationships do not have any meaning.
					//but if all relationships were diagnosed out, one can could get useful info about resource consumption
					//But the same info is available from the submitted solution
					
					// thus, do not set diagnosis
					// and use the submitted release plan as the release plan.
					//it should not be used for anything
					
					allocateOriginallyAsssignedElements(releasePlanInfo);
					setOriginallyEnabledRelationships(releasePlanInfo);
				}
			}
		}

	}

	private List<Diagnosable> getDiagnosableElementsAndRequireOthers(boolean diagnoseElements, boolean diagnoseRelations) {
		List<Diagnosable> allElements = new ArrayList<>();
		this.diagnoseElements = diagnoseElements;
		this.diagnoseRelations = diagnoseRelations;

		if (diagnoseElements)
			for (int req = 0; req < nElements; req++) 
				allElements.add(element4CSPs[req]);
		else
			requireAllElements();


		if (diagnoseRelations)
			for (Relationship4Csp relationship4Csp: relationship4Csps)
				allElements.add(relationship4Csp);
		else
			requireAllRelations();
		
		return allElements;
	}
	

	protected List<Diagnosable> getDiagnosisFastDiag(boolean diagnoseElements, boolean diagnoseRelations) {
		List<Diagnosable> allElements =  getDiagnosableElementsAndRequireOthers(diagnoseElements, diagnoseRelations);
		FastDiag fastDiag = new FastDiag(AC -> consistent(AC));
		return fastDiag.fastDiag(allElements, allElements);
	}
	
	protected DiagnosesCollection getDiagnosesHSDAG(boolean diagnoseElements, boolean diagnoseRelations) {
		List<Diagnosable> allElements =  getDiagnosableElementsAndRequireOthers(diagnoseElements, diagnoseRelations);

		ConflictDetectionHSDAG conflictDetectionHSDAG = new 
		 ConflictDetectionHSDAG( 
				 allElements, AC -> consistent(AC));
		DiagnosesCollection diagnoses = conflictDetectionHSDAG.diagnose();
		return diagnoses;
	}


	private void setRequirementsToList(List<Diagnosable> elementsToSet) {
//		StringBuilder sb = new StringBuilder();
//		sb.append("setRequirementsToList:" );
//		sb.append(" diagnoseElements "+ diagnoseElements);
//		sb.append(" diagnoseRelations "+ diagnoseRelations);
//		for (Diagnosable d : elementsToSet) {
//			sb.append(" ");
//			sb.append(d.getNameId());
//		}
//		System.out.println(sb.toString());
		if (diagnoseElements)
			for (int i = 0; i < nElements; i++) {
				element4CSPs[i].unRequire();
			}
		if (diagnoseRelations)
			for (Relationship4Csp relationship4Csp: relationship4Csps)
				relationship4Csp.unRequire();

		for (Diagnosable element : elementsToSet) {
			element.require(true);
		}
	}



	private boolean consistent(List<Diagnosable> constraints) {
		if (constraints.size() == 0) {
			return true;
		}
		setRequirementsToList(constraints);
		Solver solver = model.getSolver();
		solver.reset();
		boolean result = solver.solve();
//		System.out.println("consistent: " + result);
		//		if (result)
		//			solution.record();
		return result;
	}


	/**
	 * Adapted from
	 * /JMiniZinc/at.siemens.ct.jminizinc.diag/src/main/java/at/siemens/ct/jmz/diag/FastDiag.java
	 * 
	 * @param C
	 * @param AC
	 * @return
	 */
//	private List<Diagnosable> fastDiag(List<Diagnosable> C, List<Diagnosable> AC) {
//
//		if (C.isEmpty()) {
//			return Collections.emptyList();
//		}
//		if (consistent(C)) {
//			return Collections.emptyList();
//		}
//
//		List<Diagnosable> ACWithoutC = diffListsAsSets(AC, C);
//		Boolean searchForDiagnosis = consistent(ACWithoutC);
//		if (!searchForDiagnosis) {
//			return Collections.emptyList();
//		}
//		return fd(Collections.emptyList(), C, AC);
//	}


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
//	private List<Diagnosable> fd(List<Diagnosable> D, List<Diagnosable> C, List<Diagnosable> AC) {
//		
////		StringBuilder sb = new StringBuilder();
////		sb.append("fd:" );
////		for (Diagnosable d : AC) {
////			sb.append(" ");
////			sb.append(d.getNameId());
////		}
//
//		boolean isConsistent = consistent(AC);
////		sb.append(": isConsistent = ");
////		sb.append(isConsistent);
////		System.out.println(sb.toString());
//
//		int q = C.size();
//
//		if (!D.isEmpty()) {
//			if (isConsistent) {
//				return Collections.emptyList();
//			}
//		}
//
//		if (q == 1) {
//			return new LinkedList<Diagnosable>(C);
//		}
//
//		int k = q / 2;
//		List<Diagnosable> C1 = C.subList(0, k);
//		List<Diagnosable> C2 = C.subList(k, q);
//
//		List<Diagnosable> ACWithoutC2 = diffListsAsSets(AC, C2);
//		List<Diagnosable> D1 = fd(C2, C1, ACWithoutC2);
//
//		List<Diagnosable> ACWithoutD1 = diffListsAsSets(AC, D1);
//		List<Diagnosable> D2 = fd(D1, C2, ACWithoutD1);
//
//		return appendListsAsSets(D1, D2);
//	}
	
//	public List<Diagnosable> getMinConflictSet(List<Diagnosable> constraintsSetC) {
//
//		if (constraintsSetC.isEmpty() || consistent(constraintsSetC)) {
//			return Collections.emptyList();
//		}
//
//		return quickXPlain(Collections.emptyList(), constraintsSetC, Collections.emptyList());
//	}
//
//	/**
//	 * Function that computes minimal conflict sets in QuickXPlain
//	 * 
//	 * @param D  A subset from the user constraints set
//	 * @param C  A subset from the user constraints set
//	 * @param AC user constraints
//	 * @return a minimal conflict set
//	 * @throws DiagnosisException
//	 */
//	private List<Diagnosable> quickXPlain(List<Diagnosable> D, List<Diagnosable> C, List<Diagnosable> B) {
//
//		if (!D.isEmpty()) {
//			boolean isConsistent = consistent(B);
//			if (!isConsistent) {
//				return Collections.emptyList();
//			}
//		}
//
//		int q = C.size();
//		if (q == 1) {
//
//			return C;
//		}
//
//		int k = q / 2;
//		List<Diagnosable> C1 = C.subList(0, k);
//		List<Diagnosable> C2 = C.subList(k, q);
//		List<Diagnosable> CS1 = quickXPlain(C2, C1, appendListsAsSets(B, C2));
//		List<Diagnosable> CS2 = quickXPlain(CS1, C2, appendListsAsSets(B, CS1));
//		List<Diagnosable> tempCS = appendListsAsSets(CS1, CS2);
//
//		return tempCS;
//	}
//
//	public static List<Diagnosable> appendListsAsSets(List<Diagnosable> CS1, List<Diagnosable> CS2) {
//		List<Diagnosable> union = new ArrayList<>(CS1);
//		if (CS2 == null)
//			return union;
//
//		for (Diagnosable c : CS2) {
//			if (!union.contains(c)) {
//				union.add(c);
//			}
//		}
//		return union;
//	}
//
//
//	public static List<Diagnosable> diffListsAsSets(List<Diagnosable> ac, List<Diagnosable> c2) {
//		List<Diagnosable> diff = new ArrayList<>();
//		for (Diagnosable element : ac) {
//			if (!c2.contains(element)) {
//				diff.add(element);
//			}
//		}
//		return diff;
//	}

	public static boolean isSupported(NameType nameType) {
		switch (nameType) {
		case INCOMPATIBLE:
		case REQUIRES:
		case EXCLUDES:
		case IMPLIES:
		case DECOMPOSITION:	
			return true;

		case CONTRIBUTES:
		case DAMAGES:
		case DUPLICATES:
		case REFINES:
		case REPLACES:
		case SIMILAR:
			return false;
		}
		return false; //should never be reached
	}

	public void requireAllElements() {
		for (Element4Csp element4Csp : element4CSPs)
			element4Csp.require(true);
	}

	public void requireAllRelations() {
		for (Relationship4Csp relationship4Csp : relationship4Csps)
			relationship4Csp.require(true);
	}


	//TODO JT: remove doubles? Or is there some reason
	public int determineCapacityAvailable(Container container) {
		Double d = (Double) this.elementModel.getAttributeValues().get(container.getAttributes().get("capacity")).getValue();
		return d.intValue();
	}

}
