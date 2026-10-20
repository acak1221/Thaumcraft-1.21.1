# Линия Claude: перенос групп поверх `work` (20.10.2026 →)

Файл ведёт Claude, чтобы не править корневые документы Codex (`HANDOFF.md`, `ACTIVE_PORT.md`, `MODLOG.md`), которые он переписывает при каждом checkpoint'е. Границы: Eldritch-измерение, Brain jar, Animals, Swarm, XP, Hilltop/Mound, Death aspect orb, Champion, Taint FX и SpecialItem заняты Codex и здесь не трогаются. Порядок групп и аудит пробелов — по плану в этом же сеансе; перед каждой группой проверяется, что её никто не взял.

Порядок: малые группы (1–5), затем средние (Wand Pedestal/Adv Stabilizer/INode, Node Jar, Infernal Furnace, Warded Arcana, Mirrors), затем API-фасад, Config-флаги, Bone Bow/Primal Arrow; клиентский HUD и NeoForge — после Fabric и при возможности осмотреть клиент.

| № | Группа | Статус | Коммит | CI | Док |
|---|---|---|---|---|---|
| 1 | Алхимические данные: 10 Alt-рецептов тигля + Mirrored Glass | реализована; локально 937 JUnit, GameTest 272/273 (Pech красный и на чистой базе) | см. git log | — | [alchemy-leftovers](2026-10-20-alchemy-leftovers.md) |
| 2 | Решётка предметов, ступени/плиты greatwood+silverwood, Obsidian Tile | не начата | — | — | — |
| 3 | Плиты Travel/Warding, Warding Aura | не начата | — | — | — |
| 4 | Sceptre | не начата | — | — | — |
| 5 | Flux Scrubber | не начата | — | — | — |

Метки в записях: implemented / built / game-tested; **seen = нет** (клиент в этой среде не запускался). Любой PASS относится только к точному коммиту и прогону, указанным в таблице.
