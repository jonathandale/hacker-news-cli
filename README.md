# Hacker News CLI

Use `hcknws` to view and open links/comments of Hacker News stories.

### Prerequisites
1. [nodejs & npm](https://nodejs.org/en/download/)
2. [Lumo](https://github.com/anmonteiro/lumo#installation)
3. [Pkg](https://github.com/zeit/pkg) (For building binaries)

### Running locally
```bash
# Install deps
$ npm install

# Run with Lumo in hcknws repo
$ lumo -c src src/hcknws/core.cljs
```

### Build & Install from source
```bash
# Ensure deps are installed
$ npm install

# Build with Lumo
$ lumo -c src build.cljs

# Install globally with npm (within root of hcknws)
$ npm install -g
```

### Install manually
1. [Download release](https://github.com/jonathandale/hacker-news-cli/releases/download/1.0.0/macos.zip) (only mac at the moment)
2. Unzip & place it on your `$PATH` where your shell can find it (eg. `~/bin`)
3. Set it to be executable (`chmod a+x ~/bin/hcknws`)

### Usage
```bash
# In any dir
$ hcknws
```

### Distributing binaries
Install [Pkg](https://github.com/zeit/pkg), or similar.

```bash
# Example of building node 6 on mac.
$ pkg -t node6-macos-x64 -o builds/macos/hcknws package.json

# Zip for distribution
$ cd builds/macos
$ zip macos.zip hcknws
```
