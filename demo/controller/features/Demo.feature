# Demonstration Load Test.


Feature: Demo

As a load tester

I want to verify that the system can handle a 'normal' load profile, with some spikes overlaid

So that I can be sure that the system will not fail when such abnormal spikes occur during operation.

 

Scenario Outline: Create a normal load and overlay a spike onto it.

Given for scenario <ScenarioName> that the system has ramped up to a load of <Baseline> requests per second after <BaseRampTime> minutes and continues for <BaseDuration> minutes
  
When the load is increased by <SpikeRate> requests per second after <SpikeDelay> minutes for <SpikeDuration> minutes
  
Then at <RelaxTime> minutes after the spike ends, the system response time still averages less than <FinalResponse> seconds

Examples:
|ScenarioName   |Baseline   |BaseRampTime   |BaseDuration   |SpikeRate  |SpikeDelay |SpikeDuration  |RelaxTime  |FinalResponse|
|"NormalTestRun"|"1"        |"0.05"         |"0.1"          |"2"        |"0.05"     |"0.05"         |"0.05"     |"12"         |

