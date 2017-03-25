# bddloadgen

Table Of Contents
What the Load Generator Is For
What Are the Distinguishing Features?
Usage
Identify JBehave tests
Create performance .feature file
Implement performance “control” file
Implement control file
Run control file
Retrieve results


What the Load Generator Is For
There are two uses:
Performance testing.
Distributing functional tests across multiple test nodes (test machines or VMs) in order to execute them more rapidly.

What Are the Distinguishing Features?
Refer to the figure below.

Figure: Overall setup.


Performance test scenarios are defined in Gherkin. As such, performance test scenarios are fully scriptable. There is no “user interface”. A performance test scenario is executed by invoking it under JBehave.
The performance test scenario specifies how many Test Nodes (VMs) are needed and those are created and provisioned automatically.
The actual tests that get run against the system under test are labeled “User Tests” in the figure. The User Tests are also implemented with JBehave. The scripts/code for these tests is moved to each Test Node when the Node is provisioned (automatically). Each time a user test is executed it is run under a separate Java class loader, for full isolation.


Usage Procedure
Install the loadgen-java.jar in an HTTP repository.
Identify the JBehave tests (.feature files and step classes) that you wish to run, to generate load.
Create a performance “control” (.feature) file, written in Gherkin.
Implement the control file, in Java.
Run the control file, using JBehave.
Retrieve results

Each of these steps is explained below.

Install LoadGenerator


Identify JBehave tests


Instrument the tests

Add timelogBeginTest and timelogEndTest to each test
The TestRunner needs to know when a test has actually begun. It cannot know this on its own, because tests are invoked by JBehave, and invoking a test actually takes a significant amount of time; so in order to be as accurate as possible, we must call a method at the beginning of each test, to notify the TestRunner that a test has begun. This method is the timelogBeginTest method, in the utility class loadgen.TestRunnerUtil:

    @Given ("^that we have a headless browser available$")
public void do_given()
{
TestRunnerUtil.timelogBeginTest();  // beginning of test
…
}

Similarly, we must notify the TestRunner when a test has completed. This is done with the timelogEndTest method.

    @Then ("^a successful response is returned$")
public void do_then()
{
    ...
TestRunnerUtil.timelogEndTest();  // end of test
        }

If timelogEndTest is never called due to some kind of exceptional condition, the TestRunner will merely use the time at which the invocation of JBehave ends for that test.

Define timers, if desired
The TestRunner can not only record the time to perform a test: it can record more granular time intervals that you define. For example, suppose you want to record the times that tests take to perform a certain section of code. To do this, you can define a timer called, say, “MyTimer”, and add timer begin and end calls like this:

@When ("^a headless client requests$")
public void do_when()
{
    ...
TestRunnerUtil.timelogBegin("MyTimer");
...
TestRunnerUtil.timelogEnd("MyTimer");
...
}

In this case, the TestRunner will record the time between these calls, for each test, and LoadGenerator will compute statistics for these times.

To add these times to the graph produced by the LoadGenerator, simply add a line as follows to the test scenario step (Java file),

myPerformanceRun.graphEvent("MyTimer");

This will cause the time interval between timeLogBegin("MyTimer") and timeLogEnd("MyTimer") to be recorded as a data item, for each test, and these data items will be graphed over time. (See the Javadocs for loadgen.controller.PerformanceRun.graphEvent)
Create performance .feature file

Implement performance “control” file


Define a performance test run

LoadGenerator.performanceRun("spike", (perfRun) -> {
perfRun.setFeaturesDirectory(
perfRun.setStepsJarPath(
perfRun.stepClass(
perfRun.useProfile(
});



Implement control file

Run control file

Retrieve results


