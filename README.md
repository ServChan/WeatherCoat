# Weather Coat

[![Minecraft Version](https://img.shields.io/badge/Minecraft-26.3-brightgreen?style=flat-square&logo=minecraft)](README.md)
[![Platform](https://img.shields.io/badge/Platform-Fabric-blue?style=flat-square&logo=fabric)](README.md)
[![Java Target](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](README.md)
[![Mod Version](https://img.shields.io/badge/Version-1.0.0-purple?style=flat-square)](README.md)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

Mobs and players get wet in the rain and snowy in the snow, step by step, and dry off or melt afterwards.

## Русский

### Что это

`Weather Coat` (mod id `weathercoat`) — клиентский Fabric-мод для Minecraft 26.3. Мобы и игроки под открытым небом постепенно намокают под дождём и покрываются снегом в снегопад, а потом так же постепенно высыхают и оттаивают. Мод только рисует: на сервере и у других игроков он не нужен.

### Возможности

- **Стадии**: вид меняется не сразу, а за 3–5 шагов (по умолчанию 4); каждый шаг занимает настраиваемое время под осадками, высыхание и таяние идут теми же шагами назад;
- **Мокрый моб** темнеет и становится насыщеннее, по бокам стекают струйки с каплями на концах, на шерсти и коже появляются блики;
- **Заснеженный моб**: снег сначала ложится на открытые сверху грани (спина, голова, плечи), затем сползает по кромкам боков; места, накрытые другой частью модели (ноги под туловищем, тело под головой), остаются чистыми; на последних стадиях появляется иней и налипшие хлопья;
- текстуры генерируются из родной текстуры моба и формы его модели, поэтому эффект работает для всех мобов, ресурспаков и мобов из других модов; слои вроде шерсти овец, одежды жителей и скелетов, оболочки утопленников, внешнего слоя слизня, отметин лошадей, ошейника волка и плаща игрока тоже намокают;
- на окрашенной шерсти и крашеной коже снег остаётся белым: он рисуется отдельным неокрашенным слоем поверх;
- скины игроков обрабатываются так же, как текстуры мобов;
- экипировка мокнет и заснеживается вместе с хозяином: броня игроков и мобов, сёдла свиней, лошадей и страйдеров, конская, волчья и наутилусовая броня, ковры лам, сбруя гаста, элитры; отделка брони (узоры) остаётся как есть;
- снег только там, где он реально идёт (холодные биомы), дождь — где идёт дождь; под крышей стадия не растёт; в воде моб не сохнет; огонь и лава сразу высушивают и растапливают; в тёплых биомах снег тает вдвое быстрее, гроза мочит быстрее;
- **Догонять при появлении**: моб, попавший в поле зрения посреди долгого дождя, сразу выглядит мокрым;
- генерация идёт в фоновом потоке, неиспользуемые текстуры освобождаются через минуту, при смене ресурспаков всё пересоздаётся.

### Управление

Клавиш нет: эффект работает сам. Настройки — через Mod Menu.

### Настройки

Экран **Mod Menu → Weather Coat**: включение, игроки, число стадий, «догонять при появлении», дальность обновления, отдельные переключатели и время на стадию для намокания, высыхания, снега и таяния. Файл `config/weathercoat.json` (атомарная запись, резервная копия `.bak`).

### Установка

1. **Fabric Loader** `0.19.3+` и **Fabric API** `0.161.0+26.3`.
2. Скопируйте `weather-coat-1.0.0.jar` из `build/libs/` в папку `mods/`.
3. **Mod Menu** — опционально, открывает экран настроек.

**Требования:** Minecraft `26.3` · Java `25` · только клиент.

### Сборка

```powershell
.\gradlew.bat clean build
```

Готовый JAR: `build/libs/weather-coat-1.0.0.jar`.

---

## English

### What It Is

`Weather Coat` (mod id `weathercoat`) is a client-side Fabric mod for Minecraft 26.3. Mobs and players under open sky gradually get wet in the rain and snowy in the snow, then dry off and melt just as gradually. The mod only draws: neither the server nor other players need it.

### Features

- **Stages**: the look changes in 3–5 steps (4 by default) instead of all at once; each step takes a configurable time under precipitation, and drying and melting walk back through the same steps;
- **Wet mobs** get darker and more saturated, with water streaks running down their sides, droplets at the streak ends, and glints on fur and skin;
- **Snowy mobs**: snow settles first on faces open to the sky (back, head, shoulders), then creeps down the upper edges of the sides; parts covered by another part of the model (legs under the body, the body under the head) stay clean; the last stages add frost and stuck flakes;
- the textures are generated from the mob's own texture and the shape of its model, so the effect works for every mob, resource pack, and modded mob; layers such as sheep wool, villager and skeleton clothing, the drowned outer layer, the slime's outer gel, horse markings, wolf collars and player capes get wet too;
- snow stays white on dyed wool and dyed leather: it is drawn as a separate untinted layer on top;
- player skins are handled just like mob textures;
- equipment gets wet and snowy along with its wearer: player and mob armor, pig, horse and strider saddles, horse, wolf and nautilus armor, llama carpets, happy ghast harnesses, elytra; armor trims keep their look;
- snow only where it actually snows (cold biomes), rain only where it rains; stages do not grow under a roof; mobs do not dry while in water; fire and lava dry and melt everything at once; snow melts twice as fast in warm biomes, and thunderstorms soak faster;
- **Catch up on sight**: a mob that comes into view in the middle of a long rain looks wet right away;
- generation runs on a background thread, unused textures are released after a minute, and everything is rebuilt when resource packs change.

### Controls

There are no keys: the effect runs on its own. Settings are in Mod Menu.

### Configuration

The **Mod Menu → Weather Coat** screen: on/off, players, number of stages, catch up on sight, update distance, and separate toggles and time per stage for wetting, drying, snow, and melting. `config/weathercoat.json` (atomic write, `.bak` backup).

### Installation

1. **Fabric Loader** `0.19.3+` and **Fabric API** `0.161.0+26.3`.
2. Copy `weather-coat-1.0.0.jar` from `build/libs/` into `mods/`.
3. **Mod Menu** — optional, opens the settings screen.

**Requirements:** Minecraft `26.3` · Java `25` · client-only.

### Building

```powershell
.\gradlew.bat clean build
```

Output JAR: `build/libs/weather-coat-1.0.0.jar`.

## Лицензия / License

[MIT](LICENSE). Developed by LTS_Server.
