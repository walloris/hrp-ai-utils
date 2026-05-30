# Git Workflow Commands

## Branch Naming Convention

```
<type>/<developer-login>/<JIRA-KEY>
```

## Type Determination

| JIRA Task Type | Branch Type |
|----------------|-------------|
| Bug | `bugfix` |
| Feature | `feature` |
| Technical Debt | `techdebt` |

## Commands

### 1. Get Current JIRA Task

```bash
# Parse JIRA URL to extract task key
# Example: https://jira.sberbank.ru/browse/HRM-11330 -> HRM-11330
```

### 2. Determine Branch Type

```bash
# Check JIRA task type
# bug -> bugfix
# feature -> feature
# technical debt -> techdebt
```

### 3. Create Branch

```bash
git checkout -b <type>/<login>/<JIRA-KEY>
```

### 4. Commit Changes

```bash
git commit -m "[<JIRA-KEY>]: <task-name>"
```

### 5. Push Branch

```bash
git push origin <type>/<login>/<JIRA-KEY>
```

## Shell Script Functions

See `scripts/git_workflow.sh` for ready-to-use functions.
