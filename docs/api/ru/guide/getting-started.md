# С чего начать

Thaumcraft 4 Reload сохраняет публичный API Thaumcraft 4.2.3.5 в пакете `thaumcraft.api`, с типами Minecraft 1.21.1.
На этой странице — настройка проекта аддона, место для регистраций Thaumcraft и единицы, на которые опираются все
остальные страницы. Каждый фрагмент Java в руководствах скопирован из примера аддона
([`examples/example-addon`](../../../../examples/example-addon)), который компилируется при каждой сборке этого
репозитория: код, который вы здесь читаете, собирается с текущим API.

Переносите готовый аддон с 1.7.10? Рядом лежит [руководство по переносу](../../PORTING.md) (на английском): в нём старые
вызовы сопоставлены новым.

## Настройка проекта

Мод собирается на маппингах Mojang и Java 21 для Minecraft 1.21.1 под Fabric (Loader 0.16.14+, Fabric API) и
NeoForge 21.1, в обоих случаях с Architectury API 13.0.11+.

Установите мод в локальный Maven-репозиторий из копии этого репозитория:

```bash
./gradlew :fabric:publishToMavenLocal :neoforge:publishToMavenLocal
```

или положите jar релиза в `libs/` своего проекта. Рядом лежит `-sources.jar` с исходниками общего кода, в том числе
`thaumcraft.api`, поэтому IDE показывает документацию API.

**Fabric (Loom).** Loom перемаппит мод под ваши маппинги, так что подходят и Yarn, и Mojang.

```gradle
repositories { mavenLocal() }
dependencies {
    modImplementation "thaumcraft:Thaumcraft-fabric:0.1.0-alpha.7"   // или: modImplementation files("libs/Thaumcraft-fabric-0.1.0-alpha.7.jar")
    modImplementation "dev.architectury:architectury-fabric:13.0.11"
}
```

**NeoForge.** NeoForge работает на именах Mojang, так что jar релиза подключается как есть:
`implementation "thaumcraft:Thaumcraft-neoforge:0.1.0-alpha.7"` и запись `[[dependencies.<вашмод>]]` с
`modId = "thaumcraft"` в `neoforge.mods.toml`.

`fabric.mod.json` примера объявляет обе точки входа и зависимость:

<!-- file:examples/example-addon/src/main/resources/fabric.mod.json -->
```json
{
  "schemaVersion": 1,
  "id": "tc4_example",
  "version": "1.0.0",
  "name": "Thaumcraft 4 Reload example add-on",
  "description": "A small add-on written only against the public Thaumcraft 4 Reload API (thaumcraft.api). Development runs only.",
  "authors": ["Thaumcraft 4 Reload contributors"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["com.example.tc4addon.ExampleAddon"],
    "thaumcraft": ["com.example.tc4addon.ExampleAddon"]
  },
  "depends": {
    "fabricloader": ">=0.16.14",
    "minecraft": "1.21.1",
    "thaumcraft": "*"
  }
}
```
<!-- /file -->

## Куда идут регистрации

Предметы, блоки и типы блок-сущностей регистрируйте как в любом моде 1.21.1. Всё, что относится к Thaumcraft (аспекты,
теги объектов, рецепты, исследования, части жезлов), — в `IThaumcraftAddon.onThaumcraftReady()`:

<!-- snippet:lifecycle -->
```java
public final class ExampleAddon implements ModInitializer, IThaumcraftAddon {
    public static final String MOD_ID = "tc4_example";
    // ...
    /** Fabric "main": only the add-on's own registry objects. Thaumcraft's items may not exist yet. */
    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, id("vis_shard"), VIS_SHARD);
        // ...
    }

    /** The "thaumcraft" entrypoint: every mod's items exist, Thaumcraft's registries are open. Runs on both sides. */
    @Override
    public void onThaumcraftReady() {
        aspects();
        recipesAndResearch();
        wandParts();
        moreContent();
    }
```
<!-- /snippet -->

- **Fabric:** укажите класс в точке входа `"thaumcraft"`, как выше. Fabric вызывает точки входа `main` разных модов в
  произвольном порядке, поэтому предмета Thaumcraft может ещё не быть, когда выполняется ваш `onInitialize`.
- **NeoForge:** пометьте класс аннотацией `@IThaumcraftAddon.Addon` и дайте ему публичный конструктор без аргументов.
  Он вызывается из очереди основного потока `FMLCommonSetupEvent`.
- **На любом загрузчике:** `ThaumcraftApi.whenReady(Runnable)` из вашего инициализатора выполнит действие в тот же
  момент (сразу, если момент уже прошёл). `ThaumcraftApi.isReady()` сообщает, прошёл ли он.

```java
// NeoForge: тот же класс, найденный по аннотации вместо точки входа.
@IThaumcraftAddon.Addon
public final class MyAddon implements IThaumcraftAddon {
    @Override public void onThaumcraftReady() { /* аспекты, теги, рецепты, исследования, части жезлов */ }
}
```

Хук вызывается один раз, в основном потоке, на **обеих** физических сторонах: после того как все моды
зарегистрировали предметы, а Thaumcraft — своё содержимое, и до загрузки мира. Регистрируйте одно и то же на клиенте и
на сервере: ключи исследований, аспекты и части жезлов ищутся по имени на обеих сторонах. Порядок аддонов между собой
не задан. Реестры API не потокобезопасны: не трогайте их из параллельных конструкторов модов NeoForge.

## Единицы

Большинство ошибок переноса — ошибки единиц. Это единицы оригинала, без изменений:

| Величина | Единица |
|---|---|
| Стоимость магического рецепта (`addArcaneCraftingRecipe`, `"vis"` в JSON) | целые вис шести первичных аспектов |
| Стоимость тигля, наполнения и зачарования, банки, трубы, фиалы | единицы эссенции |
| Ёмкость `WandRod`/`StaffRod` | целые вис |
| Вис в жезле, `WandHelper.getVis`/`getAllVis`/`getMaxVis`/`addRealVis`/`consumeVis`/`consumeAllVis` | сантивис (1 вис = 100) |
| `WandHelper.addVis`, `consumeAllVisCrafting` | целые вис |
| Стоимость фокуса (`ItemFocusBasic.getVisCost`) | сантивис |
| `getActivationCooldown` фокуса | миллисекунды реального времени |
| `VisNetHandler.drainVis` | сантивис |
| Скидка `WandCap` | множитель (0.9 = на 10 % дешевле) |
| `IVisDiscountGear.getVisDiscount` | процентные пункты |

## Ключи перевода

Thaumcraft собирает названия из ключей перевода, как в 1.7.10 (теперь в `assets/<modid>/lang/en_us.json`,
`ru_ru.json` и т. д.):

| Ключ | Для чего |
|---|---|
| `tc.research_category.<CATEGORY>` | подсказка вкладки исследований |
| `tc.research_name.<KEY>`, `tc.research_text.<KEY>` | название и подзаголовок записи |
| любой ключ, переданный в `new ResearchPage(String)` | текст страницы (разметка `<BR>`, `<LINE>`, `<IMG>...</IMG>`) |
| `tc.aspect.<tag>`, `tc.aspect.help.<tag>` | название и смысл аспекта |
| `item.Wand.<tag>.rod`, `item.Wand.<tag>.cap` | слова, из которых складывается имя жезла |

## Пример аддона

[`examples/example-addon`](../../../../examples/example-addon) использует только `thaumcraft.api` (за этим следит тест
`ExampleAddonApiOnlyTest`) и показывает:

| Что | Где | Руководство |
|---|---|---|
| жизненный цикл, предметы, блок с блок-сущностью | `ExampleAddon.java` | эта страница, [эссенция и вис](essentia-and-vis.md) |
| аспект, теги объектов, сложный тег, тег существа | `ExampleAddon.aspects()` | [аспекты](aspects.md) |
| магический, бесформенный магический, тигель, наполнение; рецепты JSON | `ExampleAddon.recipesAndResearch()`, `data/tc4_example/recipe/` | [рецепты](recipes.md) |
| вкладка исследований с девятью записями (одна скрытая) | `ExampleAddon` | [исследования](research.md) |
| стержень с хуком обновления, колпачок, фокус | `ExampleAddon.wandParts()`, `SparkFocus.java` | [жезлы и фокусы](wands-and-foci.md) |
| блок, который хранит и отдаёт эссенцию и отвечает жезлу | `EssentiaBowlBlock.java`, `EssentiaBowlBlockEntity.java` | [эссенция и вис](essentia-and-vis.md) |
| амулет со скидкой, рунным зарядом и варпом | `OrderAmulet.java` | [снаряжение и безделушки](gear-and-baubles.md) |
| очки исследований и завершение исследования из предмета | `InsightScroll.java` | [исследования](research.md) |
| варп за крафт, бонус переплавки, мешки с добычей | `ExampleAddon.moreContent()` | [содержимое и мир](world-and-content.md) |

Запуск в клиенте разработки:

```bash
./gradlew :fabric:runExampleClient            # с -PwithEmi рецепты видны в EMI
```

`/thaumcraft research grant EXAMPLE_SHARDS` (и остальные ключи `EXAMPLE_*`) открывают записи в творческом мире.

## Что дальше

- [Справочник API](../../reference/README.md): каждый публичный тип и член, созданный из Javadoc.
- [LEGACY_IDS.md](../../LEGACY_IDS.md): id 1.21.1 для исходных полей `ConfigItems`/`ConfigBlocks`.
- [Договорённости](../README.md#договорённости) API, отличающиеся от прямого прочтения 1.7.10.
