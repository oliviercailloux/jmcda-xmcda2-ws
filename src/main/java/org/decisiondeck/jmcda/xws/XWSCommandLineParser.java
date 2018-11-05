package org.decisiondeck.jmcda.xws;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.decisiondeck.jmcda.exc.InvalidInvocationException;

public class XWSCommandLineParser {

    private Options m_options;
    private String m_inputDir;
    private String m_outputDir;

    public XWSCommandLineParser() {
	m_options = null;
	m_inputDir = null;
	m_outputDir = null;
    }

    public Options getCommandLineOptions() {
	if (m_options == null) {
	    final Option optIn = OptionBuilder.create("i");
	    optIn.setArgs(1);
	    optIn.setRequired(true);
	    optIn.setDescription("The directory where input files can be found.");
	    optIn.setArgName("inputDir");
	    optIn.setLongOpt("inputDir");
	    optIn.setType("File");

	    final Option optOut = OptionBuilder.create("o");
	    optOut.setArgs(1);
	    optOut.setRequired(true);
	    optOut.setDescription("The directory where output files will be written.");
	    optOut.setArgName("outputDir");
	    optOut.setLongOpt("outputDir");
	    optOut.setType("File");

	    m_options = new Options();
	    m_options.addOption(optIn);
	    m_options.addOption(optOut);
	}
	return m_options;
    }

    public String getSyntaxHelp() {
	final Options commandLineOptions = getCommandLineOptions();
	return getSyntaxHelp(commandLineOptions);
    }

    protected String getSyntaxHelp(Options commandLineOptions) {
	final HelpFormatter hlp = new HelpFormatter();
	final StringWriter wr = new StringWriter();
	final PrintWriter pwr = new PrintWriter(wr);
	hlp.printHelp(pwr, hlp.getWidth(), "prg", null, commandLineOptions, hlp.getLeftPadding(), hlp.getDescPadding(),
		null, true);
	return wr.toString();
    }

    /**
     * Parses the given arguments as if they were coming from a "main" method, and sets the input and output
     * directories.
     * 
     * @param args
     *            not <code>null</code>.
     * @throws InvalidInvocationException
     *             if unexpected arguments are found or expected ones are missing.
     */
    public void parse(String[] args) throws InvalidInvocationException {
	parse(args, getCommandLineOptions());
    }

    protected CommandLine parse(String[] args, Options commandLineOptions) throws InvalidInvocationException {
	m_inputDir = null;
	m_outputDir = null;

	final CommandLine line;
	try {
	    line = new GnuParser().parse(commandLineOptions, args);
	} catch (ParseException exc) {
	    throw new InvalidInvocationException(exc);
	}

	m_inputDir = line.getOptionValue("i");
	m_outputDir = line.getOptionValue("o");

	return line;
    }

    /**
     * Retrieves the output directory as a File. This method does not check anything related to the existence of the
     * file.
     * 
     * @return <code>null</code> if no successful parse occurred.
     * @see #getInputDir()
     */
    public File getOutputDirectory() {
	return m_outputDir == null ? null : new File(m_outputDir);
    }

    /**
     * Retrieves the output directory set as argument given to the {@link #parse(String[])} method.
     * 
     * @return <code>null</code> if no successful parse occurred.
     */
    public String getOutputDir() {
	return m_outputDir;
    }

    /**
     * Retrieves the input directory set as argument given to the {@link #parse(String[])} method.
     * 
     * @return <code>null</code> if no successful parse occurred.
     */
    public String getInputDir() {
	return m_inputDir;
    }

    /**
     * Retrieves the input directory as a File. This method does not check anything related to the existence of the
     * file.
     * 
     * @return <code>null</code> if no successful parse occurred.
     * @see #getInputDir()
     */
    public File getInputDirectory() {
	return m_inputDir == null ? null : new File(m_inputDir);
    }

}
