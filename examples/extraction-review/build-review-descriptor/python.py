import asyncio
import hashlib
import json
from dataclasses import dataclass
from pathlib import Path

# docs:start
from docudevs import ClientPipeline, P
# docs:end


# docs:start
@dataclass
class InvoiceTotals:
    total: float


def sum_lines(*, invoice) -> InvoiceTotals:
    lines = (invoice or {}).get("lines") or []
    return InvoiceTotals(total=sum(line["amount"] for line in lines))


# docs:end
async def main():
    # docs:start
    pipeline = ClientPipeline()

    invoice = pipeline.extract(
        "invoice",
        prompt="Extract the vendor name and the amount of every line item.",
        schema={
            "type": "object",
            "properties": {
                "vendor": {"type": "string"},
                "lines": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {"amount": {"type": "number"}},
                        "required": ["amount"],
                    },
                },
            },
            "required": ["vendor", "lines"],
        },
        source=P.ocr.content,
    )

    totals = pipeline.local_function(
        "totals",
        sum_lines,
        input={"invoice": invoice.result},
        output_type=InvoiceTotals,
    )

    verdict = pipeline.extract(
        "verdict",
        prompt="Does the computed total match the total printed on the invoice?",
        schema={
            "type": "object",
            "properties": {"total_matches": {"type": "boolean"}},
            "required": ["total_matches"],
        },
        input={"totals": totals.result},
    )
    pipeline.final(verdict)

    # Digests of the released producer code and its data, 64 lowercase hexadecimal characters
    producer_bundle_sha256 = hashlib.sha256(Path("producer-bundle.zip").read_bytes()).hexdigest()
    vendor_catalog_sha256 = hashlib.sha256(Path("vendor-catalog.csv").read_bytes()).hexdigest()

    descriptor = pipeline.review_descriptor(
        producer={
            "id": "invoice-producer",
            "releaseId": "2026.10.1",
            "codeSha256": producer_bundle_sha256,
            "data": [{"id": "vendor-catalog", "sha256": vendor_catalog_sha256}],
        },
        # Every local node ID maps to the ID of its implementation
        local_function_ids={"totals": "sum-lines-v1"},
        final_schema={
            "type": "object",
            "properties": {"total_matches": {"type": "boolean"}},
            "required": ["total_matches"],
        },
    )

    Path("descriptor.json").write_text(json.dumps(descriptor, allow_nan=False))
    # docs:end


asyncio.run(main())
