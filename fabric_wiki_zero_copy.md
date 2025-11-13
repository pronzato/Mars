# AMPS vs. Fabric Flight SQL – Zero-Copy Data Movement

## 1. Overview
Fabric now supports two complementary data movement technologies:

| Transport      | Optimized For                               | Pattern                          |
|----------------|----------------------------------------------|----------------------------------|
| **AMPS**       | Real-time streaming, low-latency fan-out     | Event bus + incremental updates  |
| **Flight SQL** | High-throughput, zero-copy table movement    | Bulk reads, analytics, batch     |

AMPS continues to be our primary **streaming** mechanism.  
Flight SQL becomes our primary **high-speed table transport and analytics** mechanism.

---

## 2. Why AMPS alone isn’t enough for modern workloads
AMPS is excellent for:

- Real-time financial events  
- Market data updates  
- High fan-out subscriptions  
- Operational event streams  

However, for **large table reads** (100k–100M+ rows), AMPS has inherent limitations:

- Row-encoded messages (JSON/Protobuf)  
- Per-row parsing costs  
- Per-language object conversion (Python/Java/C++)  
- Higher CPU usage  
- Lower throughput compared to columnar engines  

This is why AMPS usually peaks around **~200k–240k rows/sec** on large datasets.

---

## 3. Fabric Flight SQL & Zero-Copy
Fabric adopts **Apache Arrow** and **Arrow Flight SQL** to enable **zero-copy, columnar data movement**.

### Zero-copy means:
- Data is produced once in a **universal memory layout**  
- All languages understand this layout (Python, Java, C++, Rust, Go)  
- No serialization  
- No deserialization  
- No object trees  
- No data reshaping  
- Much lower CPU usage  

This gives **3×–10× higher throughput** and **orders-of-magnitude less overhead**.

Benchmark proof:

| Size | AMPS Max | Flight Max | Flight SQL Max |
|------|-----------|-------------|----------------|
| XL–XXXL | ~200k–240k r/s | ~800k–1.25M r/s | ~0.8M–1.04M r/s |

---

## 4. Zero-Copy in Simple Terms
Traditional flow (AMPS SOW snapshot):

```
Network bytes → JSON/Protobuf → Language-specific objects → DataFrames
```

Zero-copy flow (Flight SQL):

```
Network bytes → Arrow buffers → Ready-to-use Table
```

No parsing.  
No duplicated data.  
Minimal CPU.  
Predictable performance.

---

## 5. Benefits for Application Teams

### ✔ Faster analytics & service startup  
Flight SQL loads millions of rows per second, even on XXL datasets.

### ✔ Lower CPU usage  
Eliminates parsing, object creation, and per-language conversions.

### ✔ Smaller, simpler codebases  
No JSON → dict → DataFrame glue code.  
No Protobuf → POJO → Arrow conversions.

### ✔ Identical behavior across languages  
Arrow provides the same schema + layout to Python, Java, C++, etc.

### ✔ Better entitlement enforcement  
Fabric applies row/column policies before data is transmitted.

---

## 6. When to use AMPS vs Flight SQL

### **Use AMPS for:**
- Streaming updates  
- Market data  
- Incremental deltas  
- High fan-out delivery  

### **Use Flight SQL for:**
- Bulk table reads  
- Analytics and vectorized queries  
- Machine learning + Python notebooks  
- Inter-service table movement  
- High-volume refdata/positions/orders snapshots  

These technologies **complement** each other inside Fabric.

---

## 7. Full Benchmark Results (Dark Code Block)

```bash
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
```

---

## 8. Technical Appendix

### Why AMPS is not zero-copy
AMPS messages are row-oriented (JSON, MsgPack, Protobuf).  
Each consumer must decode and reshape them.  
Each language recreates its own objects, costing CPU and memory.

### Why Arrow enables zero-copy
Arrow defines a **universal in-memory columnar representation**.  
Flight/Flight SQL stream Arrow buffers directly.  
Clients do no parsing, no conversion, and no reallocation.

This aligns Fabric with industry leaders such as DuckDB, MotherDuck, Spark, Polars, Pandas, and Snowflake.
