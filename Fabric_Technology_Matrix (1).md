# Fabric Technology Matrix

| Technology | Role in Fabric | Key Benefit |
|-------------|----------------|--------------|
| **Polyglot API (Java / Python / CLI)** | Unified developer interface for building and consuming Fabric data and services | Consistent API design and tooling across languages; enables cross-language interoperability and automation |
| **AMPS** | High-performance event bus | Reliable SOW topics, metadata caching, and real-time updates |
| **gRPC** | Cross-language RPC transport | Service-to-service APIs with strong typing and streaming support |
| **Arrow Flight** | Tabular data transport | Zero-copy columnar exchange, scalable analytics integration |
| **S3** | Object storage backend | Unified local + cloud storage with schema-aware data access |
| **Iceberg** | Table format for S3 | Schema evolution, ACID operations, and partitioned analytics |
| **Data Lakes (GMA, FaaST, Polaris)** | Enterprise data lake integration layer | Allows unified access to data across Impala, Kudu, HDFS, Kafka, and HBase |
| **OpenShift** | Container platform | Autoscaling, blue/green deployments, and regional resilience |
| **OTel (OpenTelemetry)** | Observability framework | Unified tracing, metrics, and logs across Fabric services |
| **3Forge** | Visualization and UI framework | Enables real-time visualization of application and user data across Fabric services |
| **Copilot / AI Assist** | Developer acceleration | Auto-generation of schemas, configs, and test harnesses |
| **MCP (Model Collaboration Platform)** | Shared AI and analytics integration layer | Enables collaborative model development, testing, and orchestration within Fabric’s data ecosystem |
