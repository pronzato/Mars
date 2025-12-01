# Kerberos & TLS Smoke Tests

`fabric-test-basic` hosts standalone AMPS, gRPC, Arrow Flight/Flight SQL, Splunk, and S3 samples
that demonstrate the new keytab-based Kerberos model alongside plain and TLS-only scenarios.
Everything is self-contained so the entire module can be copied into other environments if needed.

## Configuration

All Kerberos and TLS settings now live in `org.pronzato.fabric.test.TestSecurityConfig`. Update the
constants in that class to point at your keytab, `krb5.conf`, TLS PEM files, and service principals
before running the smoke tests. Every sample reuses those values so you only need to edit them in one
place. The supplied defaults point at the demo certificates under `src/main/resources/tls` and a
placeholder keytab path.

All TLS examples ship with demo PEMs under `src/main/resources/tls`. Replace them or point at your
own trust material before connecting to real systems.

## AMPS

`org.pronzato.fabric.test.amps.TestAmpsKerberos` exercises the TLS + Kerberos AMPS scenario using the
shared configuration. Run it with:

```powershell
mvn -q -pl fabric-test-basic exec:java -Dexec.mainClass=org.pronzato.fabric.test.amps.TestAmpsKerberos
```

If the keytab configured in `TestSecurityConfig` is missing, the sample prints a reminder and exits
without throwing.

## gRPC

- Plain echo server/client: `org.pronzato.fabric.test.grpc.plain`
- TLS-only server/client: `org.pronzato.fabric.test.grpc.tls`
- TLS + Kerberos server/client: `org.pronzato.fabric.test.grpc.kerberos`

Before running the Kerberos pair ensure `CLIENT_KEYTAB`, `SERVICE_KEYTAB`, and the principals inside
`KerberosEchoClient`/`KerberosEchoServer` point at real credentials. The classes automatically skip
if the placeholders or keytabs are left unchanged.

## Arrow Flight & Flight SQL

Each protocol provides three entry points under `org.pronzato.fabric.test.flight` and
`org.pronzato.fabric.test.flightsql`:

```powershell
# Plain Flight server/client
mvn -q -pl fabric-test-basic exec:java -Dexec.mainClass=org.pronzato.fabric.test.flight.plain.PlainFlightServer
mvn -q -pl fabric-test-basic exec:java -Dexec.mainClass=org.pronzato.fabric.test.flight.plain.PlainFlightClient

# TLS-only Flight
mvn -q -pl fabric-test-basic exec:java -Dexec.mainClass=org.pronzato.fabric.test.flight.tls.TlsFlightServer
mvn -q -pl fabric-test-basic exec:java -Dexec.mainClass=org.pronzato.fabric.test.flight.tls.TlsFlightClient

# TLS + Kerberos Flight
mvn -q -pl fabric-test-basic exec:java -Dexec.mainClass=org.pronzato.fabric.test.flight.kerberos.KerberosFlightServer
mvn -q -pl fabric-test-basic exec:java -Dexec.mainClass=org.pronzato.fabric.test.flight.kerberos.KerberosFlightClient
```

The corresponding Flight SQL commands follow the same pattern under
`org.pronzato.fabric.test.flightsql.*`. Like the gRPC samples, the Kerberos versions short-circuit if
the placeholder principals or keytabs have not been replaced.

## Splunk & S3

The Splunk (`org.pronzato.fabric.test.splunk.TestSplunk`) and AWS S3
(`org.pronzato.fabric.test.s3.TestS3`) smoke tests continue to rely on TLS using the default SDK
credentials for authentication. Edit the constants at the top of each class to point at your Splunk
search endpoint or S3 bucket/prefix before running them via Maven:

```powershell
mvn -q -pl fabric-test-basic exec:java -Dexec.mainClass=org.pronzato.fabric.test.splunk.TestSplunk
mvn -q -pl fabric-test-basic exec:java -Dexec.mainClass=org.pronzato.fabric.test.s3.TestS3
```

## Notes

- The Kerberos helpers in this module duplicate the simplified Fabric model (principal + keytab with
  optional `krb5.conf`). No JAAS files or user-managed `java.security.auth.login.config` flags are
  required.
- Kerberos activation follows the global rule: **no keytab ⇒ no Kerberos**.
- Replace the demo TLS certificates with real ones in production environments.
