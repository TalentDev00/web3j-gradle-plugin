/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.IdentityFileResolver;
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory;
import org.gradle.api.internal.plugins.PluginApplicationException;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceTask;

import org.web3j.solidity.gradle.plugin.SolidityCompile;
import org.web3j.solidity.gradle.plugin.SolidityPlugin;
import org.web3j.solidity.gradle.plugin.SoliditySourceSet;

import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize;

/** Gradle plugin class for web3j code generation from Solidity contracts. */
public class Web3jPlugin implements Plugin<Project> {

    static final String ID = "org.web3j";

    public void apply(final Project target) {
        target.getPluginManager().apply(JavaPlugin.class);
        target.getPluginManager().apply(SolidityPlugin.class);
        target.getDependencies().add("implementation", "org.web3j:core:" + getProjectVersion());
        registerExtensions(target);

        final SourceSetContainer sourceSets =
                target.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();

        target.afterEvaluate(p -> sourceSets.all(sourceSet -> configure(target, sourceSet)));
    }

    protected void registerExtensions(Project project) {
        project.getExtensions().create(Web3jExtension.NAME, Web3jExtension.class, project);
    }

    private String getProjectVersion() {
        final URL versionPropsFile = getClass().getClassLoader().getResource("version.properties");

        if (versionPropsFile == null) {
            throw new PluginApplicationException(
                    "No version.properties file found in the classpath.", null);
        } else {
            try {
                final Properties versionProps = new Properties();
                versionProps.load(versionPropsFile.openStream());
                return versionProps.getProperty("version");
            } catch (IOException e) {
                throw new PluginApplicationException("Could not read version.properties file.", e);
            }
        }
    }

    /**
     * Configures code generation tasks for the Solidity source sets defined in the project (e.g.
     * main, test).
     *
     * <p>The generated task name for the <code>main</code> source set will be <code>
     * generateContractWrappers</code>, and for <code>test</code> <code>generateTestContractWrappers
     * </code>.
     */
    private void configure(final Project project, final SourceSet sourceSet) {

        final Web3jExtension extension =
                (Web3jExtension) InvokerHelper.getProperty(project, Web3jExtension.NAME);

        final File outputDir = buildSourceDir(extension, sourceSet);

        // Add source set to the project Java source sets
        sourceSet.getJava().srcDir(outputDir);

        final String srcSetName =
                sourceSet.getName().equals("main")
                        ? ""
                        : capitalize((CharSequence) sourceSet.getName());

        final String generateTaskName = "generate" + srcSetName + "ContractWrappers";

        final GenerateContractWrappers task =
                project.getTasks().create(generateTaskName, GenerateContractWrappers.class);

        // Set the sources for the generation task
        task.setSource(buildSourceDirectorySet(sourceSet));
        task.setDescription(
                "Generates " + sourceSet.getName() + " Java contract wrappers from Solidity ABIs.");

        // Set the task output directory
        task.getOutputs().dir(outputDir);

        // Set the task generated package name, classpath and group
        task.setGeneratedJavaPackageName(extension.getGeneratedPackageName());
        task.setUseNativeJavaTypes(extension.getUseNativeJavaTypes());
        task.setGroup(Web3jExtension.NAME);

        // Set task excluded contracts
        task.setExcludedContracts(extension.getExcludedContracts());
        task.setIncludedContracts(extension.getIncludedContracts());

        // Set the contract addresses length (default 160)
        task.setAddressLength(extension.getAddressBitLength());

        task.dependsOn(
                project.getTasks()
                        .withType(SolidityCompile.class)
                        .named("compile" + srcSetName + "Solidity"));

        final SourceTask compileJava =
                (SourceTask) project.getTasks().getByName("compile" + srcSetName + "Java");

        compileJava.source(task.getOutputs().getFiles().getSingleFile());
        compileJava.dependsOn(task);
    }

    private SourceDirectorySet buildSourceDirectorySet(final SourceSet sourceSet) {

        final String displayName = capitalize((CharSequence) sourceSet.getName()) + " Solidity ABI";

        final SourceDirectorySet directorySet =
                new DefaultSourceDirectorySet(
                        sourceSet.getName(),
                        displayName,
                        new IdentityFileResolver(),
                        new DefaultDirectoryFileTreeFactory());

        directorySet.srcDir(buildOutputDir(sourceSet));
        directorySet.include("**/*.abi");

        return directorySet;
    }

    private File buildSourceDir(final Web3jExtension extension, final SourceSet sourceSet) {

        if (extension.getGeneratedFilesBaseDir().isEmpty()) {
            throw new InvalidUserDataException("Generated web3j package cannot be empty");
        }

        return new File(extension.getGeneratedFilesBaseDir() + "/" + sourceSet.getName() + "/java");
    }

    protected File buildOutputDir(final SourceSet sourceSet) {
        final Convention convention =
                (Convention) InvokerHelper.getProperty(sourceSet, "convention");

        final SoliditySourceSet soliditySourceSet =
                (SoliditySourceSet) convention.getPlugins().get(SoliditySourceSet.NAME);

        return soliditySourceSet.getSolidity().getOutputDir();
    }
}
