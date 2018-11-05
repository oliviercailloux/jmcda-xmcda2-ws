package org.decisiondeck.jmcda.xws.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.utils.collection.SetBackedMap;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAssignments;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACategories;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAEvaluations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesAffectations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategories;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategoriesComparisons;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategoriesProfiles;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaLinearConstraints;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaLinearConstraints.Constraint;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaLinearConstraints.Constraint.Element;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XRankedLabel;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XValue;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XVariable;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAWriteUtils;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.assignments.ISortingAssignmentsToMultiple;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class XWSClientIris {
	private static final String XWS_IRIS_URI = "http://ernst-schroeder.uni.lu/cgi-bin/IRIS-CppXMCDA.py";
	final private XWSClient m_client = new XWSClient();
	private final ISortingAssignmentsToMultiple m_examples;
	private final EvaluationsRead m_profilesEvaluations;

	private IAssignmentsToMultiple m_solutionAssignments;

	public XWSClientIris(ISortingAssignmentsToMultiple examples, EvaluationsRead profilesEvaluations) {
		checkNotNull(examples);
		m_examples = examples;
		m_profilesEvaluations = profilesEvaluations == null ? EvaluationsUtils.newEvaluationMatrix()
				: profilesEvaluations;
		m_solutionAssignments = null;
	}

	public XWSClient getClient() {
		return m_client;
	}

	public IOrderedAssignmentsToMultiple getSolutionAssignments() {
		try {
			return m_solutionAssignments == null ? null
					: AssignmentsFactory.newOrderedAssignmentsToMultiple(m_solutionAssignments,
							m_examples.getCatsAndProfs().getCategories());
		} catch (InvalidInputException exc) {
			throw new IllegalStateException("Returned set of categories does not match given set of categories.", exc);
		}
	}

	public void initClient() {
		m_client.setServiceUri(XWS_IRIS_URI);
		final XMCDAWriteUtils writer = new XMCDAWriteUtils();

		final XMCDAAlternatives alternativesWriter = new XMCDAAlternatives();
		alternativesWriter.setMarkActiveAlternatives(true);
		alternativesWriter.setInactiveAlternatives(m_examples.getProfiles());
		final XAlternatives xAlternatives = alternativesWriter.writeAlternatives(m_examples.getAllAlternatives(), null);
		m_client.putParameter("alternatives", writer.getDoc(xAlternatives));

		final XCategoriesProfiles xCatsProfs = new XMCDACategories().write(m_examples.getCatsAndProfs());
		m_client.putParameter("categoriesProfiles", writer.getDoc(xCatsProfs));

		final XCategories xCats = new XMCDACategories().write(m_examples.getCatsAndProfs().getCategories());
		m_client.putParameter("categories", writer.getDoc(xCats));

		final XCategoriesComparisons xCatsComps = new XMCDACategories()
				.writeComparisons(m_examples.getCatsAndProfs().getCategories());
		m_client.putParameter("categoriesComparisons", writer.getDoc(xCatsComps));

		final XMCDACriteria criteriaWriter = new XMCDACriteria();
		final Map<Criterion, Interval> scales = m_examples.getScales();
		final Map<Criterion, Interval> scalesDirs = Maps.transformValues(scales, new Function<Interval, Interval>() {
			@Override
			public Interval apply(Interval input) {
				return Intervals.newDirection(input.getPreferenceDirection());
			}
		});
		criteriaWriter.setScales(scalesDirs);
		criteriaWriter.setPreferenceThresholds(
				new SetBackedMap<>(m_examples.getCriteria(), Functions.constant(Double.valueOf(0.00001))));
		criteriaWriter.setIndifferenceThresholds(
				new SetBackedMap<>(m_examples.getCriteria(), Functions.constant(Double.valueOf(0))));
		criteriaWriter
				.setVetoThresholds(new SetBackedMap<>(m_examples.getCriteria(), Functions.constant(Double.valueOf(0))));
		final XCriteria xCriteria = criteriaWriter.write(m_examples.getCriteria());
		m_client.putParameter("criteria", writer.getDoc(xCriteria));

		m_client.putParameter("criteriaLinearConstraints", writer.getDoc(getConstraints()));

		final EvaluationsRead allEvaluations = EvaluationsUtils.merge(m_examples.getAlternativesEvaluations(),
				m_profilesEvaluations);
		final XMCDAEvaluations evaluationsWriter = new XMCDAEvaluations();
		evaluationsWriter.setAlternativesOrder(m_examples.getAllAlternatives());
		evaluationsWriter.setCriteriaOrder(m_examples.getCriteria());
		final XPerformanceTable xEvaluations = evaluationsWriter.write(allEvaluations);
		m_client.putParameter("performanceTable", writer.getDoc(xEvaluations));

		final XMCDAAssignments assignmentsWriter = new XMCDAAssignments();
		assignmentsWriter.setAlternativesOrder(m_examples.getAlternatives());
		assignmentsWriter.setForceIntervals(true);
		final XAlternativesAffectations xAssignments = assignmentsWriter.write(m_examples.getAssignments());
		m_client.putParameter("alternativesAffectations", writer.getDoc(xAssignments));
	}

	public void requestSolution() throws XWSCallException, XmlException {
		m_client.requestSolution();
		if (!m_client.getSolution().containsKey("alternativesAffectations_robustAssignments")) {
			throw new XWSCallException("Robust assignments solution not found.");
		}
		final String assignmentsStr = m_client.getSolution("alternativesAffectations_robustAssignments");
		final XAlternativesAffectations xAffectations = Iterables
				.getOnlyElement(XMCDADoc.Factory.parse(assignmentsStr).getXMCDA().getAlternativesAffectationsList());
		try {
			m_solutionAssignments = new XMCDAAssignments().read(xAffectations);
		} catch (InvalidInputException exc) {
			throw new XWSCallException(exc);
		}
	}

	public void send() throws XWSCallException, XmlException {
		initClient();
		submitProblem();
		try {
			Thread.sleep(4 * 1000);
		} catch (@SuppressWarnings("unused") InterruptedException exc1) {
			// nothing
		}
		requestSolution();
	}

	public void submitProblem() throws XWSCallException {
		initClient();
		m_client.submitProblem();
	}

	private XCriteriaLinearConstraints getConstraints() {
		final XCriteriaLinearConstraints xConstraints = XMCDADoc.Factory.newInstance().addNewXMCDA()
				.addNewCriteriaLinearConstraints();
		{
			final Constraint xLambdaCeiling = xConstraints.addNewConstraint();
			/** Sample files has "lamda", not "lambda"; let's stick to this. */
			xLambdaCeiling.setName("lamda<=0.99");
			final Element xLCElement = xLambdaCeiling.addNewElement();
			final XVariable xLCVariable = xLCElement.addNewVariable();
			xLCVariable.setMcdaConcept("cutting level");
			xLCVariable.setId("lamda");
			xLCElement.addNewCoefficient().setReal(-1);
			xLambdaCeiling.addNewRhs().setReal(-0.99f);
			xLambdaCeiling.setOperator(Constraint.Operator.GEQ);
			final XValue xLCValue = xLambdaCeiling.addNewValue();
			xLCValue.setMcdaConcept("confidence level");
			final XRankedLabel xLCLabel = xLCValue.addNewRankedLabel();
			xLCLabel.setLabel("sure");
			xLCLabel.setRank(BigInteger.valueOf(1));
		}

		{
			final Constraint xSumWeights = xConstraints.addNewConstraint();
			xSumWeights.setName("sum=1");
			for (Criterion criterion : m_examples.getCriteria()) {
				final Element xSumWeightsElement = xSumWeights.addNewElement();
				xSumWeightsElement.setCriterionID(criterion.getId());
				xSumWeightsElement.addNewCoefficient().setReal(1);
			}
			xSumWeights.addNewRhs().setReal(1);
			xSumWeights.setOperator(Constraint.Operator.EQ);
			final XValue xSWValue = xSumWeights.addNewValue();
			xSWValue.setMcdaConcept("confidence level");
			final XRankedLabel xSWLabel = xSWValue.addNewRankedLabel();
			xSWLabel.setLabel("sure");
			xSWLabel.setRank(BigInteger.valueOf(1));
		}
		return xConstraints;
	}

}
