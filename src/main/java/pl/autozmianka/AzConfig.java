package pl.autozmianka;

/** Ustawienia (zerowane po restarcie gry). */
public final class AzConfig {
    private AzConfig() {
    }

    /** Docelowy procent. */
    public static int target = 30;
    /** Ile zmianek maksymalnie zuzyc. */
    public static int maxRerolls = 50;
    /** true = trafic dokladnie cel, false = cel lub wiecej. */
    public static boolean exact = true;
    /** Numer slotu przycisku "losuj" w menu (0 = lewy gorny, liczac rzedami). */
    public static int buttonSlot = 13;
    /** Numer slotu, w ktorym widac wynik (miecz z procentem w opisie). */
    public static int resultSlot = 11;
    /** Odstep miedzy kliknieciami w tickach (20 ticków = 1 sekunda). */
    public static int delayTicks = 10;
}
