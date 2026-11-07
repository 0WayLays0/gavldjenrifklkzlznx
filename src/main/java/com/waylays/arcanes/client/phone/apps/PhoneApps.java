package com.waylays.arcanes.client.phone.apps;

import com.waylays.arcanes.client.phone.PhoneApp;

import java.util.List;

/** Every app of the Arcanophone. */
public final class PhoneApps {
    private PhoneApps() {}

    public static List<PhoneApp> all() {
        return List.of(new MissivesApp(), new ContactsApp(), new PhotoApp(), new MusiqueApp(), new CarteApp(), new GalerieApp(), new MeteoApp(),
                new CalculApp(), new NotesApp(), new BanqueApp(), new EchosApp(), new GrimoireApp(), new HorlogeApp(), new BoussoleApp(),
                new ReglagesApp(), new DesApp());
    }
}
