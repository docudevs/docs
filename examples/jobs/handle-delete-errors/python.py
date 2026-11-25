import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of the job to delete

    result = await client.delete_job(job_guid)

    if result.status_code == 200:
        print(f"Deleted successfully: {result.parsed['jobsDeleted']} job(s)")
    elif result.status_code == 404:
        print("Job not found - may already be deleted or purged")
    elif result.status_code == 409:
        errors = result.parsed.get("errors", []) if result.parsed else []
        print(f"Cannot delete: {'; '.join(errors)}")  # e.g., job still processing
    else:
        print(f"Unexpected status: {result.status_code}")
    # docs:end


asyncio.run(main())
