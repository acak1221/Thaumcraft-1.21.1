# Thaumcraft 4 Reload 0.1.0-alpha.13

The research browser and research-note tooltips now read the registered add-on entry's live `getName()`/`getText()` overrides. Previously they always read static translation aliases, so an add-on setting such as hiding its `[TM]` label had no effect. Bundled research keeps its existing translations and styles.

This builds on alpha.12's add-on recipe/focus compatibility, construct-blueprint support and corrected Fortress masks/ornaments. The separately packaged Tainted Magic Fabric port requires this version for its original research-label configuration.

Verified with native client checks of both label settings and the real research-note tooltip; existing gameplay and rendering acceptance is recorded in the migration status. No add-on, donor JAR or test mod is bundled in the base archives.
