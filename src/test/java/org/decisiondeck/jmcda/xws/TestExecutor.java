package org.decisiondeck.jmcda.xws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.utils.FunctionUtils;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.exc.InvalidInvocationException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class TestExecutor {
	public static class ConstantSupplierFactory implements FunctionWithInputCheck<String, ByteSource> {

		public static final String ASTRING = "ploum";
		public static final byte[] BYTES = ASTRING.getBytes(Charsets.UTF_8);

		public ConstantSupplierFactory() {
			/** Public default constructor (necessary). */
		}

		@Override
		public ByteSource apply(String input) throws InvalidInputException {
			return ByteSource.wrap(BYTES);
		}

	}

	public static class ConstantSupplierFactoryFromFile implements FunctionWithInputCheck<File, ByteSource> {

		public ConstantSupplierFactoryFromFile() {
			/** Public default constructor (necessary). */
		}

		@Override
		public ByteSource apply(File input) throws InvalidInputException {
			return Resources
					.asByteSource(Resources.getResource(this.getClass(), "SixRealCars with criteriaValues.xml"));
		}

	}

	static public class ToCriteriaConstant implements FunctionWithInputCheck<XCriteria, Set<Criterion>> {
		public static final Set<Criterion> CRITERIA = Collections.singleton(new Criterion("g1"));

		@Override
		public Set<Criterion> apply(XCriteria input) throws InvalidInputException {
			return CRITERIA;
		}

	}

	static class ServiceConstantSource implements IXWS {

		@XWSExceptions
		public List<InvalidInputException> m_exceptions;

		@XWSInput(transformer = ConstantSupplierFactory.class)
		public ByteSource m_sourceConstantSupplier;

		@XWSInput(transformer = ConstantSupplierFactoryFromFile.class, optional = true)
		public XCriteria m_sourceCriteria;

		private boolean m_executed;

		public ServiceConstantSource() {
			m_executed = false;
			m_sourceConstantSupplier = null;
			m_sourceCriteria = null;
			m_exceptions = null;
		}

		@Override
		public void execute() {
			m_executed = true;
		}

		public ByteSource getSourceConstantSupplier() {
			return m_sourceConstantSupplier;
		}

		public XCriteria getSourceCriteria() {
			return m_sourceCriteria;
		}

		public boolean isExecuted() {
			return m_executed;
		}

	}

	static class ServiceConstantSourceWithList implements IXWS {

		@XWSExceptions
		public List<InvalidInputException> m_exceptions;

		@XWSInput(transformer = ConstantSupplierFactoryFromFile.class)
		public List<XAlternatives> m_sourceList;
		@XWSInput(transformer = ConstantSupplierFactoryFromFile.class, optional = true)
		public List<XCriteria> m_sourceListCriteria;

		private boolean m_executed;

		public ServiceConstantSourceWithList() {
			m_executed = false;
		}

		@Override
		public void execute() {
			m_executed = true;
		}

		public List<InvalidInputException> getExceptions() {
			return m_exceptions;
		}

		public List<XAlternatives> getSourceXAlternatives() {
			return m_sourceList;
		}

		public List<XCriteria> getSourceXCriteria() {
			return m_sourceListCriteria;
		}

		public boolean isExecuted() {
			return m_executed;
		}

	}

	static class ServiceInputOutput implements IXWS {

		@XWSExceptions
		public List<InvalidInputException> m_exceptions;

		@XWSInputDirectory
		public File m_inputDirectory;

		@XWSOutputDirectory
		public File m_outputDirectory;

		private boolean m_executed;

		public ServiceInputOutput() {
			m_executed = false;
		}

		@Override
		public void execute() {
			m_executed = true;
		}

		public File getInputDirectory() {
			return m_inputDirectory;
		}

		public File getOutputDirectory() {
			return m_outputDirectory;
		}

		public boolean isExecuted() {
			return m_executed;
		}

	}

	static class ServiceSetCriteria implements IXWS {

		@XWSExceptions
		public List<InvalidInputException> m_exceptions;

		@XWSInput
		public Set<Criterion> m_sourceCriteria;

		@XWSInput(transformer = ToCriteriaConstant.class)
		public Set<Criterion> m_sourceCriteriaConstant;

		private boolean m_executed;

		public ServiceSetCriteria() {
			m_executed = false;
		}

		@Override
		public void execute() {
			m_executed = true;
		}

		public List<InvalidInputException> getExceptions() {
			return m_exceptions;
		}

		public Set<Criterion> getSourceCriteria() {
			return m_sourceCriteria;
		}

		public Set<Criterion> getSourceCriteriaConstant() {
			return m_sourceCriteriaConstant;
		}

		public boolean isExecuted() {
			return m_executed;
		}

	}

	@Test
	public void testInputOutput() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		exec.setArguments(new String[] { "-i", ".", "-o", ".", "-w", ServiceInputOutput.class.getName() });
		exec.setFieldsDirectories();

		final ServiceInputOutput service = (ServiceInputOutput) exec.getWorker();
		assertEquals(new File("."), service.getInputDirectory());
		assertEquals(new File("."), service.getOutputDirectory());

		exec.execute();
		assertTrue(service.isExecuted());
	}

	@Test(expected = InvalidInvocationException.class)
	public void testMissingWorker() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		exec.setArguments(new String[] { "-i", "input", "-o", "output" });
		exec.execute();
	}

	@Test(expected = InvalidInvocationException.class)
	public void testNonExistingInputDir() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		exec.setArguments(
				new String[] { "-i", "I.NVALID__", "-o", ".", "-w", ServiceInputOutput.class.getCanonicalName() });
		exec.execute();
	}

	@Test
	public void testServiceConstantSource() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		exec.setArguments(new String[] { "-i", ".", "-o", ".", "-w", ServiceConstantSource.class.getName() });
		exec.execute();
		final ServiceConstantSource service = (ServiceConstantSource) exec.getWorker();
		assertTrue(service.m_exceptions.isEmpty());
		assertTrue(service.isExecuted());

		final ByteSource sourceConstantSupplier = service.getSourceConstantSupplier();
		assertEquals(ConstantSupplierFactory.ASTRING, sourceConstantSupplier.asCharSource(Charsets.UTF_8).read());

		final XCriteria sourceCriteria = service.getSourceCriteria();
		assertEquals(SixRealCars.getInstance().getCriteria(), new XMCDACriteria().read(sourceCriteria));
	}

	@Test
	public void testServiceList() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		exec.setWorker(ServiceConstantSourceWithList.class);
		exec.setWorker();
		final FunctionWithInputCheck<String, File> functionWithInputCheck = FunctionUtils.constant(null);
		exec.getInputTransformer().setNameToFile(functionWithInputCheck);
		final ServiceConstantSourceWithList service = (ServiceConstantSourceWithList) exec.getWorker();
		exec.execute();
		assertTrue(service.isExecuted());
		assertTrue(service.getExceptions().isEmpty());
		assertEquals(2, service.getSourceXAlternatives().size());
		assertEquals(1, service.getSourceXCriteria().size());

		assertEquals(SixRealCars.getInstance().getCriteria().size(),
				Iterables.getOnlyElement(service.getSourceXCriteria()).getCriterionList().size());
	}

	@Test(expected = InvalidInvocationException.class)
	public void testServiceMissingInputDirectory() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		exec.setWorker(ServiceConstantSourceWithList.class);
		exec.setFieldsInputs();
	}

	@Test
	public void testServiceMissingRequiredField() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		exec.setInputDirectory(new File("."));
		// exec.setOutputDirectory(new File("out"));
		exec.setWorker(ServiceConstantSourceWithList.class);
		exec.setWorker();
		final ServiceConstantSourceWithList service = (ServiceConstantSourceWithList) exec.getWorker();
		exec.execute();
		assertFalse(service.isExecuted());
		assertEquals(1, service.getExceptions().size());
	}

	@Test
	public void testServiceSetCriteria() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		exec.setWorker(ServiceSetCriteria.class);
		exec.setWorker();
		final FunctionWithInputCheck<String, File> functionWithInputCheck = FunctionUtils.constant(null);
		exec.getInputTransformer().setNameToFile(functionWithInputCheck);
		exec.getInputTransformer().setFileToSource(new ConstantSupplierFactoryFromFile());
		final ServiceSetCriteria service = (ServiceSetCriteria) exec.getWorker();
		exec.execute();
		assertTrue(service.isExecuted());
		assertTrue(service.getExceptions().isEmpty());
		assertEquals(SixRealCars.getInstance().getCriteria(), service.getSourceCriteria());
		assertEquals(ToCriteriaConstant.CRITERIA, service.getSourceCriteriaConstant());
	}

}
