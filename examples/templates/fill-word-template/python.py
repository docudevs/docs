import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient
from docudevs.models import TemplateFillRequest


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    fill_request = TemplateFillRequest(
        fields={
            "reportTitle": "Quarterly Sales",
            "date": "2024-04-01",
            "items": [
                {"product": "Widget A", "sales": 100, "revenue": 5000},
                {"product": "Widget B", "sales": 200, "revenue": 8000},
                {"product": "Widget C", "sales": 50, "revenue": 2500},
            ],
            "summary": {
                "totalRevenue": 15500,
                "growth": "15%",
            },
        }
    )

    response = await client.fill(name="sales_report", body=fill_request)

    with open("filled_report.docx", "wb") as f:
        f.write(response.content)
    print("Saved filled_report.docx")
    # docs:end


asyncio.run(main())
