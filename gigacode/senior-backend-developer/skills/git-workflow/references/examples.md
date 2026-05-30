# Git Workflow Examples

## Example 1: Bugfix Branch

### Starting Point

- JIRA URL: `https://jira.sberbank.ru/browse/HRM-11330`
- Task Type: Bug
- Developer Login: `bugreev`

### Commands

```bash
# Checkout new branch from master/developer
git checkout -b bugfix/bugreev/HRM-11330

# Make your changes
# ...

# Stage changes
git add .

# Commit with formatted message
git commit -m "[HRM-11330]: LB. Приходит 404 ошибка, при поиске автора в реалме"

# Push to remote
git push origin bugfix/bugreev/HRM-11330
```

## Example 2: Feature Branch

### Starting Point

- JIRA URL: `https://jira.sberbank.ru/browse/HRM-11331`
- Task Type: Feature
- Developer Login: `smirnov`

### Commands

```bash
# Checkout new branch from master/developer
git checkout -b feature/smirnov/HRM-11331

# Make your changes
# ...

# Stage changes
git add .

# Commit with formatted message
git commit -m "[HRM-11331]: <feature-name-from-JIRA>"

# Push to remote
git push origin feature/smirnov/HRM-11331
```

## Example 3: Tech Debt Branch

### Starting Point

- JIRA URL: `https://jira.sberbank.ru/browse/HRM-11332`
- Task Type: Technical Debt
- Developer Login: `ivanov`

### Commands

```bash
# Checkout new branch from master/developer
git checkout -b techdebt/ivanov/HRM-11332

# Make your changes
# ...

# Stage changes
git add .

# Commit with formatted message
git commit -m "[HRM-11332]: <tech-debt-description-from-JIRA>"

# Push to remote
git push origin techdebt/ivanov/HRM-11332
```

## Example 4: Using Script Functions

```bash
# Source the script
source .gigacode/skills/git-workflow/scripts/git_workflow.sh

# Create branch from JIRA URL
create_git_branch "https://jira.sberbank.ru/browse/HRM-11330"

# Commit with formatted message
commit_changes "[HRM-11330]: LB. Приходит 404 ошибка, при поиске автора в реалме"

# Push branch
push_branch
```
