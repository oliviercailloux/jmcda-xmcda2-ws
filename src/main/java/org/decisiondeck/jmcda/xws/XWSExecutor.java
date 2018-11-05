package org.decisiondeck.jmcda.xws;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.exc.InvalidInvocationException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAWriteUtils;
import org.decisiondeck.jmcda.xws.transformer.InputTransformer;
import org.decisiondeck.jmcda.xws.transformer.OutputTransformer;
import org.decisiondeck.jmcda.xws.transformer.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSink;
import com.google.common.io.Files;

/**
 * A class to execute XMCDA Web Services.
 *
 * @author Olivier Cailloux
 *
 */
public class XWSExecutor {
	private static final Logger s_logger = LoggerFactory.getLogger(XWSExecutor.class);

	public static void main(String[] args) throws IOException {
		final XWSExecutor exec = new XWSExecutor();
		s_logger.info("Starting executor with arguments {}.", args);
		exec.setArguments(args);
		try {
			exec.execute();
		} catch (InvalidInvocationException exc) {
			s_logger.error("Fatal error, terminating.", exc);
			System.err.println(exc.getLocalizedMessage());
			System.out.println(exec.getSyntaxHelp());
			return;
		}
	}

	private boolean m_allSet;

	private String[] m_args;

	private final List<InvalidInputException> m_exceptions = Lists.newLinkedList();

	private File m_inputDirectory;

	private final InputTransformer m_inputTransformer = new InputTransformer();

	private File m_outputDirectory;

	private final OutputTransformer m_outputTransformer;

	private Class<? extends IXWS> m_workerClass;

	private IXWS m_workerInstance;

	private String m_workerString;

	private boolean m_write;

	public XWSExecutor() {
		m_inputDirectory = null;
		m_outputDirectory = null;
		m_workerString = null;
		m_workerClass = null;
		m_workerInstance = null;
		m_args = null;
		m_allSet = false;
		m_write = true;
		m_outputTransformer = new OutputTransformer();
	}

	public void execute() throws InvalidInvocationException, IOException {
		/**
		 * TODO currently, no guarantee that all inputs are non null, even if
		 * not optional. The executor should probably guarantee this, or web
		 * services should check for this condition. It probably suffices to
		 * modify InputTransformer#getDocToTag (add optional parameter
		 * similarily to getDocToTags).
		 */
		m_exceptions.clear();

		setWorker();
		prepareDirectories();
		setFieldsDirectories();
		setFieldsInputs();

		/**
		 * Note that the field must be set even when no exceptions occurred.
		 * Might be set e.g. to an empty list or an empty document.
		 */
		setFieldExceptions();

		if (m_exceptions.isEmpty()) {
			try {
				m_workerInstance.execute();
			} catch (InvalidInputException exc) {
				m_exceptions.add(exc);
				setFieldExceptions();
			}
		}

		writeOutputs();
	}

	/**
	 * @return the input directory, or <code>null</code> if not set.
	 */
	public File getInputDirectory() {
		return m_inputDirectory;
	}

	/**
	 * Retrieves the input transformer this object uses. This can be used to
	 * inject other transformers and therefore change the behavior of this
	 * object regarding default transforming functions.
	 * 
	 * @return not <code>null</code>.
	 */
	public InputTransformer getInputTransformer() {
		return m_inputTransformer;
	}

	/**
	 * @return the output directory, or <code>null</code> if not set.
	 */
	public File getOutputDirectory() {
		return m_outputDirectory;
	}

	public OutputTransformer getOutputTransformer() {
		return m_outputTransformer;
	}

	public String getSyntaxHelp() {
		return new XWSCommandLineParserExt().getSyntaxHelp();
	}

	/**
	 * @return the instance worker, or <code>null</code> if not set.
	 */
	public IXWS getWorker() {
		return m_workerInstance;
	}

	/**
	 * @return the worker as a class, or <code>null</code> if not set.
	 */
	public Class<? extends IXWS> getWorkerClass() {
		return m_workerClass;
	}

	/**
	 * @return the worker as a string, or <code>null</code> if not set.
	 */
	public String getWorkerString() {
		return m_workerString;
	}

	/**
	 * <p>
	 * Parses the arguments associated to this object in order to set the input
	 * and output directory and the worker string. The arguments must be set.
	 * This overrides any possibly previously set worker instance, class or
	 * string.
	 * </p>
	 * <p>
	 * The arguments must be complete, thus must contain input directory, output
	 * directory and worker.
	 * </p>
	 * <p>
	 * After this method has exectued successfully, the three associated
	 * informations are non <code>null</code>.
	 * </p>
	 * 
	 * 
	 * @throws InvalidInvocationException
	 *             if unexpected arguments are found or expected ones are
	 *             missing.
	 * @see #setArguments(String[])
	 */
	public void parse() throws InvalidInvocationException {
		Preconditions.checkNotNull(m_args);

		final XWSCommandLineParserExt parser = new XWSCommandLineParserExt();
		parser.parse(m_args);
		m_inputDirectory = parser.getInputDirectory();
		m_outputDirectory = parser.getOutputDirectory();
		m_workerString = parser.getWorker();
		m_workerInstance = null;
		m_workerClass = null;
	}

	/**
	 * Checks and prepares the input and output directories. If the input
	 * directory is non <code>null</code>, checks that it exists and it is a
	 * directory. If the output directory is non <code>null</code>, if it does
	 * not exist, create it, if it exists, checks that it is a directory. If
	 * this object has write disabled, the output directory is not created and
	 * not checked for existence.
	 * 
	 * @throws InvalidInvocationException
	 *             if one of the checks fail, or the output directory creation
	 *             fails.
	 * @see #setWriteEnabled(boolean)
	 */
	public void prepareDirectories() throws InvalidInvocationException {
		if (m_write && m_outputDirectory != null) {
			if (!m_outputDirectory.exists()) {
				if (!m_outputDirectory.mkdirs()) {
					throw new InvalidInvocationException("Could not create directory " + m_outputDirectory + ".");
				}
			}
			if (!m_outputDirectory.isDirectory()) {
				throw new InvalidInvocationException("Output directory " + m_outputDirectory + " is not a directory.");
			}
		}

		if (m_inputDirectory != null) {
			if (!m_inputDirectory.exists()) {
				throw new InvalidInvocationException("Input directory " + m_inputDirectory + " does not exist.");
			}
			if (!m_inputDirectory.isDirectory()) {
				throw new InvalidInvocationException("Input directory " + m_inputDirectory + " is not a directory.");
			}
		}
	}

	/**
	 * Sets the arguments associated to this object.
	 * 
	 * @param args
	 *            <code>null</code> for not set.
	 */
	public void setArguments(String[] args) {
		m_args = args;
		m_allSet = false;
	}

	public void setFieldExceptions() throws InvalidInvocationException {
		setWorker();

		final List<InvalidInputException> exceptions = Lists.newLinkedList(m_exceptions);
		final Set<Field> exceptionFields = getAnnotatedFields(XWSExceptions.class);
		for (Field exceptionField : exceptionFields) {
			final Object exceptionsTransformed = new OutputTransformer().getAs(exceptions, exceptionField.getType());
			setField(exceptionField, exceptionsTransformed);
		}
	}

	/**
	 * <p>
	 * Sets the worker fields input and output directories, if corresponding
	 * annotations exist in the associated worker.
	 * </p>
	 * <p>
	 * The method first executes as if {@link #setWorker()} had been called,
	 * doing initialisation and sanity checks. Then it sets the field annotated
	 * with {@link XWSInputDirectory} to the value of the input directory set in
	 * this object, and similarily for the output directory. If a field is
	 * annotated with input directory and the input directory associated with
	 * this object is not set, an exception is thrown, and similarily for the
	 * output directory.
	 * </p>
	 * 
	 * @throws InvalidInvocationException
	 *             if a problem occurs while initialising the worker, or setting
	 *             the field, or if the input or output directory value is
	 *             missing and a corresponding annotation exists on a field of
	 *             the associated worker.
	 */
	public void setFieldsDirectories() throws InvalidInvocationException {
		setWorker();

		Field inputDirectoryfield = getAnnotatedFieldNoThrow(XWSInputDirectory.class);
		if (inputDirectoryfield != null) {
			if (m_inputDirectory == null) {
				throw new InvalidInvocationException("Input directory required but not set.");
			}
			setField(inputDirectoryfield, m_inputDirectory);
		}

		final Field outputDirectoryField = getAnnotatedFieldNoThrow(XWSOutputDirectory.class);
		if (outputDirectoryField != null) {
			if (m_outputDirectory == null) {
				throw new InvalidInvocationException("Output directory required but not set.");
			}
			setField(outputDirectoryField, m_outputDirectory);
		}
	}

	public void setFieldsInputs() throws InvalidInvocationException {
		setWorker();

		final Field[] fields = m_workerClass.getFields();
		for (final Field field : fields) {
			final XWSInput inputAnn = field.getAnnotation(XWSInput.class);
			if (inputAnn != null) {
				final String name;
				if (inputAnn.name().length() == 0) {
					name = field.getName() + ".xml";
				} else {
					name = inputAnn.name();
				}
				final boolean optional = inputAnn.optional();
				final Type targetType = field.getGenericType();
				@SuppressWarnings("unchecked")
				final Class<? extends FunctionWithInputCheck<Object, Object>> intermediateClass = (Class<? extends FunctionWithInputCheck<Object, Object>>) inputAnn
						.transformer();
				final Class<? extends FunctionWithInputCheck<Object, Object>> intermediateTransform = intermediateClass
						.equals(XWSInput.None.class) ? null : intermediateClass;
				Object targetValue = null;
				boolean setValue;
				try {
					targetValue = m_inputTransformer.get(targetType, intermediateTransform, name, m_inputDirectory,
							optional);
					setValue = true;
				} catch (InvalidInputException exc) {
					m_exceptions.add(exc);
					setValue = false;
				}
				/**
				 * Can't set the field to null when setvalue is false, as the
				 * field type could be e.g. double, which does not accept a
				 * <code>null</code> value.
				 */
				if (setValue) {
					try {
						field.set(m_workerInstance, targetValue);
					} catch (IllegalAccessException exc) {
						throw new InvalidInvocationException("Can't instantiate the field " + field.getName() + ".",
								exc);
					}
				}
			}
		}
	}

	/**
	 * Sets the input directory associated to this object.
	 * 
	 * @param inputDirectory
	 *            <code>null</code> for not set.
	 */
	public void setInputDirectory(File inputDirectory) {
		m_inputDirectory = inputDirectory;
		m_allSet = false;
	}

	/**
	 * Sets the output directory associated to this object.
	 * 
	 * @param outputDirectory
	 *            <code>null</code> for not set.
	 */
	public void setOutputDirectory(File outputDirectory) {
		m_outputDirectory = outputDirectory;
		m_allSet = false;
	}

	/**
	 * <p>
	 * Sets the worker instance and class.
	 * </p>
	 * <p>
	 * The method first checks that the parameters set are coherent: either
	 * arguments are set, then input directory, output directory and workers may
	 * not be set, or arguments is not set and then at least one of the workers
	 * form must be set among the three allowed forms, namely string, class or
	 * instance.
	 * </p>
	 * <p>
	 * If the worker instance is already set, this method does nothing more.
	 * </p>
	 * <p>
	 * If the arguments are set, they are parsed and the input and output
	 * directories and worker string are set. The worker instance and worker
	 * class are set, possibly by deduction from the arguments, or by deduction
	 * of already set worker string or class or instance.
	 * </p>
	 * <p>
	 * If the arguments and worker and directories setter methods have not been
	 * called since the last call to this method, it is not executed again,
	 * which means that the check does not occur, in particular having arguments
	 * <em>and</em> workers set is not an error. Otherwise, every second call of
	 * this method would fail.
	 * </p>
	 * 
	 * @throws InvalidInvocationException
	 *             if unexpected arguments are found or expected ones are
	 *             missing; if the worker string does not correspond to a class
	 *             or does not implement the IXWS interface, or if the worker
	 *             class has no exception field; or if the worker can't be
	 *             instantiated.
	 */
	public void setWorker() throws InvalidInvocationException {
		if (m_allSet) {
			return;
		}
		final boolean useArgs = m_args != null && m_inputDirectory == null && m_outputDirectory == null
				&& m_workerString == null;
		final boolean dontUseArgs = m_args == null
				&& (m_workerString != null || m_workerClass != null || m_workerInstance != null);

		Preconditions.checkState(useArgs || dontUseArgs, "Parameters set are not coherent.");
		if (useArgs) {
			parse();
		}

		setWorkerClass();

		setWorkerInstanceInternal();

		m_allSet = true;
	}

	/**
	 * Sets the worker that this object should use, as a class. This resets any
	 * possibly previously set worker, be it as string, class, or instance.
	 * 
	 * @param workerClass
	 *            <code>null</code> for not set.
	 */
	public void setWorker(Class<? extends IXWS> workerClass) {
		m_workerClass = workerClass;
		m_workerString = null;
		m_workerInstance = null;
		m_allSet = false;
	}

	/**
	 * Sets the worker instance that this object should use. This resets any
	 * possibly previously set worker, be it as string, class, or instance.
	 * 
	 * @param workerInstance
	 *            <code>null</code> for not set.
	 */
	public void setWorker(IXWS workerInstance) {
		m_workerClass = null;
		m_workerString = null;
		m_workerInstance = workerInstance;
		m_allSet = false;
	}

	/**
	 * Sets the worker that this object should use, as a string. This resets any
	 * possibly previously set worker, be it as string, class, or instance.
	 * 
	 * @param workerString
	 *            <code>null</code> for not set.
	 */
	public void setWorker(String workerString) {
		m_workerString = workerString;
		m_workerClass = null;
		m_workerInstance = null;
		m_allSet = false;
	}

	/**
	 * <p>
	 * Sets the worker class that this object should use by deducing it from the
	 * worker string or instance.
	 * </p>
	 * <p>
	 * The worker string or worker class or worker instance must be set.
	 * </p>
	 * <p>
	 * The method checks that the resulting worker has at least one field
	 * annotated for receiving exceptions. The method also checks that no more
	 * than one field is annotated with input directory and no more than one is
	 * annotated with output directory.
	 * </p>
	 * <p>
	 * If the worker class is already set, this method has no effect other than
	 * the sanity checks exposed here above.
	 * </p>
	 * 
	 * @throws InvalidInvocationException
	 *             if the worker string does not correspond to a class or does
	 *             not implement the {@link IXWS} interface, or if the worker
	 *             class (already set or resulting from this method call) has no
	 *             exception field.
	 */
	public void setWorkerClass() throws InvalidInvocationException {
		if (m_workerClass == null) {
			if (m_workerInstance != null) {
				m_workerClass = m_workerInstance.getClass();
			} else {
				Preconditions.checkState(m_workerString != null);

				final Class<?> workerClass;
				try {
					workerClass = Class.forName(m_workerString);
				} catch (ClassNotFoundException exc) {
					throw new InvalidInvocationException(exc);
				}
				final List<Class<?>> interfaces = Arrays.asList(workerClass.getInterfaces());
				if (!interfaces.contains(IXWS.class)) {
					throw new InvalidInvocationException("The given worker class, " + m_workerString
							+ ", does not implement " + IXWS.class.getCanonicalName() + ".");
				}

				@SuppressWarnings("unchecked")
				final Class<? extends IXWS> w = (Class<? extends IXWS>) workerClass;
				m_workerClass = w;
			}
		}
		if (getAnnotatedFields(XWSExceptions.class).isEmpty()) {
			throw new InvalidInvocationException("Exception field not found.");
		}
		getAnnotatedField(XWSInputDirectory.class);
		getAnnotatedField(XWSOutputDirectory.class);
	}

	public void setWriteEnabled(boolean writeEnabled) {
		m_write = writeEnabled;
	}

	/**
	 * <p>
	 * Writes the data set in the fields annotated with {@link XWSOutput}
	 * annotation to their respective file, if writing is enabled in this
	 * object.
	 * </p>
	 * <p>
	 * This method computes the document to write in the file according to the
	 * given and automatic transformers it is associated with. The computing is
	 * done even if writing is disabled, and an exception is thrown if a value
	 * can't be computed. If a field has a value which is <code>null</code>, no
	 * writing is done, and the associated transformed, if any, is not called.
	 * </p>
	 * <p>
	 * If writing is enabled, the output directory must be non <code>null</code>
	 * when calling this method.
	 * </p>
	 * 
	 * @throws InvalidInvocationException
	 *             if a field can't be accessed, or the transformer be
	 *             instantiated.
	 * @throws IOException
	 *             if an exception happens while writing to the destination.
	 * @see #setWriteEnabled(boolean)
	 */
	public void writeOutputs() throws InvalidInvocationException, IOException {
		Preconditions.checkState(m_allSet);

		for (final Field field : getAnnotatedFields(XWSOutput.class)) {
			final XWSOutput outputAnn = field.getAnnotation(XWSOutput.class);
			final String name;
			if (outputAnn.name().length() == 0) {
				name = field.getName() + ".xml";
			} else {
				name = outputAnn.name();
			}
			final Object value;
			try {
				value = field.get(m_workerInstance);
			} catch (IllegalArgumentException exc) {
				throw new InvalidInvocationException(exc);
			} catch (IllegalAccessException exc) {
				throw new InvalidInvocationException(exc);
			}
			if (value == null) {
				continue;
			}
			final Type type = field.getGenericType();

			final Object transformed;
			final Type transformedType;
			{
				@SuppressWarnings("unchecked")
				final Class<? extends Function<Object, ? extends XmlObject>> intermediateClass = (Class<? extends Function<Object, ? extends XmlObject>>) outputAnn
						.transformer();
				final Class<? extends Function<Object, ? extends XmlObject>> intermediateTransform = intermediateClass
						.equals(XWSOutput.None.class) ? null : intermediateClass;
				// intermediateTransform.getg
				Function<Object, ? extends XmlObject> transformer;
				// transformer.
				try {
					if (intermediateTransform == null) {
						transformer = null;
						transformedType = type;
					} else {
						transformedType = Transformers.getApplyMethodGeneric(intermediateTransform)
								.getGenericReturnType();
						transformer = intermediateTransform.newInstance();
					}
				} catch (InstantiationException exc) {
					throw new InvalidInvocationException(exc);
				} catch (IllegalAccessException exc) {
					throw new InvalidInvocationException(exc);
				}
				if (transformer == null) {
					transformed = value;
				} else {
					transformed = transformer.apply(value);
				}
			}

			final XMCDADoc doc = m_outputTransformer.getAsDoc(transformed, transformedType);

			write(m_outputDirectory, name, doc);
		}
	}

	/**
	 * The worker class must be set.
	 * 
	 * @param annotationClass
	 *            not <code>null</code>.
	 * @return <code>null</code> if not found.
	 * @throws InvalidInvocationException
	 *             if more than one corresponding field found.
	 */
	private Field getAnnotatedField(Class<? extends Annotation> annotationClass) throws InvalidInvocationException {
		Preconditions.checkState(m_workerClass != null);
		final Set<Field> fields = getAnnotatedFields(annotationClass);
		if (fields.size() >= 2) {
			throw new InvalidInvocationException(
					"Found more than one field annotated with " + annotationClass + " in " + m_workerClass + ".");
		}
		return Iterables.getOnlyElement(fields, null);
	}

	private Field getAnnotatedFieldNoThrow(Class<? extends Annotation> annotationClass) {
		try {
			return getAnnotatedField(annotationClass);
		} catch (InvalidInvocationException exc) {
			throw new IllegalStateException(exc);
		}
	}

	/**
	 * The worker class must be set.
	 * 
	 * @param annotationClass
	 *            not <code>null</code>.
	 * @return not <code>null</code>.
	 */
	private Set<Field> getAnnotatedFields(Class<? extends Annotation> annotationClass) {
		final Set<Field> fields = Sets.newLinkedHashSet();
		for (final Field field : m_workerClass.getFields()) {
			if (field.getAnnotation(annotationClass) != null) {
				fields.add(field);
			}
		}
		return fields;
	}

	private void setField(Field field, Object value) throws InvalidInvocationException {
		Preconditions.checkState(m_workerInstance != null);

		try {
			field.set(m_workerInstance, value);
		} catch (IllegalAccessException exc) {
			throw new InvalidInvocationException("Can't set the field " + field.getName() + ".", exc);
		}
	}

	/**
	 * Sets the worker instance that this object should use by instantiating the
	 * worker class, possibly deducing the worker class itself from the worker
	 * string. The worker string or worker class or worker instance must be set.
	 * If the worker instance is already set, this method has no effect.
	 * 
	 * @throws InvalidInvocationException
	 *             if the worker string does not correspond to a class or does
	 *             not implement the {@link IXWS} interface, or if the worker
	 *             can't be instantiated.
	 */
	private void setWorkerInstanceInternal() throws InvalidInvocationException {
		if (m_workerInstance != null) {
			return;
		}

		setWorkerClass();

		try {
			m_workerInstance = m_workerClass.newInstance();
		} catch (InstantiationException exc) {
			throw new InvalidInvocationException("Can't instantiate the associated worker.", exc);
		} catch (IllegalAccessException exc) {
			throw new InvalidInvocationException("Can't instantiate the associated worker.", exc);
		}
	}

	/**
	 * Writes the given document to the file corresponding to the given name, in
	 * the given output directory, if writing is enabled in this object. If
	 * write is not enabled, the parameters may be <code>null</code>.
	 * 
	 * @param outputDirectory
	 *            not <code>null</code>.
	 * @param name
	 *            not <code>null</code>.
	 * @param doc
	 *            not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while writing to the destination.
	 * @see #setWriteEnabled(boolean)
	 */
	private void write(File outputDirectory, String name, XMCDADoc doc) throws IOException {
		if (m_write) {
			Preconditions.checkNotNull(outputDirectory);
			Preconditions.checkNotNull(name);
			Preconditions.checkNotNull(doc);

			final File out = new File(outputDirectory, name);
			final ByteSink supplier = Files.asByteSink(out);
			final XMCDAWriteUtils utils = new XMCDAWriteUtils();
			utils.setValidate(m_outputTransformer.validates());
			utils.write(doc, supplier);
		}
	}

}
