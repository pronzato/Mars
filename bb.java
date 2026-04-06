main
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>

  <groupId>org.pronzato.fabric</groupId>
  <artifactId>fabric-parent</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>fabric-parent</name>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- libs -->
    <grpc.version>1.66.0</grpc.version>
    <protobuf.version>4.30.2</protobuf.version>
    <protobuf.skip>false</protobuf.skip>
    <jackson.version>2.17.2</jackson.version>
    <amps.client.version>5.3.3.4</amps.client.version>
    <arrow.version>18.3.0</arrow.version>
    <duckdb.version>1.5.1</duckdb.version>
    <duckdb.jdbc.version>1.5.1.0</duckdb.jdbc.version>
    <duckdb.payload.classifier>duckdb-${duckdb.version}</duckdb.payload.classifier>
    <aws.sdk.version>2.27.19</aws.sdk.version>
    <mongodb.driver.version>5.1.0</mongodb.driver.version>
    <nimbus.version>11.26</nimbus.version>
    <nimbus.jwt.version>10.0.2</nimbus.jwt.version>
    <junit.version>5.10.2</junit.version>
    <mockito.version>4.11.0</mockito.version>
    <bytebuddy.version>1.14.12</bytebuddy.version>
    <fabric.arrow.add-opens>--add-opens=java.base/java.nio=ALL-UNNAMED</fabric.arrow.add-opens>
    <surefire.plugin.version>3.2.5</surefire.plugin.version>
    <bouncycastle.version>1.78.1</bouncycastle.version>
    <slf4j.version>2.0.16</slf4j.version>
    <logback.version>1.5.9</logback.version>
    <jacoco.plugin.version>0.8.12</jacoco.plugin.version>
    <jacoco.skip>true</jacoco.skip>
    <shade.skip>true</shade.skip>

    <!-- default: frontend work is off unless a module opts in -->
    <vaadin.skip>true</vaadin.skip>
    <frontend.skip>true</frontend.skip>

    <node.version>v20.11.1</node.version>

    <!-- app modules override this if they want an uber jar -->
    <stitch.uber.skip>true</stitch.uber.skip>
  </properties>

  <modules>
    <module>fabric-kdc</module>
    <module>fabric-mcp</module>
    <module>fabric-lite-duckdb</module>
    <module>fabric-lite</module>
    <module>fabric-lite-ecore</module>
    <module>fabric-lite-tools</module>
    <module>fabric-lite-agents</module>
    <module>fabric-lite-config</module>
    <module>fabric-lite-lab</module>
    <module>fabric-lite-simulator</module>
    <module>fabric-lite-risk</module>
    <module>fabric-lite-sso</module>
    <module>fabric-lite-docs</module>
    <module>fabric-lite-services</module>
    <module>fabric-lite-stitch</module>
    <module>fabric-lite-stitch-demo</module>
    <module>fabric-lite-stitch-oms</module>
    <module>fabric-lite-stitch-studio</module>
    <module>fabric-lite-velocity</module>
    <module>fabric-lite-vaadin</module>
    <module>fabric-lite-aeron</module>
    <module>fabric-lite-mongodb</module>
    <module>fabric-test-oc</module>
  </modules>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-bom</artifactId>
        <version>1.66.0</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>net.bytebuddy</groupId>
        <artifactId>byte-buddy</artifactId>
        <version>${bytebuddy.version}</version>
      </dependency>
      <dependency>
        <groupId>net.bytebuddy</groupId>
        <artifactId>byte-buddy-agent</artifactId>
        <version>${bytebuddy.version}</version>
      </dependency>
      <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>${protobuf.version}</version>
      </dependency>
      <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java-util</artifactId>
        <version>${protobuf.version}</version>
      </dependency>
      <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-javalite</artifactId>
        <version>${protobuf.version}</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
      </plugins>
    </pluginManagement>
    <plugins>
      <plugin>
        <artifactId>maven-antrun-plugin</artifactId>
        <version>3.1.0</version>
        <executions>
          <execution>
            <id>fabric-build-id</id>
            <phase>process-resources</phase>
            <goals>
              <goal>run</goal>
            </goals>
            <configuration>
              <exportAntProperties>true</exportAntProperties>
              <target>
                <property name="fabric.build.file" value="${project.basedir}/.build/build-info.properties" />
                <mkdir dir="${project.basedir}/.build" />
                <propertyfile file="${fabric.build.file}" comment="Fabric build metadata">
                  <entry key="build.projectVersion" type="string" value="${project.version}" />
                  <entry key="build.number" type="int" operation="+" value="1" />
                </propertyfile>
                <property file="${fabric.build.file}" />
                <tstamp>
                  <format property="fabric.build.timestamp" pattern="yyyy-MM-dd'T'HH:mm:ss'Z'" timezone="UTC" />
                </tstamp>
                <property name="fabric.build.number" value="${build.number}" />
                <property name="fabric.build.id" value="${project.version}.${fabric.build.number}" />
                <mkdir dir="${project.build.outputDirectory}" />
                <echo file="${project.build.outputDirectory}/build-info.properties">
build.id=${fabric.build.id}
build.number=${fabric.build.number}
build.timestamp=${fabric.build.timestamp}
build.projectVersion=${project.version}
</echo>
              </target>
            </configuration>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
          <release>${maven.compiler.release}</release>
          <showDeprecation>true</showDeprecation>
          <showWarnings>true</showWarnings>
        </configuration>
      </plugin>

      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>${jacoco.plugin.version}</version>
        <configuration>
          <skip>${jacoco.skip}</skip>
        </configuration>
        <executions>
          <execution>
            <id>prepare-agent</id>
            <goals>
              <goal>prepare-agent</goal>
            </goals>
          </execution>
          <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
              <goal>report</goal>
            </goals>
          </execution>
          <execution>
            <id>jacoco-check</id>
            <phase>verify</phase>
            <goals>
              <goal>check</goal>
            </goals>
            <configuration>
              <rules>
                <rule>
                  <element>BUNDLE</element>
                  <limits>
                    <limit>
                      <counter>LINE</counter>
                      <value>COVEREDRATIO</value>
                      <minimum>0.90</minimum>
                    </limit>
                  </limits>
                </rule>
              </rules>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>

  <profiles>
    <profile>
      <id>fast</id>
      <modules>
        <module>fabric-api</module>
      </modules>
    </profile>

    <profile>
      <id>duckdb-1.1.1</id>
      <properties>
        <duckdb.version>1.1.1</duckdb.version>
        <duckdb.payload.classifier>duckdb-1.1.1</duckdb.payload.classifier>
      </properties>
    </profile>

    <profile>
      <id>duckdb-1.5.1</id>
      <properties>
        <duckdb.version>1.5.1</duckdb.version>
        <duckdb.jdbc.version>1.5.1.0</duckdb.jdbc.version>
        <duckdb.payload.classifier>duckdb-1.5.1</duckdb.payload.classifier>
      </properties>
    </profile>

    <profile>
      <id>duckdb-jdbc-version-normalize-1.5.1</id>
      <activation>
        <property>
          <name>duckdb.version</name>
          <value>1.5.1</value>
        </property>
      </activation>
      <properties>
        <duckdb.jdbc.version>1.5.1.0</duckdb.jdbc.version>
      </properties>
    </profile>

    <profile>
      <id>meta-ci</id>
      <activation>
        <activeByDefault>false</activeByDefault>
      </activation>
      <properties>
        <skipITs>true</skipITs>
        <maven.test.skip>true</maven.test.skip>
        <protobuf.skip>true</protobuf.skip>
      </properties>
      <build>
        <plugins>
          <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>${surefire.plugin.version}</version>
            <configuration>
              <includes>
                <include>**/fabric/config/**/*Test.java</include>
                <include>**/fabric/config/demo/**/*Test.java</include>
              </includes>
              <excludes>
                <exclude>**/*ConnectionManager*Test.java</exclude>
                <exclude>**/*Kerberos*Test.java</exclude>
                <exclude>**/*Duckdb*Test.java</exclude>
                <exclude>**/*DuckDB*Test.java</exclude>
                <exclude>**/*RowsetManagerRemoteTest.java</exclude>
                <exclude>**/*TabularRemoteTest.java</exclude>
                <exclude>**/*GrpcPepIntegrationTest.java</exclude>
                <exclude>**/*DataEntitlementsParityIntegrationTest.java</exclude>
                <exclude>**/*MaterializedServiceTest.java</exclude>
                <exclude>**/*Feed*Test.java</exclude>
                <exclude>**/*FileFeed*Test.java</exclude>
                <exclude>**/*ParquetFeed*Test.java</exclude>
                <exclude>**/*ImpalaFeed*Test.java</exclude>
                <exclude>**/*LegacyFabricDataServiceCatalogTest.java</exclude>
                <exclude>**/*DataNameResolverTest.java</exclude>
                <exclude>**/*RowsetManagerTest.java</exclude>
                <exclude>**/*TabularStreamEngineTest.java</exclude>
                <exclude>**/*DefaultHotCacheManagerTest.java</exclude>
                <exclude>**/*ConfigCacheLastGoodTest.java</exclude>
                <exclude>**/*Asset*Test.java</exclude>
                <exclude>**/*CompactionExecutorManifestTest.java</exclude>
              </excludes>
              <failIfNoTests>false</failIfNoTests>
            </configuration>
          </plugin>
          <plugin>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>${surefire.plugin.version}</version>
            <configuration>
              <skipTests>true</skipTests>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
</project>



duckdb
======
  
  <?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.pronzato.fabric</groupId>
    <artifactId>fabric-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>fabric-lite-duckdb</artifactId>
  <name>fabric-lite-duckdb</name>
  <packaging>jar</packaging>

  <properties>
    <duckdb.payload.1.1.1.dir>${project.basedir}/src/duckdb-1.1.1/resources</duckdb.payload.1.1.1.dir>
    <duckdb.payload.1.5.1.dir>${project.basedir}/src/duckdb-1.5.1/resources</duckdb.payload.1.5.1.dir>
  </properties>

  <build>
    <resources>
      <resource>
        <directory>${duckdb.payload.1.1.1.dir}</directory>
      </resource>
      <resource>
        <directory>${duckdb.payload.1.5.1.dir}</directory>
      </resource>
    </resources>
    <plugins>
      <plugin>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.4.2</version>
        <executions>
          <execution>
            <id>attach-duckdb-1.1.1</id>
            <phase>package</phase>
            <goals>
              <goal>jar</goal>
            </goals>
            <configuration>
              <classesDirectory>${duckdb.payload.1.1.1.dir}</classesDirectory>
              <classifier>duckdb-1.1.1</classifier>
            </configuration>
          </execution>
          <execution>
            <id>attach-duckdb-1.5.1</id>
            <phase>package</phase>
            <goals>
              <goal>jar</goal>
            </goals>
            <configuration>
              <classesDirectory>${duckdb.payload.1.5.1.dir}</classesDirectory>
              <classifier>duckdb-1.5.1</classifier>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${surefire.plugin.version}</version>
        <configuration>
          <failIfNoTests>false</failIfNoTests>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>


lite
====
  <?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.pronzato.fabric</groupId>
    <artifactId>fabric-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>fabric-lite</artifactId>
  <name>fabric-lite</name>
  <packaging>jar</packaging>

  <properties>
    <otel.version>1.38.0</otel.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.pronzato.fabric</groupId>
      <artifactId>fabric-lite-duckdb</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>${slf4j.version}</version>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>jcl-over-slf4j</artifactId>
      <version>${slf4j.version}</version>
    </dependency>
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>${logback.version}</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>${jackson.version}</version>
    </dependency>
    <dependency>
      <groupId>com.crankuptheamps</groupId>
      <artifactId>amps-client</artifactId>
      <version>${amps.client.version}</version>
    </dependency>
    <dependency>
      <groupId>io.grpc</groupId>
      <artifactId>grpc-netty-shaded</artifactId>
    </dependency>
    <dependency>
      <groupId>io.grpc</groupId>
      <artifactId>grpc-stub</artifactId>
    </dependency>
    <dependency>
      <groupId>com.nimbusds</groupId>
      <artifactId>oauth2-oidc-sdk</artifactId>
      <version>${nimbus.version}</version>
    </dependency>
    <dependency>
      <groupId>com.nimbusds</groupId>
      <artifactId>nimbus-jose-jwt</artifactId>
      <version>${nimbus.jwt.version}</version>
    </dependency>
    <dependency>
      <groupId>org.duckdb</groupId>
      <artifactId>duckdb_jdbc</artifactId>
      <version>${duckdb.jdbc.version}</version>
    </dependency>
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>s3</artifactId>
      <version>${aws.sdk.version}</version>
      <exclusions>
        <exclusion>
          <groupId>commons-logging</groupId>
          <artifactId>commons-logging</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.apache.arrow</groupId>
      <artifactId>arrow-vector</artifactId>
      <version>${arrow.version}</version>
    </dependency>
    <dependency>
      <groupId>org.apache.arrow</groupId>
      <artifactId>arrow-memory-core</artifactId>
      <version>${arrow.version}</version>
    </dependency>
    <dependency>
      <groupId>org.apache.arrow</groupId>
      <artifactId>arrow-memory-netty</artifactId>
      <version>${arrow.version}</version>
    </dependency>
    <dependency>
      <groupId>org.apache.arrow</groupId>
      <artifactId>flight-core</artifactId>
      <version>${arrow.version}</version>
    </dependency>
    <dependency>
      <groupId>org.apache.arrow</groupId>
      <artifactId>flight-sql</artifactId>
      <version>${arrow.version}</version>
    </dependency>
    <dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-api</artifactId>
      <version>${otel.version}</version>
    </dependency>
    <dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-sdk</artifactId>
      <version>${otel.version}</version>
    </dependency>
    <dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-exporter-otlp</artifactId>
      <version>${otel.version}</version>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>${junit.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-inline</artifactId>
      <version>${mockito.version}</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${surefire.plugin.version}</version>
        <configuration>
          <argLine>${fabric.arrow.add-opens}</argLine>
          <useModulePath>false</useModulePath>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>





  lab
  ===
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.pronzato.fabric</groupId>
    <artifactId>fabric-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>fabric-lite-lab</artifactId>
  <name>fabric-lite-lab</name>
  <packaging>jar</packaging>

  <properties>
    <protobuf.skip>true</protobuf.skip>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.pronzato.fabric</groupId>
      <artifactId>fabric-lite</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
      <groupId>org.pronzato.fabric</groupId>
      <artifactId>fabric-lite-duckdb</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
      <groupId>org.pronzato.fabric</groupId>
      <artifactId>fabric-lite-aeron</artifactId>
      <version>${project.version}</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.grpc</groupId>
      <artifactId>grpc-stub</artifactId>
    </dependency>
    <dependency>
      <groupId>io.grpc</groupId>
      <artifactId>grpc-protobuf</artifactId>
    </dependency>
    <dependency>
      <groupId>io.grpc</groupId>
      <artifactId>grpc-netty-shaded</artifactId>
    </dependency>
    <dependency>
      <groupId>com.google.protobuf</groupId>
      <artifactId>protobuf-java</artifactId>
    </dependency>
    <dependency>
      <groupId>javax.annotation</groupId>
      <artifactId>javax.annotation-api</artifactId>
      <version>1.3.2</version>
    </dependency>
  </dependencies>

  <build>
    <extensions>
      <extension>
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>1.7.1</version>
      </extension>
    </extensions>
    <plugins>
      <plugin>
        <groupId>org.xolstice.maven.plugins</groupId>
        <artifactId>protobuf-maven-plugin</artifactId>
        <version>0.6.1</version>
        <configuration>
          <skip>${protobuf.skip}</skip>
          <tempDirectory>${java.io.tmpdir}</tempDirectory>
          <temporaryProtoFileDirectory>
            ${java.io.tmpdir}/fabric-protoc-deps/${project.artifactId}
          </temporaryProtoFileDirectory>
          <protoSourceRoot>${project.basedir}/src/main/proto</protoSourceRoot>
          <protocArtifact>
            com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}
          </protocArtifact>
          <pluginId>grpc-java</pluginId>
          <pluginArtifact>
            io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}
          </pluginArtifact>
        </configuration>
        <executions>
          <execution>
            <id>echo</id>
            <goals>
              <goal>compile</goal>
              <goal>compile-custom</goal>
            </goals>
            <configuration>
              <outputDirectory>
                ${project.build.directory}/generated-sources/protobuf/java
              </outputDirectory>
              <clearOutputDirectory>false</clearOutputDirectory>
              <writeDescriptorSet>true</writeDescriptorSet>
              <descriptorSetFileName>echo.desc</descriptorSetFileName>
              <descriptorSetOutputDirectory>
                ${project.build.directory}/generated-resources/protobuf
              </descriptorSetOutputDirectory>
              <includeDependenciesInDescriptorSet>true</includeDependenciesInDescriptorSet>
              <includeSourceInfoInDescriptorSet>true</includeSourceInfoInDescriptorSet>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
        <version>3.5.0</version>
        <executions>
          <execution>
            <id>add-generated-sources</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>add-source</goal>
            </goals>
            <configuration>
              <sources>
                <source>${project.build.directory}/generated-sources/protobuf/java</source>
              </sources>
            </configuration>
          </execution>
          <execution>
            <id>add-proto-resources</id>
            <phase>generate-resources</phase>
            <goals>
              <goal>add-resource</goal>
            </goals>
            <configuration>
              <resources>
                <resource>
                  <directory>${project.build.directory}/generated-resources/protobuf</directory>
                </resource>
              </resources>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>



java "-Dfabric.group=platform" "-Dfabric.name=studio" "-Dfabric.instance=instance1" "-Dfabric.discoveryUrls=127.0.0.1:9507" "-Dfabric.kerberosKeytabPath=C:/dev/fabric/vault/kerberos/dfabric.keytab" "-Dfabric.kerberosUsername=dfabric" "-Dfabric.secretKeyFile=C:/dev/fabric/vault/keys/platform/platform.fabrickey" "-Dfabric.tlsTrustCertPath=C:/dev/fabric/vault/tls/demo-ca.pem" "-Dfabric.port=8080" -jar "C:/dev/fabric/fabric-lite-stitch-studio/target/fabric-lite-stitch-studio-1.0.0-SNAPSHOT-uber.jar"
            
  
  
