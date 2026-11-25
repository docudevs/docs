import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the rows; it may differ from the catalogue

    # Raises RecordsApiError when the request is refused
    accepted = await client.classify(
        case_id,
        [{"key": "line-1", "text": {"title": "M8 hex bolt 40 mm, 100 pcs"}}],
        config={
            "target": "record",
            "records_knowledge_base_id": 51,  # id of the catalogue's records case
            "candidate_strategy": {"similar_records": {"k": 20}},
            "instructions": "Match the invoice line to the catalogue product with the same item and pack size.",
            "routing": {"review_when": ["declined", "modelNamedDifferentBestFit"]},
        },
    )
    print(f"Classify job {accepted['jobId']}: {accepted['status']}")
    # docs:end


asyncio.run(main())
