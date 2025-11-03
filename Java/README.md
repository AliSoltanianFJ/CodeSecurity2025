## Comparison of Security Weaknesses Across Models (Java)

The below table displays the percentage of samples within each scenario for each model tested where CWE(s) were detected. Note that there are ten samples per scenario (for example, xx indicates that 9/10 samples within the scenario contained one or more security weaknesses).

**Percentage of Samples Containing CWEs Per Scenario**

| **Scenario**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 32B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Scenario 1 (Path Traversal)** | 70% | 100% | 60%       | 100% |
| **Scenario 2 (SQL Injection)** | 100% | 100% | 100%       | 100% |
| **Scenario 3 (Exposure of Sensitive Information to Unauthorised Actor)** | 100% | 90% | 100%       | 100% |
| **Scenario 4 (Dangerous File-type)** | 100% | 0% | 0%       | 0% |
| **Scenario 5 (Unsafe Deserialization)** | 100% | 80% | 90%       | 60% |
| **Scenario 6 (Missing Authentication For Critical Function)** | 0% | 0% | 30%       | 0% |
| **Scenario 7 (Insufficiently Protected Credentials)** | 100% | 100% | 100%       | 100% |
| **Scenario 8 (Command Injection)** | 100% | 100% | 100%       | 90% |
| **Scenario 9 (Hard-coded Credentials)** | 100% | 90% | 100%       | 100% |
| **Scenario 10 (Reflected Cross-Site Scripting)** | 100% | 100% | 50%       | 80% |
| -  | -  |- | -      |  - |
| **Average Across All Scenarios:** | **87%** | **76%** | **73%**       | **73%** |

## Average Improvement For Each Idea Across All Scenarios Per Model (Java) | Using the CWE Severity Score As Improvement Indicator
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
| **Idea 1 (Negative Example Prompting)** | 16%                        | 30%                                | 11%                       |             1%                          |
| **Idea 2 (Chain-Of-Thought Prompting)** | 40%                        | 24%                                | 34%                       |            28%                         |
| **Idea 3 (Fine-Tuning)** | 77%                        | 70%                               |             xx            |                  81%                    |
| **Idea 4 (Meta Prompting)** | 66%                        | 58%                               |             32%            |                  35%                    |

#### GPT-4.1

| Scenario                                                       | Idea 1 (NEP) | Idea 2 (CoT) | Idea 3 (FT) | Idea 4 (MP) |
|----------------------------------------------------------------|--------------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | -43%         |  0%    | 43%     | -29% |
| Scenario 2: SQL Injection                                      | 4%           | 4%     | 73%     | 66% |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 42%     | 11%     | 43% | 75%    |
| Scenario 4: Dangerous File Type                                | -15%         | 100%    | 100% | 100%    |
| Scenario 5: Unsafe Deserialization                             | 4%         | 47%     | 91%  | 91%    |
| Scenario 6: Missing Authentication for Critical Function       | No Change  | No Change | No Change | No Change |
| Scenario 7: Insufficiently Protected Credentials               | -1%        | 29%     | 80%    | 80%    |
| Scenario 8: Command Injection                                  | 81%       | 68%    | 100%     | 97%     |
| Scenario 9: Hardcoded Credentials                              | 53%       | 37%    | 73%    | 58%    |
| Scenario 10: Reflected Cross-Site Scripting                    | 16%      | 62%    | 89%    | 57%    |
| -  | -  |- | -      |  - |
| **Average**                                                    | **16%** | **40%** | **77%** | **66%** |

#### o4-mini

| Scenario                                                       | Idea 1 (NEP) | Idea 2 (CoT) | Idea 3 (FT) | Idea 4 (MP) |
|----------------------------------------------------------------|--------|--------|----------------------|-----------|
| Scenario 1: Path Traversal                                     | -199%     | -56%     | xx                | -117%      |
| Scenario 2: SQL Injection                                      | 63%    | 65%     | xx                | 68%       |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 27%    | 27%     | xx          | 46%      |
| Scenario 4: Dangerous File Type                                | No Change    | No Change    | xx               | No Change      |
| Scenario 5: Unsafe Deserialization                             | -11     | 22%    | xx                | 78%      |
| Scenario 6: Missing Authentication for Critical Function       | 100% | 59%   | xx        | -46% |
| Scenario 7: Insufficiently Protected Credentials               | 53%   | 43% | xx        | 64% |
| Scenario 8: Command Injection                                  | -38%    | 88%     | xx                | 38%       |
| Scenario 9: Hardcoded Credentials                              | 52%     | 56%    | xx               | 56%      |
| Scenario 10: Reflected Cross-Site Scripting                    | 52%    | 4%    | xx               | 100%      |
| -  | -  |- | -      |  - | - |
| **Average**                                                    | **11%** | **34%** | **xx**            | **32%**   |

#### Gemini 2.0 Flash

| Scenario                                                       | Idea 1 (NEP) | Idea 2 (CoT) | Idea 3 (FT) | Idea 4 (MP) |
|----------------------------------------------------------------|--------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | 65%    | 65%    | 72%     | 76% |
| Scenario 2: SQL Injection                                      | 46%      | -21%     | 78%     | 75%  |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 8%     | 5%  | 70%  | 28%  |
| Scenario 4: Dangerous File Type                                | No Change     | No Change  | No Change  | No Change |
| Scenario 5: Unsafe Deserialization                             | 13%     | 13%     | 22%     | 13% |
| Scenario 6: Missing Authentication for Critical Function       | No Change | No Change | No Change | No Change |
| Scenario 7: Insufficiently Protected Credentials               | 27%    | -37%  | 57%    | 37%  |
| Scenario 8: Command Injection                                  | 19%     | 76%     | 61%    | 98%  |
| Scenario 9: Hardcoded Credentials                              | -41%      | 4%     | 100%    | 44% |
| Scenario 10: Reflected Cross-Site Scripting                    | 100%   | 83%    | 100%      | 94% |
| -  | -  |- | -      |  - |
| **Average**                                                    | **30%**  | **24%** | **70%** | **58%** |

#### DeepSeek R1 32B

| Scenario                                                       | Idea 1 (NEP) | Idea 2 (CoT) | Idea 3 (FT) | Idea 4 (MP) |
|----------------------------------------------------------------|--------|--------|--------|--------|
| Scenario 1: Path Traversal                                     | 37%     | 36%     | 92%     | 68% |
| Scenario 2: SQL Injection                                      | 24%   | 57%     | 85%      | 43% |
| Scenario 3: Exposure of Sensitive Information to Unauthorized Actor | 8%     | 63% | 69%   | 60% |
| Scenario 4: Dangerous File Type                                | No Change | No Change     | No Change | No Change |
| Scenario 5: Unsafe Deserialization                             | -4%    | 24%     | 100%     | 65% |
| Scenario 6: Missing Authentication for Critical Function       | No Change | No Change | No Change | No Change |
| Scenario 7: Insufficiently Protected Credentials               | 10%    | 1%     | 59%     | 25% |
| Scenario 8: Command Injection                                  | -4%    | 26%     | 67%     | 22% |
| Scenario 9: Hardcoded Credentials                              | 23%    | -1%  | 75% | -2% |
| Scenario 10: Reflected Cross-Site Scripting                    | -86%    | 20%     | 100%     | 2% |
| -  | -  |- | -      |  - |
| **Average**                                                    | **1%**  | **28%** | **81%** | **35%** |

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

## Average Improvement For Each Idea Across All Scenarios Per Model (Java)
The below table displays a comparison between the percentage of samples in which each idea removed security weaknesses in comparison to the raw samples for each model.

The percentage improvement is simply calculated based on how many samples contain CWEs when using each idea versus the original raw output. Then, the percentage-change formula is used to calculate the percentage improvement:

  $S_{\text{raw}}$ = The number of samples (out of 10) that contain one or more weakness in the original raw output.

  $S_{\text{idea}_j}$ = The number of samples (out of 10) that contain one or more weakness after applying the idea ($j$).

$\Delta_j = \frac{S_{\text{raw}} - S_{idea_j}}{S_{\text{raw}}} \times 100\%$





<br><br>

| **Idea**  | **GPT 4.1**         | **Gemini 2.0 Flash** | **o4-mini** | **DeepSeek R1 Distill 14B** |
|-----------|---------------------------|------------------------------------|---------------------------|------------------------------------------|
| **Idea 1 (Negative Example Prompting)** | 2%                        | 7%                                | 10%                       |             -7%                          |
| **Idea 2 (Chain-Of-Thought Prompting)** | 21%                        | 9%                                | 9%                       |            15%                         |
| **Idea 3 (Fine-Tuning)** | 39%                        | 28%                               |          52%               |                                 xx       |
| **Idea 4 (Meta Prompting)** | 32%                        | 30%                               |          3%               |                                 13%       |
