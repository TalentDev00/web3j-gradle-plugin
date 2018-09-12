package org.web3j.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.web3j.codegen.SolidityFunctionWrapper;
import org.web3j.utils.Files;

public class GenerateContractWrappers extends SourceTask {

    @Input
    private String generatedJavaPackageName;

    @Input
    @Optional
    private Boolean useNativeJavaTypes;

    @Input
    @Optional
    private List<String> excludedContracts;

    @TaskAction
    @SuppressWarnings("unused")
    void generateContractWrappers() throws IOException, ClassNotFoundException {
        for (final File contractAbi : getSource()) {

            final String contractName = contractAbi.getName()
                    .replaceAll("\\.abi", "");

            if (excludedContracts == null || !excludedContracts.contains(contractName)) {
                final String packageName = MessageFormat.format(
                        getGeneratedJavaPackageName(), contractName.toLowerCase());

                final File contractBin = new File(contractAbi.getParentFile(), contractName + ".bin");
                final String outputDir = getOutputs().getFiles().getSingleFile().getAbsolutePath();

                final SolidityFunctionWrapper wrapper =
                        new SolidityFunctionWrapper(getUseNativeJavaTypes());

                wrapper.generateJavaFiles(contractName, Files.readString(contractBin),
                        Files.readString(contractAbi), outputDir, packageName);
            }
        }
    }

    // Getters and setters
    public String getGeneratedJavaPackageName() {
        return generatedJavaPackageName;
    }

    public void setGeneratedJavaPackageName(final String generatedJavaPackageName) {
        this.generatedJavaPackageName = generatedJavaPackageName;
    }

    public Boolean getUseNativeJavaTypes() {
        return useNativeJavaTypes;
    }

    public void setUseNativeJavaTypes(final Boolean useNativeJavaTypes) {
        this.useNativeJavaTypes = useNativeJavaTypes;
    }

    public List<String> getExcludedContracts() {
        return excludedContracts;
    }

    public void setExcludedContracts(final List<String> excludedContracts) {
        this.excludedContracts = excludedContracts;
    }

}
