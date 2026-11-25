import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # credential_id is the "id" returned when the credential was created or listed.
    # NEW_PROVIDER_API_KEY holds the replacement secret.
    credential_id = 42

    response = await client.rotate_model_credential(
        credential_id, os.environ["NEW_PROVIDER_API_KEY"]
    )
    if response.status_code != 200:
        raise SystemExit(f"Rotate failed: {response.status_code}")

    print(f"Credential version: {response.parsed.credential_version}")
    # docs:end


asyncio.run(main())
