import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # provider_id is the "id" returned when the provider was created or listed.
    provider_id = 7

    response = await client.delete_llm_provider(provider_id)
    if response.status_code != 204:
        raise SystemExit(f"Delete failed: {response.status_code}")

    print(f"HTTP {response.status_code}")
    # docs:end


asyncio.run(main())
