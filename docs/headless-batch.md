# Headless batch analysis

These wrappers run Ghidra 12.1.4 headless against a directory of binaries and write a short inventory. They live in this tree and do not replace `support/analyzeHeadless`.

## Requirements

- This Ghidra installation, including `support/analyzeHeadless`
- JDK 21. On this Mac the default Homebrew JDK is 25, which Ghidra will not use. Install 21 and leave `JAVA_HOME` unset if you want the launcher to find the Homebrew keg:

```bash
brew install openjdk@21
```

The launcher prefers `JAVA_HOME` when it is already set. Otherwise it uses `$(brew --prefix openjdk@21)`.

## Scripts

| Path | Role |
| --- | --- |
| `scripts/ghidra-headless` | Runs `support/analyzeHeadless` with Java 21 |
| `scripts/ghidra-analyze-dir` | Selects binaries, imports them, writes `SUMMARY.txt` |
| `scripts/ghidra_scripts/HeadlessInventory.java` | Post-script that dumps functions, imports, and strings |

From the Ghidra root:

```bash
chmod +x scripts/ghidra-headless scripts/ghidra-analyze-dir
```

## Analyze a directory

```bash
scripts/ghidra-analyze-dir /path/to/binaries
```

That creates `~/ghidra-out/<directory>-<timestamp>/` and a Ghidra project at `~/ghidra-projects/ReBatch`.

Useful flags:

```bash
scripts/ghidra-analyze-dir -h
scripts/ghidra-analyze-dir -r -n ReFirmware -o ~/ghidra-out/firmware /path/to/binaries
```

- `-r` descends into subdirectories. The default is the top directory only.
- `-n` sets the Ghidra project name. The default is `ReBatch`.
- `-o` sets the output directory. Without it, each run gets a new timestamped directory.

The script imports only files whose `file` description starts with Mach-O, ELF, PE32, or MS-DOS. Source files, text, and other non-binaries are skipped and listed in the summary.

## Output

For a program named `sample`, the output directory contains:

- `sample.functions.txt` — entry address, name, and size
- `sample.imports.txt` — library and symbol
- `sample.strings.txt` — address and defined string
- `analyze.log` — Ghidra log
- `SUMMARY.txt` — counts per binary, totals, skipped files, and the most common imports

`SUMMARY.txt` looks like this:

```text
binary                        functions    imports    strings
------                        ---------    -------    -------
sample                                5          4         24

binaries imported: 1 / 1
functions:         5
imports:           4
strings:           24
```

## One file, without the batch wrapper

```bash
mkdir -p ~/ghidra-out
scripts/ghidra-headless \
  ~/ghidra-projects ReBatch \
  -import /path/to/binary \
  -overwrite \
  -scriptPath scripts/ghidra_scripts \
  -postScript HeadlessInventory.java "$HOME/ghidra-out" \
  -log "$HOME/ghidra-out/analyze.log"
```

Pass the output directory as the post-script argument. Headless mode does not prompt for a file.

## Environment overrides

- `JAVA_HOME` — JDK 21 home. Overrides the Homebrew lookup.
- `GHIDRA_HOME` — installation root. Defaults to the parent of `scripts/`.
- `GHIDRA_PROJECT_DIR` — project parent. Defaults to `~/ghidra-projects`.
- `GHIDRA_PROJECT_NAME` — project name. Defaults to `ReBatch`. Same as `-n`.
- `GHIDRA_SCRIPT_PATH` — directory containing `HeadlessInventory.java`.
