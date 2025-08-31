package net.theevilreaper.vulpes.generator.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.key.Key;
import net.theevilreaper.vulpes.generator.gson.adapter.KeyGsonAdapter;

public final class GsonHolder {

    public static final Gson GSON;

    static {
        GSON = new GsonBuilder()
                .registerTypeAdapter(Key.class, KeyGsonAdapter.createMinestom())
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
    }
}
