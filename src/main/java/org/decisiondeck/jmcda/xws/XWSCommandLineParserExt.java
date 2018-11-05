package org.decisiondeck.jmcda.xws;

import java.io.File;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.decisiondeck.jmcda.exc.InvalidInvocationException;

public class XWSCommandLineParserExt {

    private Options m_options;
    private String m_worker;
    private XWSCommandLineParser m_parser = new XWSCommandLineParser();

    public XWSCommandLineParserExt() {
	m_options = null;
	m_worker = null;
    }

    public Options getCommandLineOptions() {
	if (m_options == null) {
	    m_options = new Options();
	    final Options baseOptions = m_parser.getCommandLineOptions();
	    @SuppressWarnings("unchecked")
	    final Collection<Option> baseCollection = baseOptions.getOptions();
	    for (Option option : baseCollection) {
		m_options.addOption(option);
	    }

	    final Option optWorker = OptionBuilder.create("w");
	    optWorker.setArgs(1);
	    optWorker.setRequired(true);
	    optWorker.setDescription("The worker class which will execute the service.");
	    optWorker.setArgName("worker");
	    optWorker.setLongOpt("worker");
	    optWorker.setType("Class");

	    m_options.addOption(optWorker);
	}
	return m_options;
    }

    public String getSyntaxHelp() {
	final Options commandLineOptions = getCommandLineOptions();
	return m_parser.getSyntaxHelp(commandLineOptions);
    }

    /**
     * Parses the given arguments as if they were coming from a "main" method, and sets the input and output directories
     * and the worker.
     * 
     * @param args
     *            not <code>null</code>.
     * @throws InvalidInvocationException
     *             if unexpected arguments are found or expected ones are missing.
     */
    public void parse(String[] args) throws InvalidInvocationException {
	m_worker = null;

	final CommandLine line = m_parser.parse(args, getCommandLineOptions());

	m_worker = line.getOptionValue("w");
    }

    /**
     * Retrieves the output directory as a File. This method does not check anything related to the existence of the
     * file.
     * 
     * @return <code>null</code> if no successful parse occurred.
     * @see #getInputDir()
     */
    public File getOutputDirectory() {
	return m_parser.getOutputDirectory();
    }

    /**
     * Retrieves the output directory set as argument given to the {@link #parse(String[])} method.
     * 
     * @return <code>null</code> if no successful parse occurred.
     */
    public String getOutputDir() {
	return m_parser.getOutputDir();
    }

    /**
     * Retrieves the input directory set as argument given to the {@link #parse(String[])} method.
     * 
     * @return <code>null</code> if no successful parse occurred.
     */
    public String getInputDir() {
	return m_parser.getInputDir();
    }

    /**
     * Retrieves the worker set as argument given to the {@link #parse(String[])} method.
     * 
     * @return <code>null</code> if no successful parse occurred.
     */
    public String getWorker() {
	return m_worker;
    }

    /**
     * Retrieves the input directory as a File. This method does not check anything related to the existence of the
     * file.
     * 
     * @return <code>null</code> if no successful parse occurred.
     * @see #getInputDir()
     */
    public File getInputDirectory() {
	return m_parser.getInputDirectory();
    }

}
