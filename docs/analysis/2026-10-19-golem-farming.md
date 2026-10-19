# Базовая Жатва — реализация19.10.2026

Интегрировано и **собрано на Fabric; не проверено в игре**. Original/source contract:
[Harvest3](2026-10-19-golem-harvest.md); native integration:
[API](2026-10-19-golem-harvest-api.md).

Root подключил core3/recipe/book/model/переводы, data baseline без ghost controls
и menu refusal для Harvest. Runtime FINAL прочитан: реальный поиск/путь/задержка/
срез ripe allowlisted crops с обычными drops. Нет base replanting, pickup, cargo
или работы по marker list; для подбора нужен отдельный Gather/игрок.

Protection gate выполняет единственный native destroy-with-drops после loaded/
border/height/reach/mobGriefing/ripe проверки и стандартных Architectury+Fabric
claim callbacks. Служебный FakeThaumcraftGolem не спавнится, не получает OP/предметы
или чужой inventory. Если профиль оказался привилегированным/непустым или callback
изменил действительный контекст, действие запрещается. Вещи, попавшие в его
инвентарь от внешнего кода, не стираются. Callback failure не восстанавливает
captured block и не повторяет drops; полная атомарность power-loss не обещана.

Для настоящего Messis-ингредиента добавлен original wheat direct row224:
Messis2/Fames1. Это не аспекты самого ядра и не награда/fixture. Meto уже выдаётся
исходной native bonus-веткой инструментов; рецепт остаётся Meto5/Messis5.

Новых тестовых обвязок/аудиторов/миров не создаётся. После FINAL runtime — actual
Fabric build1 остановился до тестов из-за отсутствующегоMelonBlock; root
согласовал melon с его действительным nativeBlock.class. Build2 и существующие
узкие aspect-data проверки **PASS42s**,6JUnit безfailures/errors/skips; actual
main/remap/production isolation. Private JAR11022073bytes/SHA256
`1353e155960af3dadbc1f87b8c02244aea95b90d0adf41e7cfe252e19443ab79`.
Игровой farmer+Gather,
claim rejection, визуальные crack/action и restart остаются на итоговый проход.
NeoForge не разрабатывается/не запускается; Bore-only поставка не заменяется.
