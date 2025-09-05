# AI Code Security Experiment Results (Multi-Language & Multi-Model Analysis)

The below graph shows the improvements of each technique in mitigating security weaknesses in the generated code snippets for each model in each language. The improvements are calculated by the severity of the detected CWEs in the code snippets.

<img width="2326" height="1309" alt="image" src="https://github.com/user-attachments/assets/fa3a1c79-1f88-4ace-84a0-a22397726fcd" />

Three different mitigation techniques were tested:

- Negative Example Prompting: Insecure code samples will be generated based on a certain CWE scenario. The model is then prompted to generate new code samples and is supplied with the bad (insecure) code examples generated from the previous time. This is done to discourage the model from generating samples similar to the insecure samples.

- Chain-of-Thought Prompting: The model is prompted with chain-of-thought prompting to generate the code samples for the CWE scenario.

- Model Fine Tuning: The model is fine-tuned using secure code samples (code samples that do not contain CWEs). The model is then prompting to generate the code samples for the CWE scenario.