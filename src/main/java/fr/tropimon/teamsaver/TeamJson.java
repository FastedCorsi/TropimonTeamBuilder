package fr.tropimon.teamsaver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class TeamJson {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TeamJson() {
    }
}
