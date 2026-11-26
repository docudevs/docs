import json

# docs:start
from pydantic import BaseModel

from docudevs import P, Pipeline


class ExtractedFields(BaseModel):
    name: str | None = None


pipeline = Pipeline().ocr(mode="AUTO", source_locations=True)

ocr_corrected = pipeline.ocr_correct(
    "ocr_corrected",
    source=P.ocr.content,
    mode="correction",
    scope="pagewise",
    source_map="preserve",
    llm_tier="mini",
)

extracted = pipeline.extract(
    "extract_corrected",
    depends_on=[ocr_corrected],
    source=ocr_corrected.result.content,
    prompt="Extract fields from the corrected OCR text.",
    schema=ExtractedFields.model_json_schema(),
    source_locations=True,
)
pipeline.final("final_result", depends_on=[extracted], output=extracted.result)

print(json.dumps(pipeline.to_dict(), indent=2))
# docs:end
