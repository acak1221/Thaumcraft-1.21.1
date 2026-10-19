# Базовая Охрана — 19.10.2026

**Интегрировано и собрано на Fabric; не проверено в игре.** Источник:
[Guard4 original contract](2026-10-19-golem-guard.md).

Root подключил реальное core4/recipe/book/model/языки и zero-ghost GUI с
объяснением заблокированных категорий. Рецепт: blank100 + Telum5/Vinculum5,
COREGUARD. Ordinary policy — враждебный класс, кроме криперов; мирные животные,
игроки, жители, питомцы и другие големы не получают базовую proactive/retaliation
авторизацию. Нет придуманного owner-defense, pack defense или player credit.

Runtime FINAL прочитан: native nearest/self-retaliation/chase/melee с исходными
home bounds/reach/cooldown20 и damage2+materialStrength; action использует
существующий shaped timer6/status4. Проверка sight перед ударом — объявленное
safety-исправление original through-wall omission. Hurt/cancellation остаётся
настоящим native mob damage, не raw health subtraction/OP/fake-player attack.

Данные остаются five-field: core4, пустые ghost carrier fields, markers для
сохранения/recall без combat-маршрутизации. Нет GuardOptions/переключателей Order
в базовой группе. Native меню не имеет ghost-слотов, сервер отвергает скрытые
51…54 toggle edits, исправляя исходный upgrade-check bypass.

Armor/health/speed/regen семи материалов не получают Guard-boost. R-darts,
декорации, advanced/enchanted weapon work, Order player/animal/creeper/PvP
categories остаются отдельно pending. Игровой combat/protection/visual/save
проход отложен; эта группа не закрывает полный M10 и не заменяет Bore-only JAR.

`golem-guard-build1` PASS42s, main/remap/production isolation;9 существующих data
JUnit (не native combat). Private JAR11038264bytes/SHA256
`e50f60f769103945f108b37e6815fa8cc738d3706edbe6b85fe743b21257ed9d`.
