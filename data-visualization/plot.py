"""
Visualisering av RQ1-experiment.

Läser alla rq1_*.csv från runner/results/, genererar interaktiva Plotly HTML-filer
per CSV samt en dashboard.html med alla serier sida vid sida.

Kör från projektroten: python data-visualization/plot.py
"""

import os
import glob
import pandas as pd
from io import StringIO
import plotly.graph_objects as go
from plotly.subplots import make_subplots

# ── Konfiguration ─────────────────────────────────────────────────────────────

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESULTS_DIR  = os.path.join(PROJECT_ROOT, "runner", "results")
PLOTS_DIR    = os.path.join(RESULTS_DIR, "plots")
CSV_PATTERN  = os.path.join(RESULTS_DIR, "rq1_*.csv")

PARADIGM_COLORS = {
    "REST":    "#64B5F6",   # ljusblå
    "GraphQL": "#212121",   # svart
    "gRPC":    "#E53935",   # röd
}
PARADIGM_ORDER = ["REST", "GraphQL", "gRPC"]

METRICS = [
    ("dp1_request_count",       "DP1 — Request count"),
    ("dp2_orchestration_ops",   "DP2 — Orchestration ops"),
    ("dp3_payload_bytes",       "DP3 — Payload bytes"),
    ("overfetch_fields",        "DP5 — Overfetch fields"),
    ("underfetch_extra_calls",  "DP4 — Underfetch extra calls"),
]

SERIES_X = {
    "D": "D-Target",
    "F": "F-Target",
    "K": "K-Target",
}

# ── Hjälpfunktioner ───────────────────────────────────────────────────────────

def load_csv(path: str) -> pd.DataFrame:
    """Läser CSV och hoppar över metadata-rader tills header-raden hittas."""
    with open(path, "r", encoding="utf-8") as f:
        lines = f.readlines()
    header_idx = next(i for i, line in enumerate(lines) if line.startswith("paradigm"))
    return pd.read_csv(StringIO("".join(lines[header_idx:])))


def x_column(df: pd.DataFrame) -> str:
    series = df["series"].iloc[0]
    return SERIES_X.get(series, "D-Target")


def fixed_params(df: pd.DataFrame) -> str:
    series = df["series"].iloc[0]
    parts = []
    if series != "D":
        parts.append(f"D={df['D-Target'].iloc[0]}")
    if series != "F":
        parts.append(f"F={df['F-Target'].iloc[0]}")
    if series != "K":
        parts.append(f"K={df['K-Target'].iloc[0]}")
    return ", ".join(parts)


def aggregate(df: pd.DataFrame, x_col: str) -> pd.DataFrame:
    ok = df[df["status"] == "ok"].copy()
    numeric = [col for col, _ in METRICS]
    return (
        ok.groupby(["paradigm", x_col])[numeric]
        .mean()
        .reset_index()
    )


# ── Plot-logik ────────────────────────────────────────────────────────────────

def plot_csv(path: str) -> str:
    """Genererar en Plotly HTML-fil för en CSV. Returnerar sökvägen till HTML-filen."""
    df = load_csv(path)
    x_col       = x_column(df)
    agg         = aggregate(df, x_col)
    fixed       = fixed_params(df)
    series_name = df["series"].iloc[0]
    sweep_min   = int(df[x_col].min())
    sweep_max   = int(df[x_col].max())
    title       = f"RQ1 — {series_name}-serie {sweep_min}–{sweep_max}  ({fixed})"

    n_metrics = len(METRICS)
    fig = make_subplots(
        rows=1, cols=n_metrics,
        subplot_titles=[label for _, label in METRICS],
        horizontal_spacing=0.06,
    )

    paradigms_present = [p for p in PARADIGM_ORDER if p in agg["paradigm"].unique()]
    n_paradigms = len(paradigms_present)
    jitter_step = 0.03
    jitter_offsets = {
        p: (i - (n_paradigms - 1) / 2) * jitter_step
        for i, p in enumerate(paradigms_present)
    }

    for col_idx, (col, _) in enumerate(METRICS, start=1):
        show_legend = col_idx == 1  # visa legend bara i första subploten
        for paradigm in paradigms_present:
            sub = agg[agg["paradigm"] == paradigm].sort_values(x_col)
            x_vals = sub[x_col] + jitter_offsets[paradigm]
            fig.add_trace(
                go.Scatter(
                    x=x_vals,
                    y=sub[col],
                    mode="lines+markers",
                    name=paradigm,
                    legendgroup=paradigm,        # synkar legend-klick över subplots
                    showlegend=show_legend,
                    line=dict(color=PARADIGM_COLORS[paradigm], width=2),
                    marker=dict(size=7),
                    hovertemplate=f"<b>{paradigm}</b><br>{x_col}=%{{x}}<br>värde=%{{y}}<extra></extra>",
                ),
                row=1, col=col_idx,
            )
        fig.update_xaxes(title_text=x_col, row=1, col=col_idx, dtick=1)
        fig.update_yaxes(title_text="Värde", row=1, col=col_idx)

    fig.update_layout(
        title=dict(text=title, font=dict(size=16), x=0.5),
        height=480,
        legend=dict(orientation="h", yanchor="bottom", y=1.08, xanchor="center", x=0.5),
        plot_bgcolor="#fafafa",
        paper_bgcolor="#ffffff",
    )

    basename = os.path.splitext(os.path.basename(path))[0]
    out_path = os.path.join(PLOTS_DIR, f"{basename}.html")
    fig.write_html(out_path, include_plotlyjs="cdn", full_html=True)
    print(f"  Sparad: {out_path}")
    return out_path


# ── Dashboard ─────────────────────────────────────────────────────────────────

def write_dashboard(html_files: list[str]):
    """Genererar dashboard.html som bäddar in alla plot-filer i iframes."""
    iframes = "\n".join(
        f'    <iframe src="{os.path.basename(f)}" title="{os.path.basename(f)}"></iframe>'
        for f in html_files
    )

    html = f"""<!DOCTYPE html>
<html lang="sv">
<head>
  <meta charset="UTF-8" />
  <title>RQ1 Dashboard</title>
  <style>
    * {{ box-sizing: border-box; margin: 0; padding: 0; }}
    body {{
      font-family: sans-serif;
      background: #f0f0f0;
      padding: 24px;
    }}
    h1 {{
      text-align: center;
      margin-bottom: 20px;
      font-size: 1.4rem;
      color: #222;
    }}
    .grid {{
      display: flex;
      flex-direction: column;
      gap: 16px;
    }}
    iframe {{
      width: 100%;
      height: 520px;
      border: 1px solid #ccc;
      border-radius: 6px;
      background: #fff;
    }}
  </style>
</head>
<body>
  <h1>RQ1 — Structural Scalability Trade-offs: REST vs GraphQL vs gRPC</h1>
  <div class="grid">
{iframes}
  </div>
</body>
</html>
"""

    out_path = os.path.join(PLOTS_DIR, "dashboard.html")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"  Dashboard: {out_path}")


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    os.makedirs(PLOTS_DIR, exist_ok=True)
    csv_files = sorted(glob.glob(CSV_PATTERN))

    if not csv_files:
        print(f"Inga CSV-filer hittades: {CSV_PATTERN}")
        return

    print(f"Hittade {len(csv_files)} CSV-fil(er):")
    html_files = [plot_csv(path) for path in csv_files]

    write_dashboard(html_files)
    print(f"\nKlart. Öppna: {os.path.join(PLOTS_DIR, 'dashboard.html')}")


if __name__ == "__main__":
    main()
