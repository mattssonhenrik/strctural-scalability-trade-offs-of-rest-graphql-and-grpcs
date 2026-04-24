# Data — Datagenerator, Node, DataStore

Paketet ansvarar för att generera och hålla det in-memory dataset som alla tre API-implementationer läser ifrån.

## Klasser

| Klass | Ansvar |
|---|---|
| `Datagenerator` | Bygger trädstrukturen utifrån parametrarna D, F, K och seed |
| `Node` | POJO: id, K fält (`k00`..`k99`), lista av barn |
| `DataStore` | Spring-komponent som håller trädet i minnet och exponerar det till API-lagren |

---

## Datagenerator

### Parametrar

| Parameter | Symbol | Beskrivning |
|-----------|--------|-------------|
| `depth` | D | Antal nivåer djupt trädet går |
| `fanOut` | F | Antal barn per nod |
| `fieldCount` | K | Antal skalärfält per nod |
| `seed` | — | Fast seed (`42`) i experiment, `-1` för slumpmässig (manuell inspektion) |

### Fältformat

- Nycklar är alltid zero-paddade: `k00`, `k01`, ..., `k99` — alltid 3 bytes
- Värden är alltid exakt 16 ASCII-tecken — alltid 16 bytes
- Varje nod bidrar med exakt **K × 19 bytes** i fältdata oavsett D

### Storleksformler

```
Antal noder  = (F^(D+1) - 1) / (F - 1)   (F > 1)
Antal fält   = antal noder × K
```

### Exempel D=2, F=2, K=2

```json
{
  "id": "000000",
  "k00": "aBcDeFgHiJkLmNoP",
  "k01": "qRsTuVwXyZ012345",
  "children": [
    { "id": "000001", "k00": "...", "k01": "...", "children": [...] },
    { "id": "000002", "k00": "...", "k01": "...", "children": [...] }
  ]
}
```

---

## Node

POJO som representerar en nod i trädet:

- `id` — unik identifierare, zero-paddad sekventiell sträng, t.ex. `"000000"`, `"000001"` (alltid 6 tecken)
- `fields` — `Map<String, String>` med K nyckel/värde-par
- `children` — lista av F barn, tom lista i löv-noder

---

## DataStore

Spring `@Component` — simulerar en databas. Håller trädet i en `HashMap` för O(1)-uppslagning per nod-id.

### Metoder

| Metod | CM2 | Beskrivning |
|---|---|---|
| `reload(D, F, K, seed)` | — | Regenererar datasetet, byggs om index |
| `getRoot()` | +1 | Returnerar rotnoden — räknas som orchestration |
| `getNode(id)` | +1 | Slår upp en nod via id |
| `getChildren(id)` | +1 | Returnerar direkta barn till en nod |

### Reload-flöde

```
POST /api/admin/reload?D=X&F=Y&K=Z&seed=42
        ↓
DataStore.reload(D, F, K, seed)
  → Datagenerator.generate()   (bygger trädet)
  → buildIndex(root)           (registrerar alla noder i HashMap)
```

Test-runnern anropar reload före varje testfall för att säkerställa att rätt dataset är laddat.
