# AI Code Security Experiment Results

## Directory Structure
```
Repository
└───Scenarios
    ├───Scenario1PathTraversal
    │   ├───CopilotRaw
    │   ├───Idea1
    │   ├───Idea2
    │   ├───Idea3
    │   └───Original
    ├───Scenario2SQLInjection
    │   ├───CopilotRaw
    │   ├───Idea1
    │   ├───Idea2
    │   └───Idea3
    ├───Scenario3ExposureOfSensitiveInformationToUnauthorisedActor
    │   ├───CopilotRaw
    │   ├───Idea1
    │   ├───Idea2
    │   └───Idea3
    ├───Scenario4DangerousFiletype
    │   ├───CopilotRaw
    │   ├───Idea1
    │   ├───Idea2
    │   └───Idea3
    └───Scenario5UnsafeDeserialization
        ├───CopilotRaw
        ├───Idea1
        ├───Idea2
        └───Idea3
```

Each scenario and the code generated for it are in separate folders. There are four folders containing code:

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
| Scenario 1: Path Traversal     | Idea 1: 60 %<br>Idea 2: 50 %<br>Idea 3: 40 %|
| Scenario 2: SQL Injection      | Idea 1: -10 %<br>Idea 2: 67 %<br>Idea 3: 67 %|
| Scenario 3: Exposure Of Sensitive Information To Unauthorised Actor | Idea 1: 70 %<br>Idea 2: 30 %<br>Idea 3: 100 %|
| Scenario 4: Dangerous Filetype | Idea 1: -60 %<br>Idea 2: 100 %<br>Idea 3: 100 %|
| Scenario 5: Unsafe Deserialisation | Idea 1: 10 %<br>Idea 2: 60 %<br>Idea 3: 0 %|


| Average Improvement For Each Idea Across All Scenarios   |                                             |
|--------------------------------|---------------------------------------------|
| **CWE Scenario**               | **Improvement**                            |
| Idea 1      | 14 %|            
| Idea 2      | 61 %|            
| Idea 3      | 61 %|

Idea 1 showed most improvement in Scenarios 1 and 3 with improvemnets of 60% and 70% respectively. Ideas 2 and 3 showed the highest improvements in Scenarios 3 and 4, with a 100% reduction in CWEs.

On average, Idea 1 showed the least reduction in security weaknesses (14%). The results show that supplying the model with the bad (insecure) code examples in the prompt did not cause a significant improvement in the security of the generated code.

Ideas 2 and 3 both showed an average reduction in CWEs of 61%. However, in one case, fine tuning (Idea 3) did not show any reduction in CWEs (all ten code generations contained at least one CWE). In comparison, chain-of-thought prompting (Idea 2) showed consistent reductions in security weaknesses with the worst performance seen in Scenario 3 with only a 30% reduction.

A common trend was seen in scenarios that involved sanitising strings of user input. Copilot often created complex regular expressions to filter out unsafe input, resulting in CWEs 1333 and 400 that could leave the host system vulnerable to denial of service attacks. CWE 400 is one the [MITRE Top 25 CWEs list](https://cwe.mitre.org/top25/archive/2024/2024_cwe_top25.html). The only other instance in which Copilot introduced a new vulnerability was in Scenario 4, where it would generate code containing CWEs 209 and 497. These CWEs describe a weakness in code where private environment paths are made visible by displaying the strack trace of exceptions to the user.