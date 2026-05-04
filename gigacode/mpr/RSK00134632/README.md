#  Скил для отработки МПР RSK00134632

## hrp-spring-boot-update
Ищет новые версии зависимостей на странице в Confluence https://confluence.sberbank.ru/pages/viewpage.action?pageId=17151460968

Необходимо заранее подключить MCP Atlassian https://sc-ci.sber.ru/sc/Python/giga_marketplace/src/branch/develop/mcp

Этот скил можно разместить как в глобальной папке .gigacode/skills, так и непосредственно внутри папки с проектом.

Запускать через команду в gigacode cli:
`/skills spring-boot-update Обнови текущий проект`

Или
`Используя скилл @путь_к_файлу_в_проекте обнови текущий проект`

Чтобы заранее разрешить выполение некоторых команд, добавить в settings.json

`"allow": [
"Bash(mvn *)",
"Bash(sed *)",
"Bash(find *)"
]`