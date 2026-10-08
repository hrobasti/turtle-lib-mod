package net.kroet.turtlelib.UpdateChecker;

/**
 * Mod loader a release must support. Modrinth filters by {@link #modrinthId()},
 * CurseForge files list the loader as {@link #curseForgeTag()}.
 */
public enum UpdateLoader {
    FABRIC("fabric", "Fabric"), NEOFORGE("neoforge", "NeoForge");

    private final String modrinthId;
    private final String curseForgeTag;

    UpdateLoader(String modrinthId, String curseForgeTag) {
        this.modrinthId = modrinthId;
        this.curseForgeTag = curseForgeTag;
    }

    public String modrinthId() {
        return modrinthId;
    }

    public String curseForgeTag() {
        return curseForgeTag;
    }
}
