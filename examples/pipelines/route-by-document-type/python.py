import asyncio
import json
import os

from docudevs import P, Pipeline, eq
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    pipeline = Pipeline().ocr(mode="AUTO", quality_artifact=True).max_nodes(2)

    classification = pipeline.extract(
        "classify_document",
        source=P.ocr.content,
        prompt="Classify this document as invoice, safety_data_sheet, or other. Only classify the document.",
        schema={
            "type": "object",
            "properties": {
                "document_type": {
                    "type": "string",
                    "enum": ["invoice", "safety_data_sheet", "other"],
                },
                "confidence": {"type": "number"},
                "reason": {"type": "string"},
            },
            "required": ["document_type", "confidence", "reason"],
        },
        llm_tier="nano",
    )

    invoice = pipeline.extract(
        "extract_invoice",
        depends_on=[classification],
        when=eq(classification.result.document_type, "invoice"),
        source=P.ocr.content,
        prompt="Extract invoice fields from the document. Return null for missing fields.",
        schema={
            "type": "object",
            "properties": {
                "invoice_number": {"type": ["string", "null"]},
                "vendor_name": {"type": ["string", "null"]},
                "total_amount": {"type": ["number", "null"]},
                "currency": {"type": ["string", "null"]},
            },
        },
        llm_tier="mini",
    )

    sds = pipeline.extract(
        "extract_sds",
        depends_on=[classification],
        when=eq(classification.result.document_type, "safety_data_sheet"),
        source=P.ocr.content,
        prompt="Extract product and safety information from the document.",
        schema={
            "type": "object",
            "properties": {
                "product_name": {"type": ["string", "null"]},
                "supplier": {"type": ["string", "null"]},
                "signal_word": {"type": ["string", "null"]},
            },
        },
        llm_tier="mini",
    )

    invoice_final = pipeline.final_candidate(
        "invoice_final",
        depends_on=[classification, invoice],
        when=eq(classification.result.document_type, "invoice"),
        output={
            "branch": "invoice",
            "classification": classification.result,
            "extracted": invoice.result,
        },
    )
    sds_final = pipeline.final_candidate(
        "sds_final",
        depends_on=[classification, sds],
        when=eq(classification.result.document_type, "safety_data_sheet"),
        output={
            "branch": "safety_data_sheet",
            "classification": classification.result,
            "extracted": sds.result,
        },
    )
    other_final = pipeline.final_candidate(
        "other_final",
        depends_on=[classification],
        when=eq(classification.result.document_type, "other"),
        output={
            "branch": "other",
            "classification": classification.result,
            "message": "No specialized extraction branch was selected.",
        },
    )
    pipeline.final_order(invoice_final, sds_final, other_final)

    with open("report.pdf", "rb") as f:
        document_data = f.read()

    guid = await client.process_pipeline_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="AUTO",
        pipeline=pipeline,
    )

    result = await client.wait_until_ready(guid, result_format="json")
    print(json.dumps(result, indent=2))

    nodes = await client.get_pipeline_nodes(guid)
    print(json.dumps(nodes, indent=2))
    # docs:end


asyncio.run(main())
