"""Render the Adaptive Apiary logo from the original bee sprite using Pillow."""

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps


SIZE = 400
HONEY = "#F5A623"
HONEY_LIGHT = "#FFE07A"
HONEY_DARK = "#C07614"
COPPER_DARK = "#5E3A18"
PARCHMENT = "#FBF1D4"


def make_logo(bee):
    background = Image.new("RGBA", (SIZE, SIZE), "#110D09")
    draw = ImageDraw.Draw(background)
    draw.rounded_rectangle((25, 25, 374, 374), radius=7, fill="#20170F",
                           outline=COPPER_DARK, width=5)
    draw.rounded_rectangle((27, 27, 372, 372), radius=5, outline=HONEY_DARK, width=3)
    draw.line((32, 28, 367, 28), fill=HONEY_LIGHT, width=2)
    draw.line((28, 33, 28, 366), fill=HONEY, width=2)
    draw.rectangle((35, 35, 364, 364), fill="#18120D", outline="#73502A", width=2)

    # Keep the original pixel silhouette and use the mod's warm colors.
    bee = bee.crop(bee.getbbox())
    amber = ImageOps.colorize(ImageOps.grayscale(bee), "#321D10", PARCHMENT,
                              mid=HONEY_DARK, blackpoint=15, midpoint=143, whitepoint=235)
    amber.putalpha(bee.getchannel("A"))
    bee = amber.resize((amber.width * 13, amber.height * 13), Image.Resampling.NEAREST)
    x = (SIZE - bee.width) // 2
    y = (SIZE - bee.height) // 2
    shadow = Image.new("RGBA", bee.size, "#0B0806")
    shadow.putalpha(bee.getchannel("A"))
    background.alpha_composite(shadow, (x + 3, y + 4))
    background.alpha_composite(bee, (x, y))
    return background


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("bee", type=Path, help="Original transparent bee sprite")
    parser.add_argument("output", type=Path, help="Output PNG path")
    args = parser.parse_args()
    with Image.open(args.bee) as source:
        bee = source.convert("RGBA")
    if bee.size != (16, 16) or bee.getbbox() is None:
        parser.error("the bee sprite must be a nonempty 16×16 PNG")
    make_logo(bee).save(args.output, optimize=True)


if __name__ == "__main__":
    main()
