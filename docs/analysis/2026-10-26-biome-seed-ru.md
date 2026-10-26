# 26.10.2026 — Пятна биомов TC4 не зависели от сида мира

## Что было

Волшебный лес и порча выбираются поверх ванильных биомов (`Tc4MultiNoiseBiomeSourceMixin`,
`BiomePatchPolicy`): островки строятся слоями LCG с сидом мира, который `Tc4RandomStateMixin`
кладёт в семплер климата `RandomState.sampler()`.

Генерация чанка этот семплер не использует. `NoiseBasedChunkGenerator.doCreateBiomes` заполняет
биомы чанка через `NoiseChunk.cachedClimateSampler` — новый `Climate.Sampler` поверх кэша шума
чанка, в котором сида мира не было. Поэтому:

- в каждом мире пятна TC4 в сгенерированных чанках раскладывались по сиду 0 (на фоне ванильного
  климата, который от сида зависит);
- `/locate biome thaumcraft:magical_forest` и всё, что спрашивает источник биомов с настоящим
  семплером, указывали в другие места. Найдено при съёмке скриншотов: `/locate` дал
  `511 71 543`, а там равнины (`/execute if biome … thaumcraft:magical_forest` → `Test failed`).

GameTest этого не видел: сервер GameTest создаёт мир с сидом 0 (`GameTestServer.WORLD_OPTIONS`,
`new WorldOptions(0L, …)`), и оба семплера совпадали.

## Исправление

`Tc4NoiseChunkSeedMixin`: `NoiseChunk` запоминает сид из своего `RandomState` и ставит его на
семплер, который возвращает `cachedClimateSampler`. Других конструкторов `Climate.Sampler` в
1.21.1 нет, кроме `RandomState` и пустого `Climate.empty()` (проверено по байткоду).

Миры, созданные прежними сборками порта, получат шов между старыми и новыми чанками: старые
чанки хранят биомы по сиду 0. Порт поддерживает только свежие миры.

## Проверки

| Проверка | Результат |
|---|---|
| Новый GameTest `generated_chunk_biomes_follow_the_world_seed` (сид 912873, шесть чанков с пятном TC4 по источнику, биомы заполняет настоящий `createBiomes`) | без миксина **падает**: чанк `[-256, -32]` — `minecraft:plains`, источник — `thaumcraft:magical_forest`; с миксином все шесть совпадают |
| Тесты биомов и узлов (9 штук, включая новый) | Fabric **9/9**, NeoForge **9/9** на свежих мирах |
| `:common:test` | **2590/2590** |
| `:fabric:build`, `:neoforge:build` | пройдено |
| Клиент | новый мир после исправления — см. скриншоты README |
