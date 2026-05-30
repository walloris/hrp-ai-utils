---
name: Git Workflow
description: Work with git branches following Sberbank standards. Trigger when user needs to create branches, commit changes, or push to remote following the specified naming convention: <type>/<developer-login>/<JIRA-KEY>. Types are derived from JIRA task: bugfix, feature, or techdebt.
---

# Git Workflow

Git workflow following Sberbank standards for the Spine project.

## Branch Naming Convention

```
<type>/<developer-login>/<JIRA-KEY>
```

### Type Determination

Based on JIRA task type:

| JIRA Type | Branch Type |
|-----------|-------------|
| Bug | `bugfix` |
| Feature | `feature` |
| Technical Debt | `techdebt` |

### Developer Login

- Uses your login from theSigma device
- Falls back to git config user.name if not available

### Example

```
bugfix/bugreev/HRM-11330
feature/smirnov/HRM-11331
techdebt/ivanov/HRM-11332
```

## Commit Message Format

```
[JIRA-KEY]: <task-name-from-JIRA>
```

### Example

```
[HRM-11330]: LB. Приходит 404 ошибка, при поиске автора в реалме
```

## Commands

### 1. Create Branch from Task

Automatically determines branch type from JIRA task and creates branch with correct naming.

```
# From any JIRA URL
git checkout -b bugfix/bugreev/HRM-11330
```

### 2. Commit Changes

Commits with formatted message based on JIRA task.

### 3. Push Branch

Pushes branch to remote repository.

## Usage

### Creating a Branch

1. Get JIRA URL from task
2. Run git checkout command with correct branch name format
3. Branch is created from master or developer

### Making Commits

1. Stage your changes: `git add .`
2. Run commit with formatted message: `git commit -m "[HRM-11330]: <task-name>"`
3. Push branch: `git push origin bugfix/bugreev/HRM-11330`

### Quick Commands

```bash
# Checkout new branch
git checkout -b bugfix/$(git config user.name || echo "developer")/<JIRA-KEY>

# Commit with formatted message
git commit -m "[<JIRA-KEY>]: <task-name>"

# Push branch
git push origin bugfix/$(git config user.name || echo "developer")/<JIRA-KEY>
```

## References

- `references/commands.md` - Detailed command reference
- `examples.md` - Complete usage examples
