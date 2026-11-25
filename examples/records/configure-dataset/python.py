import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a case that holds no documents

    # Raises RecordsApiError when the request is refused
    dataset = await client.set_dataset(
        case_id,
        {
            "key": "sku",
            "text": {"title": "title", "desc": "description"},
            "label": "category",
            "metadata": {"brand": "brand"},
        },
        embedded_text={"template": "{title}\n{description}"},
        index_settings={"top_k": 10, "hybrid": False},
    )
    print(f"Mapping version: {dataset['mappingVersion']}")


asyncio.run(main())
