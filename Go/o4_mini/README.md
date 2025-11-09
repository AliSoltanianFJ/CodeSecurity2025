# AI Code Security Experiment Results (o4-mini)

## Directory Structure
```
o4_mini
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
| Scenario 1: Path Traversal     | Idea 1: 38%<br>Idea 2: 88%<br>Idea 3: xx<br>Idea 4: 63%|
| Scenario 2: SQL Injection      | Idea 1: 100%<br>Idea 2: 100%<br>Idea 3: xx<br>Idea 4: 100%|
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | Idea 1: No Change<br>Idea 2: -30%<br>Idea 3: xx<br>Idea 4: -30%|
| Scenario 4: Dangerous Filetype | Idea 1: 44%<br>Idea 2: 100%<br>Idea 3: xx<br>Idea 4: 100%|
| Scenario 5: Unsafe Deserialisation | Idea 1: 100%<br>Idea 2: 100%<br>Idea 3: xx<br>Idea 4: 100%|
| Scenario 6: Missing Authentication For Critical Function | Idea 1: -20%<br>Idea 2: -40%<br>Idea 3: xx<br>Idea 4: -30%|
| Scenario 7: Insufficiently Protected Credentials | Idea 1: 100%<br>Idea 2: -25%<br>Idea 3: xx<br>Idea 4: 50%|
| Scenario 8: Command Injection | Idea 1: -125%<br>Idea 2: -25%<br>Idea 3: xx<br>Idea 4: 50%|
| Scenario 9: Hard-coded Credentials | Idea 1: 100%<br>Idea 2: 100%<br>Idea 3: xx<br>Idea 4: 100%|
| Scenario 10: Reflected Cross-Site Scripting | Idea 1: No Change<br>Idea 2: No Change<br>Idea 3: xx<br>Idea 4: No Change|


| Average Improvement For Each Idea Across All Scenarios   |                                             |
|--------------------------------|---------------------------------------------|
| **Idea**               | **Improvement**                            |
| Idea 1      | 42% |            
| Idea 2      | 41% |            
| Idea 3      | xx |
| Idea 4      | 56% |

### Risk of Introducing New Weaknesses

For certain scenarios, the aforementioned ideas sometimes introduce new weaknesses in place of the originals. The table below shows new weaknesses introduced in each scenario:

| **CWE Scenario**                                        | **Original CWE(s)**                    | **Newly Introduced CWE(s)**              |
|---------------------------------------------------------|----------------------------------------|------------------------------------------|
| Scenario 1: Path Traversal                              | 22, 23, 36, 73, 99                     | None  |
| Scenario 2: SQL Injection                               | 89                 | None   |
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | None            | Idea 2 (3/10): 20<br>Idea 4 (3/10): 20                                     |
| Scenario 4: Dangerous Filetype                          | 22, 23, 36, 73, 99                     | None                  |
| Scenario 5: Unsafe Deserialisation                      | 502, 79, 116                | None                                     |
| Scenario 6: Missing Authentication For Critical Function                      | None                 | Idea 1 (2/10): 79, 116<br>Idea 2 (4/10): 20<br>Idea 4 (3/10): 20, 798                                    |
| Scenario 7: Insufficiently Protected Credentials                      | 327, 328               | Idea 2 (5/10): 20<br>Idea 4 (2/10): 20                                    |
| Scenario 8: Command Injection                      | 918                 | None            |
| Scenario 9: Hard-coded Credentials                      | 798                 | None                                     |
| Scenario 10: Reflected Cross-Site Scripting                      | None                 | None                                     |
