# Favicon 使用指南

基于主 Logo (`主logo.svg`) 生成所需的 Favicon 文件。

## 在线生成工具推荐

使用以下网站可以快速生成所有必需的 Favicon 格式：

1. **Real Favicon Generator** (https://realfavicongenerator.net/)
   - 上传 `主logo.svg`
   - 自动生成所有尺寸和格式
   - 下载后按本目录结构放置

2. **Favicon.io** (https://favicon.io/)
   - 从 SVG 文本生成
   - 简单快捷

## 必需的文件清单

将生成的文件放入对应目录：

```
assets/img/brand/
├── logo/
│   └── 主logo.svg (已有)
├── favicon/
│   ├── favicon-16x16.png    (浏览器标签页图标)
│   ├── favicon-32x32.png    (书签栏图标)
│   ├── apple-touch-icon.png (180x180, iOS 主屏幕图标)
│   └── android-chrome-192x192.png (Android 主屏幕图标)
└── banner/
    └── og-image.png         (1200x630, 社交媒体分享缩略图)
```

## HTML 引用方式

```html
<!-- Favicon -->
<link rel="icon" type="image/svg+xml" href="assets/img/brand/logo/主logo.svg">
<link rel="alternate icon" type="image/png" href="assets/img/brand/favicon/favicon-32x32.png">

<!-- Apple Touch Icon -->
<link rel="apple-touch-icon" sizes="180x180" href="assets/img/brand/favicon/apple-touch-icon.png">

<!-- OG Image (社交媒体分享) -->
<meta property="og:image" content="assets/img/brand/banner/og-image.png">
```

## Logo 使用规范

- **导航栏**: 使用 `主logo.svg`, 高度 32-40px
- **登录页**: 使用 `主logo.svg`, 高度 80-120px
- **Favicon**: 使用 PNG 版本, 16x16 或 32x32

## 颜色适配

由于 Logo 是黑色的线条图：
- **浅色背景**: 直接使用
- **深色背景**: 需要白色版本 `主logo-white.svg`

如需生成白色版本，可以使用 SVG 编辑器将 `fill="#000000"` 改为 `fill="#ffffff"`。
