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

## Detect objects in an image

The example detects visible objects in `soccer.jpg` and returns normalized bounding boxes. The schema asks for a scene summary and a list of objects, each with a name, a type, a description, a `bbox` and a confidence. The `minimum` and `maximum` bounds on the coordinates state the normalized range to the model; check the values in your own code before you draw.

<Example name="extraction/detect-objects" />

## Draw Bounding Boxes Back onto the Image

After the job completes, render the normalized coordinates onto the original image so you can inspect the result visually. This Python snippet runs locally on the `result` from the example above and makes no DocuDevs call; it needs `matplotlib` and `Pillow`.

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
- State the coordinate range in your schema (`minimum` 0, `maximum` 1) and check the boxes in your own code before you use them.
- Render the boxes back onto the image whenever you tune the prompt or schema.

## When to Use Source Locations Instead

Use [Source Locations](./source-locations.md) when you are extracting text fields from a document and want a separate page-aware evidence manifest for those fields.

Use object detection when the boxes themselves are the result you want to store and consume.
