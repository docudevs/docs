import asyncio
import os

from docudevs import ClientPipeline, eq
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(api_url="https://api.docudevs.ai", token=os.environ["API_KEY"])

    # docs:start
    cp = ClientPipeline()

    route = cp.local_function("route", lambda: {"positive": True})
    guarded = cp.extract(
        "guarded",
        prompt="Return one object with boolean field observed.",
        schema={
            "type": "object",
            "properties": {"observed": {"type": "boolean"}},
            "required": ["observed"],
        },
        when=eq(route.result.positive, True),
    )
    assembled = cp.local_function(
        "assembled",
        lambda *, route, guarded: {"positive": route["positive"], "guarded": guarded},
        input={"route": route.result, "guarded": guarded.result},
    )
    cp.final(assembled)

    with open("report.pdf", "rb") as handle:
        run = await client.run_client_pipeline(
            handle.read(),
            "application/pdf",
            pipeline=cp,
        )

    print(run.final_result)
    # docs:end


asyncio.run(main())
