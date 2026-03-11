# Design Decisions & Experiment Notes

## 1. JSON-serialisering — compact vs pretty-print

**Problem:** `mapper.writerWithDefaultPrettyPrinter()` lägger till whitespace som blåser upp DP3 (payload size).

**Beslut:** Använd compact JSON (`mapper.writeValueAsString()`) i alla API-svar som mäts.
Pretty-print är OK för manuell inspektion men ska inte användas i faktiska experimentkörningar.

---

## 2. Fältnamn — variabel vs fast längd

**Problem:** Ursprungliga fältnamn (`field_root_0_1_2`) inkluderade nodväg → nyckelstorlek ökade med D.
Det betyder att payload size ökade av nyckelnamns-längd, inte bara av strukturell komplexitet.

**Beslut:** Zero-paddade fasta nycklar: `f00`, `f01`, ..., `f99`.
- Nyckellängd alltid 3 bytes
- Värdelängd alltid 16 bytes (STRING_LENGTH)
- Varje nod bidrar med exakt **K × 19 bytes** i fältdata, oavsett D

---

## 3. Metadata-fält (nodeId, depth) i JSON

**Problem:** Att lägga nodeId och depth som separata fält utanför K leder till K+2 fält per nod,
vilket skever DP3 och overfetch-mätningar.

**Beslut:** Drogs bort helt. Metadata är inte relevant för experimentdata —
det var ett läsbarhetsverktyg, inte ett datakrav.

---

## 4. Jackson som beroende

**Observation:** `spring-boot-starter-webmvc` inkluderar redan `jackson-databind` transitivt.
Den explicita raden i `build.gradle.kts` är redundant men ofarlig.

**Beslut:** Behålls explicit för tydlighetens skull.

---

## 5. Serialiseringsformat — JSON vs Protobuf

**Problem:** REST och GraphQL serialiseras via Jackson (JSON-text). gRPC använder Protocol Buffers (binärt).
Det betyder att DP3-jämförelsen delvis mäter serialiseringsformat, inte bara strukturell komplexitet:
- JSON inkluderar fältnamn som strängar i varje meddelande
- Protobuf använder fältnummer (1–2 bytes) istället

**Beslut:** Detta är en del av det vi undersöker och ska framgå i validity-avsnittet.
Jämförelsen är format-till-format, inte rå datamängd.

---

## 6. Dataset-design — ett gemensamt eller ett per testfall

**Problem:** Om ett enda stort dataset används och API:erna returnerar delar av det
(t.ex. D=2 av D=10) uppstår strukturell overfetch som är svår att tolka,
och dataset-storleken kan bli ohållbart stor (D=10, F=7 ≈ 47 miljoner noder).

**Beslut:** Ett dataset per testfall — genereras exakt matchat mot testfallets (D, F, K).
Servern laddar om datasetet mellan testfall via en `DataStore.reload(D, F, K)`-metod.

---

## 7. Overfetch — definition och mätbarhet

**Insikt:** Overfetch är helt beroende av hur target shape definieras.
Om target shape = hela datasetet → overfetch = 0 för alla API:er (meningslöst).

**Beslut:** Target shape definieras som en **delmängd** av datasetet:
- Dataset genereras med max-K för testserien (t.ex. K=7)
- Target shape för varje testfall är det aktuella K-värdet (t.ex. K=3)
- REST returnerar alltid K=7 → overfetch = (7−3) × antal noder
- GraphQL kan returnera K=3 → overfetch = 0

K-axelns experiment (RQ1-D_-F_-K0-10) är det mest relevanta för overfetch-mätning.

---

## 8. Underfetch — definition

Underfetch behandlas som processkostnad: antal extra anrop utöver det första
som krävs för att komplettera target shape.

REST kräver typiskt flera anrop för att bygga upp ett nested träd (ett per nivå/endpoint).
GraphQL kan hämta hela strukturen i ett anrop.

---

## Öppna frågor

- [ ] Hur implementeras `DataStore.reload()` utan serveromstart?
- [ ] Ska gRPC proto-definition genereras dynamiskt eller vara statisk?
- [ ] Hur mäts DP2 (server-side orchestration) konkret via proxy?
- [ ] Exakt definition av "target shape satisfied" i test-runnern
