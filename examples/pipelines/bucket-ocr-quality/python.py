import json

# docs:start
from pydantic import BaseModel

from docudevs import P, Pipeline


class ExtractedFields(BaseModel):
    name: str | None = None


pipeline = Pipeline().ocr(mode="AUTO", quality_artifact=True)

quality = pipeline.validate_bucket(
    "ocr_quality",
    input=P.ocr.quality.score,
    buckets=[
        {"name": "good", "min": 0.85},
        {"name": "review"},
    ],
    default="review",
)

extracted = pipeline.extract(
    "extract_data",
    source=P.ocr.content,
    prompt="Extract the requested fields.",
    schema=ExtractedFields.model_json_schema(),
)

pipeline.final(
    "final_result",
    depends_on=[quality, extracted],
    output={"quality": quality.result, "extracted": extracted.result},
)

print(json.dumps(pipeline.to_dict(), indent=2))
# docs:end
