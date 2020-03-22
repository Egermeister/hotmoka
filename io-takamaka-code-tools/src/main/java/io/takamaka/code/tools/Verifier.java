package io.takamaka.code.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerifiedJar;

/**
 * A tool that parses and checks a jar. It performs the same verification that
 * Takamaka performs when a jar is added to blockchain.
 * 
 * Use it for instance like this:
 * 
 * java io.takamaka.code.tools.Verifier -app test_contracts_dependency.jar -lib takamaka_base.jar
 * 
 * The -lib are the dependencies that should already be in blockchain.
 */
public class Verifier {

	public static void main(String[] args) throws IOException {
		Options options = createOptions();
		CommandLineParser parser = new DefaultParser();

	    try {
	    	CommandLine line = parser.parse(options, args);
	    	String[] appJarNames = line.getOptionValues("app");
	    	String[] libJarNames = line.getOptionValues("lib");
	    	boolean duringInitialization = line.hasOption("init");

	    	for (String appJarName: appJarNames) {
		    	Path origin = Paths.get(appJarName);
		    	byte[] bytesOfOrigin = Files.readAllBytes(origin);

		    	List<byte[]> jars = new ArrayList<>();
		    	List<String> jarNames = new ArrayList<>();
		    	jars.add(bytesOfOrigin);
		    	jarNames.add(appJarName);
		    	if (libJarNames != null)
		    		for (String lib: libJarNames) {
		    			jars.add(Files.readAllBytes(Paths.get(lib)));
		    			jarNames.add(lib);
		    		}

		    	TakamakaClassLoader classLoader = TakamakaClassLoader.of(jars.stream(), jarNames.stream());
		    	VerifiedJar verifiedJar = VerifiedJar.of(bytesOfOrigin, classLoader, duringInitialization);
		    	verifiedJar.issues().forEach(System.err::println);
		    	if (verifiedJar.hasErrors())
		    		System.err.println("Verification failed because of errors");
		    	else
		    		System.out.println("Verification succeeded");
		    }
	    }
	    catch (ParseException e) {
	    	System.err.println("Syntax error: " + e.getMessage());
	    	new HelpFormatter().printHelp("java " + Verifier.class.getName(), options);
	    }
	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption(Option.builder("app").desc("verify the given application jars").hasArgs().argName("JARS").required().build());
		options.addOption(Option.builder("lib").desc("use the given library jars").hasArgs().argName("JARS").build());
		options.addOption(Option.builder("init").desc("verify as during blockchain initialization").build());

		return options;
	}
}