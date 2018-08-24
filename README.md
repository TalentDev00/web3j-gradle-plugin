web3j Gradle Plugin
===================

Simple Gradle plugin for [web3j](https://web3j.io/). This plugin is under development and not stable.

## Plugin configuration

Before you start, you will need to install the 
[Solidity compiler](https://solidity.readthedocs.io/en/latest/installing-solidity.html)
if is not already installed in your computer.

### Using the `buildscript` convention

To install the web3j Plugin using the old Gradle `buildscript` convention, you should add 
the following to the first line of your build file (at the moment only release versions 
are supported in Gradle, not SNAPSHOT):

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'org.web3j:web3j-gradle-plugin:0.1.0.0'
    }
}
```

### Using the plugins DSL

Alternatively, if you are using the more modern plugins DSL, add the following line to your 
build file:

```groovy
plugins {
    id 'web3j-gradle-plugin' version '0.1.0.0'
}
```

You will need to add the following configuration in the first line of your `settings.gradle` 
file to resolve the artifact from the Epiphyte repository.

```groovy
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven { 
            url 'https://dl.bintray.com/ethereum/maven/' 
        }
    }
}
```

Then run this command from your project containing Solidity contracts:

```
./gradlew generateMainJava
``` 

The `generate[SourceSet]Java` task is the entry point of the web3j Plugin, 
and its execution will trigger the code generation tasks.

After applying the plugin, the base directory for generated code (by default 
`$buildDir/generated/source/web3j`) will contain a directory for each source set 
(by default `main` and `test`), and each of those a directory with the `java` code.

## Code generation

The `web3j` DSL allows to configure the generated code, e.g.:

```groovy
web3j {
    generatedPackageName = 'com.mycompany.{0}'
    generatedFilesBaseDir = "$buildDir/custom/destination"
}
```

The properties accepted by the DSL are listed in the following table: 

|  Name                   | Type       | Default value                      | Description |
|-------------------------|:----------:|:----------------------------------:|-------------|
| `generatedPackageName`  | `String`   | `${group}.web3j`                   | Generated web3j contracts package. |
| `generatedFilesBaseDir` | `String`   | `$buildDir/generated/source/web3j` | Generated code output directory. |

The `generatedPackageName` may contain a indexed value between curly brackets (`{0}`),
allowing to format the generated value using the contract name. For convenience,
when applied to a Java package name it will be converted to lower case. 

For instance, a `generatedPackageName` set to `${group}.{0}` in a project with group 
`com.mycompany`, a Solidity contract named `MyToken.sol` will be generated in the package
`com.mycompany.mytoken`. Note that this is not a Gradle property and should not be preceded by `$`.

Also, the default value contains the `${group}` property, which corresponds to your project artifact 
group (e.g. `com.mycompany`).

## Source sets

By default, all `.sol` files in `$projectDir/src/main/solidity` will be processed by the plugin.
To specify and add different source sets, use the `sourceSets` DSL:

```groovy
sourceSets {
    main {
        solidity {
            srcDir { 
                "my/custom/path/to/solidity" 
             }
        }
    }
}
```

Output directories for generated Java code will be added to your build automatically.

## Plugin tasks

The Cryptlet plugin relies on the [Java Plugin](https://docs.gradle.org/current/userguide/java_plugin.html),
which adds tasks to your project build using a naming convention on a per source set basis.

In the same way, the web3j plugin will add the `generate[Name]Java` task to your project build,
where `Name` corresponds to the capitalized source set name, by default `main` and `test`.

To obtain a list and description of all added tasks, run the command:

```
../gradlew tasks --all
```