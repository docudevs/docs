import asyncio
import json
import os

# docs:start
from docudevs import P, Pipeline
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    pipeline = Pipeline().ocr(mode="AUTO", source_locations=True, quality_artifact=True)
    extract_data = pipeline.extract(
        "extract_data",
        source=P.ocr.content,
        prompt="Extract the requested fields.",
        schema={
            "type": "object",
            "properties": {"name": {"type": "string"}},
        },
        source_locations=True,
    )
    pipeline.final(
        "final_result",
        depends_on=[extract_data],
        output=extract_data.result,
    )

    with open("report.pdf", "rb") as f:
        document_data = f.read()

    guid = await client.process_pipeline_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="AUTO",
        pipeline=pipeline,
        trace=True,
    )

    result = await client.wait_until_ready(guid, result_format="json")
    print(json.dumps(result, indent=2))

    sources = await client.get_source_locations(guid)
    print(json.dumps(sources, indent=2))

    nodes = await client.get_pipeline_nodes(guid)
    print(json.dumps(nodes, indent=2))
    # docs:end


asyncio.run(main())
