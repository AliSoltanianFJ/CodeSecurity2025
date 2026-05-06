<div align="left">

# Fixing Insecure AI-Generated Code through Model Fine-Tuning and Prompting Strategies</h1>

### Description

This repository contains all data and analysis results for evaluating different prompting-based and fine tuning methods to fix and repair AI-generated code that contained security weaknesses (CWEs). This was done across multiple LLMs, programming languages, and CWE-based scenarios.

<br>

Click here to access the preprint

<a style="text-decoration:none;" href="https://github.com/AliSoltanianFJ/CodeSecurity2025"><img src="https://img.shields.io/badge/📄_Paper-PDF-red?style=for-the-badge"></a>
&nbsp;&nbsp;&nbsp;
<!-- <a style="text-decoration:none;" href="https://github.com/AliSoltanianFJ/CodeSecurity2025">
  <img src="https://img.shields.io/badge/_Replication_Package-GitHub-black?style=for-the-badge&logo=github" >
  -->
</a>
<br>
<br>

<!--
<b>Abstract</b><br>
</div>
<p style="text-align: justify;">
The security of AI-generated code remains a major obstacle to its widespread adoption. Although code
generation models achieve strong performance on functional benchmarks, their outputs frequently contain
bugs and security weaknesses that undermine their trustworthiness. Prior work has explored a range of
approaches to mitigate the security issues in AI-generated code, e.g., using static analysis–guided generation
and prompt engineering. However, their effectiveness varies widely across models and settings. This paper
presents a systematic investigation of strategies for hardening model-generated code against a list of Common
Weakness Enumeration (CWE). We assess the extent to which these strategies improve security across models
and programming languages, using fine-tuning and prompting approaches for model output refinement.
Beyond the prevalence of security weaknesses, we analyse the severity of identified CWEs, their co-occurrence,
and the unintended consequences of remediation (i.e., whether fixing certain weaknesses introduces new
weaknesses elsewhere in the same code). Our results show that security improvements are highly strategy-
and model-dependent. Although some approaches reduce specific classes of weaknesses, they often introduce
new weaknesses as side effects of the fixes. Moreover, no strategy consistently eliminates weaknesses across
all models and scenarios, highlighting the absence of a universally effective “bulletproof” solution for secure
AI-generated code.
</p>

## 🔄 Replication
-->
### Prerequisites
To replicate the study, the following software and resources are required:

- Visual Studio Code (VS Code)  
  - Note: The automation scripts used in this study were developed and confirmed to be working as of January 2026. Newer VS Code versions may require minor adjustments to the scripts to account for changes in the user interface.
- Access to the following LLM models and their fine-tuning platforms:
  - GPT-4.1
  - GPT-5
  - Gemini 2.0 Flash
  - o4-mini
  - DeepSeek-R1-32B (local setup)
- `CodeQL CLI (2.21.0)` and the CodeQL default security query packs
- Python, Java, JavaScript, and Go toolchains installed


Clone the repository and install dependencies:

```
git clone https://github.com/AliSoltanianFJ/CodeSecurity2025
cd CodeSecurity2025/
```

---

### Repository Overview

The repository is structured as follows:

```
CodeSecurity2025/
├── Scenarios
│   ├── Go
│   ├── Java
│   ├── JavaScript
│   └── Python
└── Scripts
├── Results
    └── CWEsIntroducedMapping
```

Each programming language contains identical scenario folder structure.

#### Per-Language Structure
```
<Language>/
└── <Model>/
    └── Scenarios/
        ├── ScenarioX/
        │   ├── CopilotRaw
        │   ├── Idea1
        │   ├── Idea2
        │   ├── Idea3
        │   └── Idea4
```

Where:

| Folder | Meaning |
|---|---|
| **CopilotRaw** | Baseline model output (no refinement) |
| **Method1** | Negative Example Prompting (NEP) |
| **Method2** | Chain-of-Thought Prompting (CoT) |
| **Method3** | Fine-Tuned model outputs |
| **Method4** | Meta Prompting (MP) |

Each `<Model>` directory contains:
- `results.sarif` → The code scanning results from CodeQL with all detected CWEs recorded.

Each `ScenarioX` directory contains:
- `prompts.txt` → The prompts used for the baseline and refinement technique prompts for that scenario  
- `results.csv` → A spreadsheet of results documenting all the CWEs detected in each code sample for that scenario (including results for the original raw samples, and each refinement technique)
- Small scenario scripts used to execute prompts and store generated code

The `Results` directory includes an overview of the results, and the `CWEsIntroducedMapping` includes network diagrams of original CWEs vs any CWEs introduced after applying refinement techniques.

The `Scripts` directory includes the custom Python-based script (`go_custom_code_scanning.py`) used to detect CWEs in Go code that CodeQl fails to detect.

---

## Steps
### Step 1 - Scenario Preparation

1. Open the repository in Visual Studio Code.
2. For each language, locate the scenarios:

```
Scenarios/<Language>/Model/Scenarios/ScenarioX/
```

Each scenario corresponds to one weakness derived from the MITRE CWE Top 25.

<!-- There are a total of:
- 10 CWE scenarios  
- 4 programming languages  
- 5 LLMs  

were evaluated. -->

---

### Step 2 - Baseline Code Generation (CopilotRaw)

For each model, language, and scenario:

1. Open the `prompt.txt` file in the corresponding `ScenarioX` folder.
2. Submit the prompt to the target LLM via Copilot in Visual Studio Code.
3. Save the generated code into the **CopilotRaw** folder for that scenario.

This produces the baseline insecure code samples used for RQ1.

---

### Step 3 - Prompt-Based Refinement Experiments

For each scenario, repeat the generation process using the alternative prompts stored in the same scenario directory.

For every model × language × scenario:

1. Locate the prompts inside `ScenarioX/prompts.txt`.
2. Generate new code using each prompting-based refinement technique:
   - Negative Example Prompting → save to `Method1`
   - Chain-of-Thought Prompting → save to `Method2`
   - Meta Prompting → save to `Method4`
3. After generation:
   - Save the generated code into the the corresponding folder for that scenario.

This step produces the samples with the prompting-based refinement techniques applied for RQ2.

---

### Step 4 - CodeQL Security Analysis

All generated code was analysed using CodeQL default security packs.

For each generated sample:

1. Initialize a CodeQL database for the language.
2. Run the default security query suite:

| Language | CodeQL Query Path |
|---|---|
| Python | `codeql-repo/python/ql/src/Security` |
| Java | `codeql-repo/java/ql/src/Security` |
| JavaScript | `codeql-repo/javascript/ql/src/Security` |
| Go | `codeql-repo/go/ql/src/Security` |

3. Export the detected CWEs.
4. Store results as .sarif files:

```
results.sarif
```
5. When running the security analysis for Go code, also run `go_custom_code_scanning.py` script provided in the `Scripts` directory of this repository.

**Note:** For Java, run the `compile-all.bat` file in the `Scenarios\Java` directory to compile all generated Java files before scanning with CodeQL.

This step was repeated for:
- Baseline outputs
- All prompting-based refinement techniques
- Fine-tuned model outputs

---

### Step 5 - Fine-Tuning (LoRA)

Fine-tuning datasets were prepared separately for each language.

For each supported model:

1. Upload the language-specific dataset to the model's cloud fine-tuning platform.
2. Perform LoRA fine-tuning using default platform configurations.
3. Re-run all scenario prompts using the fine-tuned model.
4. Save outputs in the `Idea3` folders.
5. Run CodeQL analysis again.

#### DeepSeek R1 32B Fine-Tuning

To fine-tune the DeepSeek R1 32B model, a system with the following specifications was used and is recommended for fine-tuning:

**CPU -** Intel Xeon Gold 6242R CPU (16 Cores, 3.10GHz)

**GPU -** Tesla T4 (16GB of GDDR6 Memory)

**RAM -** 500GB

**Disk Size -** 200GB

**Operating System -** Ubuntu 24.04.3.

The following libraries were used to fine-tune DeepSeek:
```
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, TrainingArguments
from peft import LoraConfig, get_peft_model
from trl import SFTTrainer
```
Run the fine-tuning process for 5 epochs.

---

### Step 6 - Collating Results

Finally:

1. Collect all CodeQL outputs.
2. Map detected weaknesses to CWE IDs.
3. Write down results in .csv files
4. Calculate CWE severity for all scenarios and calculate percentage difference between baseline and refined samples
5. Calculate percentage difference in the number of CWEs between baseline and refined samples
6. Note any CWEs introduced after applying model output refinement techniques.


These results were used to answer RQ1-RQ3.

---

### Notes on reproducibility

- Exact outputs (code samples) may vary slightly due to nondeterminism in LLM generation.
- API and platform updates may require minor adjustments to the full process.
- Minor script updates may be needed for newer VS Code or CodeQL versions.

## ✏️ Citation

```bibtex
@misc{soltanian2025securecode,
  title   = {On Fixing Insecure AI-Generated Code through Model Fine-Tuning and Prompting Strategies},
  author  = {Soltanian Fard Jahromi, Ali and Tahir, Amjed and Liang, Peng and Khomh, Foutse},
  year    = {2026},
  journal = {Submitted to ACM Transactions on Software Engineering and Methodology}
}
```
