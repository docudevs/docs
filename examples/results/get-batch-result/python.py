import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    batch_guid = "<batch-guid>"  # GUID of a batch whose processing has finished

    # Batch overview: state and one entry per document
    response = await client.result(batch_guid)
    if response.status_code != 200:
        raise RuntimeError(f"HTTP {response.status_code}")
    overview = json.loads(response.content)
    print(f"state: {overview.get('state')}, completed: {overview.get('completed', 0)}, failed: {overview.get('failed', 0)}")
    for document in overview.get("documents", []):
        print(f"document {document['index']}: {document['status']}")

    # Only the results, in upload order; a document without a result is None
    results = await client.result_json(batch_guid)
    print(json.dumps(results, indent=2))
    # docs:end


asyncio.run(main())
