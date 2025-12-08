h2. Methodology – How We Compared AMPS, gRPC, Arrow Flight, and Arrow Flight SQL

This section explains *how* the benchmark numbers were generated so both developers and quants can clearly understand the timing and throughput comparisons across transports.

----

h3. Goals of the Benchmark

We designed the tests to answer three practical questions:

* How much *data per second* can each transport deliver under Fabric-style workloads?
* What *latency* does each transport exhibit under load?
* How do these metrics behave as the dataset size increases from *small → xxxl*?

We were not benchmarking any single line of code.  
We were measuring **end-to-end, real-world behavior** of the transports.

----

h3. Common Test Setup Across All Transports

Each transport (AMPS, gRPC, Flight, Flight SQL) was evaluated under the same conditions:

* Same physical/virtual host class (same CPU, RAM, OS)
* Same JVM version and Fabric build
* Same network environment
* Same schemas and data shapes (refdata, positions, orders)
* Same “t-shirt” dataset sizes:
  **small, medium, large, xl, xxl, xxxl**

For every run:

* The server/service was started cleanly.
* A warm-up period allowed JIT, caches, and TCP to stabilize.
* The steady-state window was measured for throughput and latency.

----

h3. Workloads and T-Shirt Sizes

We used simple but realistic table-shaped datasets:

* Narrow to medium-width rows (refdata)
* Wider rows with mixed numeric/string fields (positions/orders)
* Rows include common fields like IDs, desk, region, instrument, qty, value, etc.

Each dataset was generated in progressively larger "t-shirt sizes":

|| Size || Purpose ||
| small | Highlights startup overhead rather than throughput |
| medium | Begins to show steady-state characteristics |
| large | Realistic snapshot size |
| xl / xxl / xxxl | Sustained streaming that saturates the transport’s pipeline |

At each size, *every* transport received the same logical data volume.

----

h3. Metrics We Captured

For each transport × size, we captured:

* *rows/sec*  
* *MB/sec* (based on uncompressed payload sizes)  
* *latency_p95*  

Additional internal diagnostics (CPU %, GC behavior, error counts) were collected but not shown in the comparison table.

Rows/sec and MB/sec measure *throughput*.  
p95 latency measures the *tail end of delays*—the part most users actually feel.

----

h3. Equalizing Conditions for Fair Comparison

To ensure that comparisons were fair:

* **Same schema** for all transports  
* **Same logical dataset** for small → xxxl  
* **Same access semantics**:
  - AMPS → SOW snapshot of all rows  
  - gRPC → streaming call returning all rows  
  - Flight → Arrow RecordBatch stream of all rows  
  - Flight SQL → SELECT returning all rows

* **AMPS JSON → POJO decoding applied**  
  - By default, AMPS delivers JSON strings.  
  - To make AMPS comparable to gRPC and Arrow (which deliver typed data), we added a consumer-side decoder that converts JSON into typed objects before measurement.  
  - This reflects *real-world* AMPS usage correctly.

* **Steady-state only**  
  - We discarded warm-up periods for fairness.  
  - Throughput and latency were measured only during the stable portion of the run.

We did *not* micro-optimize any transport — we used reasonable, Fabric-appropriate defaults.

----

h3. How Each Transport Was Exercised

h4. AMPS

* Server: AMPS broker with SOW topic containing the dataset
* Client flow:
  - Snapshot (or snapshot+subscribe)
  - Parse JSON → POJO/typed data
  - Count rows as they arrive
  - Measure time from first row to last row

*Rows/sec = total_rows / measurement_window_seconds*

----

h4. gRPC

* Server: Streaming RPC that emits one message per row
* Client:
  - Reads and deserializes Protobuf (or equivalent)
  - Tracks per-message timestamps
  - Computes p95 latency from these

----

h4. Arrow Flight

* Server: doGet ticket streams Arrow RecordBatches
* Client:
  - Reads each batch
  - Counts rows in the batch
  - Records batch arrival times

Latency was measured per batch.

----

h4. Arrow Flight SQL

* Server: Flight SQL endpoint backed by DuckDB + Arrow
* Client:
  - Issues SELECT for the entire dataset
  - Reads Arrow RecordBatches from the result stream
  - Counts rows and timestamps batch arrivals

Latency was measured either per batch or per query depending on the test type.

----

h3. How Rows/sec and MB/sec Were Computed

{code:theme=Midnight|language=bash}
rows_per_sec = total_rows_delivered / measurement_duration_seconds
{code}

{code:theme=Midnight|language=bash}
mb_per_sec = (total_rows_delivered * bytes_per_row_uncompressed) 
             / (1024 * 1024) 
             / measurement_duration_seconds
{code}

Note: We used *uncompressed row size* for MB/sec so transports could be compared apples-to-apples regardless of internal encoding.

----

h3. How We Measured Latency_p95

Latency differed by transport:

* **AMPS & gRPC:**  
  Timestamp per message → compute distribution → take p95.

* **Flight & Flight SQL:**  
  Timestamp per batch or per entire query, depending on run type.  
  p95 reflects the upper tail of batch delivery times.

For large datasets, p95 latency is effectively:  
“how long until most of my heavy batch data arrives?”

----

h3. Why Throughput Increases with Dataset Size (small → xxxl)

This is expected and driven by:

* **Startup overhead dominates small sizes:**  
  - JIT not hot  
  - TCP slow start  
  - Initial GC passes  
  - Connection setup  
  - Broker/service initialization

* **Bigger workloads produce bigger batches:**  
  - Arrow RecordBatches more efficient at large sizes  
  - AMPS streams stabilize  
  - gRPC pipeline fills  
  - Network buffers reach optimal throughput

* **Systems only reach maximum efficiency under sustained load.**

This is why AMPS goes from ~144 rows/s (small) to ~257,000 rows/s (xxxl).

----

h3. Why Flight SQL Often Outperforms Raw Flight

Flight SQL isn’t just “SQL on top of Flight.”  
It uses a optimized columnar engine (DuckDB) that:

* Produces Arrow batches more efficiently than hand-written Flight servers  
* Uses vectorized execution, SIMD, and tight C++ loops  
* Pushes down filters and projections  
* Produces larger and more uniform batch sizes  
* Minimizes round trips

In many cases, Flight SQL becomes *faster* than raw Flight because the upstream query engine is so efficient.

----

h3. Limitations and How to Interpret Results

These tests intentionally focus on *end-to-end, high-volume reads*.  
They do not benchmark:

* Write speed  
* Tiny point lookups  
* Multi-tenant contention scenarios  
* Network jitter under failure  
* Mixed workloads

Absolute numbers will vary across environments.

**However, the *relative* behaviors hold consistently:**

* AMPS → best for streaming/fan-out  
* gRPC → best for ultra-low-latency control paths  
* Flight → fast, columnar bulk transfer  
* Flight SQL → fastest and most scalable for full-table reads

Use each transport for the scenarios it’s designed for.

