import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    api_key = os.getenv("API_KEY")
    if not api_key:
        raise ValueError("API_KEY environment variable not set")

    client = DocuDevsClient(token=api_key)

    # List your saved configurations to confirm the API key works
    response = await client.list_configurations()

    if response.status_code == 200:
        print(f"Connection successful! Status code: {response.status_code}")
    else:
        print(f"Connection failed! Status code: {response.status_code}")


asyncio.run(main())
