import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # credential_id is the "id" returned when the credential was created or listed.
    credential_id = 42

    # 204 when deleted, 409 while a provider still references the credential
    response = await client.delete_model_credential(credential_id)
    print(f"HTTP {response.status_code.value}")
    # docs:end


asyncio.run(main())
