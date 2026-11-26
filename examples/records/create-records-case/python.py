import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # One call creates the case and configures its records dataset.
    # Raises RecordsApiError when the case or the dataset is refused.
    case = await client.create_records_case(
        "Product catalogue",
        description="Reference products for matching",
        key="sku",
        text={"title": "title", "desc": "description"},
        label="category",
        metadata={"brand": "brand"},
        embedded_text="{title}\n{description}",
        index_settings={"top_k": 10, "hybrid": False},
    )
    print(f"Records case {case.id}")
    # docs:end


asyncio.run(main())
