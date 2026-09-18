# Fine-Tuning Improvement and Generalisation Gap

> **Generalisation gap:** The difference in improvement from fine-tuning between original scenarios and new scenarios. A **positive generalisation gap** indicates that fine-tuning resulted in less improvement in the new scenarios than in the original scenarios, suggesting a **worse outcome on the new scenarios**. A **negative generalisation gap** indicates that fine-tuning resulted in greater improvement in the new scenarios than in the original scenarios, suggesting **better performance on the new scenarios**. A gap of **0%** indicates equal improvement across the original and new scenarios.

## Python

### GPT-4.1

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 46% | 40% | Scenario 11 | 100% | 100% | Scenario 11 | -54% | -60% |
| Scenario 2 | 76% | 67% | Scenario 12 | 50% | 50% | Scenario 12 | 26% | 17% |
| Scenario 7 | 100% | 100% | Scenario 17 | 100% | 100% | Scenario 17 | 0% | 0% |
| **Total** | **74%** | **69%** | **Total** | **83%** | **83%** | **Total** | **-9%** | **-14%** |

### o4-mini

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 87% | 80% | Scenario 11 | 83% | 83% | Scenario 11 | 4% | -3% |
| Scenario 2 | 82% | 71% | Scenario 12 | 64% | 67% | Scenario 12 | 18% | 4% |
| Scenario 7 | No Change | No Change | Scenario 17 | 100% | 100% | Scenario 17 | 0% | 0% |
| **Total** | **85%** | **76%** | **Total** | **82%** | **83%** | **Total** | **7%** | **0%** |

### DeepSeek R1 32B

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 94% | 88% | Scenario 11 | 67% | 67% | Scenario 11 | 27% | 21% |
| Scenario 2 | 79% | 80% | Scenario 12 | 100% | 100% | Scenario 12 | -21% | -20% |
| Scenario 7 | 100% | 100% | Scenario 17 | 100% | 100% | Scenario 17 | 0% | 0% |
| **Total** | **91%** | **89%** | **Total** | **89%** | **89%** | **Total** | **2%** | **0%** |

## JavaScript

### GPT-4.1

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 71% | 40% | Scenario 11 | 77% | 75% | Scenario 11 | -6% | -35% |
| Scenario 2 | 74% | 40% | Scenario 12 | 59% | 56% | Scenario 12 | 15% | -16% |
| Scenario 7 | 73% | 40% | Scenario 17 | 38% | 36% | Scenario 17 | 35% | 4% |
| **Total** | **73%** | **40%** | **Total** | **58%** | **56%** | **Total** | **15%** | **-16%** |

### o4-mini

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 41% | 0% | Scenario 11 | 66% | 67% | Scenario 11 | -25% | -67% |
| Scenario 2 | 58% | 0% | Scenario 12 | 59% | 44% | Scenario 12 | -1% | -44% |
| Scenario 7 | 36% | 0% | Scenario 17 | 25% | 30% | Scenario 17 | 11% | -30% |
| **Total** | **45%** | **0%** | **Total** | **50%** | **47%** | **Total** | **-5%** | **-47%** |

### DeepSeek R1 32B

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 58% | 0% | Scenario 11 | 75% | 75% | Scenario 11 | -17% | -75% |
| Scenario 2 | 73% | 70% | Scenario 12 | 77% | 75% | Scenario 12 | -4% | -5% |
| Scenario 7 | 81% | 40% | Scenario 17 | 100% | 100% | Scenario 17 | -19% | -60% |
| **Total** | **71%** | **37%** | **Total** | **84%** | **83%** | **Total** | **-13%** | **-47%** |

## Go

### GPT-4.1

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 100% | 100% | Scenario 11 | 67% | 67% | Scenario 11 | 33% | 33% |
| Scenario 2 | No Change | No Change | Scenario 12 | No Change | No Change | Scenario 12 | 0% | 0% |
| Scenario 7 | 100% | 100% | Scenario 17 | 100% | 100% | Scenario 17 | 0% | 0% |
| **Total** | **100%** | **100%** | **Total** | **84%** | **84%** | **Total** | **11%** | **11%** |

### o4-mini

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 100% | 100% | Scenario 11 | 100% | 100% | Scenario 11 | 0% | 0% |
| Scenario 2 | 100% | 100% | Scenario 12 | No Change | No Change | Scenario 12 | 0% | 0% |
| Scenario 7 | -30% | -25% | Scenario 17 | 100% | 100% | Scenario 17 | -130% | -125% |
| **Total** | **57%** | **58%** | **Total** | **100%** | **100%** | **Total** | **-43%** | **-42%** |

### DeepSeek R1 32B

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 100% | 100% | Scenario 11 | 77% | 77% | Scenario 11 | 23% | 23% |
| Scenario 2 | No Change | No Change | Scenario 12 | 100% | 100% | Scenario 12 | 0% | 0% |
| Scenario 7 | 100% | 100% | Scenario 17 | 34% | 33% | Scenario 17 | 66% | 67% |
| **Total** | **100%** | **100%** | **Total** | **70%** | **70%** | **Total** | **30%** | **30%** |

## Java

### GPT-4.1

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 43% | 43% | Scenario 11 | 63% | 64% | Scenario 11 | -20% | -21% |
| Scenario 2 | 73% | 0% | Scenario 12 | 55% | 50% | Scenario 12 | 18% | -50% |
| Scenario 7 | 80% | 0% | Scenario 17 | 59% | 54% | Scenario 17 | 21% | -54% |
| **Total** | **65%** | **14%** | **Total** | **59%** | **56%** | **Total** | **6%** | **-42%** |

### o4-mini

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 82% | 67% | Scenario 11 | 44% | 46% | Scenario 11 | 38% | 21% |
| Scenario 2 | 61% | 0% | Scenario 12 | 63% | 58% | Scenario 12 | -2% | -58% |
| Scenario 7 | 75% | 0% | Scenario 17 | 70% | 67% | Scenario 17 | 5% | -67% |
| **Total** | **73%** | **22%** | **Total** | **59%** | **57%** | **Total** | **14%** | **-35%** |

### DeepSeek R1 32B

| **Original Scenario** | | | **New Scenario** | | | **Generalisation Gap** | | |
|---|---|---|---|---|---|---|---|---|
| | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** | | **Severity Improvement** | **No. of CWEs Improvement** |
| Scenario 1 | 92% | 80% | Scenario 11 | 66% | 67% | Scenario 11 | 26% | 13% |
| Scenario 2 | 85% | 50% | Scenario 12 | 58% | 54% | Scenario 12 | 27% | -4% |
| Scenario 7 | 59% | 30% | Scenario 17 | 58% | 53% | Scenario 17 | 1% | -23% |
| **Total** | **79%** | **53%** | **Total** | **61%** | **58%** | **Total** | **18%** | **-5%** |