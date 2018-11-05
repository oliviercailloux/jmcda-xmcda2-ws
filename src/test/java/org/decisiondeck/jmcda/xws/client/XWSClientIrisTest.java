package org.decisiondeck.jmcda.xws.client;

import org.junit.Test;

public class XWSClientIrisTest {
    @Test
    public void testIrisTwo() throws Exception {
	// final XMCDASortingProblemReader reader = new XMCDASortingProblemReader();
	// reader.setAlternativesParsingMethod(AlternativesParsingMethod.SEEK_CONCEPT);
	// reader.setSourceMain(Resources.asByteSource(getClass().getResource("Two identical alternatives.xml")));
	//
	// final ISortingResultsToMultiple input = reader.readSortingResultsToMultiple();
	//
	// final XWSClientIris clientIris = new XWSClientIris(
	// SortingProblemUtils.getResultsWithAllAlternativesAssigned(input), input.getProfilesEvaluations());
	// clientIris.submitProblem();
	// Thread.sleep(1000);
	// clientIris.requestSolution();
	// final IOrderedAssignmentsToMultiple result = clientIris.getSolutionAssignments();
	// assertEquals(2, result.getAlternatives().size());
	//
	// final NavigableSet<Category> catsA1 = result.getCategories(new Alternative("a1"));
	// final Iterator<Category> a1Iterator = catsA1.iterator();
	// assertEquals(new Category("Cat2"), a1Iterator.next());
	// assertEquals(new Category("Cat3"), a1Iterator.next());
	// assertFalse(a1Iterator.hasNext());
	//
	// final NavigableSet<Category> catsA2 = result.getCategories(new Alternative("a2"));
	// final Iterator<Category> a2Iterator = catsA2.iterator();
	// assertEquals(new Category("Cat2"), a2Iterator.next());
	// assertEquals(new Category("Cat3"), a2Iterator.next());
	// assertFalse(a2Iterator.hasNext());
    }

    @Test
    public void testIris() throws Exception {
	// // final SixRealCars data = SixRealCars.getInstance();
	// // final XWSClientIris clientIris = new XWSClientIris(data.getAsSortingResults75Both(),
	// // data.getProfilesEvaluations());
	// final XMCDASortingProblemReader reader = new XMCDASortingProblemReader();
	// reader.setAlternativesParsingMethod(AlternativesParsingMethod.SEEK_CONCEPT);
	// reader.setSourceMain(Resources.asByteSource(getClass().getResource(
	// "Housing - Multiple constraints, relaxed.xml")));
	//
	// final ISortingResultsToMultiple input = reader.readSortingResultsToMultiple();
	// // export(input);
	//
	// reader.setSourceMain(Resources.asByteSource(getClass().getResource(
	// "Housing - Multiple constraints, relaxed - Answers.xml")));
	// final ISortingResultsToMultiple expected = reader.readSortingResultsToMultiple();
	//
	// final XWSClientIris clientIris = new XWSClientIris(
	// SortingProblemUtils.getResultsWithAllAlternativesAssigned(input), input.getProfilesEvaluations());
	// clientIris.submitProblem();
	// Thread.sleep(1000);
	// clientIris.requestSolution();
	// final IOrderedAssignmentsToMultiple res = clientIris.getSolutionAssignments();
	//
	// assertEquals(input.getAlternatives(), res.getAlternatives());
	// final IOrderedAssignmentsToMultiple assignments = expected.getAssignments();
	// assertEquals(assignments, res);

    }

}
