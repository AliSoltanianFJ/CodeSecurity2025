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

Three different ideas were tested:

- Idea 1: Insecure code samples will be generated based on a certain CWE scenario. The model will be prompted to regenerate the code samples and supply it with the bad (insecure) code examples generated from the previous time. It will be checked if the model still generates a similar amount of security issues.

- Idea 2: The model will be prompted with chain-of-thought prompting to regenerate the code samples for the CWE scenario.

- Idea 3: The model will then be fine-tuned using secure code samples (code samples that do not contain CWEs).

- Idea 4: The model is prompted to generate a "meta prompt" with the goal of generating secure code. The model is then prompted to generate the code samples for the CWE scenario with the meta prompt appended to the original instructions.

A summary of the improvements of reducing security weaknesses (CWEs) for each idea is shown below. The tables display the percentage of CWEs removed comapared to the original raw output from Copilot.

| Summary Table of Improvements  |                                             |
|--------------------------------|---------------------------------------------|
| **CWE Scenario**               | **Improvements**                            |
| Scenario 1: Path Traversal     | Idea 1: 100 %<br>Idea 2: 100 %<br>Idea 3: 30 %<br>Idea 4: 50 %|
| Scenario 2: SQL Injection      | Idea 1: -100 %<br>Idea 2: 100 %<br>Idea 3: 100 %<br>Idea 4: -50 %|
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | Idea 1: No Change<br>Idea 2: No Change<br>Idea 3: No Change<br>Idea 4: No Change|
| Scenario 4: Dangerous Filetype | Idea 1: 40 %<br>Idea 2: 90 %<br>Idea 3: 100 %<br>Idea 4: 100 %|
| Scenario 5: Unsafe Deserialisation | Idea 1: 33 %<br>Idea 2: 11 %<br>Idea 3: 100 %<br>Idea 4: 100 %|
| Scenario 6: Missing Authentication For Critical Function | Idea 1: 100 %<br>Idea 2: 100 %<br>Idea 3: 100 %<br>Idea 4: 100 %|
| Scenario 7: Insufficiently Protected Credentials | Idea 1: 100 %<br>Idea 2: 20 %<br>Idea 3: 100 %<br>Idea 4: 40 %|
| Scenario 8: Command Injection | Idea 1: 100 %<br>Idea 2: 100 %<br>Idea 3: 100 %<br>Idea 4: 100 %|
| Scenario 9: Hard-coded Credentials | Idea 1: 30 %<br>Idea 2: 0 %<br>Idea 3: 80%<br>Idea 4: 50 %|
| Scenario 10: Reflected Cross-Site Scripting | Idea 1: 0 %<br>Idea 2: 83 %<br>Idea 3: 83%<br>Idea 4: 17 %|


| Average Improvement For Each Idea Across All Scenarios   |                                             |
|--------------------------------|---------------------------------------------|
| **Idea**               | **Improvement**                            |
| Idea 1      | 45 % |            
| Idea 2      | 67 % |            
| Idea 3      | 88 % |
| Idea 4      | 56 % |

### Risk of Introducing New Weaknesses

For certain scenarios, the aforementioned ideas sometimes introduce new weaknesses in place of the originals. The table below shows new weaknesses introduced in each scenario:

| **CWE Scenario**                                        | **Original CWE(s)**                    | **Newly Introduced CWE(s)**              |
|---------------------------------------------------------|----------------------------------------|------------------------------------------|
| Scenario 1: Path Traversal                              | [22, 23, 36, 73, 99], [117]                     | None  |
| Scenario 2: SQL Injection                               | [117]                 | None   |
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | None            | None                                     |
| Scenario 4: Dangerous Filetype                          | [22, 23, 36, 73, 99]                     | Idea 2 (1/10): [79, 116]                  |
| Scenario 5: Unsafe Deserialisation                      | [502]                 | Idea 1 (6/10): [79, 116]<br>Idea 2 (8/10): [79, 116]                                    |
| Scenario 6: Missing Authentication For Critical Function                      | 306                 | None                                   |
| Scenario 7: Insufficiently Protected Credentials                      | [327, 328]               | Idea 4 (5/10): [798], [20]                                     |
| Scenario 8: Command Injection                      | [79, 116]                 | None            |
| Scenario 9: Hard-coded Credentials                      | [798]                 | Idea 4 (5/10): [117], [798], [20]                                     |
| Scenario 10: Reflected Cross-Site Scripting                      | [79, 116]                 | Idea 1 (1/10): [117]                                     |
