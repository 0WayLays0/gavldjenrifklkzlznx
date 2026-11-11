package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.client.smart.PhoneApp;

import java.util.List;

/** Every app of the smartphone. */
public final class PhoneApps {
    private PhoneApps() {}

    public static List<PhoneApp> all() {
        return List.of(new TelephoneApp(), new MessagesApp(), new ContactsApp(), new PhotoApp(), new MurmuresApp(), new CarteApp(), new GalerieApp(),
                new MeteoApp(), new JournalApp(), new CalculApp(), new NotesApp(), new HorlogeApp(), new BoussoleApp(), new SerpentApp(), new DesApp(),
                new ReglagesApp());
    }
}
