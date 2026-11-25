import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a records case with an active category tree

    rows = [
        {
            "key": "6410000999999",
            "text": {"title": "Running socks 3-pack", "description": "Cotton blend"},
        },
    ]

    # Raises RecordsApiError when the request is refused
    accepted = await client.classify(
        case_id,
        rows,
        config={"target": "category", "llm_level": "MINI", "target_depth": [4, 5]},
    )
    print(f"Classify job {accepted['jobId']}: {accepted['status']}")


asyncio.run(main())
