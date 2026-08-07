# Lozify - Product Requirements Document (PRD)

## 1. Product Overview

**Product Name:** Lozify  
**Version:** 1.0.0 MVP  
**Platform:** Android Native  
**Target Users:** Individual note-takers who value speed and simplicity

**Product Vision:**  
Lozify is a lightweight, card-based note-taking Android application inspired by flomo's minimalist philosophy. It enables users to capture thoughts instantly through a frictionless interface with rich text features like #tags, @relations, and multi-image attachments.

**Core Value Proposition:**
- **Instant Capture**: Bottom sheet editor accessible via FAB
- **Visual Organization**: Tag-based filtering and card waterfall layout
- **Rich Context**: Image attachments, bold/highlight formatting, note relationships
- **Zero Friction**: No folders, no categories — just write and tag

## 2. User Personas

**Primary Persona: Alex - The Rapid Thinker**
- Age: 25-40
- Occupation: Knowledge worker, student, or creative professional
- Pain Points: Traditional note apps are too heavy; folders create friction
- Goals: Capture fleeting ideas instantly, find notes by context (#tags)

## 3. Core Features (MVP Scope)

### 3.1 Main Feed (Home Screen)
**Description:** Waterfall-style card list displaying all notes

**UI Specifications:**
- Background: #F7F8FA (light gray-white)
- Cards: White (#FFFFFF), 12dp rounded corners, 16dp margins
- Layout: Single column, newest note at top
- Card Content: Direct display of note text (no title field)

**Card Display Rules:**
- **Text Folding**: Auto-collapse content exceeding 5 lines
  - Fold indicator: Blue "展开" button (#4C88FF) at bottom-left of card
  - Tap to expand full content inline
- **Multi-Image Grid**: 2-9 images displayed in 3-column grid
  - Layout: Square thumbnails, 4dp spacing, 8dp corner radius
  - Scaling: CenterCrop
- **Tag Display**: #tags rendered in blue (#4C88FF) inline with text

**Interactions:**
- Tap card → Open card detail bottom sheet
- Tap tag → Filter feed by that tag
- Pull to refresh → Reload note list

### 3.2 Note Editor (Bottom Sheet)
**Description:** Half-screen modal for creating/editing notes

**Entry Points:**
- FAB (+) button at bottom-right (green #00C853)
- Card menu → Edit

**Editor UI:**
- Input: Multi-line text field with rich text preview
- Toolbar (bottom row):
  1. `#` Tag button
  2. 📷 Image picker
  3. **B** Bold button
  4. ☐ Checkbox/todo button
  5. `...` More menu

**Toolbar Features:**
- **#Tag**: Insert `#` prefix; typing space after tag text triggers blue highlight
- **Image Picker**: Multi-select from gallery (2-9 images), auto-compress on save
- **Bold**: Apply bold formatting to selected text
- **Checkbox**: Insert `[ ]` todo item
- **More Menu** (`...`):
  - Yellow highlight (background #FFF3C4)
  - Underline
  - @Relation (insert link to another note)
  - Camera (take photo directly)
  - Undo / Redo

**Save Behavior:**
- Auto-save on sheet dismiss
- Extract #tags and save to tag table
- Copy selected images to app private storage
- Create AttachmentEntity records

### 3.3 Tag System & Filtering
**Description:** Organize notes via hashtags

**Tag Rules:**
- Format: `#tagname` (alphanumeric + Chinese characters)
- Display Color: Blue (#4C88FF) — managed by theme, not per-tag
- Extraction: Regex-based parsing on save

**Side Drawer (Left Swipe):**
- Top Section: Placeholder UI (e.g., contribution heatmap frame)
- Tag List Section:
  - Display all unique tags with note counts
  - Tap tag → Filter home feed to show only notes with that tag
  - Top bar updates to show tag name as title
  - Return to "All Notes" via back button or drawer

### 3.4 @Relation System
**Description:** Link notes together via @mentions

**Implementation:**
- Trigger: Tap `@` button in editor toolbar
- UI: Half-screen bottom sheet with searchable note list
- Selection: Tap note → Insert `@NoteName` at cursor as blue text
- Storage: Create NoteRelationEntity (from → to relationship)
- Display: @mentions rendered as clickable blue spans
- Navigation: Tap @mention → Navigate to related note detail

### 3.5 Multi-Image Attachments
**Description:** Attach 2-9 images per note

**User Flow:**
1. Tap image button in editor
2. Select multiple images from gallery
3. Images auto-compress (target: <500KB per image)
4. Images copied to `context.filesDir/images/` (app private storage)
5. AttachmentEntity records store internal file paths

**Display Rules:**
- 1 image: Full-width, aspect ratio preserved
- 2-9 images: 3-column grid, square thumbnails
- Tap image → Full-screen viewer (future enhancement)

### 3.6 Card Operations Menu
**Description:** Bottom sheet with card actions

**Trigger:** Tap `...` button at top-right of card

**Actions (Priority Order):**
1. **Top Row** (3 icons):
   - Share: Android Intent.ACTION_SEND (text + images)
   - Edit: Reopen editor with pre-filled content
   - Copy: Copy text to clipboard
2. **Middle Section**:
   - Pin to Top
   - Related Notes (show @mentions)
   - Version History
3. **Bottom Section**:
   - Delete (red text, with confirmation dialog)

**MVP Constraints:**
- Pin/Related/History → Show "正在开发" Toast placeholder
- Only Share/Edit/Copy/Delete are functional

### 3.7 Rich Text Formatting
**Description:** Inline text styling

**Supported Formats:**
- **Bold**: `**text**` or toolbar button
- **Underline**: Via more menu
- **Yellow Highlight**: Background color #FFF3C4 via more menu
- **Checkboxes**: `[ ]` / `[x]` todo items
- **#Tags**: Auto-styled blue text (#4C88FF)
- **@Mentions**: Auto-styled blue clickable text (#4C88FF)

**Technical Approach:**
- Store raw text in database
- Render as `AnnotatedString` in Compose via regex-based styling
- Support undo/redo via ViewModel state stack

## 4. Non-Functional Requirements

### 4.1 Performance
- App launch: <2 seconds on mid-range devices
- Note creation: <500ms from FAB tap to editor open
- Image compression: <1 second per image on device
- Feed scroll: 60 FPS smooth scrolling

### 4.2 Data Persistence
- Local-only storage (no cloud sync in MVP)
- Soft delete pattern for notes (isDeleted flag)
- Database: Room with proper migration strategy

### 4.3 Privacy
- All data stored in app private storage
- No network permissions in MVP
- Images copied to isolated app directory

### 4.4 Accessibility
- Content descriptions for all interactive elements
- Support TalkBack screen reader
- Minimum touch target size: 48dp

## 5. Out of Scope (Post-MVP)

- Cloud sync & backup
- Full-text search with FTS4
- Export to Markdown/PDF
- Dark mode theme
- Widgets
- Collaboration features
- Web/desktop clients
- Image OCR
- Audio/video attachments

## 6. Success Metrics (Post-Launch)

- Daily note creation rate
- Tag usage frequency
- Image attachment adoption
- Feature discovery (% users using @relations)

## 7. Open Questions & Decisions

Tracked in [decisions.md](decisions.md)
