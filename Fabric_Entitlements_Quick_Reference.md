# Fabric Entitlements Architecture – Quick Reference

| Component | High-Level Role | In Fabric Entitlements Context | Key Benefits |
|------------|----------------|--------------------------------|---------------|
| **Fabric SDK** | Unified, cross-platform API layer (Java, Python, CLI) | Orchestrates data access and entitlement enforcement, and holds all dataset schemas for consistent validation and cross-language access | One SDK for all runtimes; centralized schema and policy management; consistent logic everywhere |
| **Apache Arrow** | Open in-memory columnar data format | Provides a shared, high-performance data representation for all data sources | Zero-copy, cross-language interoperability; efficient processing |
| **Apache Gandiva** | JIT compiler for Arrow expressions using LLVM | Executes entitlement filters and column masks directly on Arrow data | High-performance, vectorized enforcement at runtime |
| **Apache Substrait** | Open standard for portable query and policy plans | Defines entitlement rules (filters, joins, masks) in a portable, language-neutral format | “Write once, enforce anywhere” across all Fabric runtimes |

---

## How It Works – Simplified Flow

1. **Policy Definition:**  
   - Entitlement rules are written once in **Substrait** (e.g., `user.region = dataset.region`).  
   - Stored and versioned in Fabric metadata.

2. **Data Access:**  
   - Fabric SDK retrieves data from any connected source and normalizes it into **Arrow** batches.  
   - All dataset schemas are managed centrally within the SDK to ensure compatibility across runtimes.

3. **Policy Enforcement:**  
   - Fabric compiles the Substrait-defined rules into **Gandiva** expressions.  
   - Gandiva applies filters and masks in memory on Arrow data, enforcing row- and column-level entitlements.

4. **Result Delivery:**  
   - The filtered data is returned through Fabric APIs or Arrow Flight endpoints with consistent semantics across Java, Python, and C++ runtimes.

---

### In One Line
> **Fabric unifies Arrow, Gandiva, and Substrait to deliver fast, consistent, schema-aware row-level entitlement enforcement across all runtimes.**
