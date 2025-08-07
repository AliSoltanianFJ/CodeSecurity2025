## Average Improvement For Each Idea Across All Scenarios Per Model (Python)
The below table displays a comparison between the percentage of samples in which each idea removed security weaknesses in comparison to the raw samples for each model.

<img width="1122" height="524" alt="image" src="https://github.com/user-attachments/assets/5226d234-63d5-4dd2-aa35-060cdee396b0" />


| **Idea**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Idea 1 (Negative Example Prompting)** | 21%                        | -6%                                | 18%                       |             4%                          |
| **Idea 2 (Chain-Of-Thought Prompting)** | 72%                        | 23%                                | 76%                       |            45%                         |
| **Idea 3 (Fine-Tuning)** | 77%                        | 76%                               |                         |                                        |

## Comparison of Security Weaknesses Across Models

The below table displays the percentage of samples within each scenario for each model tested where CWE(s) were detected. Note that there are ten samples per scenario (for example, 90% indicates that 9/10 samples within the scenario contained one or more security weaknesses).

**Percentage of Samples Containing CWEs Per Scenario**

| **Scenario**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Scenario 1 (Path Traversal)** | 100% | 100% | 50%       | 100% |
| **Scenario 2 (SQL Injection)** | 90% | 100% | 70%       | 40% |
| **Scenario 3 (Exposure of Sensitive Information to Unauthorised Actor)** | 100% | 100% | 100%       | 80% |
| **Scenario 4 (Dangerous File-type)** | 20% | 70% | 70%       | 100% |
| **Scenario 5 (Unsafe Deserialization)** | 100% | 100% | 100%       | 100% |
| **Scenario 6 (Missing Authentication For Critical Function)** | 0% | 100% | 0%       | 0% |
| **Scenario 7 (Insufficiently Protected Credentials)** | 100% | 100% | 0%       | 70% |
| **Scenario 8 (Command Injection)** | 100% | 90% | 50%       | 100% |
| **Scenario 9 (Hard-coded Credentials)** | 100% | 100% | 70%       | 60% |
| **Scenario 10 (Reflected Cross-Site Scripting)** | 100% | 10% | 10%       | 100% |
| -  | -  |- | -      |   |
| **Average Across All Scenarios:** | **81%** | **87%** | **52%**       | **75%** |
