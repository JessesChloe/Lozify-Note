# Lozify - UI Design Specification

## 1. Design Philosophy

**Core Principles:**
- **Speed over perfection**: Fast note capture is the primary goal
- **Visual clarity**: Clean cards with generous whitespace
- **Minimal chrome**: No heavy navigation bars or complex menus
- **Context over hierarchy**: Tags replace folder structures

**Design Inspiration:** flomo's minimalist card-based interface

## 2. Color Palette

### 2.1 Base Colors
```kotlin
// Background
val BackgroundColor = Color(0xFFF7F8FA)  // Light gray-white

// Card & Surfaces
val CardBackground = Color(0xFFFFFFFF)   // Pure white
val CardBorder = Color(0xFFE5E7EB)       // Subtle gray border (optional)

// Text
val TextPrimary = Color(0xFF1F2937)      // Near-black for body text
val TextSecondary = Color(0xFF6B7280)    // Gray for metadata

// Accent Colors
val AccentGreen = Color(0xFF00C853)      // FAB, primary actions
val AccentBlue = Color(0xFF4C88FF)       // Tags, @mentions, expand button
val HighlightYellow = Color(0xFFFFF3C4)  // Text highlight background
val ErrorRed = Color(0xFFEF4444)         // Delete button
```

### 2.2 Color Usage Rules
- **Green (#00C853)**: Reserved for FAB and primary action buttons
- **Blue (#4C88FF)**: All #tags and @mentions (managed by theme, not per-tag)
- **Yellow (#FFF3C4)**: Text highlight background only
- **Red (#EF4444)**: Destructive actions (delete) only
- **No custom tag colors**: All tags use the same blue for visual consistency

## 3. Typography

### 3.1 Font Family
- **Primary Font**: System Default (Roboto on Android)
- **Fallback**: Sans-serif stack

### 3.2 Type Scale
```kotlin
// Material3 Typography
val Typography = Typography(
    // Card content body
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        color = TextPrimary
    ),
    // Metadata (timestamps, counts)
    bodySmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        color = TextSecondary
    ),
    // Top bar titles
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    ),
    // Button labels
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )
)
```

### 3.3 Text Formatting Styles
```kotlin
// Bold text
SpanStyle(fontWeight = FontWeight.Bold)

// Underline
SpanStyle(textDecoration = TextDecoration.Underline)

// Yellow highlight
SpanStyle(background = Color(0xFFFFF3C4))

// #Tag / @Mention
SpanStyle(color = Color(0xFF4C88FF))
```

## 4. Layout & Spacing

### 4.1 Grid System
- **Base unit**: 4dp
- **Common spacing**: 8dp, 12dp, 16dp, 24dp
- **Screen padding**: 16dp horizontal margins

### 4.2 Card Specifications
```kotlin
// NoteCard dimensions
cardElevation = 0.dp              // Flat design, no shadow
cardShape = RoundedCornerShape(12.dp)
cardPadding = 16.dp               // Internal padding
cardMarginBottom = 12.dp          // Spacing between cards
cardBackgroundColor = Color.White
```

### 4.3 Component Sizes
```kotlin
// FAB (Floating Action Button)
fabSize = 56.dp
fabIcon = 24.dp
fabColor = AccentGreen

// Bottom sheet drag handle
handleWidth = 32.dp
handleHeight = 4.dp
handleColor = Color(0xFFD1D5DB)

// Image thumbnails in 3-column grid
thumbnailSize = (screenWidth - 32.dp - 8.dp) / 3  // 4dp gaps
thumbnailCornerRadius = 8.dp

// Touch targets (minimum)
minTouchTarget = 48.dp
```

## 5. Screen Layouts

### 5.1 Home Screen (Main Feed)
```
┌─────────────────────────────────┐
│  [☰]  Lozify           [Search] │  ← Top bar (optional in MVP)
├─────────────────────────────────┤
│                                 │
│  ┌───────────────────────────┐ │
│  │ Note content here...      │ │
│  │ #tag #another             │ │
│  │                           │ │
│  │ [📷] [📷] [📷]            │ │  ← 3-column image grid
│  │ [📷] [📷]                 │ │
│  │                           │ │
│  │ 2 hours ago          [...] │  ← Timestamp + menu
│  └───────────────────────────┘ │
│                                 │
│  ┌───────────────────────────┐ │
│  │ Another note...           │ │
│  │ #tag                      │ │
│  │ 展开                      │  ← Expand button (5+ lines)
│  │ 1 day ago            [...] │
│  └───────────────────────────┘ │
│                                 │
│                          [ + ] │  ← FAB (green)
└─────────────────────────────────┘
```

**Key Elements:**
- Top bar: Drawer icon (left) + title + search icon (right)
- Cards: White background, 12dp rounded, 16dp side margins
- FAB: Fixed at bottom-right, 16dp from edges

### 5.2 Note Editor (Bottom Sheet)
```
┌─────────────────────────────────┐
│         ━━━━━                   │  ← Drag handle
├─────────────────────────────────┤
│ [×]                      [✓]    │  ← Cancel + Save
├─────────────────────────────────┤
│                                 │
│ [Multiline text input area...]  │
│                                 │
│ #tag @mention **bold** text     │  ← Rich text preview
│                                 │
│                                 │
├─────────────────────────────────┤
│ [#] [📷] [B] [☐] [...]          │  ← Toolbar
└─────────────────────────────────┘
```

**Key Elements:**
- Height: 60% of screen (half-screen modal)
- Drag handle: Swipe down to dismiss
- Toolbar: Fixed at bottom, 56dp height
- Auto-save on dismiss (no explicit save button needed, but show checkmark for clarity)

### 5.3 Side Drawer (Left Menu)
```
┌─────────────────────────────────┐
│                                 │
│  [User Avatar/Name]             │  ← Header (future)
│                                 │
│  ┌─────────────────────────┐   │
│  │ Contribution Heatmap    │   │  ← Placeholder area
│  │ [░░▓▓░░▓░░░░░░▓▓░░░]   │   │
│  └─────────────────────────┘   │
│                                 │
│  All Notes               (128) │  ← Default view
│                                 │
│  TAGS                           │
│  #work                    (45) │
│  #ideas                   (32) │
│  #读书笔记                  (18) │
│  #meeting                  (9) │
│                                 │
└─────────────────────────────────┘
```

**Key Elements:**
- Width: 80% of screen width (max 320dp)
- Background: White
- Tag list: Tappable items with note counts
- Selected tag: Highlighted background (#F3F4F6)

### 5.4 Card Operations Menu (Bottom Sheet)
```
┌─────────────────────────────────┐
│         ━━━━━                   │
├─────────────────────────────────┤
│                                 │
│  [Share Icon] [Edit Icon] [Copy Icon]  │  ← Top 3 actions
│    Share        Edit       Copy  │
│                                 │
├─────────────────────────────────┤
│  Pin to Top                     │
│  Related Notes              >   │
│  Version History            >   │
├─────────────────────────────────┤
│  Delete                         │  ← Red text
└─────────────────────────────────┘
```

**Key Elements:**
- Top row: Icon + label for primary actions
- Middle: List items with right chevron
- Delete: Separated by divider, red (#EF4444) text

## 6. Component Specifications

### 6.1 Note Card
**Anatomy:**
```
┌─────────────────────────────────┐
│ [16dp padding all sides]        │
│                                 │
│ Note content text here with     │  ← Body text (16sp)
│ #tag highlighting and @mentions │
│                                 │
│ [Image Grid - if images exist]  │
│                                 │
│ [Expand button - if >5 lines]   │  ← Blue link text
│                                 │
│ 2 hours ago          [... icon] │  ← Footer (14sp gray)
└─────────────────────────────────┘
```

**States:**
- **Default**: White background, no border
- **Hover** (on tablets): Light gray background (#F9FAFB)
- **Pressed**: Slight scale down (0.98) with haptic feedback

### 6.2 Text Folding Logic
```kotlin
// Pseudo-code
if (lineCount > 5) {
    Text(
        text = noteContent,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis
    )
    TextButton(
        text = "展开",  // Chinese: "Expand"
        color = AccentBlue,
        onClick = { expanded = true }
    )
} else {
    Text(text = noteContent)
}
```

**Interaction:**
- Initial state: Show first 5 lines + "展开" button
- Tapped: Expand to full content inline (no navigation)
- Expanded state persists until card scrolls off screen

### 6.3 Image Grid Layout
**Rules:**
- 1 image: Full-width, aspect ratio preserved, max height 240dp
- 2-9 images: 3-column grid with square thumbnails

**3-Column Grid Calculation:**
```kotlin
val screenWidth = LocalConfiguration.current.screenWidthDp.dp
val cardPadding = 16.dp
val imageSpacing = 4.dp

val availableWidth = screenWidth - (cardPadding * 2)
val thumbnailSize = (availableWidth - (imageSpacing * 2)) / 3

LazyVerticalGrid(
    columns = GridCells.Fixed(3),
    horizontalArrangement = Arrangement.spacedBy(imageSpacing),
    verticalArrangement = Arrangement.spacedBy(imageSpacing)
) {
    items(images) { imageUri ->
        AsyncImage(
            model = imageUri,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(thumbnailSize)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}
```

### 6.4 Tag Rendering
**Visual Style:**
```kotlin
// Inline with text, no background chip
ClickableText(
    text = buildAnnotatedString {
        append("Regular text ")
        withStyle(SpanStyle(color = Color(0xFF4C88FF))) {
            pushStringAnnotation(tag = "TAG", annotation = "work")
            append("#work")
            pop()
        }
        append(" more text")
    },
    onClick = { offset ->
        // Handle tag click → filter feed
    }
)
```

**Not using Material Chips:** Tags are inline styled text, not separate chip components, to maintain text flow and minimize visual noise.

### 6.5 FAB (Floating Action Button)
```kotlin
FloatingActionButton(
    onClick = { /* Open editor */ },
    containerColor = Color(0xFF00C853),  // Green
    modifier = Modifier
        .padding(16.dp)
        .size(56.dp)
) {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "Create note",
        tint = Color.White
    )
}
```

**Position:** Bottom-right corner with 16dp margins from screen edges.

## 7. Interactions & Animations

### 7.1 Bottom Sheet Transitions
- **Entry:** Slide up from bottom (300ms, easing: FastOutSlowIn)
- **Exit:** Slide down to bottom (250ms, easing: FastOutLinearIn)
- **Drag dismiss:** Follow finger with spring animation

### 7.2 Card Tap Feedback
- **Ripple effect:** Material3 default ripple
- **Scale animation:** None (keep simple)
- **Haptic feedback:** Light tap vibration (HapticFeedbackConstants.CLICK)

### 7.3 List Animations
- **New card appears:** Fade in + slide down (200ms)
- **Card delete:** Fade out + shrink vertically (300ms)
- **Scroll behavior:** Standard LazyColumn (no custom overscroll)

### 7.4 Tag Filter Transition
- **Drawer closes:** Slide out left (250ms)
- **Feed updates:** Crossfade between filtered/unfiltered (300ms)
- **Top bar title change:** Text crossfade (200ms)

## 8. Accessibility

### 8.1 Content Descriptions
```kotlin
// FAB
contentDescription = "Create new note"

// Card menu button
contentDescription = "Note options"

// Expand button
contentDescription = "Expand full note"

// Tag items in drawer
contentDescription = "Filter by tag: work, 45 notes"
```

### 8.2 Touch Target Sizes
- Minimum: 48dp × 48dp (Material Design guideline)
- FAB: 56dp × 56dp (already compliant)
- Toolbar buttons: 48dp × 48dp
- Drawer tag items: Full width, 56dp height

### 8.3 Color Contrast
- Text on white: #1F2937 (near-black) → 14.6:1 contrast ✓
- Blue tags: #4C88FF on white → 4.7:1 contrast ✓
- Yellow highlight: #FFF3C4 with dark text → maintain 4.5:1 ✓

## 9. Responsive Design

### 9.1 Screen Size Adaptations
- **Small phones (<360dp)**: Single column, FAB may overlap last card slightly
- **Standard phones (360-480dp)**: Optimal experience (design target)
- **Large phones (>480dp)**: Same layout, increased card margins (up to 24dp)
- **Tablets (>600dp)**: Consider two-column layout (post-MVP)

### 9.2 Orientation Handling
- **Portrait**: Primary design target
- **Landscape**: Same layout, adjust bottom sheet height to 80% screen

## 10. Empty States

### 10.1 No Notes Yet
```
┌─────────────────────────────────┐
│                                 │
│         [Large Icon]            │
│                                 │
│    Start capturing ideas!       │
│                                 │
│  Tap the + button to create     │
│  your first note                │
│                                 │
│                          [ + ]  │
└─────────────────────────────────┘
```

### 10.2 No Results (Tag Filter)
```
┌─────────────────────────────────┐
│  [←]  #work                     │  ← Filtered view
├─────────────────────────────────┤
│                                 │
│      [Search Icon]              │
│                                 │
│   No notes with #work           │
│                                 │
│                          [ + ]  │
└─────────────────────────────────┘
```

## 11. Dark Mode (Post-MVP)

**Color Palette Adjustments:**
```kotlin
// Dark mode colors (future reference)
val DarkBackground = Color(0xFF1F2937)
val DarkCard = Color(0xFF374151)
val DarkTextPrimary = Color(0xFFF9FAFB)
val DarkAccentBlue = Color(0xFF60A5FA)  // Lighter blue for contrast
```

**Note:** MVP only supports light mode. Dark mode requires accessibility audit.

## 12. Design Tokens (Material3 Theme)

```kotlin
// Theme configuration
MaterialTheme(
    colorScheme = lightColorScheme(
        primary = Color(0xFF00C853),      // Green
        onPrimary = Color.White,
        secondary = Color(0xFF4C88FF),    // Blue
        background = Color(0xFFF7F8FA),   // Light gray
        surface = Color.White,
        onSurface = Color(0xFF1F2937),
        error = Color(0xFFEF4444)
    ),
    typography = Typography,
    shapes = Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp)
    )
)
```

## 13. Icon Assets

**Required Icons (Material Icons):**
- `Icons.Default.Add` - FAB create button
- `Icons.Default.Menu` - Drawer toggle
- `Icons.Default.Search` - Search (future)
- `Icons.Default.MoreVert` - Card options menu
- `Icons.Default.Tag` - Tag button in editor
- `Icons.Default.Image` - Image picker button
- `Icons.Default.FormatBold` - Bold formatting
- `Icons.Default.CheckBox` - Checkbox/todo
- `Icons.Default.MoreHoriz` - More options
- `Icons.Default.Share` - Share action
- `Icons.Default.Edit` - Edit action
- `Icons.Default.ContentCopy` - Copy action
- `Icons.Default.Delete` - Delete action
- `Icons.Default.Close` - Close/cancel
- `Icons.Default.Check` - Confirm/save

**Custom Icons:** None required for MVP (use Material Icons throughout).

## 14. UI Implementation Checklist

- [ ] Define Color.kt with all palette colors
- [ ] Define Typography.kt with type scale
- [ ] Create LozifyTheme.kt wrapper for MaterialTheme
- [ ] Build NoteCard composable with fold logic
- [ ] Build ImageGrid composable (3-column)
- [ ] Build EditorSheet composable with toolbar
- [ ] Build DrawerContent composable with tag list
- [ ] Build CardOperationsSheet composable
- [ ] Implement tag text styling with AnnotatedString
- [ ] Test all components in @Preview functions
- [ ] Verify accessibility with TalkBack
- [ ] Test on multiple screen sizes (emulator)
