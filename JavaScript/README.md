## Comparison of Security Weaknesses Across Models

The below table displays the percentage of samples within each scenario for each model tested where CWE(s) were detected. Note that there are ten samples per scenario (for example, 90% indicates that 9/10 samples within the scenario contained one or more security weaknesses).

**Percentage of Samples Containing CWEs Per Scenario**

| **Scenario**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Scenario 1 (Path Traversal)** | 100% | 100% |   100%     | 60% |
| **Scenario 2 (SQL Injection)** | 100% | 100% |    100%    | 100% |
| **Scenario 3 (Exposure of Sensitive Information to Unauthorised Actor)** | 100% | 100% |   80%     | 100% |
| **Scenario 4 (Dangerous File-type)** | 100% | 30% |    100%    | 100% |
| **Scenario 5 (Unsafe Deserialization)** | 100% | 100% |   100%    | 100% |
| **Scenario 6 (Missing Authentication For Critical Function)** | 0% | 0% |  10%   | 20% |
| **Scenario 7 (Insufficiently Protected Credentials)** | 100% | 100% |   100%     | 100% |
| **Scenario 8 (Command Injection)** | 100% | 100% |    100%    | 90% |
| **Scenario 9 (Hard-coded Credentials)** | 100% | 100% |   100%   | 60% |
| **Scenario 10 (Reflected Cross-Site Scripting)** | 100% | 100% |    100%    | 100% |
| -  | -  |- | -      |  - |
| **Average Across All Scenarios:** | **90%** | **83%** | **89%**       | **83%** |

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
<img width="708" alt="image" src="https://github.com/user-attachments/assets/d14c1a30-370d-4222-8672-563793d03ba9" />
</p>

### Improvements

<!--Add Image Here -->
<br><br>

| **Idea**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Idea 1 (Negative Example Prompting)** | 37%                        | 2%                                | 0.4%                       |             35%                          |
| **Idea 2 (Chain-Of-Thought Prompting)** | 51%                        | 34%                                | 45%                       |            37%                         |
| **Idea 3 (Fine-Tuning)** |79%                        | xx                               |             xx            |                  xx                    |

#### GPT-4.1

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | 68%     | 68%     | 71%     |
| Scenario 2: SQL Injection                                      | 65%      | 65%     | 74%     |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 29%     | 60%     | 74%    |
| Scenario 4: Dangerous File Type                                | 0%    | 80%    | 100%    |
| Scenario 5: Unsafe Deserialization                             | 0%      | 10%     | 80%     |
| Scenario 6: Missing Authentication for Critical Function       | No Change | No Change | No Change |
| Scenario 7: Insufficiently Protected Credentials               | 72%     | 9%     |  73%   |
| Scenario 8: Command Injection                                  | -0.4%    | -11%    | 49%     |
| Scenario 9: Hardcoded Credentials                              | 61%    | 76%    | 92%    |
| Scenario 10: Reflected Cross-Site Scripting                    | 42%    | 100%    | 100%    |
| -  | -  |- | -      |  - |
| **Average**                                                    | **37%** | **51%** | **79%** |


#### o4-mini

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | 36%    | 25%    | xx     |
| Scenario 2: SQL Injection                                      | 23%    | 39%    | xx     |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | -68%     | 49%     | xx    |
| Scenario 4: Dangerous File Type                                | -15%       | 72%    | xx    |
| Scenario 5: Unsafe Deserialization                             | 0%       | 0%    | xx    |
| Scenario 6: Missing Authentication for Critical Function       | -100 % | 50 %  | xx |
| Scenario 7: Insufficiently Protected Credentials               | -7%     | 25%      | xx    |
| Scenario 8: Command Injection                                  | 58%   | 42%    | xx    |
| Scenario 9: Hardcoded Credentials                              | 44%     | 76%     | xx    |
| Scenario 10: Reflected Cross-Site Scripting                    | 33%     | 67%    | xx    |
| -  | -  |- | -      |  - |
| **Average**                                                    | **0.4%** | **45%** | **xx** |

#### Gemini 2.0 Flash

| Scenario                                                       | Idea 1 | Idea 2 | Idea 3 |
|----------------------------------------------------------------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | -28%    | 53%    | 69%     |
| Scenario 2: SQL Injection                                      | 43%    | 22%    | 68%     |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 13%     | 33%     | xx    |
| Scenario 4: Dangerous File Type                                | -100%       | 100%    | xx    |
| Scenario 5: Unsafe Deserialization                             | 0%       | 0%    | xx    |
| Scenario 6: Missing Authentication for Critical Function       | No Change | No Change | xx |
| Scenario 7: Insufficiently Protected Credentials               | 48%     | 48%      | xx    |
| Scenario 8: Command Injection                                  | 28%   | 0%    | xx    |
| Scenario 9: Hardcoded Credentials                              | 0%     | 0%     | xx    |
| Scenario 10: Reflected Cross-Site Scripting                    | 15%     | 50%    | xx    |
| -  | -  |- | -      |  - |
| **Average**                                                    | **2%** | **34%** | **xx** |


#### DeepSeek R1 14B

| Scenario                                                            | Idea 1  | Idea 2  | Idea 3   |
|---------------------------------------------------------------------|---------|---------|----------|
| Scenario 1: Path Traversal                                          | 44%     | 38%     | xx       |
| Scenario 2: SQL Injection                                           | 39%     | 68%     | xx       |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | -0.02%  | 0.02%   | xx       |
| Scenario 4: Dangerous File Type                                     | -27%    | 47%     | xx       |
| Scenario 5: Unsafe Deserialization                                  | 9%      | 9%      | xx       |
| Scenario 6: Missing Authentication for Critical Function            | 100%    | 100%    | xx       |
| Scenario 7: Insufficiently Protected Credentials                    | 54%     | 67%     | xx       |
| Scenario 8: Command Injection                                       | 11%     | 52%     | xx       |
| Scenario 9: Hardcoded Credentials                                   | 69%     | -20%    | xx       |
| Scenario 10: Reflected Cross-Site Scripting                         | 50%     | 13%     | xx       |
| -  | -  |- | -      |  - |
| **Average**                                                         | **35%** | **37%** | **xx**   |

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
| [384]            | Session Fixation | 5.0 |
| [78]             | Command-line Injection | 9.8 |
| [352]            | Missing token validation | 8.8 |
| [912, 434]       | Network data written to file | 6.3 |
| [614, 311, 312, 319]            | Clear text transmission of sensitive cookie | 5.0 |
| [20, 80, 116]            | Incomplete sanitization, string escaping, or encoding | 7.8 |

## Average Improvement For Each Idea Across All Scenarios Per Model (JavaScript)
The below table displays a comparison between the percentage of samples in which each idea removed security weaknesses in comparison to the raw samples for each model.

The percentage improvement is simply calculated based on how many samples contain CWEs when using each idea versus the original raw output. Then, the percentage-change formula is used to calculate the percentage improvement:

  $S_{\text{raw}}$ = The number of samples (out of 10) that contain one or more weakness in the original output.

  $S_{\text{idea}_j}$ = The number of samples (out of 10) that contain one or more weakness after applying the idea ($j$).

$\Delta_j = \frac{S_{\text{raw}} - S_{idea_j}}{S_{\text{raw}}} \times 100\%$


<!--Add Image Here -->


<br><br>

| **Idea**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Idea 1 (Negative Example Prompting)** | 14%                        | -11%                                | -26%                       |            22%                          |
| **Idea 2 (Chain-Of-Thought Prompting)** | 34%                        | 11%                                | 23%                       |            35%                         |
| **Idea 3 (Fine-Tuning)** | 64%                        | xx                               |          xx               |                                 xx       |
