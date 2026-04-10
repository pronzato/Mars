{
            "type": "java",
            "name": "StitchStudioApp",
            "request": "launch",
            "mainClass": "org.pronzato.fabric.stitch.studio.StitchStudioApp",
            "projectName": "fabric-lite-stitch-studio",
            "vmArgs": "--add-opens=java.base/java.nio=org.apache.arrow.memory.core,ALL-UNNAMED -Dfabric.group=platform -Dfabric.name=studio -Dfabric.instance=instance1-${env:USERNAME} -Dfabric.discoveryUrls=127.0.0.1:9507 -Dfabric.kerberosKeytabPath=C:/dev/fabric/vault/kerberos/dfabric.keytab -Dfabric.kerberosUsername=dfabric -Dfabric.secretKeyFile=C:/dev/fabric/vault/keys/platform/platform.fabrickey -Dfabric.tlsTrustCertPath=C:/dev/fabric/vault/tls/demo-ca.pem -Dfabric.port=8080 -Dfabric.stitch.runtime.configInstance=instance1"
        }
