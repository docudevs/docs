import json

# docs:start
from docudevs import P, Pipeline

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
    schema={"type": "object", "properties": {"name": {"type": "string"}}},
)

pipeline.final(
    "final_result",
    depends_on=[quality, extracted],
    output={"quality": quality.result, "extracted": extracted.result},
)

print(json.dumps(pipeline.to_dict(), indent=2))
# docs:end
