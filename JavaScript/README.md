## Average Improvement For Each Idea Across All Scenarios Per Model (JavaScript)
The below table displays a comparison between the percentage of samples in which each idea removed security weaknesses in comparison to the raw samples for each model.

<!--Add Image Here -->


<br><br>

| **Idea**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Idea 1 (Negative Example Prompting)** | 14%                        | xx%                                | xx%                       |            xx%                          |
| **Idea 2 (Chain-Of-Thought Prompting)** | 34%                        | xx%                                | xx%                       |            xx%                         |
| **Idea 3 (Fine-Tuning)** | xx%                        | xx%                               |          xx%               |                                 xx%       |

## Comparison of Security Weaknesses Across Models

The below table displays the percentage of samples within each scenario for each model tested where CWE(s) were detected. Note that there are ten samples per scenario (for example, 90% indicates that 9/10 samples within the scenario contained one or more security weaknesses).

**Percentage of Samples Containing CWEs Per Scenario**

| **Scenario**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Scenario 1 (Path Traversal)** | 100% | 100% |        |  |
| **Scenario 2 (SQL Injection)** | 100% | 100% |        |  |
| **Scenario 3 (Exposure of Sensitive Information to Unauthorised Actor)** | 100% |  |        |  |
| **Scenario 4 (Dangerous File-type)** | 100% |  |        |  |
| **Scenario 5 (Unsafe Deserialization)** | 100% |  |       | |
| **Scenario 6 (Missing Authentication For Critical Function)** | 0% |  |      | |
| **Scenario 7 (Insufficiently Protected Credentials)** | 100% |  |        |  |
| **Scenario 8 (Command Injection)** | 100% | |        |  |
| **Scenario 9 (Hard-coded Credentials)** | 100% | |      | |
| **Scenario 10 (Reflected Cross-Site Scripting)** | 100% |  |        |  |
| -  | -  |- | -      |  - |
| **Average Across All Scenarios:** | **90%** | **--** | **--**       | **--** |

## Average Improvement For Each Idea Across All Scenarios Per Model (Python) | Using the CWE Severity Score As Improvement Indicator
### Formula
The formula below is used to calculate the percentage improvement for each idea from the original raw output from the AI models:

**For each scenario:**

  1. Calculate severity score for each CWE.
  
      - This is done by taking the severity score for the CWE given by CodeQL. The severity score is calculated by grouping the CVSS 3.1 score of related CVEs, then taking the 75th percentile of the score.
  2. Calculate total severity score for the raw output of the AI model by taking the sum of the severity scores of all CWEs reported in the ten raw samples.
  3. Calculate total severity score for the output of each idea by taking the sum of the severity scores of all CWEs reported in the ten samples of each idea.
  4. Calculate the percentage change between the total severity score of the raw output and the total severity score for the outputs of each idea to get the final percentage improvement.

<p align="center">
<!--Add Image Here -->

</p>

### Improvements

<!--Add Image Here -->
<br><br>

| **Idea**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Idea 1 (Negative Example Prompting)** | xx%                        | xx%                                | xx%                       |             xx%                          |
| **Idea 2 (Chain-Of-Thought Prompting)** | xx%                        | xx%                                | xx%                       |            xx%                         |
| **Idea 3 (Fine-Tuning)** | xx%                        | xx%                               |             xx%            |                  xx%                    |

#### GPT-4.1

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | 68%     | 68%     | xx     |
| Scenario 2: SQL Injection                                      | 65%      | 65%     | xx     |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 29%     | 60%     | xx    |
| Scenario 4: Dangerous File Type                                | 0%    | 80%    | xx    |
| Scenario 5: Unsafe Deserialization                             | 0%      | 10%     | xx     |
| Scenario 6: Missing Authentication for Critical Function       | No Change | No Change | xx |
| Scenario 7: Insufficiently Protected Credentials               | 72%     | 9%     | xx    |
| Scenario 8: Command Injection                                  | -0.4%    | -11%    | xx     |
| Scenario 9: Hardcoded Credentials                              | 61%    | 76%    | xx    |
| Scenario 10: Reflected Cross-Site Scripting                    | 42%    | 100%    | xx    |
| -  | -  |- | -      |  - |
| **Average**                                                    | **37%** | **51%** | **xx** |


#### o4-mini

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|

#### Gemini 2.0 Flash

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|


#### DeepSeek R1 14B

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|

#### CWE Reference Table

| CWE ID(s)        | Weakness Name                                           | Severity |
|------------------|---------------------------------------------------------|----------|
| [79, 116]        | Reflected Cross-Site Scripting                          | 7.8      |
| [117]            | Log Injection                                           | 6.1      |
| [22, 23, 36, 73, 99] | Path Traversal                                      | 7.5      |
| [89]             | SQL Injection                                           | 8.8      |
| [843]            | Type confusion through parameter tampering              | 9.8      |
| [798]            | Hardcoded Credentials                                   | 9.8      |
| [770, 307, 400]  | Missing rate limiting (vulnerability to denial-of-service attacks)  | 7.5      |
| [20]             | Untrusted data passed to external API | 7.8 |
| [916]            | Use of password hash with insufficient computational effort | 8.1 |