package eu.openreq.keljucaas.domain.release;

import org.chocosolver.solver.variables.BoolVar;

import eu.openreq.keljucaas.services.CSPPlanner.DiagnosableClass;

public interface Diagnosable {
	public void  require(boolean include);
	public void unRequire();
	public BoolVar getIsIncluded();
	public String getNameId();
	public Integer getId();
	public DiagnosableClass getDiagnosableClass();
}

//TODO 
//USE something like the following to order diagnoses by HSDAG approach to select the best one
//@Test
//public void whenThenComparing_thenSortedByAgeName(){
//    Comparator<Employee> employee_Age_Name_Comparator
//      = Comparator.comparing(Employee::getAge)
//        .thenComparing(Employee::getName);
//   
//    Arrays.sort(someMoreEmployees, employee_Age_Name_Comparator);
//   
//    assertTrue(Arrays.equals(someMoreEmployees, sortedEmployeesByAgeName));
//}