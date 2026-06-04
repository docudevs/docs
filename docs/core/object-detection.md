---
title: Object Detection
description: Detect visible objects in images and return normalized bounding boxes as structured JSON.
sidebar_position: 10
---

# Object Detection

DocuDevs can detect visible objects in normal images by using the standard structured-extraction flow with an image-specific schema.

Use this when you want the model to return bounding boxes directly in the extraction result.

Typical examples:

- soccer players, the ball, and field markings in a sports photo
- pallets, forklifts, and boxes in a warehouse image
- tools, safety gear, and signage in an inspection image

## What You Get

Object detection returns normal structured JSON. The bounding boxes are part of your result payload, not a separate artifact.

Example shape:

```json
{
  "scene_summary": "A soccer match in progress near the goal.",
  "objects": [
    {
      "name": "goalkeeper in green jersey",
      "object_type": "player",
      "description": "Goalkeeper standing in front of the net.",
      "bbox": {
        "left": 0.61,
        "top": 0.18,
        "right": 0.74,
        "bottom": 0.78
      },
      "confidence": 0.93
    }
  ]
}
```

## Coordinate System

`bbox.left`, `bbox.top`, `bbox.right`, and `bbox.bottom` use normalized image coordinates from `0..1`.

- origin is at the top-left of the image
- `left < right`
- `top < bottom`
- the box should cover the visible extent of the object only

This is different from [Source Locations](./source-locations.md). Source locations are a separate evidence artifact for extracted text fields, while object-detection boxes are returned directly in your structured result.

## Python SDK Example

The example below mirrors the public notebook `06-source-locations.ipynb`, which detects visible objects in `soccer.jpg` and returns normalized bounding boxes.

```python
import json
import os

from docudevs import DocuDevsClient, json_schema
from pydantic import BaseModel, Field


class BoundingBox(BaseModel):
    left: float = Field(ge=0, le=1)
    top: float = Field(ge=0, le=1)
    right: float = Field(ge=0, le=1)
    bottom: float = Field(ge=0, le=1)


class DetectedObject(BaseModel):
    name: str = Field(description="Short human-readable name, such as 'ball' or 'player in red jersey'")
    object_type: str = Field(description="Category such as player, ball, goal, field_marking, or equipment")
    description: str = Field(description="Brief visual description that helps identify the boxed object")
    bbox: BoundingBox = Field(description="Normalized bounding box in image coordinates")
    confidence: float = Field(ge=0, le=1)


class SoccerObjectDetection(BaseModel):
    scene_summary: str
    objects: list[DetectedObject]


prompt = """
Detect the main visible objects in this soccer image.

Return visible objects only. Include soccer players, the ball, and any prominent
sports equipment or field markings that are clearly visible. Do not invent
objects that are hidden, implied, or outside the frame.

For each object:
- Use a concise source-image label in name.
- Set object_type to a short category such as player, ball, goal, field_marking, or equipment.
- Set bbox to the visible extent of the object only, not a group or surrounding whitespace.
- Use normalized coordinates from 0 to 1 in image space with origin at the top-left.
- Set confidence from 0 to 1.

Return JSON only. The JSON must match the provided schema.
""".strip()

client = DocuDevsClient(
    api_url=os.getenv("DOCUDEVS_API_URL", "https://api.docudevs.ai"),
    token=os.getenv("DOCUDEVS_API_KEY"),
)

with open("soccer.jpg", "rb") as f:
    job_guid = await client.submit_and_process_document(
        document=f.read(),
        document_mime_type="image/jpeg",
        prompt=prompt,
        schema=json_schema(SoccerObjectDetection),
        ocr="PREMIUM",
        llm="DEFAULT",
    )

result = await client.wait_until_ready(
    job_guid,
    timeout=600,
    poll_interval=5,
    result_format="json",
)

print(f"Job completed: {job_guid}")
print(json.dumps(result, indent=2, ensure_ascii=False))
```

## Draw Bounding Boxes Back onto the Image

After the job completes, render the normalized coordinates onto the original image so you can inspect the result visually.

```python
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle
from PIL import Image


image = Image.open("soccer.jpg").convert("RGB")


def clamp(value: float, minimum: float = 0.0, maximum: float = 1.0) -> float:
    return max(minimum, min(maximum, float(value)))


def bbox_to_pixels(bbox: dict[str, float]) -> tuple[float, float, float, float]:
    left = clamp(bbox["left"]) * image.width
    top = clamp(bbox["top"]) * image.height
    right = clamp(bbox["right"]) * image.width
    bottom = clamp(bbox["bottom"]) * image.height
    return left, top, max(right - left, 1), max(bottom - top, 1)


fig, ax = plt.subplots(figsize=(14, 14 * image.height / image.width))
ax.imshow(image)

colors = plt.cm.tab20.colors
for index, obj in enumerate(result["objects"]):
    left, top, width, height = bbox_to_pixels(obj["bbox"])
    color = colors[index % len(colors)]
    ax.add_patch(Rectangle((left, top), width, height, fill=False, linewidth=4, edgecolor=color))
    ax.text(
        left,
        max(top - 12, 16),
        f'{index + 1}. {obj["name"]} ({obj["confidence"]:.2f})'[:56],
        color="white",
        fontsize=10,
        bbox={"facecolor": color, "edgecolor": color, "pad": 3},
    )

ax.set_title(result["scene_summary"])
ax.axis("off")
plt.show()
```

## Best Practices

- Ask for visible objects only, not implied or hidden ones.
- Keep `object_type` short and stable so downstream code can group results reliably.
- Tell the model to box the object itself, not surrounding whitespace or nearby context.
- Validate the bounding-box shape in your schema so invalid coordinates are rejected early.
- Render the boxes back onto the image whenever you tune the prompt or schema.

## When to Use Source Locations Instead

Use [Source Locations](./source-locations.md) when you are extracting text fields from a document and want a separate page-aware evidence manifest for those fields.

Use object detection when the boxes themselves are the result you want to store and consume.
