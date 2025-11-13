h1. AMPS vs. Fabric Flight SQL – Zero-Copy Data Movement

h2. 1. Overview
Fabric supports two complementary data movement technologies:

|| Transport || Optimized For || Pattern ||
| *AMPS* | Real-time streaming, low-latency fan-out | Event bus + incremental updates |
| *Flight SQL* | High-throughput, zero-copy table movement | Bulk reads, analytics, batch |

AMPS remains our primary *streaming* system.  
Flight SQL becomes our primary *table movement / analytics* system.

----

h2. 2. Why AMPS alone isn’t enough for modern workloads
AMPS excels at:
* Real-time updates
* Market data streams
* High fan-out subscriptions
* Operational events

But for *large table reads* (100k–100M+ rows), AMPS has limitations:
* Row-encoded messages (JSON/Protobuf)
* Per-row parsing costs
* Per-language conversion (Python/Java/C++)
* Higher CPU usage
* Lower throughput than columnar engines

This is why AMPS peaks around *~200k–240k rows/sec*.

----

h2. 3. Fabric Flight SQL & Zero-Copy

Fabric uses Apache Arrow + Arrow Flight SQL to enable *zero-copy* columnar movement.

*Zero-copy means*:
* Data is produced once in Arrow’s universal memory layout
* All languages (Python/Java/C++/Rust/Go) read it directly
* No serialization or deserialization
* No per-row decoding
* No duplicated objects
* Much lower CPU usage

Benchmark example:

|| Size || AMPS Max || Flight Max || Flight SQL Max ||
| XL–XXXL | ~200k–240k r/s | ~800k–1.25M r/s | ~0.8M–1.04M r/s |

----

h2. 4. Zero-Copy in Simple Terms

Traditional AMPS SOW flow:
{code}
Network bytes → JSON/Protobuf → language-specific objects → DataFrames
{code}

Zero-copy Flight SQL flow:
{code}
Network bytes → Arrow buffers → Ready-to-use Table
{code}

No parsing.  
No duplicated data.  
Minimal CPU.

----

h2. 5. Benefits for Application Teams

* *Faster analytics & startup times*
* *Lower CPU usage* (no parsing)
* *Simpler codebases*
* *Consistent behavior across Python/Java/C++*
* *Entitlements enforced before data leaves Fabric*

----

h2. 6. When to use AMPS vs Flight SQL

h3. Use AMPS for:
* Streaming updates
* Real-time events
* Market data
* High fan-out delivery

h3. Use Flight SQL for:
* Bulk table reads
* Analytics queries
* Python/ML ingestion
* Inter-service table transfer
* Large refdata/positions/orders snapshots

Technologies are complementary.

----

h2. 7. Full Benchmark Results (Dark Code Block)

{code:theme=Midnight|linenumbers=false|language=bash}
# ===========================
# Fabric Transport Benchmarks
# Rows/sec | MB/sec | p95 Latency
# ===========================

Size   Service       Rows/sec       MB/sec   Latency_p95
-----  ------------  -------------  -------- -------------
small  AMPS                162.1        0.02       108.84
small  FLIGHT              522.5        0.03       339.72
small  FLIGHT_SQL     1,016,044.3      60.84       217.99
small  GRPC               531.9         0.03       984.21

medium AMPS              7,932.6        0.89        48.07
medium FLIGHT           16,479.7        0.99       168.67
medium FLIGHT_SQL    1,039,489.4       62.24       172.09
medium GRPC              5,678.3        0.34       782.16

large  AMPS             20,104.7        2.25         0.00
large  FLIGHT          196,069.8       11.74       189.78
large  FLIGHT_SQL    1,004,390.4       60.14       184.30
large  GRPC            60,200.4         3.60        10.88

xl     AMPS            146,047.1       16.34         0.00
xl     FLIGHT          795,897.5       47.66       290.68
xl     FLIGHT_SQL    1,039,231.3       62.23       178.44
xl     GRPC           135,821.9         8.13         0.16

xxl    AMPS            230,410.4       25.78         0.00
xxl    FLIGHT        1,252,480.4       74.99       110.66
xxl    FLIGHT_SQL      818,883.5       49.03       292.03
xxl    GRPC           174,333.8        10.44         0.13

xxxl   AMPS            241,095.5       26.97         0.00
xxxl   FLIGHT          879,274.4       52.65       253.14
xxxl   FLIGHT_SQL      863,594.4       51.71       295.69
xxxl   GRPC           162,167.6         9.71         0.13
{code}

----

h2. 8. Technical Appendix

*AMPS is not zero-copy because* JSON/Protobuf messages must be parsed and converted into objects by each language runtime.

*Arrow enables zero-copy because* all languages understand the same in-memory columnar structure. Flight/Flight SQL simply pass Arrow buffers directly.

This aligns Fabric with modern data engines such as DuckDB, MotherDuck, Spark, Polars, Pandas, Snowflake, and BigQuery.
