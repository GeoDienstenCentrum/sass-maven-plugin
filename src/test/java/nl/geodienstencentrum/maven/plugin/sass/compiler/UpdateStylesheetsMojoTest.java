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
package nl.geodienstencentrum.maven.plugin.sass.compiler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;

import org.junit.Rule;
import org.junit.Test;

/**
 * Testcase for
 * {@link nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo }
 * .
 *
 * @author Mark C. Prins
 */
public class UpdateStylesheetsMojoTest {

	/**
	 * Test resources.
	 */
	@Rule
	public TestResources resources = new TestResources();

	/**
	 * test rule.
	 */
	@Rule
	public MojoRule rule = new MojoRule();

	/**
	 * Test method for
	 * {@link nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute() }
	 * .
	 *
	 * @throws Exception if any
	 * @see
	 * nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute()
	 */
	@Test
	public void testExecute() throws Exception {
		final File projectCopy = this.resources
                .getBasedir("maven-sass-test");
		final File pom = new File(projectCopy, "pom.xml");
		assumeNotNull("POM file should not be null.", pom);
		assumeTrue("POM file should exist as file.",
				pom.exists() && pom.isFile());

		final UpdateStylesheetsMojo myMojo = (UpdateStylesheetsMojo) this.rule
				.lookupConfiguredMojo(projectCopy, "update-stylesheets");
		assumeNotNull(myMojo);

		// test if execution was succesful, if not fail
		myMojo.execute();
		TestResources.assertDirectoryContents(
				new File(projectCopy.getAbsolutePath()
                        + "/target/maven-sass-test-1.0-SNAPSHOT/css/"),
            				"compiled.css.map", "compiled.css");
		// this may fail when line endings differ, eg. on Windows
		// set up git to check out with native file endings
		TestResources.assertFileContents(projectCopy, "expected.css",
                "target/maven-sass-test-1.0-SNAPSHOT/css/compiled.css");
	}

	/**
	 * Test method for
	 * {@link nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute() }
	 * . This tests a more complete example that produces two stylesheets.
	 *
	 * @throws Exception if any
	 * @see
	 * nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute()
	 */
	@Test
    public void testExecute_complete_test() throws Exception {
		final File projectCopy = this.resources.getBasedir("complete-test");
		final File pom = new File(projectCopy, "pom.xml");
		assertNotNull("POM file should not be null.", pom);
		assertTrue("POM file should exist as file.",
				pom.exists() && pom.isFile());

		final UpdateStylesheetsMojo myMojo = (UpdateStylesheetsMojo) this.rule
				.lookupConfiguredMojo(projectCopy, "update-stylesheets");
		assertNotNull(myMojo);

		// test if execution was succesful, if not fail
		myMojo.execute();
		TestResources.assertDirectoryContents(
				new File(projectCopy.getAbsolutePath() + "/target/css/"),
				"compiled.css.map", "compiled.css", "print.css.map",
				"print.css");

		// this may fail when line endings differ, eg. on Windows
		// set up git to check out with native file endings
		TestResources.assertFileContents(projectCopy, "expected_compiled.css",
				"target/css/compiled.css");

		TestResources.assertFileContents(projectCopy, "expected_print.css",
				"target/css/print.css");
	}

	/**
	 * Test method for
	 * {@link nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute() }
	 * on a misconfigured project.
	 *
	 * @throws Exception if any
	 * @see
	 * nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute()
	 */
	@Test(expected = MojoFailureException.class)
	public void testFailExecute() throws Exception {
		final File projectCopy = this.resources
				.getBasedir("maven-compass-misconfigured-test");
		final File pom = new File(projectCopy, "pom.xml");
		assertNotNull("POM file should not be null.", pom);
		assertTrue("POM file should exist as file.",
				pom.exists() && pom.isFile());

		final UpdateStylesheetsMojo myMojo = (UpdateStylesheetsMojo) this.rule
				.lookupConfiguredMojo(projectCopy, "update-stylesheets");
		assertNotNull(myMojo);

		myMojo.execute();
		fail("A MojoFailureException should have been thrown executing UpdateStylesheetsMojo.");
	}

	@Test
    public void testSassSyntaxFile() throws Exception {
        final File projectCopy = this.resources.getBasedir("maven-sass-syntax-test");
		final File pom = new File(projectCopy, "pom.xml");
		assertTrue("The POM file should exist as a file", pom.exists() && pom.isFile());

		final UpdateStylesheetsMojo mojo = (UpdateStylesheetsMojo) this.rule.lookupConfiguredMojo(projectCopy, "update-stylesheets");
		assertNotNull(mojo);

		mojo.execute();

		TestResources.assertFileContents(projectCopy, "expected_images.css", "target/css/images.css");
	}

	/**
	 * test for non-existance of sourcemaps.
	 *
	 * @throws Exception if any
	 */
	@Test
	public void testNoSourceMaps() throws Exception {
		final File projectCopy = this.resources.getBasedir("maven-sass-test-no-sourcemap");
		final File pom = new File(projectCopy, "pom.xml");
		assumeTrue("The POM file should exist as a file", pom.exists() && pom.isFile());

		final UpdateStylesheetsMojo mojo = (UpdateStylesheetsMojo) this.rule.lookupConfiguredMojo(projectCopy, "update-stylesheets");
		assumeNotNull(mojo);

		mojo.execute();
		TestResources.assertDirectoryContents(
				new File(projectCopy.getAbsolutePath() + "/target/css/"),
				"compiled.css",
				"print.css");

		// this may fail when line endings differ, eg. on Windows
		// set up git to check out with native file endings
		// TestResources.assertFileContents(projectCopy, "expected_compiled.css",
		//		"target/css/compiled.css");
		// TestResources.assertFileContents(projectCopy, "expected_print.css",
		//		"target/css/print.css");
	}

	/**
	 * Test method for
	 * {@link nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute() }
	 * .
	 *
	 * @throws Exception if any
	 * @see
	 * nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute()
	 */
	@Test
	public void testExecuteMavenCompassFaultyResources() throws Exception {
		final File projectCopy = this.resources
				.getBasedir("maven-compass-faulty-resources-test");
		final File pom = new File(projectCopy, "pom.xml");
		assumeNotNull("POM file should not be null.", pom);
		assumeTrue("POM file should exist as file.",
				pom.exists() && pom.isFile());

		final UpdateStylesheetsMojo myMojo = (UpdateStylesheetsMojo) this.rule
				.lookupConfiguredMojo(projectCopy, "update-stylesheets");
		assertNotNull(myMojo);

		// test if execution was succesful, if not fail
		myMojo.execute();
		TestResources.assertDirectoryContents(
				new File(projectCopy.getAbsolutePath()
						+ "/target/maven-compass-faulty-resources-test-1.0-SNAPSHOT/css/"),
				"compiled.css.map", "compiled.css");
		// this may fail when line endings differ, eg. on Windows
		// set up git to check out with native file endings
		TestResources.assertFileContents(projectCopy, "expected.css",
				"target/maven-compass-faulty-resources-test-1.0-SNAPSHOT/css/compiled.css");
	}

	/**
	 * Test method for
	 * {@link nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute() },
	 * test the skip config parameter.
	 *
	 * @throws Exception if any
	 * @see
	 * nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute()
	 */
	@Test
	public void testSkipExecutionProject() throws Exception {
		final File projectCopy = this.resources
				.getBasedir("skip-execution-project");
		final File pom = new File(projectCopy, "pom.xml");
		assumeNotNull("POM file should not be null.", pom);
		assumeTrue("POM file should exist as file.",
				pom.exists() && pom.isFile());

		final UpdateStylesheetsMojo myMojo = (UpdateStylesheetsMojo) this.rule
				.lookupConfiguredMojo(projectCopy, "update-stylesheets");
		assertNotNull(myMojo);

		myMojo.execute();
		// the target directory should not exist
		assertFalse(
				(new File(projectCopy.getAbsolutePath()
					+ "/target/skip-execution-project-1.0-SNAPSHOT/css/")).exists());
    }

    /**
     * Test method for
	 * {@link nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute() }
     * .
     *
     * @throws Exception if any
     * @see
     * nl.geodienstencentrum.maven.plugin.sass.compiler.UpdateStylesheetsMojo#execute()
     */
    @Test
    public void testCompile_maven_sass_umlaut_test() throws Exception {
        final File projectCopy = this.resources
                .getBasedir("maven-sass-umlaut-test");
        final File pom = new File(projectCopy, "pom.xml");
        assumeNotNull("POM file should not be null.", pom);
        assumeTrue("POM file should exist as file.",
                pom.exists() && pom.isFile());

        final UpdateStylesheetsMojo myMojo = (UpdateStylesheetsMojo) this.rule
                .lookupConfiguredMojo(projectCopy, "update-stylesheets");
        assumeNotNull(myMojo);

        // test if execution was succesful, if not fail
        myMojo.execute();
        TestResources.assertDirectoryContents(
                new File(projectCopy.getAbsolutePath()
                        + "/target/classes/META-INF/resources/css2/"),
                "compiled.css.map", "compiled.css");
		// this may fail when line endings differ, eg. on Windows
        // set up git to check out with native file endings
        TestResources.assertFileContents(projectCopy, "expected.css",
                "target/classes/META-INF/resources/css2/compiled.css");
    }
}
