package org.decisiondeck.jmcda.xws.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.decision_deck.utils.persist.XmlReadUtils;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAWriteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

/**
 * A class to help access XMCDA Web Services. Define the parameters, define the
 * service URI, and send.
 *
 * @author Olivier Cailloux
 *
 */
public class XWSClient {

	private static final Logger s_logger = LoggerFactory.getLogger(XWSClient.class);

	static public DocumentBuilder getDocumentBuilder() throws XWSCallException {
		final DocumentBuilder builder;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException exc) {
			throw new XWSCallException(exc);
		}
		return builder;
	}

	private final Map<String, XMCDADoc> m_parameters = Maps.newLinkedHashMap();

	private String m_serviceUri;

	private Map<String, String> m_solution;

	private Node m_submitProblemNode;

	private String m_submitProblemReturnMessage;

	private String m_ticket;

	public XWSClient() {
		m_serviceUri = null;
		m_submitProblemReturnMessage = null;
		m_ticket = null;
		m_submitProblemNode = null;
		m_solution = null;
	}

	public void clearParameters() {
		m_parameters.clear();
		m_submitProblemNode = null;
	}

	public Map<String, XMCDADoc> getParameters() {
		return Collections.unmodifiableMap(m_parameters);
	}

	/**
	 * @return <code>null</code> for not set.
	 */
	public String getServiceUri() {
		return m_serviceUri;
	}

	public Map<String, String> getSolution() {
		return Collections.unmodifiableMap(m_solution);
	}

	public String getSolution(String keyName) {
		checkNotNull(keyName);
		return m_solution.get(keyName);
	}

	public Node getSubmitProblemNode() throws XWSCallException {
		if (m_submitProblemNode != null) {
			return m_submitProblemNode;
		}
		final Document newDocument = XWSClient.getDocumentBuilder().newDocument();
		m_submitProblemNode = newDocument.createElement("submitProblem");
		newDocument.appendChild(m_submitProblemNode);
		for (String key : m_parameters.keySet()) {
			final Element elem = newDocument.createElement(key);
			elem.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
			elem.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "xsd:string");
			final XMCDADoc doc = m_parameters.get(key);
			// final String stringParam = XmlUtils.toString(node);
			elem.setTextContent(doc.toString());
			/**
			 * Can't add the node as node, must add as string (thus with '<'
			 * quoted, e.g.).
			 */
			// for (Node child = node.getFirstChild(); child != null; child =
			// child.getNextSibling()) {
			// final Node imported = newDocument.importNode(child, true);
			// elem.appendChild(imported);
			// }
			m_submitProblemNode.appendChild(elem);
		}
		return m_submitProblemNode;
	}

	/**
	 * Retrieves the ticket string that has been set manually or that has been
	 * read in reply to a {@link #submitProblem()} call.
	 * 
	 * @return <code>null</code> if not set manually and no successful submit
	 *         problem happened yet, or if set to <code>null</code> manually.
	 * @see #submitProblem()
	 * @see #setTicket(String)
	 */
	public String getTicket() {
		return m_ticket;
	}

	public void putAllParameters(Map<? extends String, ? extends XMCDADoc> m) {
		m_parameters.putAll(m);
		m_submitProblemNode = null;
	}

	public void putParameter(String key, XMCDADoc doc) {
		checkNotNull(key);
		checkNotNull(doc);
		m_submitProblemNode = null;
		m_parameters.put(key, doc);
	}

	public Map<String, String> requestSolution() throws XWSCallException {
		checkState(m_serviceUri != null);
		checkState(m_ticket != null);
		final StreamSource source = new StreamSource(
				new StringReader("<requestSolution><ticket>" + m_ticket + "</ticket></requestSolution>"));
		final DOMResult result = new DOMResult();
		// final StringResult finalResultStr = new StringResult();
		final boolean replied = new WebServiceTemplate().sendSourceAndReceiveToResult(m_serviceUri, source, result);
		if (!replied) {
			throw new XWSCallException("No reply from service.");
		}
		final Node resultNode = result.getNode();
		m_solution = XmlReadUtils.getChildsTextContents(resultNode.getFirstChild());

		if (!m_solution.containsKey("ticket")) {
			throw new XWSCallException("Solution ticket not found.");
		}
		if (!m_solution.containsKey("service-status")) {
			throw new XWSCallException("Service status not found.");
		}

		final String solutionTicket = m_solution.remove("ticket");
		if (!solutionTicket.equals(m_ticket)) {
			throw new XWSCallException("Ticket does not match.");
		}
		final String status = m_solution.remove("service-status");
		if (!status.equals("0")) {
			throw new XWSCallException("Unexpected status: " + status + ".");
		}

		return m_solution;
	}

	/**
	 * @param serviceUri
	 *            <code>null</code> for not set.
	 */
	public void setServiceUri(String serviceUri) {
		m_serviceUri = serviceUri;
	}

	public void setSubmitProblemNode(Node submitProblemNode) {
		m_submitProblemNode = submitProblemNode;
	}

	public void setTicket(String ticket) {
		m_ticket = ticket;
	}

	public String submitProblem() throws XWSCallException {
		checkState(m_serviceUri != null);
		final Node problemNode = getSubmitProblemNode();
		final DOMSource source = new DOMSource(problemNode);
		s_logger.debug("Sending source: {} to {}.", XmlReadUtils.toString(problemNode), m_serviceUri);
		final DOMResult result = new DOMResult();
		final boolean replied = new WebServiceTemplate().sendSourceAndReceiveToResult(m_serviceUri, source, result);
		if (!replied) {
			throw new XWSCallException("No reply from service.");
		}

		m_submitProblemReturnMessage = getXPath(result.getNode(), "//message").replace("\n", "");
		if (m_submitProblemReturnMessage == null) {
			throw new XWSCallException("No return message.");
		}
		s_logger.info("Result message from {}: {}.", m_serviceUri, m_submitProblemReturnMessage);
		m_ticket = getXPath(result.getNode(), "//ticket");
		s_logger.info("Result ticket from {}: {}.", m_serviceUri, m_ticket);
		return m_ticket;
	}

	public void writeInputParameters(File outputDir) throws IOException {
		outputDir.mkdirs();
		for (String param : m_parameters.keySet()) {
			final XMCDADoc doc = m_parameters.get(param);
			// final String nodeStr = XmlUtils.toString(node);
			final File outFile = new File(outputDir, param + ".xml");
			new XMCDAWriteUtils().write(doc, Files.asByteSink(outFile));
			// Files.write(nodeStr, outFile, Charsets.UTF_8);
		}
	}

	public void writeSolution(File outputDir) throws IOException {
		outputDir.mkdirs();
		for (String solutionKey : m_solution.keySet()) {
			final String contents = m_solution.get(solutionKey);
			final File outFile = new File(outputDir, solutionKey + ".xml");
			Files.write(contents, outFile, Charsets.UTF_8);
		}
	}

	private String getXPath(Node node, String xPath) {
		final XPathExpression expr = XPathExpressionFactory.createXPathExpression(xPath);
		return expr.evaluateAsString(node);
	}

}