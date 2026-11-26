import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the playbook's case

    # A dry run reads the files and validates the mapping without writing anything.
    # Both calls raise an error that names the row and the value at the first cell that cannot be mapped.
    dry_run = await client.import_gold(case_id, "gold.xlsx", "mapping.yaml", dry_run=True)
    print(dry_run.summary())

    imported = await client.import_gold(case_id, "gold.xlsx", "mapping.yaml")
    print(imported.summary())
    # docs:end


asyncio.run(main())
