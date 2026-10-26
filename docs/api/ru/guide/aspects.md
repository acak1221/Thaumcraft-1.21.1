# Аспекты, теги объектов и сканирование

Всё в Thaumcraft состоит из аспектов: 48 исходных (`Aspect.AIR`, `Aspect.ORDER`, `Aspect.MAGIC`...) и тех, что
регистрируют аддоны. `AspectList` — упорядоченное отображение «аспект → количество».

```java
AspectList list = new AspectList().add(Aspect.FIRE, 4).add(Aspect.MAGIC, 2);
int fire = list.getAmount(Aspect.FIRE);           // 4
Aspect[] sorted = list.getAspectsSorted();        // в порядке, в котором их рисует игра
Aspect ignis = Aspect.getAspect("ignis");         // по тегу; null, если такого нет
```

`add` складывает количества, `merge` оставляет большее, `remove` вычитает, `copy` делает независимую копию.

## Новый аспект

Исходный конструктор сразу регистрирует аспект:

<!-- snippet:aspect -->
```java
// An aspect of its own: a compound of two registered aspects, a white glyph tinted with its colour (this example
// borrows an original glyph), and the lang keys tc.aspect.tempus / tc.aspect.help.tempus.
TEMPUS = new Aspect("tempus", 0xB68CFF, new Aspect[]{Aspect.VOID, Aspect.ORDER},
        ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/ordo.png"), 1);
```
<!-- /snippet -->

- Тег уникален и в нижнем регистре; повторная регистрация тега бросает исключение.
- Текстура — квадратный белый или серый знак (у исходных 32×32), окрашиваемый цветом аспекта. Знак из 1.7.10 подходит
  без изменений.
- Компоненты делают аспект составным: стол исследований собирает его из них, его можно кристаллизовать и переносить
  как эссенцию. `new Aspect(tag, color, chatColor, blend)` создаёт первичный (без компонентов); игроки получают его при
  первом входе, но вис жезлов, скипетров и амулетов, ретрансляторы вис, осколки и руды есть только для шести исходных
  первичных.
- Ключи перевода: `tc.aspect.<tag>` (смысл под названием) и `tc.aspect.help.<tag>`.
- Регистрируйте одни и те же аспекты в одном порядке на обеих сторонах, в `onThaumcraftReady`: клиент без аспекта не
  сможет прочитать предметы и знания, которые его упоминают.

Дальше аспект аддона хранится, синхронизируется, сканируется, собирается на столе исследований, переносится как
эссенция в банках, трубах, фиалах и кристаллах и рисуется везде, где рисуются аспекты. Полный контракт — в Javadoc
[`Aspect`](../../reference/thaumcraft.api.aspects.md#aspect).

## Теги объектов: аспекты предметов

<!-- snippet:object-tags -->
```java
ThaumcraftApi.registerObjectTag(new ItemStack(Items.CLOCK), new AspectList().add(TEMPUS, 4).add(Aspect.MECHANISM, 2));
// The 1.7.10 ore dictionary call keeps working: "ingotCopper" is the c:ingots/copper tag.
ThaumcraftApi.registerObjectTag("ingotCopper", new AspectList().add(Aspect.METAL, 3).add(Aspect.EXCHANGE, 1));
// 1.7.10: ThaumcraftApi.registerObjectTag(new ItemStack(item, 1, meta), aspects). There is no metadata now.
ThaumcraftApi.registerObjectTag(new ItemStack(VIS_SHARD), new AspectList().add(Aspect.MAGIC, 2).add(Aspect.CRYSTAL, 2));
```
<!-- /snippet -->

`ThaumcraftApi.registerObjectTag` принимает:

| Форма | Значение |
|---|---|
| `ItemStack` | этот предмет (с его повреждением, если предмет изнашивается) |
| `Item, int metadata` / `ResourceLocation, int metadata` | повреждение изнашиваемого предмета; `32767` — любое |
| `TagKey<Item>` | все предметы тега |
| `String` | имя рудного словаря 1.7.10 (`"ingotCopper"`) или id тега с пространством имён (`"c:ingots/copper"`) |

Старые имена рудного словаря разрешает `OreDictionaryNames`: имена, которыми пользовался сам Thaumcraft 4, сохраняют
исходный состав (`thaumcraft:original_ore/*`), а соглашение Forge `<вид><Материал>` — это тег `c:` (`ingotCopper` →
`c:ingots/copper`, `gemNetherQuartz` → `c:gems/nether_quartz`, `dyeLightBlue` → `c:dyes/light_blue`).

Теги — это данные: когда выполняется `onThaumcraftReady`, теги ещё не привязаны. Регистрация по тегу сохраняется и
применяется заново при старте мира и при входе клиента на сервер; она заполняет каждый предмет тега без собственной
регистрации. Регистрация самого предмета всегда важнее тега.

### Сложные теги

Сложный тег добавляет аспекты к тем, что предмет получает из рецепта, а не заменяет их. Ему нужны рецепты работающего
сервера, поэтому вызов переезжает из `postInit` 1.7.10 в событие запуска сервера:

<!-- snippet:complex-tag -->
```java
// A complex tag adds to the aspects the item gets from its recipe, so it needs the recipes of a running server:
// 1.7.10 add-ons called it in postInit, in 1.21.1 it goes into a server start event.
ServerLifecycleEvents.SERVER_STARTED.register(server -> ThaumcraftApi.registerComplexObjectTag(
        new ItemStack(ESSENTIA_BOWL), new AspectList().add(Aspect.VOID, 2), server.overworld()));
```
<!-- /snippet -->

На NeoForge тот же вызов идёт в `ServerStartedEvent` с `event.getServer().overworld()`.

### Чтение аспектов

`ThaumcraftApiHelper.getObjectAspects(stack, level)` отвечает, какие аспекты даст сканирование предмета (его
регистрация, тег, рецепт или вывод, которым порт наделяет предметы без регистрации, плюс содержимое контейнера
эссенции); форма без мира не видит аспектов из рецептов. Контейнеры эссенции сообщают содержимое через
`IEssentiaContainerItem.getAspects(stack)`: так делают банки, кристаллизованная эссенция, эссенция блуждающего огонька и
бобы маны. Фиалы в этом порте этот интерфейс не реализуют; их аспект входит в их сканируемые аспекты.

## Теги существ

<!-- snippet:entity-tag -->
```java
// An entity the original never knew, by its registry id: the thaumometer gives these aspects for scanning a frog.
ThaumcraftApi.registerEntityTag("minecraft:frog", new AspectList().add(Aspect.WATER, 2).add(Aspect.BEAST, 2).add(TEMPUS, 1));
```
<!-- /snippet -->

Существа ванили и Thaumcraft, которые были в 1.7.10, сохраняют исходные имена (`"Zombie"`, `"Thaumcraft.Firebat"`);
любое другое существо называется id реестра (`"mymod:my_entity"`, `"minecraft:frog"`). Фильтры
`ThaumcraftApi.EntityTagsNBT` по-прежнему выбирают строку по данным существа, как в оригинале (например, строка
заряженного крипера). Если правый клик по существу делает что-то своё (тихоня забирает предмет из руки, житель
торгует), этот клик срабатывает раньше, чем таумометр успевает сканировать.

## Обработчики сканирования

`ThaumcraftApi.registerScanEventhandler(IScanEventHandler)` сохраняет исходный хук: если таумометр не нашёл ни
существа, ни блока, обработчики опрашиваются на сервере в порядке регистрации, и первый не-null `ScanResult` — то, что
отсканировано. Создавайте его через `ScanResult.item(stack)` или `ScanResult.entity(entity)`; числовой конструктор
`id`/`meta` из 1.7.10 работает, но id реестра действуют только в текущем запуске. Как в оригинале, явление (тип 3)
засчитывается только для узла ауры; любая другая строка явления ничего не даёт.

```java
// Взгляд в пустоту, стоя в воде, сканирует ведро воды.
ThaumcraftApi.registerScanEventhandler((stack, level, player) ->
        player.isInWater() ? ScanResult.item(new ItemStack(Items.WATER_BUCKET)) : null);
```

## Очки исследований

Сканирование даёт игроку очки исследований по отсканированным аспектам. Аддон может добавлять и читать их напрямую:
`ThaumcraftApiHelper.addAspectPool(player, aspect, amount)` и `getAspectPool(player, aspect)` (см.
[исследования](research.md#знания-игрока)).
