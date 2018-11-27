/*
 * Copyright 2014-2015 Mark Prins, GeoDienstenCentrum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.geodienstencentrum.maven.plugin.sass.report;

import com.google.common.primitives.Ints;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import nl.geodienstencentrum.maven.plugin.sass.AbstractSassMojo;
import nl.geodienstencentrum.maven.plugin.sass.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * SCSSLintMojo executes scss-lint goal.
 *
 * @author mprins
 * @since 2.3
 */
@Mojo(name = "scss-lint",
		defaultPhase = LifecyclePhase.COMPILE,
		executionStrategy = "once-per-session",
		threadSafe = true
	)
public class SCSSLintMojo extends AbstractSassMojo {

	/**
	 * Output file for the plugin.
	 *
	 * @since 2.3
	 */
	@Parameter(defaultValue = "${project.build.directory}/scss-lint.xml", readonly = true)
	private File outputFile;

	/**
	 * scss-lint exit codes and messages.
	 */
	public enum ExitCode {
		/** "No lints were found" exitcode. */
		CODE_0(0, "No lints were found"),
		/** "One or more warnings were reported" exitcode. */
		CODE_1(1, "Lints with a severity of 'warning' were reported (no errors)"),
		/** "One or more errors were reported" exitcode. */
		CODE_2(2, "One or more errors were reported (and any number of warnings)"),
		/** "Command line usage error" exitcode. */
		CODE_64(64, "Command line usage error (invalid flag, etc.)"),
		/** "" exitcode. */
		CODE_66(66, "Input file did not exist or was not readable"),
		/** "Input file did not exist or was not readable" exitcode. */
		CODE_70(70, "Internal software error"),
		/** "Internal software error" exitcode. */
		CODE_78(78, "Configuration error");
		/** "Configuration error" exitcode. */

		private static final HashMap<Integer, ExitCode> LOOKUP = new HashMap<>();

		static {
			for (final ExitCode c : EnumSet.allOf(ExitCode.class)) {
				LOOKUP.put(c.code, c);
			}
		}

		private final String msg;
		private final int code;

		ExitCode(final int code, final String msg) {
			this.code = code;
			this.msg = msg;
		}

		String msg() {
			return msg;
		}

		int code() {
			return code;
		}

		static ExitCode getExitCode(final int code) {
			return LOOKUP.get(code);
		}

		/**
		 * Returns a string representation of this {@code ExitCode}.
		 *
		 * @return code and message concatanated
		 */
		@Override
		public String toString() {
			return code + ": " + msg;
		}
	};

	/**
	 * Execute the lint script.
	 *
	 * @see org.apache.maven.plugin.Mojo#execute()
	 * @throws MojoExecutionException when the execution of the plugin
	 *         errored
	 * @throws MojoFailureException when the Sass compilation fails
	 *
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.isSkip()) {
			return;
		}
		final Log log = this.getLog();

		Set<String> sourceDirs = this.getSourceDirs();
		if (sourceDirs.isEmpty()) {
			return;
		}
		// create directory
		this.outputFile.getParentFile().mkdirs();

		log.info("Linting Sass sources in: " + this.getSassSourceDirectory());

		final StringBuilder sassScript = new StringBuilder();
		this.buildBasicSassScript(sassScript);

		log.debug("scss-lint ruby script:\n" + sassScript);

		System.setProperty("org.jruby.embed.localcontext.scope", "threadsafe");
		final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		final ScriptEngine jruby = scriptEngineManager.getEngineByName("jruby");
		ScriptContext context = jruby.getContext();

		ArrayList<String> argv = new ArrayList<>();
		argv.add("--format=Checkstyle");
		argv.add("--no-color");
		argv.add("-o" + this.outputFile);
		argv.addAll(sourceDirs);
		context.setAttribute(ScriptEngine.ARGV,
				argv.toArray(new String[argv.size()]),
				ScriptContext.GLOBAL_SCOPE);
		try {
			log.info("Reporting scss lint in: " + this.outputFile.getAbsolutePath());
			ExitCode result = ExitCode.getExitCode(
			        Ints.checkedCast((Long) jruby.eval(sassScript.toString(),
			                             context)));
			log.debug("scss-lint result: " + result.toString());
			switch (result) {
				case CODE_0:
					log.info(result.msg());
					break;
				case CODE_1:
					log.warn(result.msg());
					break;
				case CODE_2:
					log.error(result.toString());
					if (this.failOnError) {
						throw new MojoFailureException(result.toString());
					}
					break;
				// CHECKSTYLE:OFF:FallThrough
				case CODE_64:
				// fall through
				case CODE_66:
				// fall through
				case CODE_70:
				// fall through
				case CODE_78:
				// fall through
				default:
					log.error(result.toString());
					throw new MojoExecutionException(result.toString());
				// CHECKSTYLE:ON
			}
		} catch (final ScriptException e) {
			throw new MojoExecutionException(
					"Failed to execute scss-lint Ruby script:\n" + sassScript, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void buildBasicSassScript(final StringBuilder sassScript)
			throws MojoExecutionException {
		final Log log = this.getLog();
		// build up script
		sassScript.append("require 'scss_lint'\n");
		sassScript.append("require 'scss_lint/cli'\n");
		sassScript.append("require 'scss_lint_reporter_checkstyle'\n");

		if (log.isDebugEnabled()) {
			// make ruby give use some debugging info when requested
			sassScript.append("require 'pp'\n");
			sassScript.append("puts 'parameters: '\n");
			sassScript.append("pp ARGV\n");
		}
		sassScript.append("logger = SCSSLint::Logger.new(STDOUT)\n");
		sassScript.append("SCSSLint::CLI.new(logger).run(ARGV)\n");
	}

	/**
	 * Get the names of the sources.
	 * @return a set of String
	 */
	private Set<String> getSourceDirs() {
		Set<String> dirs = new HashSet<>();
		final List<Resource> resourceList = this.getResources();
		if (resourceList.isEmpty()) {
			dirs.add(getSassSourceDirectory().getPath());
		}
		for (final Resource source : resourceList) {
			dirs.addAll(source.getDirectoriesAndDestinations(this.getLog()).keySet());
		}
		return dirs;
	}
}
