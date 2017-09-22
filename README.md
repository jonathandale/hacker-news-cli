# Hacker News CLI

Use hcknws to view top Hacker News stories.

### Prerequisites
1. [nodejs & npm](https://nodejs.org/en/download/)
2. [Lumo](https://github.com/anmonteiro/lumo#installation)

### Running locally
```bash
# Install deps
$ npm install

# Run with Lumo on hcknws repo
$ lumo -c src src/hcknws/core.cljs
```

### Build & Install
```bash
# Ensure deps are installed
$ npm install

# Build with Lumo
$ lumo -c src build.cljs

# Install globally with npm (within root of hcknws)
$ npm install -g
```

### Usage
```bash
# In any dir
$ hcknws
```
