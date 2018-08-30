package org.web3j.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Web3jPluginTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    private File buildFile;
    private File sourceDir;

    @Before
    public void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle");

        final URL resource = getClass().getClassLoader()
                .getResource("solidity/StandardToken.sol");

        sourceDir = new File(resource.getFile()).getParentFile();

        final File settingsFile = testProjectDir.newFile("settings.gradle");
        final String settingsFileContent = "pluginManagement {\n" +
                "    repositories {\n" +
                "        mavenCentral()\n" +
                "        mavenLocal()\n" +
                "    }\n" +
                "}\n";

        Files.write(settingsFile.toPath(), settingsFileContent.getBytes());
    }

    @Test
    public void generateContractWrappers() throws IOException {

        final String buildFileContent = "plugins {\n" +
                // FIXME Plugin version shouldn't be specified here
                "   id 'web3j' version '0.1.0'\n" +
                "}\n" +
                "web3j {\n" +
                "    generatedPackageName = 'org.web3j.test'\n" +
                "}\n" +
                "sourceSets {\n" +
                "   main {\n" +
                "       solidity {\n" +
                "           srcDir {" +
                "               '" + sourceDir.getAbsolutePath() + "'\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}\n" +
                "repositories {\n" +
                "   mavenCentral()\n" +
                "   mavenLocal()\n" +
                "}\n";

        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        final GradleRunner generateMainJava = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("generateContractWrappers")
                .forwardOutput()
                .withDebug(true);

        final BuildResult success = generateMainJava.build();
        assertEquals(SUCCESS, success.task(":generateContractWrappers").getOutcome());

        final File web3jContractsDir = new File(testProjectDir.getRoot(),
                "build/generated/source/web3j/main/java");

        final File generatedContract = new File(web3jContractsDir,
                "org/web3j/test/StandardToken.java");

        assertTrue(generatedContract.exists());

        final BuildResult upToDate = generateMainJava.build();
        assertEquals(UP_TO_DATE, upToDate.task(":generateContractWrappers").getOutcome());
    }

}
