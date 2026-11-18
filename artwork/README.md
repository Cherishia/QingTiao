# 轻跳无字桌面图标制作说明

制作模式：内置 imagegen 编辑模式，非 CLI。

最终参考图片为用户提供的“奶油背景下的蓝绿色披风少女.png”，原文件未修改。遵照最新要求，去除所有文字，改为适合桌面小尺寸显示的头肩头像，保留金色短发、白色蝴蝶结、蓝色眼睛、蓝绿色披风和奶油底色。完整正方形 PNG 位于 app/src/main/res/drawable-nodpi/qingtiao_cover.png；普通及圆形自适应图标和首页小图标共同引用。最终形状由桌面裁切。首页继续使用浅冰蓝纯色，不放人物背景图。

## 最终提示词

Use case: style-transfer / precise-object-edit. Edit target: the supplied cream-background illustration of a blonde girl with a white hair bow and blue-green cape. Create ONE polished Android LAUNCHER APP ICON from this exact character. The user specifically wants NO TEXT and a design that works as an app icon rather than a full illustration. Remove ALL text, including 轻跳, JUMP, letters and numbers. Crop and recompose to HEAD AND SHOULDERS ONLY: a large friendly face centered in the square, the white bow fully visible above, a small clear portion of the blue-green cape and navy collar below; exclude the seated body, hands, knees, boots and long trailing cape. Keep her recognizable blonde bob, original blue eyes, gentle smile, large white bow, hair clips, cape colors and original soft flat anime style. Simplify tiny hair/clothing details into clean broad shapes for readability at 48 pixels. Calm cream #FAF1DF background, soft restrained contrast, crisp beautiful silhouette, subtle flat depth, no heavy border or busy shading. Center the face around (50%,48%), with bow, head and shoulders forming a compact balanced avatar inside the central 72% and a circular launcher mask. Leave useful cream breathing room, while the face is large and recognizable. Output a SQUARE 1:1 full-bleed canvas; the system will apply rounded-square or circular clipping, so do not pre-round the outside corners. No title, typography, symbols, badges, watermark, frame, device mockup or icon sheet.
