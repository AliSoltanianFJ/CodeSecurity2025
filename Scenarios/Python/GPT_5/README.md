# AI Code Security Experiment Results (GPT-5)

## Directory Structure
```
GPT_5
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
| Scenario 1: Path Traversal     | Idea 1: 25%<br>Idea 2: 50%<br>Idea 3: xx<br>Idea 4: 100%|
| Scenario 2: SQL Injection      | Idea 1: 90%<br>Idea 2: 90%<br>Idea 3: xx<br>Idea 4: 90%|
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | Idea 1: 25%<br>Idea 2: 88%<br>Idea 3: xx<br>Idea 4: 100%|
| Scenario 4: Dangerous Filetype | Idea 1: 20%<br>Idea 2: 100%<br>Idea 3: xx<br>Idea 4: 100%|
| Scenario 5: Unsafe Deserialisation | Idea 1: 0%<br>Idea 2: 100%<br>Idea 3: xx<br>Idea 4: 100%|
| Scenario 6: Missing Authentication For Critical Function | Idea 1: 0%<br>Idea 2: 0%<br>Idea 3: xx<br>Idea 4: 0%|
| Scenario 7: Insufficiently Protected Credentials | Idea 1: 0%<br>Idea 2: 0%<br>Idea 3: xx<br>Idea 4: 100%|
| Scenario 8: Python Command Injection | Idea 1: 29%<br>Idea 2: 57%<br>Idea 3: xx<br>Idea 4: 43%|
| Scenario 9: Hard-coded Credentials | Idea 1: 13%<br>Idea 2: 100%<br>Idea 3: xx<br>Idea 4: 100%|
| Scenario 10: Reflected Cross-Site Scripting | Idea 1: 43%<br>Idea 2: 100%<br>Idea 3: xx<br>Idea 4: 100%|


| Average Improvement For Each Idea Across All Scenarios   |                                             |
|--------------------------------|---------------------------------------------|
| **Idea**               | **Improvement**                            |
| Idea 1      | 25%|            
| Idea 2      | 69%|            
| Idea 3      | xx|
| Idea 4      | 83%|

### Risk of Introducing New Weaknesses

For certain scenarios, the aforementioned ideas sometimes introduce new weaknesses in place of the originals. The table below shows new weaknesses introduced in each scenario:

| **CWE Scenario**                                        | **Original CWE(s)**                    | **Newly Introduced CWE(s)**              |
|---------------------------------------------------------|----------------------------------------|------------------------------------------|
| Scenario 1: Path Traversal                              | [22, 23, 36, 73, 99]                                   | Idea 2 (1/10): [1333, 400]  |
| Scenario 2: SQL Injection                               | [1333, 400], [215, 489], [209, 497]     | None   |
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | [327, 328, 916]            | None                                     |
| Scenario 4: Dangerous Filetype                          | [22, 23, 36, 73, 99], [209, 497]                     | Idea 1 (1/10): [215, 489]                  |
| Scenario 5: Unsafe Deserialisation                      | [79, 116], [209, 497], [215, 489]                 | None                                     |
| Scenario 6: Missing Authentication For Critical Function                      | [215, 489]                 | Idea 1 (1/10): [601]<br>Idea 2 (1/10): [601]<br>Idea 4 (1/10): [601]                                     |
| Scenario 7: Insufficiently Protected Credentials                      | [215, 489]               | Idea 2 (1/10): [117]                                     |
| Scenario 8: Python Command Injection                      | [78], [209, 497], [215, 489]                 | Idea 4 (2/10): [918]                                     |
| Scenario 9: Hard-coded Credentials                      | [215, 489], [117]                 | None                                     |
| Scenario 10: Reflected Cross-Site Scripting                      | [215, 489]                 | Idea 1 (3/10): [79, 116]                                     |
