#!/usr/bin/env bash
set -euo pipefail

# docs:start
RUN_ID="<run-id>"  # runId of the registered run

RUN=$(curl -s -S -f "https://api.docudevs.ai/extraction-review-runs/$RUN_ID" \
  -H "Authorization: $API_KEY")

echo "$RUN" | jq '{
    reference: {
      jobGuid: .identity.jobGuid,
      artifactId: .identity.artifactId,
      resultFingerprint: .identity.resultFingerprint
    },
    expectedReviewRevision: .reviewRevision,
    claims: [{
      claimId: "check-total-0001",
      claimRevision: 1,
      identity: {
        reporterId: "invoice-checker",
        externalIssueId: "check-0001",
        referenceVersion: "dataset-2026-10",
        comparatorVersion: "2"
      },
      address: {scope: "BASE", pointer: "/total"},
      observed: {present: true, valueJson: "0"},
      proposedValueJson: "125.5",
      rationaleJson: "\"reference differs from extracted value\"",
      priority: "HIGH"
    }],
    audit: {evidenceArtifactPaths: [], exampleEligibility: false}
  }' |
  curl -s -S -f -X POST "https://api.docudevs.ai/extraction-review-runs/$RUN_ID/claims" \
    -H "Authorization: $API_KEY" \
    -H "Idempotency-Key: evaluator-2026-10-06-0001" \
    -H "Content-Type: application/json" \
    --data-binary @- | jq '{runId: .event.runId, reviewRevision: .state.reviewRevision}'
# docs:end
