# AI Code Security Experiment Results (Multi-Language & Multi-Model Analysis)

The below graph shows the improvements of each technique in mitigating security weaknesses in the generated code snippets for each model in each language. The improvements are calculated by the severity of the detected CWEs in the code snippets.

<img width="2080" height="1333" alt="image" src="https://github.com/user-attachments/assets/12a27068-53f6-4490-a162-56c823c215b5" />

Four different mitigation techniques were tested:

- Negative Example Prompting: Insecure code samples will be generated based on a certain CWE scenario. The model is then prompted to generate new code samples and is supplied with the bad (insecure) code examples generated from the previous time. This is done to discourage the model from generating samples similar to the insecure samples.

- Chain-of-Thought Prompting: The model is prompted with chain-of-thought prompting to generate the code samples for the CWE scenario.

- Model Fine Tuning: The model is fine-tuned using secure code samples (code samples that do not contain CWEs). The model is then prompted to generate the code samples for the CWE scenario.

- Meta Prompting: The model is prompted to generate a "meta prompt" with the goal of generating secure code. The model is then prompted to generate the code samples for the CWE scenario with the meta prompt appended to the original instructions.