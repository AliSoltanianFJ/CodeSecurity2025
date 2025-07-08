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
