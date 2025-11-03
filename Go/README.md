## Comparison of Security Weaknesses Across Models (Go)

The below table displays the percentage of samples within each scenario for each model tested where CWE(s) were detected. Note that there are ten samples per scenario (for example, xx indicates that 9/10 samples within the scenario contained one or more security weaknesses).

**Percentage of Samples Containing CWEs Per Scenario**

| **Scenario**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 32B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Scenario 1 (Path Traversal)** | xx | xx | xx       | xx |
| **Scenario 2 (SQL Injection)** | xx | xx | xx       | xx |
| **Scenario 3 (Exposure of Sensitive Information to Unauthorised Actor)** | xx | xx | xx       | xx |
| **Scenario 4 (Dangerous File-type)** | xx | xx | xx       | xx |
| **Scenario 5 (Unsafe Deserialization)** | xx | xx | xx       | xx |
| **Scenario 6 (Missing Authentication For Critical Function)** | xx | xx | xx       | xx |
| **Scenario 7 (Insufficiently Protected Credentials)** | xx | xx | xx       | xx |
| **Scenario 8 (Command Injection)** | xx | xx | xx       | xx |
| **Scenario 9 (Hard-coded Credentials)** | xx | xx | xx       | xx |
| **Scenario 10 (Reflected Cross-Site Scripting)** | xx | xx | xx       | xx |
| -  | -  |- | -      |  - |
| **Average Across All Scenarios:** | **xx** | **xx** | **xx**       | **xx** |

## Average Improvement For Each Idea Across All Scenarios Per Model (Go) | Using the CWE Severity Score As Improvement Indicator
### Formula
The formula below is used to calculate the percentage improvement for each idea from the original raw output from the AI models:

**For each scenario:**

  1. Calculate severity score for each CWE.
  
      - This is done by taking the severity score for the CWE given by CodeQL. The severity score is calculated by grouping the CVSS 3.1 score of related CVEs, then taking the 75th percentile of the score.
  2. Calculate total severity score for the raw output of the AI model by taking the sum of the severity scores of all CWEs reported in the ten raw samples.
  3. Calculate total severity score for the output of each idea by taking the sum of the severity scores of all CWEs reported in the ten samples of each idea.
  4. Calculate the percentage change between the total severity score of the raw output and the total severity score for the outputs of each idea to get the final percentage improvement.

<p align="center">
<img width="708" alt="image" src="https://github.com/user-attachments/assets/d14c1a30-370d-4222-8672-563793d03ba9" />
</p>

### Improvements

| **Idea**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 32B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Idea 1 (Negative Example Prompting)** | xx                        | xx                                | xx                       |             xx                          |
| **Idea 2 (Chain-Of-Thought Prompting)** | xx                        | xx                                | xx                       |            xx                         |
| **Idea 3 (Fine-Tuning)** | xx                        | xx                               |             xx            |                  xx                    |
| **Idea 4 (Meta Prompting)** | xx                        | xx                               |             xx            |                  xx                    |

#### GPT-4.1

| Scenario                                                            | Idea 1 (NEP) | Idea 2 (CoT) | Idea 3 (FT) | Idea 4 (MP) |
|---------------------------------------------------------------------|--------------|----------|--------|--------|
| Scenario 1: Path Traversal                                          | xx           | xx     | xx     | xx    |
| Scenario 2: SQL Injection                                           | xx           | xx     | xx     | xx    |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | xx           | xx     | xx     | xx    |
| Scenario 4: Dangerous File Type                                     | xx           | xx     | xx     | xx    |
| Scenario 5: Unsafe Deserialization                                  | xx           | xx     | xx     | xx    |
| Scenario 6: Missing Authentication for Critical Function            | xx           | xx     | xx     | xx    |
| Scenario 7: Insufficiently Protected Credentials                    | xx           | xx     | xx     | xx    |
| Scenario 8: Command Injection                                       | xx           | xx     | xx     | xx    |
| Scenario 9: Hardcoded Credentials                                   | xx           | xx     | xx     | xx    |
| Scenario 10: Reflected Cross-Site Scripting                         | xx           | xx     | xx     | xx    |
| -  | -  |- | -      |  - |
| **Average**                                                         | **xx** | **xx** | **xx** | **xx** |

#### o4-mini

| Scenario                                                       | Idea 1 (NEP) | Idea 2 (CoT) | Idea 3 (FT) | Idea 4 (MP) |
|----------------------------------------------------------------|--------|--------|----------------------|-----------|
| Scenario 1: Path Traversal                                     | xx     | xx     | xx                | xx      |
| Scenario 2: SQL Injection                                      | xx    | xx     | xx                | xx       |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | xx    | xx     | xx          | xx      |
| Scenario 4: Dangerous File Type                                | xx    | xx    | xx               | xx      |
| Scenario 5: Unsafe Deserialization                             | xx     | xx    | xx                | xx      |
| Scenario 6: Missing Authentication for Critical Function       | xx | xx   | xx        | xx |
| Scenario 7: Insufficiently Protected Credentials               | xx   | xx | xx        | xx |
| Scenario 8: Command Injection                                  | xx    | xx     | xx                | xx       |
| Scenario 9: Hardcoded Credentials                              | xx     | xx    | xx               | xx      |
| Scenario 10: Reflected Cross-Site Scripting                    | xx    | xx    | xx               | xx      |
| -  | -  |- | -      |  - | - |
| **Average**                                                    | **xx** | **xx** | **xx**            | **xx**   |

#### Gemini 2.0 Flash

| Scenario                                                       | Idea 1 (NEP) | Idea 2 (CoT) | Idea 3 (FT) | Idea 4 (MP) |
|----------------------------------------------------------------|--------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | xx    | xx    | xx     | xx |
| Scenario 2: SQL Injection                                      | xx      | xx     | xx     | xx  |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | xx     | xx  | xx  | xx  |
| Scenario 4: Dangerous File Type                                | xx     | xx  | xx  | xx |
| Scenario 5: Unsafe Deserialization                             | xx     | xx     | xx     | xx |
| Scenario 6: Missing Authentication for Critical Function       | xx | xx | xx | xx |
| Scenario 7: Insufficiently Protected Credentials               | xx    | xx  | xx    | xx  |
| Scenario 8: Command Injection                                  | xx     | xx     | xx    | xx  |
| Scenario 9: Hardcoded Credentials                              | xx      | xx     | xx    | xx |
| Scenario 10: Reflected Cross-Site Scripting                    | xx   | xx    | xx      | xx |
| -  | -  |- | -      |  - |
| **Average**                                                    | **xx**  | **xx** | **xx** | **xx** |

#### DeepSeek R1 32B

| Scenario                                                       | Idea 1 (NEP) | Idea 2 (CoT) | Idea 3 (FT) | Idea 4 (MP) |
|----------------------------------------------------------------|--------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | xx     | xx     | xx     | xx |
| Scenario 2: SQL Injection                                      | xx   | xx     | xx      | xx |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | xx     | xx | xx   | xx |
| Scenario 4: Dangerous File Type                                | xx | xx     | xx | xx |
| Scenario 5: Unsafe Deserialization                             | xx    | xx     | xx     | xx |
| Scenario 6: Missing Authentication for Critical Function       | xx | xx | xx | xx |
| Scenario 7: Insufficiently Protected Credentials               | xx    | xx     | xx     | xx |
| Scenario 8: Command Injection                                  | xx    | xx     | xx     | xx |
| Scenario 9: Hardcoded Credentials                              | xx    | xx  | xx | xx |
| Scenario 10: Reflected Cross-Site Scripting                    | xx    | xx     | xx     | xx |
| -  | -  |- | -      |  - |
| **Average**                                                    | **xx**  | **xx** | **xx** | **xx** |

#### CWE Reference Table

| CWE ID(s)        | Weakness Name                                           | Severity |
|------------------|---------------------------------------------------------|----------|
| [79, 116]        | Reflected Cross-Site Scripting                          | 6.1      |
| [1333, 400]      | Insecure regular expression                             | 7.5      |
| [798]            | Hardcoded Credentials                                   | 9.8      |
| [89]             | SQL Injection                                           | 8.8      |
| [327, 328]  | Use of broken/weak hashing algorithm on sensitive data  | 7.5      |
| [209]       | Information exposure through an exception               | 5.4      |
| [22, 23, 36, 73, 99] | Path Traversal                                     | 7.5      |
| [78]             | Command Injection                                       | 9.8      |
| [502]            | Deserialization of user-controlled data                 | 9.8      |
| [215, 489]       | Flask app is run in debug mode                          | 7.5      |
| [601]            | URL Redirection from Remote Source                      | 6.1      |
| [117]            | Log Injection                                           | 7.8      |
| [20]             | Overly permissive regular expression ranges match a wider range of characters than intended | 5.0 |
| [501]             | Trust boundary violation | 8.8 |
| [78] | Executing a command with a relative path | 5.4 |
| [23] | Partial Path Traversal Vulnerability | 9.3 |
| [113] | HTTP Response Splitting | 6.1 |
| [807, 290] | User-controlled bypass of sensitive method | 7.8

## Average Improvement For Each Idea Across All Scenarios Per Model (Go)
The below table displays a comparison between the percentage of samples in which each idea removed security weaknesses in comparison to the raw samples for each model.

The percentage improvement is simply calculated based on how many samples contain CWEs when using each idea versus the original raw output. Then, the percentage-change formula is used to calculate the percentage improvement:

  $S_{\text{raw}}$ = The number of samples (out of 10) that contain one or more weakness in the original raw output.

  $S_{\text{idea}_j}$ = The number of samples (out of 10) that contain one or more weakness after applying the idea ($j$).

$\Delta_j = \frac{S_{\text{raw}} - S_{idea_j}}{S_{\text{raw}}} \times 100\xx$





<br><br>

| **Idea**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Idea 1 (Negative Example Prompting)** | xx                        | xx                                | xx                       |             xx                          |
| **Idea 2 (Chain-Of-Thought Prompting)** | xx                        | xx                                | xx                       |            xx                         |
| **Idea 3 (Fine-Tuning)** | xx                        | xx                               |          xx               |                                 xx       |
| **Idea 4 (Meta Prompting)** | xx                        | xx                               |          xx               |                                 xx       |
