import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    schema = {
        "type": "object",
        "properties": {
            "scene_summary": {"type": "string"},
            "objects": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "name": {
                            "type": "string",
                            "description": "Short human-readable name, such as ball or player in red jersey",
                        },
                        "object_type": {
                            "type": "string",
                            "description": "Category such as player, ball, goal, field_marking, or equipment",
                        },
                        "description": {
                            "type": "string",
                            "description": "Brief visual description that helps identify the boxed object",
                        },
                        "bbox": {
                            "type": "object",
                            "description": "Normalized bounding box in image coordinates",
                            "properties": {
                                "left": {"type": "number", "minimum": 0, "maximum": 1},
                                "top": {"type": "number", "minimum": 0, "maximum": 1},
                                "right": {"type": "number", "minimum": 0, "maximum": 1},
                                "bottom": {"type": "number", "minimum": 0, "maximum": 1},
                            },
                            "required": ["left", "top", "right", "bottom"],
                        },
                        "confidence": {"type": "number", "minimum": 0, "maximum": 1},
                    },
                    "required": ["name", "object_type", "description", "bbox", "confidence"],
                },
            },
        },
        "required": ["scene_summary", "objects"],
    }

    prompt = """Detect the main visible objects in this soccer image.

Return visible objects only. Include soccer players, the ball, and any prominent
sports equipment or field markings that are clearly visible. Do not invent
objects that are hidden, implied, or outside the frame.

For each object:
- Use a concise source-image label in name.
- Set object_type to a short category such as player, ball, goal, field_marking, or equipment.
- Set bbox to the visible extent of the object only, not a group or surrounding whitespace.
- Use normalized coordinates from 0 to 1 in image space with origin at the top-left.
- Set confidence from 0 to 1.

Return JSON only. The JSON must match the provided schema."""

    with open("soccer.jpg", "rb") as f:
        image_data = f.read()

    job_id = await client.submit_and_process_document(
        document=image_data,
        document_mime_type="image/jpeg",
        prompt=prompt,
        schema=json.dumps(schema),
        ocr="PREMIUM",
        llm="DEFAULT",
    )

    result = await client.wait_until_ready(
        job_id,
        timeout=600,
        poll_interval=5,
        result_format="json",
    )
    print(json.dumps(result, indent=2, ensure_ascii=False))
    # docs:end


asyncio.run(main())
