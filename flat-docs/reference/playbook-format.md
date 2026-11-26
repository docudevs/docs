---
sidebar_position: 7
title: Playbook format
description: The clause-based playbook JSON format, its JSON Schema, and a worked example.
---
<!-- Generated from docs/reference/playbook-format.md. Do not edit by hand. -->

# Playbook format

A contract-analysis playbook is one JSON document. The same document is stored
as the draft, accepted by import and returned by export. Field names are
camelCase. The format is generic: the playbook's own content supplies the
vocabulary, so the same engine reviews any kind of contract.

Import and export use these endpoints. Both belong to a knowledge base
(a case with a `contract-analysis` profile):

- `POST /knowledge-bases/{caseId}/profiles/contract-analysis/import` with the body
  `{"draftRevision": <observed revision>, "playbook": {...}}` replaces the whole
  draft. There is no merge.
- `GET /knowledge-bases/{caseId}/profiles/contract-analysis/export?view=draft|published`
  returns the canonical document.

## Playbook fields

| Field | Meaning |
| --- | --- |
| `profileVersion` | `"2026-11"`, the only supported value. Import sets it. A stored playbook in any other format is refused when a run starts. |
| `name`, `description` | Display name and summary. |
| `perspective` | `ourParty` and `counterparty`, free text such as "the customer" and "the supplier". Every judgement is made from our party's side. |
| `scope` | Optional. Which contracts the playbook is for: `contractTypes`, `businessLines` and `jurisdictions` (lists of non-empty text) and `from` (non-empty text). Only `contractTypes` can put a contract outside the scope; see [Contract-analysis playbooks](../core/contract-analysis-playbooks.md). |
| `facts` | Deal facts that clause conditions depend on. Each has `id`, `label`, `description`, `valueType` (`string`, `number`, `integer`, `boolean`, `date`, `choice`), `values` for `choice`, and `extractionGuidance`. Facts are read from the contract during analysis. |
| `clauses` | The clauses, below. |

## Clause fields

| Field | Meaning |
| --- | --- |
| `id` | Stable identifier matching `^[a-z0-9_]+$`. |
| `name`, `group` | Human name and the playbook section it belongs to. |
| `preferredWording` | Our preferred verbatim wording, if any. |
| `interpretation` | What the clause does and why it matters. |
| `requirementLevel` | `mandatory`, `expected`, `preferred`, `optional` or `none`. Decides what a missing clause means. |
| `appliesWhen` | Condition over facts; absent means always. Paths are written `facts.<factId>`. Operators include `eq`, `in`, `and`, `or`, `not` and `exists`. |
| `stance` | The playbook's general position: `acceptable`, `negotiable`, `problematic` or `dealBreaker`. Shown to reviewers; it does not change a verdict. |
| `marketStandard` | Market-standard wording or description. |
| `alternatives` | Wording we may accept instead. Each has `id`, `description`, `acceptability` (`acceptable`, `acceptableWithModifications`, `problematic`) and `rationale`. |
| `redFlags` | What makes the clause unacceptable. Each has `id`, `flag`, `severity` (`critical`, `high`, `medium`, `low`), `triggeredBy` (`wording` or `silence`), `whereApplies`, `whyProblematic`, `whenTriggered` and `favours` (`us`, `counterparty`, `neutral`). A `silence` flag is about missing wording and is triggered only after the contract has been checked for it. |
| `riskFactors` | Facts that change how much the clause matters. Each has `id`, `when` (a condition), `effect` (`raises` or `lowers`) and `reason`. Each matching factor moves the effective severity of the clause's red flags by one level. |
| `negotiationStance` | What to push for and what to concede. |
| `identificationHints` | How to find the clause. Each has `id`, `phrase`, `whereInContract`, `whyMatches`, `howToApply` and `falsePositiveRisk`. |
| `relatedClauses` | Identifiers of related clauses. |
| `notes` | Free text for reviewers. It is never sent to the analysis. |

Every clause and every line item inside it carries `sourceRefs` (the guideline
passages it came from) and `authorship` (`generated`, `human_edited` or
`human_authored`). Items you import without source references are
`human_authored`.

Import fills in `profileType`, `revision`, `sourceSet` and any missing
`authorship`, so a hand-written file needs none of them. Export returns them,
and the schema below describes the exported document.

Import also accepts two hand-written shortcuts: `relatedClauses` as plain
strings or as `{"clauseId": "..."}` objects, and `notes` as a string or
`{"text": "..."}`. Export always writes the canonical form. Import validates the
whole file and reports every problem with its JSON path in a `422` response,
for example a duplicate id, a `relatedClauses` entry that names no clause, or an
`appliesWhen` that uses an undeclared fact.

Import also rejects:

- empty text: optional text fields are either omitted or non-empty, and
  required text fields are not blank;
- text longer than 8192 characters, and a clause whose `preferredWording`,
  `marketStandard` and identification hints together exceed 8192 characters;
- a source reference whose `documentId` is not one of the playbook's source
  documents. Import takes the source documents from the case's confirmed
  guideline sources, so a hand-written file for a case without confirmed
  sources must not carry `sourceRefs`;
- a resolved source reference without `sourceElementIds` or `pageNumbers`,
  page numbers that are not positive integers, and two source references with
  the same `id`;
- the alternative ids `preferred` and `none`, which are reserved for the
  analysis result's matched wording.

## Example

A playbook with one clause, in the form you can write by hand and import:

```json
{
  "profileVersion": "2026-11",
  "name": "Supply agreement playbook",
  "description": "Positions for supply agreements.",
  "perspective": {
    "ourParty": "the customer",
    "counterparty": "the supplier"
  },
  "facts": [
    {
      "id": "contract_type",
      "label": "Contract type",
      "description": "Whether the agreement covers goods or services.",
      "valueType": "choice",
      "values": [
        "goods",
        "services"
      ],
      "extractionGuidance": "Read the subject of the agreement from the opening articles."
    }
  ],
  "clauses": [
    {
      "id": "limitation_of_liability",
      "name": "Limitation of liability",
      "group": "General clauses",
      "preferredWording": "Each party's total liability under this agreement is limited to the fees paid in the twelve months before the claim, except for death, personal injury, fraud and wilful misconduct.",
      "interpretation": "Caps what the supplier can owe the customer and what the customer can owe the supplier. A low cap leaves the customer carrying most of the loss from a supplier failure.",
      "requirementLevel": "mandatory",
      "appliesWhen": {
        "eq": [
          "facts.contract_type",
          "goods"
        ]
      },
      "stance": "negotiable",
      "marketStandard": "A cap of 12 months of fees with carve-outs for death, personal injury, fraud and wilful misconduct.",
      "alternatives": [
        {
          "id": "fixed_cap",
          "description": "A fixed sum cap that is at least the annual contract value.",
          "acceptability": "acceptableWithModifications",
          "rationale": "Acceptable when the sum is not lower than the yearly fees."
        }
      ],
      "redFlags": [
        {
          "id": "cap_one_sided",
          "flag": "Only the supplier's liability is capped",
          "severity": "high",
          "triggeredBy": "wording",
          "whereApplies": "The liability article",
          "whyProblematic": "The customer stays exposed without limit while the supplier is protected.",
          "whenTriggered": "The cap names the supplier only.",
          "favours": "counterparty"
        },
        {
          "id": "no_carve_outs",
          "flag": "No exceptions to the cap",
          "severity": "medium",
          "triggeredBy": "silence",
          "whereApplies": "The liability article",
          "whyProblematic": "Death, injury and fraud claims would also be capped.",
          "whenTriggered": "No wording excludes death, personal injury or fraud from the cap.",
          "favours": "counterparty"
        }
      ],
      "riskFactors": [],
      "negotiationStance": "Ask for a mutual cap with the standard carve-outs. Concede the exact sum if it is at least the annual fees.",
      "identificationHints": [
        {
          "id": "cap_phrase",
          "phrase": "total liability shall not exceed",
          "whereInContract": "Liability or indemnity article",
          "whyMatches": "Caps are almost always introduced with this wording.",
          "howToApply": "Read the whole article, including exceptions listed after the cap.",
          "falsePositiveRisk": "Do not mistake an insurance coverage minimum for a liability cap."
        }
      ],
      "relatedClauses": [],
      "notes": "Escalate to legal when the cap is below annual fees."
    }
  ]
}
```

## JSON Schema

This is the schema of the exported document. A file you write by hand may omit
the fields that import fills in (see above); the rest must match:

```json
{
  "$defs": {
    "Alternative": {
      "additionalProperties": false,
      "properties": {
        "acceptability": {
          "enum": [
            "acceptable",
            "acceptableWithModifications",
            "problematic"
          ],
          "title": "Acceptability",
          "type": "string"
        },
        "authorship": {
          "enum": [
            "generated",
            "human_edited",
            "human_authored"
          ],
          "title": "Authorship",
          "type": "string"
        },
        "description": {
          "minLength": 1,
          "title": "Description",
          "type": "string"
        },
        "id": {
          "pattern": "^[a-z0-9_]+$",
          "title": "Id",
          "type": "string"
        },
        "rationale": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Rationale"
        },
        "sourceRefs": {
          "items": {
            "$ref": "#/$defs/SourceRefV2"
          },
          "title": "Sourcerefs",
          "type": "array"
        }
      },
      "required": [
        "id",
        "description",
        "acceptability",
        "authorship"
      ],
      "title": "Alternative",
      "type": "object"
    },
    "Clause": {
      "additionalProperties": false,
      "properties": {
        "alternatives": {
          "items": {
            "$ref": "#/$defs/Alternative"
          },
          "title": "Alternatives",
          "type": "array"
        },
        "appliesWhen": {
          "anyOf": [
            {
              "$ref": "#/$defs/Condition"
            },
            {
              "type": "null"
            }
          ],
          "default": null
        },
        "authorship": {
          "enum": [
            "generated",
            "human_edited",
            "human_authored"
          ],
          "title": "Authorship",
          "type": "string"
        },
        "group": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Group"
        },
        "id": {
          "pattern": "^[a-z0-9_]+$",
          "title": "Id",
          "type": "string"
        },
        "identificationHints": {
          "items": {
            "$ref": "#/$defs/IdentificationHint"
          },
          "title": "Identificationhints",
          "type": "array"
        },
        "interpretation": {
          "minLength": 1,
          "title": "Interpretation",
          "type": "string"
        },
        "marketStandard": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Marketstandard"
        },
        "name": {
          "minLength": 1,
          "title": "Name",
          "type": "string"
        },
        "negotiationStance": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Negotiationstance"
        },
        "notes": {
          "anyOf": [
            {
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Notes"
        },
        "preferredWording": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Preferredwording"
        },
        "redFlags": {
          "items": {
            "$ref": "#/$defs/RedFlag"
          },
          "title": "Redflags",
          "type": "array"
        },
        "relatedClauses": {
          "items": {
            "pattern": "^[a-z0-9_]+$",
            "type": "string"
          },
          "title": "Relatedclauses",
          "type": "array"
        },
        "requirementLevel": {
          "enum": [
            "mandatory",
            "expected",
            "preferred",
            "optional",
            "none"
          ],
          "title": "Requirementlevel",
          "type": "string"
        },
        "riskFactors": {
          "items": {
            "$ref": "#/$defs/RiskFactor"
          },
          "title": "Riskfactors",
          "type": "array"
        },
        "sourceRefs": {
          "items": {
            "$ref": "#/$defs/SourceRefV2"
          },
          "title": "Sourcerefs",
          "type": "array"
        },
        "stance": {
          "anyOf": [
            {
              "enum": [
                "acceptable",
                "negotiable",
                "problematic",
                "dealBreaker"
              ],
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Stance"
        }
      },
      "required": [
        "id",
        "name",
        "interpretation",
        "requirementLevel",
        "authorship"
      ],
      "title": "Clause",
      "type": "object"
    },
    "Condition": {
      "additionalProperties": false,
      "properties": {
        "and": {
          "anyOf": [
            {
              "items": {
                "$ref": "#/$defs/Condition"
              },
              "type": "array"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "And"
        },
        "eq": {
          "anyOf": [
            {
              "items": {},
              "type": "array"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Eq"
        },
        "exists": {
          "anyOf": [
            {
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Exists"
        },
        "gt": {
          "anyOf": [
            {
              "items": {},
              "type": "array"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Gt"
        },
        "gte": {
          "anyOf": [
            {
              "items": {},
              "type": "array"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Gte"
        },
        "in": {
          "anyOf": [
            {
              "items": {},
              "type": "array"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "In"
        },
        "lt": {
          "anyOf": [
            {
              "items": {},
              "type": "array"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Lt"
        },
        "lte": {
          "anyOf": [
            {
              "items": {},
              "type": "array"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Lte"
        },
        "ne": {
          "anyOf": [
            {
              "items": {},
              "type": "array"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Ne"
        },
        "not": {
          "anyOf": [
            {
              "$ref": "#/$defs/Condition"
            },
            {
              "type": "null"
            }
          ],
          "default": null
        },
        "or": {
          "anyOf": [
            {
              "items": {
                "$ref": "#/$defs/Condition"
              },
              "type": "array"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Or"
        }
      },
      "title": "Condition",
      "type": "object"
    },
    "Fact": {
      "additionalProperties": false,
      "properties": {
        "authorship": {
          "enum": [
            "generated",
            "human_edited",
            "human_authored"
          ],
          "title": "Authorship",
          "type": "string"
        },
        "description": {
          "minLength": 1,
          "title": "Description",
          "type": "string"
        },
        "extractionGuidance": {
          "minLength": 1,
          "title": "Extractionguidance",
          "type": "string"
        },
        "id": {
          "pattern": "^[a-z0-9_]+$",
          "title": "Id",
          "type": "string"
        },
        "label": {
          "minLength": 1,
          "title": "Label",
          "type": "string"
        },
        "sourceRefs": {
          "items": {
            "$ref": "#/$defs/SourceRefV2"
          },
          "title": "Sourcerefs",
          "type": "array"
        },
        "valueType": {
          "enum": [
            "string",
            "number",
            "integer",
            "boolean",
            "date",
            "choice"
          ],
          "title": "Valuetype",
          "type": "string"
        },
        "values": {
          "anyOf": [
            {
              "items": {
                "minLength": 1,
                "type": "string"
              },
              "type": "array"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Values"
        }
      },
      "required": [
        "id",
        "label",
        "description",
        "valueType",
        "extractionGuidance",
        "authorship"
      ],
      "title": "Fact",
      "type": "object"
    },
    "IdentificationHint": {
      "additionalProperties": false,
      "properties": {
        "authorship": {
          "enum": [
            "generated",
            "human_edited",
            "human_authored"
          ],
          "title": "Authorship",
          "type": "string"
        },
        "falsePositiveRisk": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Falsepositiverisk"
        },
        "howToApply": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Howtoapply"
        },
        "id": {
          "pattern": "^[a-z0-9_]+$",
          "title": "Id",
          "type": "string"
        },
        "phrase": {
          "minLength": 1,
          "title": "Phrase",
          "type": "string"
        },
        "sourceRefs": {
          "items": {
            "$ref": "#/$defs/SourceRefV2"
          },
          "title": "Sourcerefs",
          "type": "array"
        },
        "whereInContract": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Whereincontract"
        },
        "whyMatches": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Whymatches"
        }
      },
      "required": [
        "id",
        "phrase",
        "authorship"
      ],
      "title": "IdentificationHint",
      "type": "object"
    },
    "Perspective": {
      "additionalProperties": false,
      "properties": {
        "counterparty": {
          "minLength": 1,
          "title": "Counterparty",
          "type": "string"
        },
        "ourParty": {
          "minLength": 1,
          "title": "Ourparty",
          "type": "string"
        }
      },
      "required": [
        "ourParty",
        "counterparty"
      ],
      "title": "Perspective",
      "type": "object"
    },
    "PlaybookScope": {
      "additionalProperties": false,
      "properties": {
        "businessLines": {
          "items": {
            "minLength": 1,
            "type": "string"
          },
          "title": "Businesslines",
          "type": "array"
        },
        "contractTypes": {
          "items": {
            "minLength": 1,
            "type": "string"
          },
          "title": "Contracttypes",
          "type": "array"
        },
        "from": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "From"
        },
        "jurisdictions": {
          "items": {
            "minLength": 1,
            "type": "string"
          },
          "title": "Jurisdictions",
          "type": "array"
        }
      },
      "title": "PlaybookScope",
      "type": "object"
    },
    "RedFlag": {
      "additionalProperties": false,
      "properties": {
        "authorship": {
          "enum": [
            "generated",
            "human_edited",
            "human_authored"
          ],
          "title": "Authorship",
          "type": "string"
        },
        "favours": {
          "anyOf": [
            {
              "enum": [
                "us",
                "counterparty",
                "neutral"
              ],
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Favours"
        },
        "flag": {
          "minLength": 1,
          "title": "Flag",
          "type": "string"
        },
        "id": {
          "pattern": "^[a-z0-9_]+$",
          "title": "Id",
          "type": "string"
        },
        "severity": {
          "enum": [
            "critical",
            "high",
            "medium",
            "low"
          ],
          "title": "Severity",
          "type": "string"
        },
        "sourceRefs": {
          "items": {
            "$ref": "#/$defs/SourceRefV2"
          },
          "title": "Sourcerefs",
          "type": "array"
        },
        "triggeredBy": {
          "enum": [
            "wording",
            "silence"
          ],
          "title": "Triggeredby",
          "type": "string"
        },
        "whenTriggered": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Whentriggered"
        },
        "whereApplies": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Whereapplies"
        },
        "whyProblematic": {
          "anyOf": [
            {
              "minLength": 1,
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Whyproblematic"
        }
      },
      "required": [
        "id",
        "flag",
        "severity",
        "triggeredBy",
        "authorship"
      ],
      "title": "RedFlag",
      "type": "object"
    },
    "RiskFactor": {
      "additionalProperties": false,
      "properties": {
        "authorship": {
          "enum": [
            "generated",
            "human_edited",
            "human_authored"
          ],
          "title": "Authorship",
          "type": "string"
        },
        "effect": {
          "enum": [
            "raises",
            "lowers"
          ],
          "title": "Effect",
          "type": "string"
        },
        "id": {
          "pattern": "^[a-z0-9_]+$",
          "title": "Id",
          "type": "string"
        },
        "reason": {
          "minLength": 1,
          "title": "Reason",
          "type": "string"
        },
        "sourceRefs": {
          "items": {
            "$ref": "#/$defs/SourceRefV2"
          },
          "title": "Sourcerefs",
          "type": "array"
        },
        "when": {
          "$ref": "#/$defs/Condition"
        }
      },
      "required": [
        "id",
        "when",
        "effect",
        "reason",
        "authorship"
      ],
      "title": "RiskFactor",
      "type": "object"
    },
    "SourceDocumentV2": {
      "additionalProperties": false,
      "properties": {
        "authorId": {
          "anyOf": [
            {
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Authorid"
        },
        "authorship": {
          "anyOf": [
            {
              "enum": [
                "generated",
                "human_edited",
                "human_authored"
              ],
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Authorship"
        },
        "contentFingerprint": {
          "title": "Contentfingerprint",
          "type": "string"
        },
        "effectiveDate": {
          "anyOf": [
            {
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Effectivedate"
        },
        "filename": {
          "title": "Filename",
          "type": "string"
        },
        "id": {
          "title": "Id",
          "type": "string"
        },
        "rationale": {
          "anyOf": [
            {
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Rationale"
        },
        "sourceRole": {
          "anyOf": [
            {
              "enum": [
                "policy",
                "guidance",
                "example",
                "superseded"
              ],
              "type": "string"
            },
            {
              "type": "null"
            }
          ],
          "default": null,
          "title": "Sourcerole"
        },
        "supersedesDocumentIds": {
          "items": {
            "type": "string"
          },
          "title": "Supersedesdocumentids",
          "type": "array"
        }
      },
      "required": [
        "id",
        "filename",
        "contentFingerprint"
      ],
      "title": "SourceDocumentV2",
      "type": "object"
    },
    "SourceManifestV2": {
      "additionalProperties": false,
      "properties": {
        "authority": {
          "additionalProperties": true,
          "title": "Authority",
          "type": "object"
        },
        "documentIds": {
          "items": {
            "type": "string"
          },
          "title": "Documentids",
          "type": "array"
        },
        "documents": {
          "items": {
            "$ref": "#/$defs/SourceDocumentV2"
          },
          "title": "Documents",
          "type": "array"
        },
        "fingerprint": {
          "title": "Fingerprint",
          "type": "string"
        }
      },
      "required": [
        "documentIds",
        "fingerprint",
        "documents"
      ],
      "title": "SourceManifestV2",
      "type": "object"
    },
    "SourceRefV2": {
      "additionalProperties": false,
      "properties": {
        "documentId": {
          "title": "Documentid",
          "type": "string"
        },
        "evidenceStatus": {
          "enum": [
            "resolved",
            "unresolved"
          ],
          "title": "Evidencestatus",
          "type": "string"
        },
        "id": {
          "title": "Id",
          "type": "string"
        },
        "pageNumbers": {
          "items": {
            "type": "integer"
          },
          "title": "Pagenumbers",
          "type": "array"
        },
        "quote": {
          "title": "Quote",
          "type": "string"
        },
        "sourceElementIds": {
          "items": {
            "type": "string"
          },
          "title": "Sourceelementids",
          "type": "array"
        }
      },
      "required": [
        "id",
        "documentId",
        "quote",
        "evidenceStatus"
      ],
      "title": "SourceRefV2",
      "type": "object"
    }
  },
  "additionalProperties": false,
  "properties": {
    "clauses": {
      "items": {
        "$ref": "#/$defs/Clause"
      },
      "title": "Clauses",
      "type": "array"
    },
    "description": {
      "anyOf": [
        {
          "minLength": 1,
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "default": null,
      "title": "Description"
    },
    "facts": {
      "items": {
        "$ref": "#/$defs/Fact"
      },
      "title": "Facts",
      "type": "array"
    },
    "name": {
      "minLength": 1,
      "title": "Name",
      "type": "string"
    },
    "perspective": {
      "$ref": "#/$defs/Perspective"
    },
    "processing": {
      "additionalProperties": true,
      "title": "Processing",
      "type": "object"
    },
    "profileType": {
      "const": "contract-analysis",
      "title": "Profiletype",
      "type": "string"
    },
    "profileVersion": {
      "const": "2026-11",
      "title": "Profileversion",
      "type": "string"
    },
    "review": {
      "additionalProperties": true,
      "title": "Review",
      "type": "object"
    },
    "revision": {
      "minimum": 1,
      "title": "Revision",
      "type": "integer"
    },
    "scope": {
      "anyOf": [
        {
          "$ref": "#/$defs/PlaybookScope"
        },
        {
          "type": "null"
        }
      ],
      "default": null
    },
    "sourceSet": {
      "$ref": "#/$defs/SourceManifestV2"
    }
  },
  "required": [
    "profileVersion",
    "profileType",
    "revision",
    "name",
    "perspective",
    "sourceSet"
  ],
  "title": "Playbook",
  "type": "object"
}
```
