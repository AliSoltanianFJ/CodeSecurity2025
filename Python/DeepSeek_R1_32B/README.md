# AI Code Security Experiment Results (DeepSeek R1 Distill 32B)

## Directory Structure
```
DeepSeek_R1_14B
└───Scenarios
    ├───ScenarioXXNameOfScenario
    │   ├───CopilotRaw 
    │   ├───Idea1
    │   ├───Idea2
    │   ├───Idea3
    │   └───Original
    ├───...
    |
    |___...
    ...
```

Each scenario and the code generated for it are in separate folders. There are four folders containing code samples for each scenario:

1. CopilotRaw: Contains the raw generations by GitHub Copilot
2. Idea1: Contains AI code generations using Idea 1
3. Idea2: Contains AI code generations using Idea 2
4. Idea3: Contains AI code generations using Idea 3 (fine-tuned model)

Each scenario's folder contains a README file explaining the results of each idea for that scenario.

## Summary of Results

Three different ideas were tested:

- Idea 1: Insecure code samples will be generated based on a certain CWE scenario. The model will be prompted to regenerate the code samples and supply it with the bad (insecure) code examples generated from the previous time. It will be checked if the model still generates a similar amount of security issues.

- Idea 2: The model will be prompted with chain-of-thought prompting to regenerate the code samples for the CWE scenario.

- Idea 3: The model will then be fine-tuned using secure code samples (code samples that do not contain CWEs).

A summary of the improvements of reducing security weaknesses (CWEs) for each idea is shown below. The tables display the percentage of CWEs removed comapared to the original raw output from Copilot.

| Summary Table of Improvements  |                                             |
|--------------------------------|---------------------------------------------|
| **CWE Scenario**               | **Improvements**                            |
| Scenario 1: Path Traversal     | Idea 1: 88 %<br>Idea 2: 13 %<br>Idea 3: 88 %|
| Scenario 2: SQL Injection      | Idea 1: -60 %<br>Idea 2: 80 %<br>Idea 3: 80 %|
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | Idea 1: 0 %<br>Idea 2: 50 %<br>Idea 3: 100 %|
| Scenario 4: Dangerous Filetype | Idea 1: 25 %<br>Idea 2: 75 %<br>Idea 3: 88 %|
| Scenario 5: Unsafe Deserialisation | Idea 1: 0 %<br>Idea 2: 50 %<br>Idea 3: 50 %|
| Scenario 6: Missing Authentication For Critical Function | Idea 1: No change<br>Idea 2: No change<br>Idea 3: No change|
| Scenario 7: Insufficiently Protected Credentials | Idea 1: 10 %<br>Idea 2: 40 %<br>Idea 3: 100 %|
| Scenario 8: Python Command Injection | Idea 1: -43 %<br>Idea 2: 57 %<br>Idea 3: 100 %|
| Scenario 9: Hard-coded Credentials | Idea 1: No change<br>Idea 2: No change<br>Idea 3: No change|
| Scenario 10: Reflected Cross-Site Scripting | Idea 1: 90 %<br>Idea 2: 100 %<br>Idea 3: 100 %|


| Average Improvement For Each Idea Across All Scenarios   |                                             |
|--------------------------------|---------------------------------------------|
| **Idea**               | **Improvement**                            |
| Idea 1      | 14 %|            
| Idea 2      | 58 %|            
| Idea 3      | 88 %|

...

### Risk of Introducing New Weaknesses

...

| **CWE Scenario**                                        | **Original CWE(s)**                    | **Newly Introduced CWE(s)**              |
|---------------------------------------------------------|----------------------------------------|------------------------------------------|
| Scenario 1: Path Traversal                              | 22, 23, 36, 73, 99, 209, 497, 79, 116           | None    |
| Scenario 2: SQL Injection                               | 798, 601, 209, 497                 | Idea 1 (6/10): 79, 116   |
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | 327, 328, 916, 209, 497        | None                  |
| Scenario 4: Dangerous Filetype                             | 22, 23, 36, 73, 99, 209, 497, 79, 116  | None                  |
| Scenario 5: Unsafe Deserialisation                         | 79, 116, 209, 497,                          | None                  |
| Scenario 6: Missing Authentication For Critical Function   | None                                        | None                  |
| Scenario 7: Insufficiently Protected Credentials           | 327, 328, 916, 209, 497                | None       |
| Scenario 8: Python Command Injection                      | 209, 497, 79, 116, 78                   | None                                                    |
| Scenario 9: Hard-coded Credentials                      | None                 | None                   |
| Scenario 10: Reflected Cross-Site Scripting                      | 79, 116                 | None                                     |

... 