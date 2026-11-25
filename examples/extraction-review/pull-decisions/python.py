import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    cursor = 0  # nextCursor from the previous pull; 0 for the first pull

    page = await client.pull_extraction_review_decisions(after_ordinal=cursor, limit=100)

    for event in page.parsed["events"]:
        print(event["cursorOrdinal"], event["eventId"], event["kind"])

    # Persist nextCursor exactly as returned, even when events is empty
    cursor = page.parsed["nextCursor"]
    print(f"Next cursor: {cursor}")
    # docs:end


asyncio.run(main())
