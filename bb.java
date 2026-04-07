
vault\git\dev\platform\secret\stitch-ssl.json
{
  "envelope" : {
    "group" : "platform",
    "type" : "SECRET",
    "name" : "stitch-ssl",
    "uuid" : "97e51637-32d7-43e3-8b93-7e4f0eb27765",
    "enabled" : true,
    "lifecycleStatus" : "ACTIVE",
    "createdTimeUtc" : "2026-04-06T00:00:00Z",
    "createdBy" : "stitch-sso-bootstrap",
    "updatedTimeUtc" : "2026-04-06T00:00:00Z",
    "updatedBy" : "stitch-sso-bootstrap"
  },
  "valueType" : "TEXT",
  "status" : "ACTIVE",
  "lastRotatedUtc" : "2026-04-06T00:00:00Z",
  "metadata" : {
    "owner" : "platform",
    "team" : "stitch-studio",
    "tags" : [ "stitch", "https", "keystore", "dev", "placeholder" ],
    "notes" : "Dummy placeholder for Stitch HTTPS keystore password secret. Rotate to a real value in Studio before production use."
  },
  "material" : {
    "textValueEnc" : "v1:zcUpKj3mdeq1gkuh:Jt67yE2S6iG0bMWq2iTJG7NFrDW291C0x4ZcWpk+wpKyXMvA9j0d/1cPeRywpw6ApFx8CxfeBCI=",
    "encFormat" : "FABRIC_AES_GCM_V1",
    "keyId" : "v1"
  }
}



vault\git\dev\platform\secret\stitch-sso.json
{
  "envelope" : {
    "group" : "platform",
    "type" : "SECRET",
    "name" : "stitch-sso",
    "uuid" : "a892a7ea-214d-4cc5-a512-5c687f12ffa1",
    "enabled" : true,
    "lifecycleStatus" : "ACTIVE",
    "createdTimeUtc" : "2026-04-06T00:00:00Z",
    "createdBy" : "stitch-sso-bootstrap",
    "updatedTimeUtc" : "2026-04-06T00:00:00Z",
    "updatedBy" : "stitch-sso-bootstrap"
  },
  "valueType" : "TEXT",
  "status" : "ACTIVE",
  "lastRotatedUtc" : "2026-04-06T00:00:00Z",
  "metadata" : {
    "owner" : "platform",
    "team" : "stitch-studio",
    "tags" : [ "stitch", "sso", "oidc", "client-secret", "dev", "placeholder" ],
    "notes" : "Dummy placeholder for Stitch OIDC client secret value. Rotate to a real value in Studio before production use."
  },
  "material" : {
    "textValueEnc" : "v1:zcUpKj3mdeq1gkuh:Jt67yE2S6iG0bMWq2iTJG7NFrDW291C0x4ZcWpk+wpKyXMvA9j0d/1cPeRywpw6ApFx8CxfeBCI=",
    "encFormat" : "FABRIC_AES_GCM_V1",
    "keyId" : "v1"
  }
}



