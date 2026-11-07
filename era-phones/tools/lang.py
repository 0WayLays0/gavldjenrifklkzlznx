"""The mod's texts in English and French (src/main/resources/assets/eraphones/lang)."""
import kit

P = "phone.eraphones."

T = {
    # Items, keys, the creative tab
    "item.eraphones.radiotelephone": ("Brick Phone", "Radiotéléphone"),
    "item.eraphones.gsm": ("GSM Handset", "Téléphone GSM"),
    "item.eraphones.clapet": ("Flip Phone", "Téléphone à clapet"),
    "item.eraphones.smartphone": ("Smartphone", "Smartphone"),
    "item.eraphones.chargeur": ("Phone Charger", "Chargeur"),
    "item.eraphones.radiotelephone.era": ("Made in %s", "Modèle de %s"),
    "item.eraphones.gsm.era": ("Made in %s", "Modèle de %s"),
    "item.eraphones.clapet.era": ("Made in %s", "Modèle de %s"),
    "item.eraphones.smartphone.era": ("Made in %s", "Modèle de %s"),
    "itemGroup.eraphones": ("Era Phones", "Téléphones d'Époque"),
    "key.eraphones.phone": ("Take out the phone", "Sortir le téléphone"),
    "key.categories.eraphones": ("Era Phones", "Téléphones d'Époque"),
    "tooltip.eraphones.number": ("Number: %s", "Numéro : %s"),
    "tooltip.eraphones.battery": ("Battery: %s%%", "Batterie : %s %%"),
    "tooltip.eraphones.open": ("Use it, or press the phone key while it is in your hotbar", "Utilise-le, ou appuie sur la touche du téléphone s'il est dans ta barre d'action"),
    "tooltip.eraphones.charger": ("Hold it with a phone in your other hand and keep using it", "Tiens-le avec un téléphone dans l'autre main et maintiens l'utilisation"),
    "message.eraphones.no_phone": ("No phone in hand or in your hotbar", "Aucun téléphone en main ni dans ta barre d'action"),
    "message.eraphones.charger_needs_phone": ("Hold a phone in your other hand", "Tiens un téléphone dans l'autre main"),
    "message.eraphones.charging": ("Charging… %s%%", "Charge… %s %%"),
    "message.eraphones.charged": ("Battery full", "Batterie pleine"),
    # What the network says
    "info.eraphones.bad_number": ("\"%s\" is not a phone number", "« %s » n'est pas un numéro de téléphone"),
    "info.eraphones.no_network": ("No network here", "Pas de réseau ici"),
    "info.eraphones.unassigned": ("The number %s is not assigned", "Le numéro %s n'est pas attribué"),
    "info.eraphones.directory_full": ("The directory is full", "Le répertoire est plein"),
    "info.eraphones.bad_handle": ("Choose a name of 2 to 20 letters or figures", "Choisis un nom de 2 à 20 lettres ou chiffres"),
    "info.eraphones.handle_taken": ("@%s is already taken", "@%s est déjà pris"),
    "info.eraphones.battery_low": ("Battery low", "Batterie faible"),
    "info.eraphones.unread_waiting": ("%s unread messages wait on your phone", "%s messages non lus t'attendent sur ton téléphone"),
    # Game masters' commands
    "command.eraphones.unknown_number": ("No line has the number %s", "Aucune ligne n'a le numéro %s"),
    "command.eraphones.sent": ("Message sent to %s from a hidden number", "Message envoyé au %s depuis un numéro masqué"),
    "command.eraphones.unreachable": ("The phone %s cannot be reached (off, flat, or no one carries it)", "Le téléphone %s n'est pas joignable (éteint, sans batterie ou personne ne le porte)"),
    "command.eraphones.calling": ("Calling %s from a hidden number", "Appel du %s depuis un numéro masqué"),
    "command.eraphones.haunted": ("The phone %s goes wrong for %s s", "Le téléphone %s se dérègle pendant %s s"),
    "command.eraphones.published": ("Published: %s", "Publié : %s"),
    "command.eraphones.no_phones": ("No phone on them", "Aucun téléphone sur lui"),
    "command.eraphones.number_line": ("%s — %s (%s%%)", "%s — %s (%s %%)"),
    "command.eraphones.hold_phone": ("Hold a phone", "Tiens un téléphone"),
    # The phones
    P + "name": ("Phone", "Téléphone"),
    P + "about": ("Line %s", "Ligne %s"),
    P + "hidden": ("Unknown number", "Numéro inconnu"),
    P + "incoming": ("Incoming call", "Appel entrant"),
    P + "calling": ("Calling", "Appel en cours"),
    P + "call_short": ("CALL", "APPEL"),
    P + "call_over": ("Call ended", "Appel terminé"),
    P + "answer_key": ("[%s] to answer", "[%s] pour répondre"),
    P + "speak": ("Speak…", "Parler…"),
    P + "speak_hint": ("Take out the phone ([%s]) to speak", "Sors le téléphone ([%s]) pour parler"),
    P + "accept": ("Answer", "Répondre"),
    P + "decline": ("Decline", "Refuser"),
    P + "end": ("End", "Fin"),
    P + "call": ("Call", "Appeler"),
    P + "missed_call": ("Missed call", "Appel manqué"),
    P + "missed_short": ("%s missed call(s)", "%s appel(s) manqué(s)"),
    P + "you": ("You:", "Toi :"),
    P + "battery_empty": ("Battery flat", "Batterie vide"),
    P + "keypad": ("Keypad", "Clavier"),
    P + "recents": ("Recents", "Récents"),
    P + "no_calls": ("No call yet", "Aucun appel"),
    P + "directory": ("Directory", "Répertoire"),
    P + "my_number": ("My number", "Mon numéro"),
    P + "contact_name": ("Name", "Nom"),
    P + "contact_number": ("Number", "Numéro"),
    P + "no_contacts": ("No contact yet: write a name and a number above", "Aucun contact : écris un nom et un numéro au-dessus"),
    P + "share_position": ("Share my position", "Partager ma position"),
    P + "to": ("Number or name…", "Numéro ou nom…"),
    P + "to_number": ("To number", "Au numéro"),
    P + "write_to": ("Write to %s", "Écrire au %s"),
    P + "write": ("Write", "Écrire"),
    P + "new_message": ("New message", "Nouveau message"),
    P + "new_messages": ("%s new message(s)", "%s nouveau(x) message(s)"),
    P + "no_messages": ("No conversation yet: write to someone with ✎", "Aucune conversation : écris à quelqu'un avec ✎"),
    P + "cannot_reply": ("One cannot answer a hidden number", "On ne peut pas répondre à un numéro masqué"),
    P + "inbox": ("Inbox", "Boîte de réception"),
    P + "read": ("Read", "Lire"),
    P + "reply": ("Reply", "Répondre"),
    P + "sent": ("Message sent", "Message envoyé"),
    P + "send": ("Send", "Envoyer"),
    P + "erase": ("Erase", "Effacer"),
    P + "empty": ("Empty", "Vide"),
    P + "add": ("Add", "Ajouter"),
    P + "save": ("Save", "Enregistrer"),
    P + "saved": ("Saved", "Enregistré"),
    P + "removed": ("Removed", "Supprimé"),
    P + "remove": ("Remove", "Retirer"),
    P + "select": ("Select", "Choisir"),
    P + "back": ("Back", "Retour"),
    P + "ok": ("OK", "OK"),
    P + "menu": ("Menu", "Menu"),
    P + "bad_number_short": ("Wrong number", "Numéro invalide"),
    P + "no_network_short": ("No network", "Pas de réseau"),
    P + "ringing": ("Ringing", "Sonnerie"),
    P + "silent": ("Silent", "Silencieux"),
    P + "profile.normal": ("Normal", "Normal"),
    P + "profile.silent": ("Silent", "Silencieux"),
    P + "theme": ("Theme", "Thème"),
    P + "theme.blue": ("Ocean", "Océan"),
    P + "theme.pink": ("Candy", "Bonbon"),
    P + "theme.green": ("Jungle", "Jungle"),
    P + "theme.orange": ("Sunset", "Couchant"),
    P + "score": ("Score: %s", "Score : %s"),
    P + "best": ("Best: %s", "Record : %s"),
    P + "best_short": ("Best %s", "Rec. %s"),
    P + "game_over": ("Game over", "Perdu !"),
    P + "play_again": ("Click to play again", "Clique pour rejouer"),
    P + "play_again_short": ("OK: again", "OK : rejouer"),
    P + "new_post": ("%s posted", "%s a publié"),
    P + "post": ("Post", "Publier"),
    P + "post_hint": ("What's happening?", "Quoi de neuf ?"),
    P + "no_posts": ("Nothing posted yet", "Rien de publié pour l'instant"),
    P + "handle_choose": ("Choose the name you will sign your posts with", "Choisis le nom avec lequel tu signeras tes publications"),
    P + "handle_hint": ("name", "nom"),
    P + "journal_title": ("The Town Courier", "Le Courrier de la Ville"),
    P + "no_news": ("No news today", "Pas de nouvelles aujourd'hui"),
    P + "swipe": ("Swipe up to open", "Glisse vers le haut pour ouvrir"),
    P + "search": ("Search", "Rechercher"),
    P + "photos": ("PHOTOS", "PHOTOS"),
    P + "photo_mode": ("PHOTO", "PHOTO"),
    P + "photo_count": ("%s photos", "%s photos"),
    P + "no_photos": ("No photo yet: take one with the Camera", "Aucune photo : prends-en une avec l'Appareil"),
    P + "delete_hint": ("✕ to delete this photo", "✕ pour supprimer cette photo"),
    P + "wallpaper": ("Wallpaper", "Fond d'écran"),
    P + "finish": ("Finish", "Finition"),
    P + "sounds": ("Sounds", "Sons"),
    P + "now": ("now", "à l'instant"),
    P + "minutes": ("%s min", "%s min"),
    P + "hours": ("%s h", "%s h"),
    P + "days": ("%s d", "%s j"),
    P + "day": ("Day %s", "Jour %s"),
    P + "note": ("Note", "Note"),
    P + "no_notes": ("No note yet: + to write one", "Aucune note : + pour en écrire une"),
    P + "mark": ("Mark here", "Marquer ici"),
    P + "place": ("Place %s", "Lieu %s"),
    P + "places": ("Places", "Lieux"),
    P + "no_places": ("No place marked yet: tap Mark here to keep this spot", "Aucun lieu marqué : touche Marquer ici pour garder cet endroit"),
    P + "spawn": ("Town", "La ville"),
    P + "next_hours": ("Next hours", "Prochaines heures"),
    P + "forecast": ("Forecast", "Prévisions"),
    P + "moon": ("Moon", "Lune"),
    P + "sun": ("Sun", "Soleil"),
    P + "clear_for": ("Clear for about %s more min", "Beau temps encore ~%s min"),
    P + "rain_in": ("Rain in about %s min", "Pluie dans ~%s min"),
    P + "rain_ends": ("The rain stops in about %s min", "La pluie s'arrête dans ~%s min"),
    P + "storm_ends": ("The storm ends in about %s min", "L'orage cesse dans ~%s min"),
    P + "no_change": ("No change in sight", "Pas de changement en vue"),
    P + "sky.range": ("H:%s° L:%s°", "Max %s° Min %s°"),
    P + "sky.clear": ("Clear", "Dégagé"),
    P + "sky.rain": ("Rain", "Pluie"),
    P + "sky.thunder": ("Thunderstorm", "Orage"),
    P + "sky.snow": ("Snow", "Neige"),
    P + "sky.night": ("Clear night", "Nuit claire"),
    P + "world_time": ("Time in town", "Heure en ville"),
    P + "real_time": ("Real time", "Heure réelle"),
    P + "start": ("Start", "Démarrer"),
    P + "stop": ("Stop", "Arrêter"),
    P + "reset": ("Reset", "Remise à zéro"),
    P + "throw": ("Throw", "Lancer"),
    P + "last_throws": ("Last throws", "Derniers lancers"),
}

APPS = {"telephone": ("Phone", "Téléphone"), "messages": ("Messages", "Messages"), "contacts": ("Contacts", "Contacts"), "carte": ("Maps", "Plans"),
        "photo": ("Camera", "Appareil photo"), "galerie": ("Photos", "Photos"), "meteo": ("Weather", "Météo"), "journal": ("News", "Journal"),
        "calcul": ("Calculator", "Calculette"), "notes": ("Notes", "Notes"), "horloge": ("Clock", "Horloge"), "boussole": ("Compass", "Boussole"),
        "serpent": ("Snake", "Serpent"), "des": ("Dice", "Dés"), "reglages": ("Settings", "Réglages"), "murmures": ("Whispers", "Murmures"),
        "appel": ("Call", "Appel")}
MENU = {"messages": ("Messages", "Messages"), "contacts": ("Directory", "Répertoire"), "calls": ("Call log", "Journal d'appels"),
        "snake": ("Games", "Jeux"), "calc": ("Calculator", "Calculatrice"), "ring": ("Ringing", "Sonnerie"), "number": ("My number", "Mon numéro"),
        "camera": ("Camera", "Appareil photo"), "gallery": ("Gallery", "Galerie"), "settings": ("Settings", "Réglages")}
CALL_END = {"bad_number": ("Wrong number", "Numéro invalide"), "no_network": ("No network", "Pas de réseau"), "busy": ("Busy", "Occupé"),
            "unassigned": ("Number not assigned", "Numéro non attribué"), "unreachable": ("Cannot be reached", "Correspondant injoignable"),
            "declined": ("Call declined", "Appel refusé"), "no_answer": ("No answer", "Pas de réponse"), "ended": ("Call ended", "Appel terminé"),
            "lost": ("Call lost", "Appel interrompu"), "battery": ("Battery flat", "Batterie vide"), "": ("Call ended", "Appel terminé")}
KINDS = {"0": ("Outgoing", "Sortant"), "1": ("Incoming", "Entrant"), "2": ("Missed", "Manqué"), "3": ("Declined", "Refusé")}
WEEK = [("Monday", "Lundi"), ("Tuesday", "Mardi"), ("Wednesday", "Mercredi"), ("Thursday", "Jeudi"), ("Friday", "Vendredi"), ("Saturday", "Samedi"),
        ("Sunday", "Dimanche")]
MOON = [("Full moon", "Pleine lune"), ("Waning gibbous", "Gibbeuse décroissante"), ("Last quarter", "Dernier quartier"),
        ("Waning crescent", "Dernier croissant"), ("New moon", "Nouvelle lune"), ("Waxing crescent", "Premier croissant"),
        ("First quarter", "Premier quartier"), ("Waxing gibbous", "Gibbeuse croissante")]


def all_texts():
    t = dict(T)
    for k, v in APPS.items():
        t[P + "app." + k] = v
    for k, v in MENU.items():
        t[P + "menu." + k] = v
    for k, v in CALL_END.items():
        t[P + "call_end." + k] = v
    for k, v in KINDS.items():
        t[P + "call_kind." + k] = v
    for i, v in enumerate(WEEK):
        t[P + "weekday." + str(i)] = v
    for i, v in enumerate(MOON):
        t[P + "moon." + str(i)] = v
    return t


def main(extra=None):
    t = all_texts()
    if extra:
        t.update(extra)
    kit.write_json("lang/en_us.json", {k: v[0] for k, v in sorted(t.items())})
    kit.write_json("lang/fr_fr.json", {k: v[1] for k, v in sorted(t.items())})
