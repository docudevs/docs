import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a records case

    hits = await client.search_records(
        case_id,
        "Soft sports sock",
        top_k=10,
        filters={"subtree": "4.12", "attributes": {"brand": "Foo"}},
    )

    for result in hits.get("results") or []:
        print(result["key"], result.get("label"), result.get("score"))


asyncio.run(main())
