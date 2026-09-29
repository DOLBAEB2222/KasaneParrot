# Архитектура KasaneParrot

## 1. Слои и модули

```
┌────────────────────────────────────────────────────────────┐
│  KasaneParrotPlugin (сборка + жизненный цикл)              │
├────────────────────────────────────────────────────────────┤
│  config.KasaneConfig      ← config.yml (immutable-снапшот) │
│  text.MessageService      ← messages_ru.yml (MiniMessage)  │
│  key.PluginKeys           ← NamespacedKey + PDC-хелперы    │
├────────────────────────────────────────────────────────────┤
│  module.PluginModule / ModuleManager                       │
│  (единый enable/disable/reload для всех подсистем)         │
├────────────────────────────────────────────────────────────┤
│  parrot:   CourierRegistry · CourierParrot · CourierState  │
│            ParrotModelService (вариант + имя)              │
│  flight:   MovementAnalyzer · TargetActivity               │
│            LandingSpotFinder · LandingSpot · FlightPath    │
│  delivery: DeliveryService (машина состояний)              │
│            DeliverySession · DeliveryRequest · TimeCalc    │
│  gui:      MenuService · PlayerSelectMenu                  │
│            DestinationMenu · ConfirmMenu · HeadCache       │
│  frame:    DeliveryFrameService (невидимые рамки)          │
│  shoulder: ShoulderService                                 │
├────────────────────────────────────────────────────────────┤
│  listener: ParrotInteract · FrameInteraction ·             │
│            PlayerConnection · Menu · CourierProtection     │
│  event:    ParrotPouched · DeliveryStarted/Completed/Failed│
│  command:  /kasane                                         │
└────────────────────────────────────────────────────────────┘
```

Правило зависимостей: слушатели → сервисы → конфиг. Сервисы читают
конфиг через `plugin.cfg()` (живой доступ, корректный `/kasane reload`).

## 2. Данные на сущностях (PDC)

| Ключ | Тип | Где | Смысл |
|---|---|---|---|
| `courier` | byte | попугай | зарезервировано: пометка Kasane-питомца |
| `pouched` | byte | попугай | мешочек надет (восстановление после рестарта) |
| `original_variant` | string | попугай | исходный вариант окраса |
| `owner` | string(uuid) | попугай | хозяин (быстрая проверка) |
| `delivery_frame` | byte | рамка | это рамка доставки |
| `frame_session` | string(uuid) | рамка | сессия, к которой относится рамка |

Ленивое восстановление: если `pouched` стоит, а записи в реестре нет
(рестарт сервера), `ParrotInteractListener.resolveCourier` пересоздаёт
`CourierParrot` из PDC.

## 3. Машина состояний доставки

```
NORMAL ─Shift+ПКМ мешочком─► POUCHED ─подтверждение─► ASCENDING
                                ▲                        │
                                │                  высота 100 + скрытие
                                │                        ▼
                                │                    VANISHED ◄── WAIT
                                │                        │        (цель вышла:
                                │                  таймер времени   ждём входа)
                                │                        ▼
REUNITED/POUCHED ◄─ RETURNING ◄─ DEPARTING ◄─ HANDOFF ◄─ DESCENDING
                        ▲          (рамка)   (пауза 2с)     ▲ 50 блоков над целью
                        │                                     │
                        └────── RETURNING_FAILED ◄────────────┘
                                   (ошибка: нет места/таймаут/потеря)
```

`DeliveryService.tick()` (период `flight.tick-period`) диспетчеризует
фазы по времени; длительности фаз вычисляются из конфига
(`высота / скорость × 50 мс`). На этапе 2 внутрь фаз встраиваются
`FlightController`-ы без изменения оркестратора.

## 4. Ключевые алгоритмы

### 4.1 Время доставки (`DeliveryTimeCalculator`)

```
d = dist(отправитель, получатель)         (разные миры → cross-world-seconds)
d' = clamp(d, min-distance, max-distance)
t = lerp(min-seconds, max-seconds, (d' - min-distance) / (max-distance - min-distance))
```

### 4.2 Умная посадка (`MovementAnalyzer` + `LandingSpotFinder`)

1. **Анализ цели** (приоритеты): мёртв → спит → в транспорте → элитры →
   полёт → плавание; иначе по сглаженной скорости окна (10 семплов × 2
   тика): ≥0.22 бл/тик — бег, ≥0.08 — ходьба, крауч, иначе стоит.
2. **Стратегия**: стоящему/идущему — посадка на ближайшую безопасную
   площадку; бегущему — посадка с упреждением; элитрам/транспорту —
   hover-follow до остановки (этап 2), но не дольше `max-wait-seconds`.
3. **Поиск площадки**: кандидаты = дуга перед взглядом (±60°) + кольца
   радиусами `min-distance..radius`; для каждого — столб вниз
   (`y+5 .. y-10`) до твёрдой поверхности с 2 блоками воздуха; фильтр
   опасных материалов (лава, кактус, огонь, …) и воды; скоринг
   «ближе к цели — лучше, штраф за перепад высоты».

### 4.3 Полёт (этап 2, заготовки уже в коде)

`FlightPath` — кубическая Безье (`straight`/`arc`), `Easing` —
разгон/торможение (`EASE_IN_OUT_CUBIC` по умолчанию). Взлёт — спираль
радиуса `spiral-radius`, снижение — дуга к точке посадки, возврат —
дуга к хозяину. Позиция применяется телепортами (клиент интерполирует),
направление — из касательной Безье.

### 4.4 Живое GUI (Paper Menu Type API)

- Окно: `MenuType.GENERIC_9xN.builder().title(...).build(player).open()`.
- Обновление контента — правка top-инвентаря (клиент видит сразу).
  В 1.21.8 сменить заголовок открытого окна чистым API нельзя
  (`setTitle` принимает только String и сломан), поэтому заголовок
  фиксируется при открытии, а динамика — в слотах.
- **Оптимизация join/quit**: `MenuService.scheduleOnlineRefresh()`
  планирует единственную отложенную задачу (`refresh-delay-ticks`);
  пачка событий схлопывается в один рефреш, перерисовываются только
  открытые `PlayerSelectMenu`.
- Клики/драги в наших окнах всегда отменяются — меню чистый UI.

## 5. Рамки доставки

Невидимый `ItemFrame` крепится к верхней грани блока-земли (`BlockFace.UP`),
`itemDropChance=0`. `PlayerItemFrameChangeEvent`:
- `LEFT_CLICK` → отмена + свой обработчик: `world.dropItemNaturally`,
  рамка удаляется (поведение из ТЗ);
- `PLACE/REMOVE/ROTATE` → отмена (взять правым кликом нельзя).
Взрыв/физика (`HangingBreakEvent`) → дроп предмета без потери.
Таймер жизни (`lifetime-seconds`) → автосброс предмета на землю.

## 6. Плечо

`HumanEntity#setShoulderEntityLeft/Right` — deprecated в Bukkit, но это
единственный чистый способ, и клиент рендерит на плече только попугаев
(наш случай). Снятие — `releaseLeft/RightShoulderEntity()` перед новой
отправкой, если попугай «на плече».

## 7. Конфиг

Единая точка — `config.KasaneConfig` (immutable вложенные классы).
Полный перечень параметров с комментариями — в самом `config.yml`.

## 8. Дорожная карта

| Этап | Содержимое | Статус |
|---|---|---|
| 1 | Исследование, архитектура, скелет модулей | ✅ |
| 2 | Полётные контроллеры: спираль, сплайн, hover-follow, частицы/звуки | ⏳ |
| 3 | paperweight runServer, игровой тест, полировка таймингов | ⏳ |
| 4 | Persistence сессий (YAML), ресурспак-раздача, история доставок | ⏳ |
| 5 | Folia (EntityScheduler), локализация EN, API для других плагинов | идея |
