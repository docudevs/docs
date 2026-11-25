import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # revision_id is the revision "id" under provider.latestRevision when the provider was created.
    revision_id = 12

    response = await client.set_default_embedding_binding(revision_id)
    if response.status_code != 200:
        raise SystemExit(f"Bind failed: {response.status_code} {response.content.decode()}")

    # Counts of cases on the default revision, on older revisions and on the original index
    overview = await client.get_embedding_overview()
    if overview.status_code != 200:
        raise SystemExit(f"Overview failed: {overview.status_code}")
    print(json.dumps(json.loads(overview.content)["defaultBinding"], indent=2))
    # docs:end


asyncio.run(main())
