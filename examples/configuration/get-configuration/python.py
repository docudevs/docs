import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    response = await client.get_configuration("invoice-config")
    if response.status_code != 200:
        raise SystemExit(f"Get configuration failed: {response.status_code}")

    config = response.parsed

    print(f"Prompt: {config.prompt}")
    print(f"OCR: {config.ocr}")
    print(f"LLM: {config.llm}")
    # docs:end


asyncio.run(main())
