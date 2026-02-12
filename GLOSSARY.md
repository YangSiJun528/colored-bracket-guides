# Bracket Guide Lines - Glossary

## Core Terms

| Term | Abbreviation | Description |
|------|-------------|-------------|
| **Opening Bracket** | OB | The left/opening bracket character (`{`, `(`, `[`, etc.) |
| **Closing Bracket** | CB | The right/closing bracket character (`}`, `)`, `]`, etc.) |
| **Guide Column** | GC | The column where the vertical guide line is rendered. Always the leftmost position to avoid crossing code. |
| **Interior Element** | IE | Any non-empty line between OB and CB (body lines). Their indent columns influence GC calculation. |
| **Bracket Pair** | - | A matched OB + CB pair with a calculated nesting depth. |

## Position Terms

| Term | Description |
|------|-------------|
| **col_start** | Column index (0-based) of the first non-whitespace character on the OB line |
| **col_end** | Column index (0-based) of the first non-whitespace character on the CB line |
| **IE_cols** | Set of first non-whitespace column indices for all IE lines (empty/whitespace-only lines excluded) |

## GC Algorithm

```
GC = min(col_start, col_end, min(IE_cols))
```

The vertical guide is always placed at the leftmost content position across the entire scope, ensuring it never crosses over code text.

## Guide Segments

### Vertical Line
- Drawn at GC x-position
- Spans from OB line top to CB line bottom
- Rendered for multi-line pairs only

### Horizontal Connectors

| Type | Condition | Shape | Position |
|------|-----------|-------|----------|
| **OB Connector** | `col_start > GC` | `┌──` | Top of OB line, from GC to OB |
| **CB Connector** | `col_end > GC` | `└──` | Bottom of CB line, from GC to CB |
| **Single-line** | `openLine == closeLine` | `───` | Bottom of line, from OB to CB |

## Cases

### Case A: `col_start == col_end`
```
{           ← OB = GC
│  foo
│  bar
}           ← CB = GC
```
No horizontal connectors needed.

### Case B-1: `col_start < col_end` (OB is further left)
```
{           ← OB = GC
│  content
└──)        ← CB connector (└)
```

### Case B-2: `col_start > col_end` (CB is further left)
```
┌──{        ← OB connector (┌)
│  content
}           ← CB = GC
```

### Minimum Column Anchoring (IE is further left)
```
  ┌──{      ← OB connector (col_start > GC)
  │x        ← IE = GC (leftmost content)
  │  content
  └──}      ← CB connector (col_end > GC)
```

## Color Terms

| Term | Description |
|------|-------------|
| **Depth** | Nesting level (0-based). Determined by stack size at OB push time. |
| **Rainbow Color** | Foreground color assigned to bracket characters based on depth, cycling through palette. |
| **Active Scope** | The innermost bracket pair containing the caret. Rendered with full opacity + background highlight. |
| **Inactive Scope** | All other visible pairs. Rendered with reduced opacity (`inactiveOpacity`). |

## Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `displayMode` | `ALL` | `ALL` = show all scopes, `ACTIVE_ONLY` = show only active scope |
| `lineWidth` | `0.7` | Stroke width for inactive guide lines |
| `activeLineWidth` | `1.0` | Stroke width for active guide lines |
| `inactiveOpacity` | `0.15` | Alpha multiplier for inactive scope colors |
| `caretDebounceMs` | `50` | Delay before re-rendering after caret movement |
