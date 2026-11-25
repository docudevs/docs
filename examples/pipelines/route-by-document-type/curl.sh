#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  while true; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
}

GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@report.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  --data-binary @- <<'JSON'
{
  "mimeType": "application/pdf",
  "ocr": "AUTO",
  "extractionMode": "PIPELINE",
  "pipeline": {
    "version": "2026-05",
    "nodes": [
      {
        "id": "classify_document",
        "type": "extract",
        "source": "$ocr.content",
        "prompt": "Classify this document as invoice, safety_data_sheet, or other. Only classify the document.",
        "schema": {
          "type": "object",
          "properties": {
            "document_type": {
              "type": "string",
              "enum": [
                "invoice",
                "safety_data_sheet",
                "other"
              ]
            },
            "confidence": {
              "type": "number"
            },
            "reason": {
              "type": "string"
            }
          },
          "required": [
            "document_type",
            "confidence",
            "reason"
          ]
        },
        "llm": {
          "tier": "nano"
        }
      },
      {
        "id": "extract_invoice",
        "type": "extract",
        "dependsOn": [
          "classify_document"
        ],
        "when": {
          "eq": [
            "$nodes.classify_document.result.document_type",
            "invoice"
          ]
        },
        "source": "$ocr.content",
        "prompt": "Extract invoice fields from the document. Return null for missing fields.",
        "schema": {
          "type": "object",
          "properties": {
            "invoice_number": {
              "type": [
                "string",
                "null"
              ]
            },
            "vendor_name": {
              "type": [
                "string",
                "null"
              ]
            },
            "total_amount": {
              "type": [
                "number",
                "null"
              ]
            },
            "currency": {
              "type": [
                "string",
                "null"
              ]
            }
          }
        },
        "llm": {
          "tier": "mini"
        }
      },
      {
        "id": "extract_sds",
        "type": "extract",
        "dependsOn": [
          "classify_document"
        ],
        "when": {
          "eq": [
            "$nodes.classify_document.result.document_type",
            "safety_data_sheet"
          ]
        },
        "source": "$ocr.content",
        "prompt": "Extract product and safety information from the document.",
        "schema": {
          "type": "object",
          "properties": {
            "product_name": {
              "type": [
                "string",
                "null"
              ]
            },
            "supplier": {
              "type": [
                "string",
                "null"
              ]
            },
            "signal_word": {
              "type": [
                "string",
                "null"
              ]
            }
          }
        },
        "llm": {
          "tier": "mini"
        }
      },
      {
        "id": "invoice_final",
        "type": "final",
        "dependsOn": [
          "classify_document",
          "extract_invoice"
        ],
        "when": {
          "eq": [
            "$nodes.classify_document.result.document_type",
            "invoice"
          ]
        },
        "output": {
          "branch": "invoice",
          "classification": "$nodes.classify_document.result",
          "extracted": "$nodes.extract_invoice.result"
        }
      },
      {
        "id": "sds_final",
        "type": "final",
        "dependsOn": [
          "classify_document",
          "extract_sds"
        ],
        "when": {
          "eq": [
            "$nodes.classify_document.result.document_type",
            "safety_data_sheet"
          ]
        },
        "output": {
          "branch": "safety_data_sheet",
          "classification": "$nodes.classify_document.result",
          "extracted": "$nodes.extract_sds.result"
        }
      },
      {
        "id": "other_final",
        "type": "final",
        "dependsOn": [
          "classify_document"
        ],
        "when": {
          "eq": [
            "$nodes.classify_document.result.document_type",
            "other"
          ]
        },
        "output": {
          "branch": "other",
          "classification": "$nodes.classify_document.result",
          "message": "No specialized extraction branch was selected."
        }
      }
    ],
    "finals": [
      {
        "node": "invoice_final"
      },
      {
        "node": "sds_final"
      },
      {
        "node": "other_final"
      }
    ],
    "ocr": {
      "mode": "AUTO",
      "qualityArtifact": true
    },
    "concurrency": {
      "maxNodes": 2
    }
  }
}
JSON

wait_for_job "$GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$GUID/json" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/status/$GUID" \
  -H "Authorization: $API_KEY" | jq '.pipeline.nodes'
# docs:end
