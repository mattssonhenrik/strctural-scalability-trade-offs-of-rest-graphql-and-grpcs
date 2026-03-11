# Datagenerator

Generates controlled, nested JSON structures used as dataset for the API experiment.

## Parameters

| Parameter | Symbol | Description |
|-----------|--------|-------------|
| `depth` | D | How many levels deep the tree goes |
| `fanOut` | F | Number of child nodes per node |
| `fieldCount` | K | Number of scalar fields per node |
| `seed` | — | Random seed (`-1` = random each run, fixed value = reproducible) |

## Output structure

Each node contains:
- **K fields** with fixed-length keys (`f00`, `f01`, ...) and 16-character random string values
- A `"children"` array with F child nodes (absent at leaf level)

Example with D=2, F=2, K=2:
```json
{
  "f00": "aBcDeFgHiJkLmNoP",
  "f01": "qRsTuVwXyZ012345",
  "children": [
    {
      "f00": "...",
      "f01": "...",
      "children": [ {"f00": "...", "f01": "..."}, {"f00": "...", "f01": "..."} ]
    },
    {
      "f00": "...",
      "f01": "...",
      "children": [ {"f00": "...", "f01": "..."}, {"f00": "...", "f01": "..."} ]
    }
  ]
}
```

## Payload predictability

Keys are zero-padded (`f00`, `f01`, ...) so key length is always exactly 3 bytes.
Values are always 16 bytes.

Each node contributes exactly **K × 19 bytes** in field data, regardless of depth.
This ensures fair payload (DP3) comparison when varying D.

## Size formulas

```
Total nodes  ≈ (F^(D+1) - 1) / (F - 1)
Total fields ≈ nodes × K
```

## Usage

```java
// Fixed seed — same output every run (use in actual experiments)
Datagenerator gen = new Datagenerator(3, 2, 4, 42);

// Random seed — different output each run (use for manual inspection)
Datagenerator gen = new Datagenerator(3, 2, 4, -1);

gen.printStats();          // prints node/field count summary
String json = gen.generateJson();  // returns JSON string
ObjectNode tree = gen.generate();  // returns Jackson object tree
```
