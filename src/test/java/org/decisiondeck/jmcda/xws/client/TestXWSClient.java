package org.decisiondeck.jmcda.xws.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.utils.matrix.SparseMatrixD;
import org.decision_deck.utils.persist.XmlReadUtils;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternativesMatrix;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAVarious;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesComparisons;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodMessages;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.io.Resources;

public class TestXWSClient {

	@Test
	public void testClientConcStaticBody() throws Exception {
		final XWSClient client = new XWSClient();
		client.setServiceUri(XWS_CONCORDANCE_URI);
		client.setSubmitProblemNode(
				XmlReadUtils.readNode(getClass().getResource("SixRealCars - XWSConcordance submitProblem body.xml")));
		client.submitProblem();
		client.requestSolution();
		final String msgString = client.getSolution("messages");
		final XMethodMessages methodMessages = Iterables
				.getOnlyElement(XMCDADoc.Factory.parse(msgString).getXMCDA().getMethodMessagesList());
		final List<String> strMessages = new XMCDAVarious().readMessages(methodMessages);
		final String resultMessage = Iterables.getOnlyElement(strMessages);
		assertEquals("Everything is ok.", resultMessage);

		final XAlternativesComparisons x = Iterables.getOnlyElement(
				XMCDADoc.Factory.parse(client.getSolution("concordance")).getXMCDA().getAlternativesComparisonsList());
		final SparseMatrixD<Alternative, Alternative> matrix = new XMCDAAlternativesMatrix()
				.readAlternativesFloatMatrix(x);
		final SparseAlternativesMatrixFuzzy expected = SixRealCars.getInstance().getConcordance();
		assertTrue(expected.approxEquals(matrix, 1e-6));
	}

	@Test
	public void testClientConc() throws Exception {
		final XWSClient client = new XWSClient();
		client.setServiceUri(XWS_CONCORDANCE_URI);
		client.putParameter("performances", new XMCDAReadUtils()
				.getXMCDADoc(Resources.asByteSource(getClass().getResource("../SixRealCars with criteriaValues.xml"))));
		client.putParameter("weights", new XMCDAReadUtils()
				.getXMCDADoc(Resources.asByteSource(getClass().getResource("../SixRealCars with criteriaValues.xml"))));
		client.putParameter("criteria", new XMCDAReadUtils()
				.getXMCDADoc(Resources.asByteSource(getClass().getResource("../SixRealCars with criteriaValues.xml"))));
		client.submitProblem();
		client.requestSolution();
		final String msgString = client.getSolution("messages");
		final XMethodMessages methodMessages = Iterables
				.getOnlyElement(XMCDADoc.Factory.parse(msgString).getXMCDA().getMethodMessagesList());
		final List<String> strMessages = new XMCDAVarious().readMessages(methodMessages);
		final String resultMessage = Iterables.getOnlyElement(strMessages);
		assertEquals("Everything is ok.", resultMessage);

		final XAlternativesComparisons x = Iterables.getOnlyElement(
				XMCDADoc.Factory.parse(client.getSolution("concordance")).getXMCDA().getAlternativesComparisonsList());
		final SparseMatrixD<Alternative, Alternative> matrix = new XMCDAAlternativesMatrix()
				.readAlternativesFloatMatrix(x);
		final SparseAlternativesMatrixFuzzy expected = SixRealCars.getInstance().getConcordance();
		assertTrue(expected.approxEquals(matrix, 1e-6));
	}

	private static final String XWS_CONCORDANCE_URI = "https://webservices.decision-deck.org/soap/ElectreConcordance-J-MCDA.py";

}