import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a case that holds documents and no records dataset

    docs = await client.search_documents(case_id, "termination notice period", top_k=5)

    for result in docs.get("results") or []:
        print(result.get("filename"), "page", result.get("pageNumber"))
    # docs:end


asyncio.run(main())
