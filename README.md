# Simple Prototype to Populate IM Knowledge Graph DB

## Background

We are experimenting a DB model to host IM data using a Knowledge Graph Pattern.

The motivation for this prototype is to validate the DB model, identify indexing strategies and assess its capability
to scale using a distributed DB engine (YugabyteDB).

## Building the Prototype

To be able to build this prototype, project IMPersistence needs to be successfully compiled and installed locally.
See https://github.com/endeavourhealth-discovery/IMPersistence for details about this step.

Once cloned locally, the prototype can be compiled using:

`mvn clean install`

## Running the prototype

At the moment, the prototype expects CSV input in the following format:

`system_id,resource_type,resource_id,updated_at,patient_id,resource_data,resource_checksum,resource_metadata`

Only the parsing of JSON `resource_data` is required by this process.

F.e. such a resource derived from FHIR resource convention is similar to:

```json
{
  "resourceType": "Observation",
  "id": "ddd13f7f-f49a-45cd-b6e4-e33a22ebb9e8",
  "meta": {
    "profile": [
      "http://endeavourhealth.org/fhir/StructureDefinition/primarycare-observation"
    ]
  },
  "extension": [
    {
      "url": "http://endeavourhealth.org/fhir/StructureDefinition/primarycare-recorded-by-extension",
      "valueReference": {
        "reference": "Practitioner/13f981b1-db01-49e6-8c11-28d1cce023c6"
      }
    },
    {
      "url": "http://endeavourhealth.org/fhir/StructureDefinition/primarycare-recorded-date-extension",
      "valueDateTime": "2014-12-03T00:00:00+00:00"
    }
  ],
  "status": "unknown",
  "code": {
    "coding": [
      {
        "system": "http://snomed.info/sct",
        "code": "717381000000109",
        "display": "Excepted from atrial fibrillation quality indicators - informed dissent (finding)"
      },
      {
        "system": "http://read.info/ctv3",
        "code": "XaLFj",
        "display": "Excepted from atrial fibrillation qual indic: Inform dissent"
      }
    ],
    "text": "Excepted from atrial fibrillation qual indic: Inform dissent"
  },
  "subject": {
    "reference": "Patient/f3a29c9d-3e12-4d1d-9ff1-b4b54d9a802e"
  },
  "encounter": {
    "reference": "Encounter/3185edeb-e274-43ef-ac8e-0d47b95f90b1"
  },
  "effectiveDateTime": "2014-12-03T00:00:00+00:00",
  "performer": [
    {
      "reference": "Practitioner/13f981b1-db01-49e6-8c11-28d1cce023c6"
    }
  ],
  "valueQuantity": {
    "value": 0.0
  }
}

```
The extraction of values and references is done using a ResourceVisitor instance from project IMPersistence.

To build the extended Knowledge Graph, we build associated `isA` SNOMED CT concept hierarchies. That is, if the concept is
`24700007` (Multiple Sclerosis), the corresponding expanded isA hierarchy will be:

```sql
        "116680003": [
            6118003,
            138875005,
            404684003,
            128139000,
            23853001,
            246556002,
            363170005,
            64572001,
            118940003,
            414029004,
            362975008,
            363171009,
            39367000,
            80690008,
            362965005
        ]
```
The actual meaning of the above codes can be determined using a SNOMED CT browser.

Please note that, for the time being, only SNOMED CT concept expansion is supported.

We store expanded hierarchy into an ancillary table (CONCEPT_HIERARCHY) which is used to perform conceptual search 
using a containment operator. F.e. retrieving all records related to "Disorder of the central nervous system"
(code: `23853001`). To build the hierarchy we install locally Hermes (https://github.com/wardle/hermes), we intend 
to use IMDiscovery whenever possible.

The configuration to access Hermes is defined in `application.yml`

For running unit tests, we use a dockerized test image of PostgreSQL. Please also note that tests disable referential
integrity as data is somewhat fragmentary (missing PERSON node in particular).