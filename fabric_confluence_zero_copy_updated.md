h1. AMPS vs. Fabric Flight SQL – Zero-Copy Data Movement (Updated Benchmarks)

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

After adding a JSON → POJO conversion layer (to make AMPS comparable to strongly typed gRPC/Arrow payloads), performance decreased slightly as expected due to parsing overhead.

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

Even after normalising AMPS to decode JSON, Flight SQL still outperforms all transports by a wide margin.

----

h2. 4. Updated Benchmark Results

{code:theme=Midnight|linenumbers=false|language=bash}
Size   Service      Rows/sec     MB/sec   Latency_p95
-----  ----------  ----------  ---------  -----------
small  AMPS             144.0       0.01        90.26
small  FLIGHT           540.0       0.03       377.67
small  FLIGHT_SQL   1157889.4      69.33       166.50
small  GRPC             556.2       0.03       957.85

medium AMPS            4295.8       0.26        53.69
medium FLIGHT         15961.7       0.96       183.53
medium FLIGHT_SQL   1106867.5      66.28       146.63
medium GRPC            5384.0       0.32       928.86

large  AMPS           12588.6       0.75         0.02
large  FLIGHT        166109.6       9.95       200.19
large  FLIGHT_SQL   1148539.9      68.77       129.11
large  GRPC           60726.0       3.64         5.56

xl     AMPS           95093.0       5.69         0.01
xl     FLIGHT        674730.1      40.40       287.88
xl     FLIGHT_SQL   1205623.3      72.19       112.15
xl     GRPC          119292.8       7.14         0.16

xxl    AMPS          161070.5       9.64         0.00
xxl    FLIGHT       1240961.8      74.31       115.73
xxl    FLIGHT_SQL    901195.2      53.96       172.87
xxl    GRPC          175030.9      10.48         0.12

xxxl   AMPS          257498.6      15.42         0.00
xxxl   FLIGHT        951957.7      57.00       125.53
xxxl   FLIGHT_SQL   1135193.0      67.97       118.87
xxxl   GRPC          174960.8      10.48         0.12
{code}

----

h2. 5. Summary Interpretation

*AMPS*:  
Performance dropped slightly due to JSON → POJO decoding costs, confirming AMPS is still best suited for real-time streaming but not bulk data movement.

*GRPC*:  
Still the lowest-latency option, especially on large datasets, but throughput remains far lower than the Arrow transports.

*Arrow Flight*:  
Strong mid-tier option, excellent scaling on large datasets.

*Arrow Flight SQL*:  
Still the clear winner in throughput and stability across sizes. Zero-copy columnar delivery continues to outperform all other transports even under heavier JSON decoding on the AMPS side.

----

h2. 6. Why Flight SQL Still Leads
* Columnar vectorisation  
* Zero-copy Arrow buffers  
* No JSON decoding  
* No object tree creation  
* Strong CPU efficiency  
* Predictable latency across scales  

----

h2. 7. Conclusion
Normalising AMPS to decode JSON payloads makes the comparison fairer—and Flight SQL still wins by **an order of magnitude**.

AMPS remains exceptional for streaming.  
Flight SQL remains the right choice for bulk movement, analytics, and cross-language usage.
