h1. Fabric Tapestry – User Guide

This guide covers:
# Creating a new Tapestry application module
# Building datasets using the Flow API
# Creating live and static tables driven by metadata
# Running Tapestry in LOCAL vs SERVICE mode
# Using GitHub Copilot to auto-generate Tapestry configs and screens

---

h2. 1. Creating a New Tapestry App

h3. Step 1 – Add dependency
In your new module’s pom.xml:

{code}
<dependency>
  <groupId>org.pronzato.fabric</groupId>
  <artifactId>fabric-tapestry</artifactId>
</dependency>
{code}

h3. Step 2 – Create YourTapestryServer.java
Your server should:

* Start Javalin
* Configure static resources
* Instantiate:
** UiDataViewRegistry
** UiTableViewRegistry
** UiActionRegistry
** FabricPdp
** SubscriptionBackend
** UiTableExecutionService
* Register everything via:
{code}
UiFrameworkJavalinPlugin.register(app, dvRegistry, tvRegistry, actionRegistry, execService, subscriptionBackend);
{code}

h3. Step 3 – Add HTML routes
Create a class like YourTapestryRoutes that uses the standard layout:

{code}
<div class="fabric-surface fabric-surface-elevated">
  <div class="fabric-table-container" data-table-id="your-table-id"></div>
</div>
{code}

Use renderLayout to keep consistent styling across pages.

---

h2. 2. Creating a Dataset Using the Flow API

Fabric Flow defines extraction → transformation → materialization pipelines.

Example:

{code}
FlowBuilder.newFlow("positions")
  .fromParquet("s3://bucket/positions/*.parquet")
  .materializeAsDataset("positions_dataset")
  .build();
{code}

Dataset metadata is stored under:

*vault/metadata/<group>/datasets/...*

Tapestry will reference the same dataset id in UiDataView.sourceRef.

---

h2. 3. Creating a Live Table

h3. Step 1 – UiDataView
Defines the data source.

{code}
UiDataView view = UiDataView.builder()
  .withId("yourgroup-positions")
  .withTitle("Positions")
  .withSourceType(UiDataSourceType.FLIGHT_SQL_SERVICE) // or LOCAL_DATASET
  .withSourceRef("positions_dataset")                  // dataset id or service SQL
  .withLive(true)
  .build();
{code}

h3. Step 2 – UiTableView
Defines how the UI renders the dataset.

{code}
UiTableView table = UiTableView.builder()
  .withId("yourgroup-positions-table")
  .withDataViewId("yourgroup-positions")
  .withColumns(
      UiColumnDef.of("symbol","Symbol","STRING"),
      UiColumnDef.of("quantity","Qty","DOUBLE"),
      ...
  )
  .withLive(true)
  .withSelectable(true)
  .withShowToolbar(true)
  .build();
{code}

h3. Step 3 – Register in registries

{code}
dvRegistry.register(view);
tvRegistry.register(table);
{code}

---

h2. 4. Running Tapestry in LOCAL vs SERVICE mode

Tapestry supports two execution backends:

* *LOCAL_DATASET:* DuckDB/Parquet → ideal for developer workflows
* *FLIGHT_SQL_SERVICE:* curated Flight SQL services → ideal for UAT/PROD

Switch modes via:

{code}
-Dtapestry.sourceMode=local
-Dtapestry.sourceMode=service
{code}

A pill will appear in the header:
* LOCAL DATASET MODE (green)
* SERVICE MODE (blue)

---

h2. 5. Using Copilot with Tapestry (NEW SECTION)

GitHub Copilot excels with Tapestry because Tapestry is:
* Declarative
* Metadata-driven
* Predictable in structure
* Consistent across applications

h3. Recommended Copilot prompts

*Generate a UiDataView + UiTableView for a new dataset*
{code}
Create a UiDataView and UiTableView in Tapestry for dataset "mygroup-trades".
Use LOCAL_DATASET mode by default.
Match column structure from this parquet schema:
[schema pasted here]
{code}

*Generate a new Tapestry app template*
{code}
Create a new Tapestry app module for group "rates".
Include:
- RatesTapestryServer
- RatesTapestryConfig with three datasets
- Routes with three pages (positions, trades, refdata)
Use TapestryConfig.sourceMode for backend switching.
{code}

*Add actions to an existing table*
{code}
Add row-level and table-level actions to the "rates-trades-table".
Generate UiActionDefs and modify the toolbar.
Use FLOW handlers for production.
{code}

*Extend a table with sorting/filtering*
{code}
Add sorting, filter row, and custom formatting to the "positions" table.
Use currency formatting for PnL.
{code}

h3. How Copilot learns your structure
Copilot sees:
* Registry patterns
* UiDataView/UiTableView builders
* Typical naming conventions
* renderLayout pattern
* dataset discovery APIs

So Copilot can:
* Generate entire configs  
* Generate new screens  
* Expand existing tables  
* Create Flow definitions  
* Wire actions into the UI  
* Produce HTML pages following Tapestry’s design

h3. Copilot workflow
1. Paste a parquet schema or SQL schema.  
2. Ask Copilot to generate UiDataView + UiTableView.  
3. Ask Copilot to generate a page under TapestryRoutes.  
4. Refresh your browser — the dashboard appears immediately.  

This brings dashboard development down to:
→ Seconds of metadata → copy/paste → working UI.

---

h2. 6. Summary

Tapestry gives you:
* A fast way to turn datasets into dashboards  
* Metadata-driven UI generation  
* Service and local execution paths  
* Automatic entitlements and live updates  
* Excellent Copilot support for code generation  

Fabric Tapestry = *developer velocity* + *production stability*.
