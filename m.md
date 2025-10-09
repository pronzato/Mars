# Fabric Foundation Services Roadmap (18-Month Plan)

**Audience:** Senior Leadership Team  
**Tone:** Technical Delivery & Execution Focus  
**Owner:** [Your Name / Platform Engineering Lead]  
**Date:** [Month Year]

---

## 1. Objective & Context

- Deliver a unified, modern foundation for **Reference Data**, **Positions**, **Entitlements**, and **Order State** services.  
- Leverage **Fabric** to create a consistent data, entitlement, and API layer across all foundational domains.  
- Build on the strengths of existing platforms while introducing a **path to greater scalability, consistency, and observability**.  
- Ensure a **smooth migration journey** that maximizes existing investments and minimizes disruption.

**Key Goals**
- Strengthen architectural consistency and reduce duplication of effort.  
- Enable scalable, high-availability, polyglot APIs for both existing and new use cases.  
- Integrate seamlessly with enterprise data platforms (**PPP**, **Polaris**, **GMA**) while maintaining domain autonomy.  
- Deliver globally consistent services that are regionally autonomous (AMRS, EMEA, APAC).

---

## 2. Roadmap Overview (18 Months)

| Phase | Timeline | Focus | Key Deliverables |
|-------|-----------|-------|------------------|
| **Phase 1: Foundation** | Q1–Q2 | Establish Fabric Core + SDLC | Core Fabric platform, reference integration, CI/CD automation |
| **Phase 2: Integration & Enablement** | Q3–Q4 | Reference Data & Positions integration | Unified APIs, OpenShift deployments, Fabric replication |
| **Phase 3: Expansion** | Q1–Q2 (Next Year) | Entitlements & Order State enhancement | Unified Entitlements API, Order State scalability |
| **Phase 4: Optimization** | Q3–Q4 (Next Year) | Global rollout + observability | Active-active HA, OpenTelemetry, AI automation |

---

## 3. Architecture Principles

- **Federated by Design:** Global footprint (AMRS, EMEA, APAC) with local autonomy and resilience.  
- **Composable Fabric Layer:** A consistent data and API abstraction that complements existing systems.  
- **Open Standards:** gRPC, Arrow Flight, S3, Iceberg, JSON/Parquet for broad interoperability.  
- **Flexible Deployment:** Bare metal, VM, or OpenShift — same architecture everywhere.  
- **Observable by Default:** OpenTelemetry-based tracing and metrics integrated into all services.  
- **Future-Ready, Not Vendor-Tied:** Modular design that supports evolving UI and backend technologies.

---

## 4. Foundation Service Areas

### 4.1 Reference Data

**Current Landscape**
- Several mature implementations (REIT, RD2, RD3) serving critical production workloads.  
- Strong domain knowledge embedded across teams; opportunity to unify delivery and monitoring.

**Roadmap Objectives**
- Design a **mission-critical, scalable** Reference Data service using Fabric’s unified API layer.  
- Introduce **automated data quality, lineage tracking, and monitoring** as first-class features.  
- Integrate with the **enterprise Reference Data platform (PPP)** while extending to additional vendor and derived datasets.  
- Provide **adoption pathways** and compatibility layers for current client applications.

**Milestone Deliverables**
- Unified Reference Data API (Fabric-based).  
- Data quality and lineage metrics service.  
- Composition and vendor dataset integration pipelines.

---

### 4.2 Positions

**Current Landscape**
- Strong foundational capabilities in place with well-understood APIs and requirements.  
- Opportunities exist to enhance **performance, deployment automation, and entitlement integration**.

**Roadmap Objectives**
- Complete **OpenShift deployment** for regional scalability and reliability.  
- Optimize **Fabric Position API** for high-throughput, low-latency workloads.  
- Extend support for **aggregated and virtual positions** with entitlement overlays.

**Milestone Deliverables**
- Fabric Position Service with Arrow Flight API.  
- Unified entitlement-aware position model.  
- Latency and throughput benchmarking toolkit.

---

### 4.3 Entitlements

**Current Landscape**
- Multiple active solutions (EES, enterprise wrappers) supporting identity and access workflows.  
- Opportunity to consolidate interfaces and extend functionality for business-level entitlements.

**Roadmap Objectives**
- Build a **Fabric Entitlements API** that harmonizes enterprise IAM integration.  
- Enable **application-level delegation** — a single, standardized API for system-to-system authorization.  
- Support **fine-grained data-level entitlements** (row and column masking) via Apache Ranger or equivalent.  
- Establish a **business-aware data model** for entitlements relationships.

**Milestone Deliverables**
- Unified Entitlements Service integrated with enterprise IAM.  
- Event and audit capture layer via Fabric.  
- Entitlements registry managed through the Fabric Schema Registry.

---

### 4.4 Order State

**Current Landscape**
- A robust, high-volume Order State service built atop the existing OS platform.  
- Recent performance improvements demonstrate progress; next step is scaling for expanded use cases.

**Roadmap Objectives**
- Focus first on **stability, resilience, and horizontal scalability**.  
- Explore a **Fabric-based streaming interface** (FlightSQL + AMPS bridge) for entitlement-aware access.  
- Introduce **engine recovery** capabilities for stateless trading engines.  
- Enhance **multi-system visibility**, linking algorithmic and manual trading activities.

**Milestone Deliverables**
- Fabric Order State service with high-volume ingestion and recovery support.  
- Enhanced entitlement model for Order State data.  
- Unified event API for related activity tracking.

---

## 5. Roadmap Structure & Delivery Framework

- **Automated SDLC:** End-to-end CI/CD pipelines integrated with Jira, testing, and deployment.  
- **C1 vs D1 Separation:** Clear delineation between container management and business logic.  
- **Market Data Connectivity:** Standardized integration patterns and feed enablement guidelines.  
- **Polyglot Fabric API:** Unified access for Python, Java, C++, and R clients.  
- **UI Abstraction:** Fabric decouples data delivery from UI technologies (3Forge, Dash, React).

---

## 6. Cross-Cutting Technical Enablers

### 6.1 Infrastructure
- Global active-active topology across AMRS, EMEA, and APAC.  
- Rolling updates and OpenShift-based autoscaling.  
- Hybrid deployment model to coexist with current compute environments.  
- Built-in observability using OpenTelemetry.

### 6.2 Data Layer
- Regional S3 integration with Fabric-managed cross-region replication.  
- Native integration with enterprise data platforms (**PPP**, **Polaris**, **GMA**).  
- Support for Arrow, Parquet, JSON, and CSV formats.  
- Iceberg-based time-travel and historical access.

### 6.3 API & Compute
- High-performance gRPC + Arrow Flight APIs.  
- SDKs for Python, Java, and C++.  
- Fine-grained access control and entitlement enforcement.  
- Modular architecture to evolve alongside compute frameworks.

### 6.4 UI Layer
- Strategic platform: **3Forge**.  
- Alternative enablement: Dash, React (via Fabric abstraction).  
- Unified data egress ensures UI agility without data duplication.

---

## 7. AI & Automation

**Vision:** Make the new platform not only faster and more scalable, but also **intelligent and self-assisting** from day one.

### 7.1 Model-Context Protocol (MCP) Integration
Fabric MCP embeds context-aware AI directly into the platform.

**Benefits**
- **Developer Productivity:** Auto-generate service templates, schema definitions, and test scaffolds from schema metadata.  
- **Self-Documenting APIs:** Fabric schemas and lineage become conversational — developers and support can query them using natural language.  
- **Automated Troubleshooting:** AI can analyze OpenTelemetry traces and suggest remediation actions.  
- **Data Observability:** AI-driven anomaly detection in Reference Data and Positions streams.  
- **Accelerated Onboarding:** New teams can query MCP to understand service contracts and dependencies.

### 7.2 CoPilot Integration
- Use **Fabric Chronicle** with CoPilot to replay production data for regression testing and debugging.  
- Auto-generate boilerplate code, integration examples, and UIs (e.g., trading dashboards).  
- CoPilot-driven **“explain this API”** or **“generate test for this schema”** functionality embedded in dev tooling.  
- Reduces manual scripting and testing cycles, improving time-to-production.

**Outcome**
Fabric evolves into a **self-explanatory, self-observing, and semi-autonomous platform** that lowers maintenance burden while improving delivery velocity.

---

## 8. Reducing Technical Debt & Simplifying the Landscape

**Current Challenge**
- Multiple parallel systems across domains with overlapping data pipelines, APIs, and entitlement models.  
- Inconsistent deployment models and versioning standards increase operational overhead.

**Fabric’s Simplification Approach**
1. **Unified Data Contract:** Standard schema registry and API model across all domains.  
2. **One Access Layer:** Polyglot Fabric API replaces domain-specific client libraries.  
3. **Centralized Observability:** OpenTelemetry integration across all services and regions.  
4. **Config-Driven Deployments:** Consistent SDLC pipelines eliminate ad-hoc setups.  
5. **Entitlement Consolidation:** Single entitlement API replacing multiple IAM integrations.  
6. **Code Reuse:** Shared service templates and builders reduce boilerplate duplication.

**Outcome**
- Fewer codebases to maintain.  
- Simplified onboarding for new developers and applications.  
- Predictable deployment, monitoring, and debugging experience across all services.  
- Reduced operational risk and ongoing maintenance cost.

---

## 9. Adoption & Evolution Strategy

- **Collaborative migration model:** Coexistence of current and Fabric-based services.  
- **Compatibility adapters:** Minimize client code changes during onboarding.  
- **Progressive rollout:** Prioritize value-driven use cases per domain.  
- **Shared learning:** Documentation, playbooks, and cross-team enablement sessions.  
- **Demonstrable value early:** Positions and Reference Data as near-term integration candidates.

---

## 10. Risks & Mitigations

| Consideration | Potential Impact | Mitigation |
|----------------|------------------|-------------|
| Integration complexity across systems | Medium | Phased rollout + compatibility tooling |
| Skill development on Fabric platform | Medium | Training and enablement sessions |
| Cross-region governance | Medium | Federated coordination with regional leads |
| Competing initiatives | Medium | Alignment through enterprise architecture council |
| Resourcing | High | Clear milestones and measurable business outcomes |

---

## 11. Next Steps

1. Confirm sequencing and resource alignment for Q1 kickoff.  
2. Begin Fabric core and SDLC automation rollout.  
3. Launch joint migration pods for Reference Data and Positions.  
4. Develop detailed Entitlements and Order State design blueprints.  
5. Establish cross-domain governance cadence.

---

## 12. Summary

- Fabric provides a **unified, modern foundation** that enhances and extends existing systems.  
- The roadmap builds on **current strengths**, introducing observability, AI-assisted automation, and scalability.  
- The 18-month plan ensures **measured, low-risk evolution** toward a simplified, future-ready platform.  
- Together, we are positioning our foundation services for the next decade of data-driven growth.

