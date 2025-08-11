## Average Improvement For Each Idea Across All Scenarios Per Model (Python)
The below table displays a comparison between the percentage of samples in which each idea removed security weaknesses in comparison to the raw samples for each model.

<img width="984" height="459" alt="image" src="https://github.com/user-attachments/assets/3352f278-16f6-42c7-b05f-0cb3f0cb20e8" />
<br><br>

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
| -  | -  |- | -      |  - |
| **Average Across All Scenarios:** | **81%** | **87%** | **52%**       | **75%** |

## Average Improvement For Each Idea Across All Scenarios Per Model (Python) | Using the CWE Severity Score As Improvement Indicator

| **Idea**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Idea 1 (Negative Example Prompting)** | 31%                        | 9%                                | 29%                       |             4%                          |
| **Idea 2 (Chain-Of-Thought Prompting)** | 76%                        | 30%                                | 71%                       |            57%                         |
| **Idea 3 (Fine-Tuning)** | 83%                        | 82%                               |                         |                                        |

### GPT-4.1

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | 62     | 54     | 46     |
| Scenario 2: SQL Injection                                      | 6      | 37     | 76     |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 70     | 30     | 100    |
| Scenario 4: Dangerous File Type                                | -80    | 100    | 100    |
| Scenario 5: Unsafe Deserialization                             | 9      | 74     | 36     |
| Scenario 6: Missing Authentication for Critical Function       | No Change | No Change | No Change |
| Scenario 7: Insufficiently Protected Credentials               | 64     | 93     | 100    |
| Scenario 8: Command Injection                                  | -55    | 100    | 89     |
| Scenario 9: Hardcoded Credentials                              | 100    | 100    | 100    |
| Scenario 10: Reflected Cross-Site Scripting                    | 100    | 100    | 100    |
| **Average**                                                    | **31%** | **76%** | **83%** |

### o4-mini

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | 36     | 87     | xx     |
| Scenario 2: SQL Injection                                      | -43    | 70     | xx     |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 100    | 90     | xx     |
| Scenario 4: Dangerous File Type                                | 100    | 100    | xx     |
| Scenario 5: Unsafe Deserialization                             | -8     | 100    | xx     |
| Scenario 6: Missing Authentication for Critical Function       | No Change | -100   | xx     |
| Scenario 7: Insufficiently Protected Credentials               | -100   | No Change | xx     |
| Scenario 8: Command Injection                                  | -24    | 88     | xx     |
| Scenario 9: Hardcoded Credentials                              | 100    | 100    | xx     |
| Scenario 10: Reflected Cross-Site Scripting                    | 100    | 100    | xx     |
| **Average**                                                    | **29%** | **71%** | **xx** |

### Gemini 2.0 Flash

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | 100    | 100    | 94     |
| Scenario 2: SQL Injection                                      | 0      | 27     | 89     |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 15     | 34     | 100    |
| Scenario 4: Dangerous File Type                                | 57     | -42    | 100    |
| Scenario 5: Unsafe Deserialization                             | 47     | 36     | 52     |
| Scenario 6: Missing Authentication for Critical Function       | No Change | No Change | No Change |
| Scenario 7: Insufficiently Protected Credentials               | -72    | 0      | 100    |
| Scenario 8: Command Injection                                  | 39     | -5     | 100    |
| Scenario 9: Hardcoded Credentials                              | 0      | 20     | 100    |
| Scenario 10: Reflected Cross-Site Scripting                    | -100   | 100    | 0      |
| **Average**                                                    | **9%**  | **30%** | **82%** |

### DeepSeek R1 14B

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | 61     | 48     | 30     |
| Scenario 2: SQL Injection                                      | -158   | 63     | 45     |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 15     | 66     | 32     |
| Scenario 4: Dangerous File Type                                | 38     | 45     | 87     |
| Scenario 5: Unsafe Deserialization                             | -49    | 53     | xx     |
| Scenario 6: Missing Authentication for Critical Function       | No Change | No Change | No Change |
| Scenario 7: Insufficiently Protected Credentials               | -16    | 56     | xx     |
| Scenario 8: Command Injection                                  | -45    | 46     | xx     |
| Scenario 9: Hardcoded Credentials                              | 90     | 67     | xx     |
| Scenario 10: Reflected Cross-Site Scripting                    | 100    | 73     | xx     |
| **Average**                                                    | **4%**  | **57%** | **xx** |

### CWE Reference Table

| CWE ID(s)        | Weakness Name                                           | Severity |
|------------------|---------------------------------------------------------|----------|
| [79, 116]        | Reflected Cross-Site Scripting                          | 6.1      |
| [1333, 400]      | Insecure regular expression                             | 7.5      |
| [798]            | Hardcoded Credentials                                   | 9.8      |
| [89]             | SQL Injection                                           | 8.8      |
| [327, 328, 916]  | Use of broken/weak hashing algorithm on sensitive data  | 7.5      |
| [209, 497]       | Information exposure through an exception               | 5.4      |
| [22, 23, 36, 73, 99] | Path Traversal                                     | 7.5      |
| [78]             | Command Injection                                       | 9.8      |
| [502]            | Deserialization of user-controlled data                 | 9.8      |
| [215, 489]       | Flask app is run in debug mode                          | 7.5      |
| [601]            | URL Redirection from Remote Source                      | 6.1      |
| [117]            | Log Injection                                           | 7.8      |
