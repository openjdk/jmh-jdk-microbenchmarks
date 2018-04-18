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
