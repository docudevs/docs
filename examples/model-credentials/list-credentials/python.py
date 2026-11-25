import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    response = await client.list_model_credentials()
    if response.status_code != 200:
        raise SystemExit(f"List failed: {response.status_code}")

    for credential in response.parsed or []:
        print(f"{credential.id}: {credential.name} ({credential.status})")
    # docs:end


asyncio.run(main())
