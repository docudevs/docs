import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # provider_id is the "id" returned when the provider was created or listed.
    provider_id = 7

    # List the current bindings of DEFAULT, NANO, MINI and HIGH
    keys = await client.list_llm_keys()
    if keys.status_code != 200:
        raise SystemExit(f"List failed: {keys.status_code}")
    print(keys.json())

    # Bind the HIGH level to the provider; pass None to clear a binding
    response = await client.update_llm_key_binding("HIGH", provider_id)
    if response.status_code != 200:
        raise SystemExit(f"Bind failed: {response.status_code}")
    # docs:end


asyncio.run(main())
