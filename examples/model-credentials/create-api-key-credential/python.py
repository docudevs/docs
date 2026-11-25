import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # PROVIDER_API_KEY holds the API key issued by your model provider.
    response = await client.create_model_credential(
        name="Provider embeddings",
        auth_type="API_KEY",
        secret=os.environ["PROVIDER_API_KEY"],
    )
    if response.status_code not in (200, 201):
        raise SystemExit(f"Create failed: {response.status_code} {response.content.decode()}")

    credential = json.loads(response.content)
    print(f"Credential {credential['id']}: {credential['status']}")
    # docs:end


asyncio.run(main())
