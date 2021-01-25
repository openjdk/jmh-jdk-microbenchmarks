# Code Tools : JMH JDK Microbenchmarks

The JMH JDK Microbenchmarks is a collection of microbenchmarks for measuring
the performance of the JDK API and JVM features using
the [JMH](http://openjdk.java.net/projects/code-tools/jmh/) framework. 


## Building and running the project

Currently, the project can be built and run with JDK 8 and later. This is
a Maven project and is built by:

    $ mvn clean install

After building, the executable jar is target/jmh-jdk-microbenchmarks-[version].jar.
Run the benchmarks with:

    $ java -jar target/jmh-jdk-microbenchmarks-*.jar [optional jmh parameters]

See the entire list of benchmarks using:

    $ java -jar target/jmh-jdk-microbenchmarks-*.jar -l [optional regex to select benchmarks]

For example:

    $ java -jar target/jmh-jdk-microbenchmarks-1.0-SNAPSHOT.jar -l .*bulk_par_lambda.*
    Benchmarks: 
    org.openjdk.bench.java.util.stream.tasks.DictionaryWordValue.Lambda.bulk_par_lambda
    org.openjdk.bench.java.util.stream.tasks.IntegerMax.Lambda.bulk_par_lambda
    org.openjdk.bench.java.util.stream.tasks.IntegerSum.Lambda.bulk_par_lambda
    org.openjdk.bench.java.util.stream.tasks.PrimesFilter.t100.Lambda.bulk_par_lambda
    org.openjdk.bench.java.util.stream.tasks.PrimesFilter.t10000.Lambda.bulk_par_lambda

And the same regex syntax works to run the same set:

    $ java -jar target/jmh-jdk-microbenchmarks-1.0-SNAPSHOT.jar .*bulk_par_lambda.*

## Troubleshooting

### Build of micros-javac module got stuck

If you build got stuck on `[get] Getting: https://download.java.net/openjdk/jdk11/ri/openjdk-11+28_windows-x64_bin.zip` then you are probably experiencing some networking or web proxy obstacles. 

Solution is to download required reference JDK from [https://download.java.net/openjdk/jdk11/ri/openjdk-11+28_windows-x64_bin.zip](https://download.java.net/openjdk/jdk11/ri/openjdk-11+28_windows-x64_bin.zip) manually and then build the project with property pointing to the local copy:

    $ mvn clean install -Djavac.benchmark.openjdk.zip.download.url=file:///<your download location>/openjdk-11+28_windows-x64_bin.zip

Note: Please use `openjdk-11+28_windows-x64_bin.zip` to build the project no matter what target platform is.

### Execution of micros-javac benchmarks fail with java.lang.IllegalAccessError

If you experience following exception during benchmarks execution:

	java.lang.IllegalAccessError: superclass access check failed: class
	org.openjdk.bench.langtools.javac.JavacBenchmark$2 (in unnamed module) cannot access class
	com.sun.tools.javac.main.JavaCompiler

It is caused by recently enabled Jigsaw enforcement and micros-javac benchmarks requirement to access several jdk.compiler module private packages. 

Solution is to export required packages by adding following options to the command line:

	$ java --add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
	--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
	--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
	--add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED \
	--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
	-jar target/jmh-jdk-microbenchmarks-1.0-SNAPSHOT.jar [optional jmh parameters]

### Execution of micros-javac benchmarks takes several hours

micros-javac benchmarks consist of two sets of benchmarks: 
 * `SingleJavacBenchmark` (which is parametrized) measures each single javac compilation stage in an isolated run. This benchmark is designed for exact automated performance regression testing and it takes several ours to execute completely. 
 * `GroupJavacBenchmark` is grouping the measurements of all javac compilation stages into one run and its execution should take less than 30 minutes on a regular developers computer.

Solution to speed up javac benchmarking is to select only `GroupJavacBenchmark` for execution using following command line:

	  $ java -jar target/jmh-jdk-microbenchmarks-1.0-SNAPSHOT.jar .*GroupJavacBenchmark.*
