# Исследование (этап 1)

Дата: 2026-09-29. Цель: Paper 1.21.8, ванильные GUI, похожие проекты.

## 1. Похожие проекты

| Проект | Что взяли |
|---|---|
| [ParrotMail (Spigot, 1.17–1.20.1)](https://www.spigotmc.org/resources/parrotmail-1-17-1-20-1.110574/) | Доставка предметов попугаями между игроками; поведение реализовано деревом поведения (behavior tree); неудачные доставки персистятся в файл. Подтверждение идеи сессий + восстановления груза. |
| [PostPigeon (Spigot)](https://www.spigotmc.org/resources/postpigeon.60841/) | Приручение едой, инвентарь попугая, `pigeon post <player>`. Идея «попугай-переносчик». |
| [Delivery Master (Modrinth)](https://modrinth.com/plugin/delivery) | Только GUI наград, без сущностей — референс по меню. |

Отличия KasaneParrot: живое GUI на Menu Type API, физический полёт
с посадкой, невидимые рамки, возврат на плечо, детальный конфиг.

## 2. Paper 1.21.8 — проверенные API

### Menu Type API (ванильные GUI)
- Доки: https://docs.papermc.io/paper/dev/menu-type-api/ (написано для 1.21.8)
- `MenuType.GENERIC_9X1..GENERIC_9X6` — сундучные меню;
  `MenuType.GENERIC_9X6.builder().title(Component).build(player)` → `InventoryView`,
  затем `view.open()`.
- `InventoryView#setTitle(Component)` — **живая смена заголовка** без
  переоткрытия окна (используем для счётчика онлайна).
- Обновление содержимого — правка `view.getTopInventory()`.
- API помечен `@Experimental` → `@SuppressWarnings("UnstableApiUsage")`.
- В 1.21.8 `MenuType.Typed<V, B>` имеет **два** генерика
  (`Typed<InventoryView, InventoryViewBuilder<InventoryView>>`),
  у 9x3/9x6 builder — `LocationInventoryViewBuilder`.

### Попугаи
- `Parrot#getVariant()/setVariant(Parrot.Variant)` — RED/BLUE/GREEN/CYAN/GRAY
  (javadoc 1.21.8). Смена «модельки» = смена варианта + ресурспак.
- Приручение — ваниль (семечки), `Tameable#isTamed/getOwnerUniqueId`.

### Плечо
- `HumanEntity#setShoulderEntityLeft/Right(Entity)`,
  `releaseLeft/RightShoulderEntity()` — deprecated («нет чёткой
  семантики сериализации»), но клиент рендерит на плече только
  попугаев — подходит. Метод сам убирает сущность из мира.

### Видимость/скрытие попугая в полёте
- `Entity#setVisibleByDefault(false)` + `Player#showEntity/hideEntity`
  (Paper, 1.20.2+; подтверждено javadoc 1.21.3/1.21.9).
- `Entity#getScheduler()` (EntityScheduler) — Folia-совместимые
  per-entity задачи (заложено в дорожную карту, пока BukkitScheduler).

### Рамки
- `ItemFrame#setVisible(boolean)`, `setFixed(boolean)`,
  `setItemDropChance(float)`, `Hanging#setFacingDirection(BlockFace, boolean)`.
- `PlayerItemFrameChangeEvent` — пакет **io.papermc.paper.event.player**
  (в org.bukkit.event.player такого класса в 1.21.8 НЕТ), вложенный enum
  `ItemFrameChangeAction`: `PLACE`, `REMOVE`, `ROTATE` (константы LEFT_CLICK
  нет). ЛКМ по заполненной рамке = действие `REMOVE` — его и перехватываем:
  отмена ванили + свой дроп предмета + удаление рамки.

### Прочее
- `Player/OfflinePlayer#getRespawnLocation()` — точка спавна
  (кровать/якорь; `getBedSpawnLocation` deprecated с 1.20.4).
- `Entity#teleportAsync(Location)` — телепорты без лагов главного потока.
- `Bukkit.createProfile(uuid, name)` + `SkullMeta#setPlayerProfile` —
  головы для меню (кэш + LRU).
- `EntityRemoveEvent` (io.papermc.paper.event.entity) — защита курьера
  от деспавна (Cause.DESPAWN).
- MiniMessage, Adventure — в составе Paper.

## 3. Ресурспак «Pesky Parrots»

Файл: `Pesky Parrots.zip` (в корне репозитория, загружен автором).

- OptiFine-пак: CEM-модель `optifine/cem/parrot.jem`, emissive-текстуры
  (`*_e.png` + `emissive.properties`).
- Перетекстурирует **все 5 вариантов** попугаев; у каждого ~47 случайных
  скинов с весами (`optifine/mob/parrot/parrot_<variant>.properties`,
  скины типа «Mr. Seedyton», «Hedwig», «Ender Parrot», «Owl», …).
- Ванильные клиенты видят только базовые текстуры
  (`textures/entity/parrot/parrot_*.png`) — они тоже переопределены.
- `pack.mcmeta`: `pack_format: 9` (1.19.x) — для 1.21.8 нужно обновить
  до актуального формата (иначе клиент ругается на версию).
- **Вывод для плагина**: смена варианта — рабочий механизм смены
  «модельки». Для гарантированно отличимого курьерского вида на этапе 4
  можно сделать один вариант детерминированным (одна текстура без
  рандома) — правкой `.properties` внутри пака.

## 4. Ограничения среды разработки (примечание)

В песочке разработки наружная сеть ограничена (GitHub/PyPI), поэтому
компиляция валидируется через GitHub Actions CI (`.github/workflows/build.yml`)
— сборка на каждый пуш + артефакт-jar. JDK локально — 25 (jdk4py),
совместимость обеспечена `--release 21`.

## 5. Открытые вопросы к следующему этапу

1. Интерполяция телепортов: проверить плавность на реальном клиенте
   (Paper интерполирует entity-телепорты ~3 тика; при необходимости —
   `setVelocity`-гибрид).
2. `EntityRemoveEvent#setCancelled` — проверить поддержку отмены
   в 1.21.8 (иначе защита деспавна через `setPersistent(true)`).
3. Звуки/частицы полёта — вынести в конфиг (матрица фаз).
4. Подтвердить поведение `PlayerItemFrameChangeEvent.LEFT_CLICK`
   на живом сервере (предмет не должен дублироваться с нашим дропом).
