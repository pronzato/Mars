S3 AWS PHYSICAL = 
{
  "envelope" : {
    "group" : "platform",
    "type" : "CONNECTION",
    "name" : "s3-fabric",
    "uuid" : "f8d5297f-a0c8-43d3-ba1d-dd1e10a7ec36",
    "enabled" : true,
    "lifecycleStatus" : "ACTIVE",
    "createdTimeUtc" : "2025-10-11T16:26:42Z",
    "createdBy" : "Alice",
    "updatedTimeUtc" : "2026-01-02T21:26:18.506Z",
    "updatedBy" : "Alice"
  },
  "connectionType" : "S3",
  "s3" : {
    "region" : "us-east-2",
    "bucket" : "pronz-fabric1",
    "basePrefix" : "",
    "pathStyleAccess" : false
  },
  "physicalSites" : [ ],
  "physical" : {
    "connectionType" : "S3",
    "uriOrEndpoint" : "s3://pronz-fabric1",
    "regionOrLocation" : "us-east-2",
    "options" : { },
    "tags" : { }
  },
  "mode" : "PHYSICAL"
}




ENDPOINT OVERRIDE STYLE = 
{
  "envelope" : {
    "group" : "platform",
    "type" : "CONNECTION",
    "name" : "s3-fabric",
    "uuid" : "f8d5297f-a0c8-43d3-ba1d-dd1e10a7ec36",
    "enabled" : true,
    "lifecycleStatus" : "ACTIVE",
    "createdTimeUtc" : "2025-10-11T16:26:42Z",
    "createdBy" : "Alice",
    "updatedTimeUtc" : "2026-01-02T21:26:18.506Z",
    "updatedBy" : "Alice"
  },
  "connectionType" : "S3",
  "s3" : {
    "region" : "us-east-2",
    "bucket" : "pronz-fabric1",
    "basePrefix" : "",
    "pathStyleAccess" : true,
    "endpointOverride" : "foo.host.com"
  },
  "physicalSites" : [ ],
  "physical" : {
    "connectionType" : "S3",
    "uriOrEndpoint" : "https://foo.host.com",
    "regionOrLocation" : "us-east-2",
    "options" : { },
    "tags" : { }
  },
  "mode" : "PHYSICAL"
}


