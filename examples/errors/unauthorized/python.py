import asyncio

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token="invalid-key")

    response = await client.list_cases()
    if response.status_code == 401:
        print("Authentication failed (401)")


asyncio.run(main())
