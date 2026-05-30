# Git Workflow

Git workflow following Sberbank standards for the Spine project.

## Files

- `SKILL.md` - Overview and quick reference
- `references/commands.md` - Command reference
- `references/examples.md` - Usage examples

## Branch Naming

```
<type>/<developer-login>/<JIRA-KEY>
```

## Usage

Source the script to use functions:
```bash
source .gigacode/skills/git-workflow/scripts/git_workflow.sh
create_git_branch "https://jira.sberbank.ru/browse/HRM-11330"
```
