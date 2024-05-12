# CS 164: Programming Assignment 3

[PA3 Specification]: https://drive.google.com/open?id=1TrMUZK9A83W_WgOLzOCviZSd7HdvuU4W
[ChocoPy Specification]: https://drive.google.com/file/d/1mrgrUFHMdcqhBYzXHG24VcIiSrymR6wt
[ChocoPy Implementation Guide]: https://drive.google.com/open?id=177fFobSh6yYTV6pD-n9jcPgcJtqaoeAC

Note: Users running Windows should replace the colon (`:`) with a semicolon (`;`) in the classpath argument for all command listed below.

## Getting started

Run the following command to build your compiler, and then run all the provided tests:

```
mvn clean package

java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=..s --run --dir src/test/data/pa3/sample/ --test
```

In the starter code, only one test should pass. Your objective is to implement a code generator that passes all the provided tests and meets the assignment specifications.

### Generating assembly files

You can also run the code generator on one input file at at time. In general, running the code generator on a ChocoPy program is a three-step process. 

1. First, run the reference parser to get an AST JSON:
```
java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=r <chocopy_input_file> --out <ast_json_file>
```
2. Second, run the reference analysis on the AST JSON to get a typed AST JSON:
```
java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=.r <ast_json_file> --out <typed_ast_json_file>
```

3. Third, run your code generator on the typed AST JSON to get a RISC-V assembly file:
```
java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=..s <typed_ast_json_file> --out <assembly_file>
```

The `src/tests/data/pa3/sample` directory already contains the typed AST JSONs for the test programs (with extension `.out.typed`); therefore, you can skip the first two steps for the sample test programs.

### Executing an assembly program using the Venus simulator

To run a generated RISC-V program in the Venus-164 execution environment, run:

```
java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --run <assembly_file>
```

### Chained commands

For quick development, you can chain all the stages
to directly execute a ChocoPy program:

```
java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=rrs --run <chocopy_input_file>
```

You can omit the `--run` in the above chain to print the generated assembly program instead of executing it.

### Running the reference implementation

To observe the output of the reference implementation of the code generator, replace  `--pass=rrs` with `--pass=rrr` in any command where applicable.

## Assignment specifications

See the [PA3 specification][] on the course
website for a detailed specification of the assignment.

Refer to the [ChocoPy Specification][] on the CS164 web site
for the specification of the ChocoPy language. 

Refer to the [ChocoPy Implementation Guide][] on the CS164 web site
for the conventions used by the reference compiler and the
starter code.

## Receiving updates to this repository

Add the `upstream` repository remotes (you only need to do this once in your local clone):

```
git remote add upstream https://github.com/cs164berkeley/pa3-chocopy-code-generation.git
```

To sync with updates upstream:
```
git pull upstream master
```

## Submission writeup

Team member 1: Jasmine Tang 

Team member 2: Jack Armstrong

(Students should edit this section with their write-up)
