# Линия Claude: перенос групп поверх `work` (20.10.2026 →)

Файл ведёт Claude, чтобы не править корневые документы Codex (`HANDOFF.md`, `ACTIVE_PORT.md`, `MODLOG.md`), которые он переписывает при каждом checkpoint'е. Границы: Eldritch-измерение, Brain jar, Animals, Swarm, XP, Hilltop/Mound, Death aspect orb, Champion, Taint FX и SpecialItem заняты Codex и здесь не трогаются. Порядок групп и аудит пробелов — по плану в этом же сеансе; перед каждой группой проверяется, что её никто не взял.

Порядок: малые группы (1–5), затем средние (Wand Pedestal/Adv Stabilizer/INode, Node Jar, Infernal Furnace, Warded Arcana, Mirrors), затем API-фасад, Config-флаги, Bone Bow/Primal Arrow; клиентский HUD и NeoForge — после Fabric и при возможности осмотреть клиент.

| № | Группа | Статус | Коммит | CI | Док |
|---|---|---|---|---|---|
| 1 | Алхимические данные: 10 Alt-рецептов тигля + Mirrored Glass | реализована; локально 937 JUnit, GameTest 272/273 (Pech красный и на чистой базе) | `03a25b9` | run 69 на `work`: success | [alchemy-leftovers](2026-10-20-alchemy-leftovers.md) |
| 2 | Решётка предметов, ступени/плиты greatwood+silverwood (Obsidian Tile перенесён в группу 3) | реализована; локально 954 JUnit (+17), GameTest 274/274 при сиде мира 424242, `:fabric:remapJar` PASS | см. git log | — | [item-grate-and-wood-stairs](2026-10-20-item-grate-and-wood-stairs.md) |
| 3 | Obsidian Tile, плиты Travel/Warding, Warding Aura | не начата | — | — | — |
| 4 | Sceptre | не начата | — | — | — |
| 5 | Flux Scrubber | не начата | — | — | — |

Метки в записях: implemented / built / game-tested; **seen = нет** (клиент в этой среде не запускался). Любой PASS относится только к точному коммиту и прогону, указанным в таблице.

## Заметка: два GameTest зависят от случайного сида мира

`fabric/run-gametest/server.properties` по умолчанию содержит `level-seed=` (пусто), поэтому каждый новый локальный мир GameTest получает случайный сид, а затем сохраняется в игнорируемой `fabric/run-gametest/world`. На чистой базе `03a25b9` (без кода группы 2) и в нескольких чередующихся запусках по-разному краснели `pechoriginaltradeusesnativegameplay` и `magicalbiomesgeneratenaturallyandspawnactualpech`; при `level-seed=424242` и свежем мире все 274 сценария прошли с кодом группы 2. Это не принятый мной PASS «в обход» и не причина группы 2: вывод — эти два теста Codex зависят от сида (генерация леса и подлинных стеков Pech); на GitHub каждый прогон создаёт свежий мир, поэтому красный там возможен время от времени независимо от изменений. Чтобы сравнивать прогоны, я фиксирую сид локально (файл в `.gitignore`).
