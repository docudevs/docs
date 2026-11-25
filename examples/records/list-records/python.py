import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case

    # Follows the continuation tokens until every matching record was read
    async for record in client.list_records(case_id, subtree="4.12", attributes={"brand": "Foo"}):
        print(record["key"], record.get("label"))
    # docs:end


asyncio.run(main())
