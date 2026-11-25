import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # ENTRA_CLIENT_SECRET holds the client secret value copied from Entra.
    response = await client.create_model_credential(
        name="Enterprise AI gateway",
        description="Shared by chat and embeddings",
        auth_type="ENTRA_CLIENT_SECRET",
        tenant_id="00000000-0000-0000-0000-000000000000",
        client_id="11111111-1111-1111-1111-111111111111",
        secret=os.environ["ENTRA_CLIENT_SECRET"],
        scope="api://22222222-2222-2222-2222-222222222222/.default",
        cloud="PUBLIC",
        secret_expires_at="2027-08-25T00:00:00Z",
    )
    if response.status_code not in (200, 201):
        raise SystemExit(f"Create failed: {response.status_code} {response.content.decode()}")

    credential = json.loads(response.content)
    print(f"Credential {credential['id']}: {credential['status']}")
    # docs:end


asyncio.run(main())
