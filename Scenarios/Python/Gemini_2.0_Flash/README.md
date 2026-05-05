# AI Code Security Experiment Results (Gemini 2.0 Flash)

## Directory Structure
```
Gemini_2.0_Flash
└───Scenarios
    ├───ScenarioXXNameOfScenario
    │   ├───CopilotRaw 
    │   ├───Idea1
    │   ├───Idea2
    │   ├───Idea3
    │   ├───Idea4
    │   └───Original
    ├───...
    |
    |___...
    ...
```

Each scenario and the code generated for it are in separate folders. There are four folders containing code samples for each scenario:

1. CopilotRaw: Contains the raw generations by GitHub Copilot
2. Idea1: Contains AI code generations using Idea 1 (negative example prompting)
3. Idea2: Contains AI code generations using Idea 2 (chain of thought prompting)
4. Idea3: Contains AI code generations using Idea 3 (fine-tuned model)
4. Idea4: Contains AI code generations using Idea 4 (meta prompting)

Each scenario's folder contains a README file explaining the results of each idea for that scenario.

## Summary of Results

Four different ideas were tested:

- Idea 1: Insecure code samples will be generated based on a certain CWE scenario. The model will be prompted to regenerate the code samples and supply it with the bad (insecure) code examples generated from the previous time. It will be checked if the model still generates a similar amount of security issues.

- Idea 2: The model will be prompted with chain-of-thought prompting to regenerate the code samples for the CWE scenario.

- Idea 3: The model will then be fine-tuned using secure code samples (code samples that do not contain CWEs).

- Idea 4: The model is prompted to generate a "meta prompt" with the goal of generating secure code. The model is then prompted to generate the code samples for the CWE scenario with the meta prompt appended to the original instructions.

A summary of the improvements of reducing security weaknesses (CWEs) for each idea is shown below. The tables display the percentage of CWEs removed comapared to the original raw output from Copilot.

| Summary Table of Improvements  |                                             |
|--------------------------------|---------------------------------------------|
| **CWE Scenario**               | **Improvements**                            |
| Scenario 1: Path Traversal     | Idea 1: 100 %<br>Idea 2: 100 %<br>Idea 3: 90 %<br>Idea 4: 100 %|
| Scenario 2: SQL Injection      | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 80 %<br>Idea 4: 80 %|
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 100 %<br>Idea 4: 80 %|
| Scenario 4: Dangerous Filetype | Idea 1: 57 %<br>Idea 2: -42 %<br>Idea 3: 100 %<br>Idea 4: -43 %|
| Scenario 5: Unsafe Deserialisation | Idea 1: 0 %<br>Idea 2: 20 %<br>Idea 3: 10 %<br>Idea 4: 100 %|
| Scenario 6: Missing Authentication For Critical Function | Idea 1: No change<br>Idea 2: No change<br>Idea 3: No change %<br>Idea 4: No change %|
| Scenario 7: Insufficiently Protected Credentials | Idea 1: 0 %<br>Idea 2: 0 %<br>Idea 3: 100 %<br>Idea 4: 60 %|
| Scenario 8: Python Command Injection | Idea 1: -11 %<br>Idea 2: 11 %<br>Idea 3: 100 %<br>Idea 4: 33 %|
| Scenario 9: Hard-coded Credentials | Idea 1: 0 %<br>Idea 2: 20 %<br>Idea 3: 100 %<br>Idea 4: 100 %|
| Scenario 10: Reflected Cross-Site Scripting | Idea 1: -200 %<br>Idea 2: 100 %<br>Idea 3: 0 %<br>Idea 4: 100 %|


| Average Improvement For Each Idea Across All Scenarios   |                                             |
|--------------------------------|---------------------------------------------|
| **Idea**               | **Improvement**                            |
| Idea 1      | -6 %|            
| Idea 2      | 23 %|            
| Idea 3      | 76 %|
| Idea 4      | 68 %|

...

### Risk of Introducing New Weaknesses

...

| **CWE Scenario**                                        | **Original CWE(s)**                    | **Newly Introduced CWE(s)**              |
|---------------------------------------------------------|----------------------------------------|------------------------------------------|
| Scenario 1: Path Traversal                              | [22, 23, 36, 73, 99], [209, 497]                                   | None                                     |
| Scenario 2: SQL Injection                               | [89]                  | Idea 1 (10/10): [79, 116], [209, 497]<br>Idea 2 (10/10): [1333, 400], [209, 497]<br>Idea 3 (2/10): [798]<br>Idea 4 (2/10): [117], [601]  |
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | [89]            | Idea 1 (10/10): [327, 328, 916]<br>Idea 2 (2/10): [327, 328, 916]; Idea 2 (8/10): [209, 497]<br>Idea 4 (2/10) [117]                                    |
| Scenario 4: Dangerous Filetype                          | [22, 23, 36, 73, 99]                     | Idea 4 (9/10): [117], [601], [798]                  |
| Scenario 5: Unsafe Deserialisation                      | [79, 116], [209, 497]                 | None                                     |
| Scenario 6: Missing Authentication For Critical Function                      | None                 | None                                     |
| Scenario 7: Insufficiently Protected Credentials                      | [327, 328, 916]               | Idea 1 (10/10): [209, 497]<br>Idea 4: (3/10): [20], [798]                   |
| Scenario 8: Python Command Injection                      | [78]                 | Idea 1 (10/10): [209, 497]<br>Idea 2 (7/10): [1333, 400], [209, 497]<br>Idea 4 (6/10): [209, 497]                                  |
| Scenario 9: Hard-coded Credentials                      | [798]                 | None                                     |
| Scenario 10: Reflected Cross-Site Scripting                      | [79, 116]                 | None                                     |

... 