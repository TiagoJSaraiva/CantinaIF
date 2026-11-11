package dao;

import utils.Database;

/** Banco H2 em memoria isolado por classe de teste. */
final class TestDb {

    private TestDb() {
    }

    static void start(Class<?> teste) {
        Database.shutdown();
        Database.init("jdbc:h2:mem:t_" + teste.getSimpleName()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", null, null);
    }

    static void stop() {
        Database.shutdown();
    }
}
