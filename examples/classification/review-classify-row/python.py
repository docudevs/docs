import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    job_id = 9  # jobId returned when the job was started
    row_id = 17  # rowId of a result row

    review = await client.review_classify(
        case_id,
        job_id,
        row_id,
        "correct",
        code="4.12.3.1",
        reason="Plain cotton sock, no sport use.",
    )
    write = review.get("labelWrite") or {}
    print(review["reviewId"], review["decision"], write.get("importId"), write.get("status"))
    # docs:end


asyncio.run(main())
