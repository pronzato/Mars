namespaces_equities.json

{
    "scopeType":  "namespace",
    "scopeId":  "namespace:equities/equities",
    "policies":  [
                     {
                         "schemaVersion":  "1.0",
                         "policyId":  "namespace:equities/equities",
                         "group":  "oms",
                         "name":  "equities",
                         "type":  "NAMESPACE",
                         "scopeType":  "NAMESPACE",
                         "resourceId":  "namespace:equities/equities",
                         "policyUsage":  "DATASET",
                         "usageGroup":  "namespace-equities",
                         "description":  "Default namespace access for equities datasets.",
                         "version":  "1",
                         "enabled":  true,
                         "lifecycleStatus":  "ACTIVE",
                         "metadata":  {

                                      },
                         "bindings":  [
                                          {
                                              "subject":  "group:oms",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "group:oms",
                                              "action":  "write",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "group:platform",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "app:oms/data-service",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          }
                                      ]
                     },
                     {
                         "schemaVersion":  "1.0",
                         "policyId":  "namespace:refdata/equities",
                         "group":  "oms",
                         "name":  "equities-refdata-visibility",
                         "type":  "NAMESPACE",
                         "scopeType":  "NAMESPACE",
                         "resourceId":  "namespace:refdata/equities",
                         "policyUsage":  "DATASET",
                         "usageGroup":  "namespace-refdata",
                         "description":  "Visibility scope for refdata assets needed by OMS simulator and data-service joins.",
                         "version":  "1",
                         "enabled":  true,
                         "lifecycleStatus":  "ACTIVE",
                         "metadata":  {

                                      },
                         "bindings":  [
                                          {
                                              "subject":  "group:oms",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "group:platform",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "app:oms/data-service",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          }
                                      ]
                     },
                     {
                         "schemaVersion":  "1.0",
                         "policyId":  "namespace:marketdata/equities",
                         "group":  "oms",
                         "name":  "equities-marketdata-visibility",
                         "type":  "NAMESPACE",
                         "scopeType":  "NAMESPACE",
                         "resourceId":  "namespace:marketdata/equities",
                         "policyUsage":  "DATASET",
                         "usageGroup":  "namespace-marketdata",
                         "description":  "Visibility scope for marketdata assets needed by OMS simulator and data-service joins.",
                         "version":  "1",
                         "enabled":  true,
                         "lifecycleStatus":  "ACTIVE",
                         "metadata":  {

                                      },
                         "bindings":  [
                                          {
                                              "subject":  "group:oms",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "group:platform",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "app:oms/data-service",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          }
                                      ]
                     },
                     {
                         "schemaVersion":  "1.0",
                         "policyId":  "namespace:analytics/equities",
                         "group":  "oms",
                         "name":  "equities-analytics-visibility",
                         "type":  "NAMESPACE",
                         "scopeType":  "NAMESPACE",
                         "resourceId":  "namespace:analytics/equities",
                         "policyUsage":  "DATASET",
                         "usageGroup":  "namespace-analytics",
                         "description":  "Visibility scope for analytics views needed by OMS simulator startup.",
                         "version":  "1",
                         "enabled":  true,
                         "lifecycleStatus":  "ACTIVE",
                         "metadata":  {

                                      },
                         "bindings":  [
                                          {
                                              "subject":  "group:oms",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "group:platform",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "app:oms/data-service",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          }
                                      ]
                     }
                 ],
    "relationships":  [

                      ],
    "labels":  {
                   "managedBy":  "simulator",
                   "defaultPlatformRead":  "true"
               },
    "notes":  [
                  "Namespace baseline for equities datasets."
              ],
    "envelope":  {
                     "group":  "oms",
                     "type":  "ENTITLEMENT",
                     "name":  "overrides/namespaces/equities",
                     "uuid":  "edc6a530-c11b-36eb-bf77-56c9edbbd1cc",
                     "enabled":  true,
                     "lifecycleStatus":  "ACTIVE",
                     "createdTimeUtc":  "2026-03-27T00:00:00Z",
                     "createdBy":  "simulator",
                     "updatedTimeUtc":  "2026-03-27T00:00:00Z",
                     "updatedBy":  "simulator"
                 }
}


  
namespaces_oms_mini.json

{
    "scopeType":  "namespace",
    "scopeId":  "namespace:oms-mini/oms_mini",
    "policies":  [
                     {
                         "schemaVersion":  "1.0",
                         "policyId":  "namespace:oms-mini/oms_mini",
                         "group":  "oms-mini",
                         "name":  "oms-mini",
                         "type":  "NAMESPACE",
                         "scopeType":  "NAMESPACE",
                         "resourceId":  "namespace:oms-mini/oms_mini",
                         "policyUsage":  "DATASET",
                         "usageGroup":  "namespace-oms-mini",
                         "description":  "Namespace baseline access for oms-mini datasets.",
                         "version":  "1",
                         "enabled":  true,
                         "lifecycleStatus":  "ACTIVE",
                         "metadata":  {

                                      },
                         "bindings":  [
                                          {
                                              "subject":  "group:oms-mini",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "group:platform",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "group:oms-mini",
                                              "action":  "write",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "user:lenny",
                                              "action":  "write",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "group:desk1",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          },
                                          {
                                              "subject":  "group:desk2",
                                              "action":  "read",
                                              "effect":  "ALLOW",
                                              "enabled":  true,
                                              "obligations":  {
                                                                  "rowFilters":  [

                                                                                 ],
                                                                  "columnsAllow":  [

                                                                                   ],
                                                                  "columnsMask":  [

                                                                                  ],
                                                                  "datasetScope":  [

                                                                                   ],
                                                                  "methodsAllow":  [

                                                                                   ],
                                                                  "parameters":  {

                                                                                 }
                                                              },
                                              "attributes":  {

                                                             },
                                              "context":  {

                                                          }
                                          }
                                      ]
                     }
                 ],
    "relationships":  [

                      ],
    "labels":  {
                   "managedBy":  "oms-mini",
                   "defaultPlatformRead":  "true"
               },
    "notes":  [
                  "Namespace visibility for oms-mini dataset reads and writes."
              ],
    "envelope":  {
                     "group":  "oms-mini",
                     "type":  "ENTITLEMENT",
                     "name":  "overrides/namespaces/oms-mini",
                     "uuid":  "05794a2c-9e54-31dd-86a6-e6e059f0829d",
                     "enabled":  true,
                     "lifecycleStatus":  "ACTIVE",
                     "createdTimeUtc":  "2026-03-28T00:00:00Z",
                     "createdBy":  "oms-mini",
                     "updatedTimeUtc":  "2026-03-28T00:00:00Z",
                     "updatedBy":  "oms-mini"
                 }
}








  
\platform\connection\s3-fabric.json

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
    "pathStyleAccess" : false,
    "credentialsSecretRef" : "secret://platform/s3-fabric"
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

  
\platform\connection\s3-fabric-platform.json

{
  "envelope" : {
    "group" : "platform",
    "type" : "CONNECTION",
    "name" : "s3-fabric-platform",
    "uuid" : "e9d6c3c8-5c76-41e6-8c7a-8a0d0e6f3c9b",
    "enabled" : true,
    "lifecycleStatus" : "ACTIVE",
    "createdTimeUtc" : "2025-12-08T15:25:01.438Z",
    "createdBy" : "Alice",
    "updatedTimeUtc" : "2026-01-02T21:26:18.504Z",
    "updatedBy" : "Alice"
  },
  "connectionType" : "S3",
  "s3" : {
    "region" : "us-east-2",
    "bucket" : "pronz-fabric1",
    "basePrefix" : "",
    "pathStyleAccess" : false,
    "credentialsSecretRef" : "secret://platform/s3-fabric"
  },
  "physicalSites" : [ ],
  "client" : {
    "physicalRef" : "platform/s3-fabric",
    "connectionRole" : "rw",
    "options" : { },
    "tags" : { }
  },
  "mode" : "CLIENT"
}

  
\lab\connection\s3-fabric.json

{
  "envelope" : {
    "group" : "lab",
    "type" : "CONNECTION",
    "name" : "s3-fabric",
    "uuid" : "76f83c04-b86d-4c6d-9f50-f0bfc3d7b065",
    "enabled" : true,
    "lifecycleStatus" : "ACTIVE",
    "createdTimeUtc" : "2026-03-01T14:19:07.777Z",
    "createdBy" : "codex",
    "updatedTimeUtc" : "2026-03-01T14:19:07.777Z",
    "updatedBy" : "codex"
  },
  "connectionType" : "S3",
  "s3" : {
    "region" : "us-east-2",
    "bucket" : "pronz-fabric1",
    "basePrefix" : "",
    "pathStyleAccess" : false,
    "credentialsSecretRef" : "secret://lab/s3-fabric"
  },
  "physicalSites" : [ ],
  "client" : {
    "physicalRef" : "platform/s3-fabric",
    "connectionRole" : "rw",
    "options" : { },
    "tags" : { }
  },
  "mode" : "CLIENT"
}

