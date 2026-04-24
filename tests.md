# RQ1 — Test Matrix

Each test case sweeps one variable (OVAT). Amount of runs = Test/API × 3 paradigms (REST, GraphQL, gRPC).

| Test case             | D      | F      | K      | Test / API | Amount of runs | Done? |
|-----------------------|--------|--------|--------|:----------:|:--------------:|:-----:|
| RQ1-D0-10-F3-K3       | 0–10   | 3      | 3      | 10         | 30             | yes   |
| RQ1-D0-10-F5-K5       | 0–10   | 5      | 5      | 20         | 60             | no   |
| RQ1-D0-10-F7-K7       | 0–10   | 7      | 7      | 30         | 90             | no   |
| RQ1-D3-F0-10-K3       | 3      | 0–10   | 3      | 40         | 120            | yes    |
| RQ1-D5-F0-10-K5       | 5      | 0–10   | 5      | 50         | 150            | No    |
| RQ1-D7-F0-10-K7       | 7      | 0–10   | 7      | 60         | 180            | No    |
| RQ1-D3-F3-K0-10       | 3      | 3      | 0–10   | 70         | 210            | yes    |
| RQ1-D5-F5-K0-10       | 5      | 5      | 0–10   | 80         | 240            | No    |
| RQ1-D7-F7-K0-10       | 7      | 7      | 0–10   | 90         | 270            | No    |
| RQ1-D0-100-F30-K30    | 0–100  | 30     | 30     | 100        | 300            | No    |
| RQ1-D0-100-F50-K50    | 0–100  | 50     | 50     | 110        | 330            | No    |
| RQ1-D0-100-F70-K70    | 0–100  | 70     | 70     | 120        | 360            | No    |
| RQ1-D30-F0-100-K30    | 30     | 0–100  | 30     | 130        | 390            | No    |
| RQ1-D50-F0-100-K50    | 50     | 0–100  | 50     | 140        | 420            | No    |
| RQ1-D70-F0-100-K70    | 70     | 0–100  | 70     | 150        | 450            | No    |
| RQ1-D30-F30-K0-100    | 30     | 30     | 0–100  | 160        | 480            | No    |
| RQ1-D50-F50-K0-100    | 50     | 50     | 0–100  | 170        | 510            | No    |
| RQ1-D70-F70-K0-100    | 70     | 70     | 0–100  | 180        | 540            | No    |



# RQ1 — D, F, K-serier
./gradlew :runner:run --args="RQ1"

# RQ2 — S-serie (protobuf vs GraphQL crossover)
./gradlew :runner:run --args="RQ2"

# Utan argument körs RQ1 som default:
./gradlew :runner:run   # → kör RQ1

# plot data från csv filer
python data-visualization/plot.py             