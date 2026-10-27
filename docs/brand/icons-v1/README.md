# Mascot icons, v1

Shawn's first set, cut from `../guru-mascot-sheet-v1.webp`. These are the untouched originals.

The ones the app uses are copied into `android/src/main/res/drawable-nodpi/`:
mascot_hero, the 5 expressions, the 5 poses, the 3 feature icons, scene_dinner and category_dinner.
The app copy of `mascot_hero.png` has a scrap of the logo lettering removed from its right edge.

Not in the app yet:
- `app_icon.png`: has rounded corners baked in. Needs a face-only layer plus a background layer for Android Studio's Image Asset tool.
- `style_*`: style explorations, not app art.
- Concert, travel, game night and day trip scenes/categories: outside the restaurants-only demo.

Known issues: small (100-350 px), so they blur if shown large. pose_relaxed (tail) and expression_helpful (hands) are cut off at the edges.
