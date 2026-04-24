// Usage: node experiment-calc.js [--sweep 7] [--max 7] [--ms 2] [--runs 2]
//  copy: tools\calc.bat --sweep 10 --ms 5 --runs 2 --max 10
// Flags:
//   --sweep N   sweep max (default 7)
//   --max N     max baseline n to show in table (default 7)
//   --ms N      estimated ms per REST request (default 2)
//   --runs N    N_RUNS per test case (default 2)

const args = process.argv.slice(2);
function flag(name, def) {
    const i = args.indexOf('--' + name);
    return i !== -1 ? parseInt(args[i + 1]) : def;
}

const SWEEP_MAX = flag('sweep', 7);
const MAX_N     = flag('max',   7);
const MS        = flag('ms',    2);
const RUNS      = flag('runs',  2);

function totalNodes(d, f) {
    if (f <= 1) return d + 1;
    return Math.round((Math.pow(f, d + 1) - 1) / (f - 1));
}
function restReqs(d, f) {
    if (d === 0) return 1;
    return 1 + totalNodes(d - 1, f);
}
function formatTime(ms) {
    if (ms < 1000)     return '<1s';
    if (ms < 60000)    return (ms / 1000).toFixed(0) + 's';
    if (ms < 3600000)  return (ms / 60000).toFixed(0) + 'min';
    return (ms / 3600000).toFixed(1) + 'h';
}
function fmt(n) {
    if (n > 1e9) return '>1B';
    if (n > 1e6) return (n / 1e6).toFixed(1) + 'M';
    if (n > 1e3) return Math.round(n / 1e3) + 'k';
    return n.toString();
}

console.log(`\nSweep 0-${SWEEP_MAX}  |  N_RUNS=${RUNS}  |  ~${MS}ms/REST-req\n`);
console.log('Baseline        | Total nodes | Total REST reqs | Est. time');
console.log('----------------|-------------|-----------------|----------');

for (let n = 1; n <= MAX_N; n++) {
    let totalReqs  = 0;
    let totalNodes_ = 0;

    // D-series: sweep D=0..SWEEP_MAX, F=n, K=n fixed
    for (let d = 0; d <= SWEEP_MAX; d++) {
        totalReqs   += RUNS * restReqs(d, n);
        totalNodes_ += RUNS * totalNodes(d, n);
    }
    // F-series: sweep F=1..SWEEP_MAX, D=n, K=n fixed (skip F=0)
    for (let f = 1; f <= SWEEP_MAX; f++) {
        totalReqs   += RUNS * restReqs(n, f);
        totalNodes_ += RUNS * totalNodes(n, f);
    }
    // K-series: sweep K=1..SWEEP_MAX, D=n, F=n fixed (REST reqs unchanged by K)
    for (let k = 1; k <= SWEEP_MAX; k++) {
        totalReqs   += RUNS * restReqs(n, n);
        totalNodes_ += RUNS * totalNodes(n, n);
    }

    const label = `D=${n} F=${n} K=${n}`.padEnd(15);
    console.log(
        label + ' | ' +
        fmt(totalNodes_).padStart(11) + ' | ' +
        fmt(totalReqs).padStart(15) + ' | ' +
        formatTime(totalReqs * MS)
    );
}
console.log('');
