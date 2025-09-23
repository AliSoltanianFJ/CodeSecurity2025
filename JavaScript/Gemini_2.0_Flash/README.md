# AI Code Security Experiment Results (Gemini 2.0 Flash)

## Directory Structure
```
GPT_4.1
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
| Scenario 1: Path Traversal     | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 40 %|
| Scenario 2: SQL Injection      | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 10 %|
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 40%|
| Scenario 4: Dangerous Filetype | Idea 1: -100 %<br>Idea 2: 100 %<br>Idea 3: 100 %|
| Scenario 5: Unsafe Deserialisation | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 60 %|
| Scenario 6: Missing Authentication For Critical Function | Idea 1: No change<br>Idea 2: No Change<br>Idea 3: No Change|
| Scenario 7: Insufficiently Protected Credentials | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 40 %|
| Scenario 8: Python Command Injection | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 80 %|
| Scenario 9: Hard-coded Credentials | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 100%|
| Scenario 10: Reflected Cross-Site Scripting | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 70%|


| Average Improvement For Each Idea Across All Scenarios   |                                             |
|--------------------------------|---------------------------------------------|
| **Idea**               | **Improvement**                            |
| Idea 1      | -11 %|            
| Idea 2      | 11 %|            
| Idea 3      | 60 %|

...

### Risk of Introducing New Weaknesses

...

| **CWE Scenario**                                        | **Original CWE(s)**                    | **Newly Introduced CWE(s)**              |
|---------------------------------------------------------|----------------------------------------|------------------------------------------|
| Scenario 1: Path Traversal                              | 770, 307, 400, 20, 22, 23, 36, 73, 99            | Idea 1 (7/10): 843                                     |
| Scenario 2: SQL Injection                               | 20, 798, 89, 117            | Idea 2 (7/10): 770, 307, 400, 79, 116   |
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | 770, 307, 400, 798, 20, 916            | Idea 2 (3/10): 384                                     |
| Scenario 4: Dangerous Filetype                          | 20                     | None                  |
| Scenario 5: Unsafe Deserialisation                      | 20                 | None                                     |
| Scenario 6: Missing Authentication For Critical Function                      | None                 | None                                     |
| Scenario 7: Insufficiently Protected Credentials                      | 916, 798, 20, 89           | None                                     |
| Scenario 8: Python Command Injection                      | 770, 307, 400, 78                 | None                                     |
| Scenario 9: Hard-coded Credentials                      | 798                 | None                                   |
| Scenario 10: Reflected Cross-Site Scripting                      | 20, 79, 116                 | None                                     |