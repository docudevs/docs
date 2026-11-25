import asyncio
import os
from dataclasses import dataclass

# docs:start
from docudevs import ClientPipeline, P
# docs:end
from docudevs.docudevs_client import DocuDevsClient


# docs:start
@dataclass
class TopicCount:
    count: int


def count_topics(*, summary) -> TopicCount:
    topics = (summary or {}).get("topics") or []
    return TopicCount(count=len(topics))


# docs:end
async def main():
    client = DocuDevsClient(api_url="https://api.docudevs.ai", token=os.environ["API_KEY"])

    # docs:start
    cp = ClientPipeline()

    summary = cp.extract(
        "summary",
        prompt="List the distinct topics this document covers.",
        schema={
            "type": "object",
            "properties": {"topics": {"type": "array", "items": {"type": "string"}}},
            "required": ["topics"],
        },
        source=P.ocr.content,
    )

    counted = cp.local_function(
        "counted",
        count_topics,
        input={"summary": summary.result},
        output_type=TopicCount,
    )

    verdict = cp.extract(
        "verdict",
        prompt=(
            "You are given a topic count computed from this document. "
            "Answer whether it matches the number of distinct topics you see."
        ),
        schema={
            "type": "object",
            "properties": {"topic_count_matches": {"type": "boolean"}},
            "required": ["topic_count_matches"],
        },
        input={"counts": counted.result},
    )
    cp.final(verdict)

    with open("report.pdf", "rb") as handle:
        run = await client.run_client_pipeline(
            handle.read(),
            "application/pdf",
            pipeline=cp,
            timeout=900.0,
            poll_interval=2.0,
            on_event=lambda name, payload: print(name, payload),
        )

    print(run.final_result)
    # docs:end


asyncio.run(main())
